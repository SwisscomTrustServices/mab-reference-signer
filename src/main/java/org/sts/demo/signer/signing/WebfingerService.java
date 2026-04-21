package org.sts.demo.signer.signing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;
import org.sts.demo.signer.signing.domain.SigningJourney;
import org.sts.demo.signer.web.dto.CibaWebfingerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.sts.demo.signer.signing.mab.AuthPolicy.policyFor;
import static org.sts.demo.signer.signing.util.ValidationUtils.requireNonBlank;

@Service
public class WebfingerService {

    private final ObjectMapper objectMapper;
    private final WebClient mtlsWebClient;
    private final OidcEndpoints endpoints;
    private final QtspProperties props;

    private static final Logger log = LoggerFactory.getLogger(WebfingerService.class);

    public WebfingerService(@Qualifier("qtspMtlsWebClient") WebClient mtlsWebClient,
                            OidcEndpoints endpoints,
                            QtspProperties props,
                            ObjectMapper objectMapper) {
        this.mtlsWebClient = mtlsWebClient;
        this.endpoints = endpoints;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public Mono<CibaWebfingerResponse> checkIdentifier(String identifier, SigningJourney journey) {
        String normalizedIdentifier = normalizeIdentifier(identifier);
        var policy = policyFor(journey);
        String resource = "urn:mab:acct:" + normalizedIdentifier + ":" + policy.namespace().getValue().toLowerCase();
        String credentialId = policy.credentialId().getValue();

        URI uri = UriComponentsBuilder.fromUriString(endpoints.webfingerUri())
                .queryParam("client_id", props.getClient().getClientId())
                .queryParam("client_secret", props.getClient().getClientSecret())
                .queryParam("resource", resource)
                .queryParam("credentialID", credentialId)
                .build(true)
                .toUri();

        log.info("Webfinger GET {}", uri);

        return mtlsWebClient.get()
                .uri(uri)
                .accept(MediaType.parseMediaType("application/jrd+json"), MediaType.APPLICATION_JSON)
                .exchangeToMono(resp -> {
                    int status = resp.statusCode().value();
                    if (status == 404) {
                        log.info("Webfinger status=404 resource={}", resource);
                        return resp.releaseBody()
                                .thenReturn(new CibaWebfingerResponse(
                                        false, resource, null, null, null, null,
                                        "Identifier not onboarded"));
                    }
                    return resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .doOnNext(body -> log.info("Webfinger response status={} body={}", status, body))
                            .flatMap(body -> classifyWebfingerResponse(status, body));
                });
    }

    private Mono<CibaWebfingerResponse> classifyWebfingerResponse(int status, String body) {
        if (status >= 200 && status < 300) {
            return Mono.just(toResponse(body));
        }
        return Mono.error(new IllegalStateException("Webfinger failed: " + status + " body=" + body));
    }

    private CibaWebfingerResponse toResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode properties = root.path("properties");
            String eligible = text(properties.path("urn:mab:sign:eligible"));
            boolean onboarded = "true".equalsIgnoreCase(eligible);
            String subject = text(root.path("subject"));
            String platform = text(properties.path("urn:mab:sign:platform"));
            String status = text(properties.path("urn:mab:sign:status"));
            String evidenceExpiryDate = text(properties.path("urn:mab:sign:evidenceExpiryDate"));

            String message;
            if (status != null) {
                message = "Serial mismatch: " + status;
            } else if (onboarded) {
                message = "Identifier is onboarded";
            } else {
                message = "Identifier is not onboarded";
            }

            return new CibaWebfingerResponse(onboarded, subject, eligible, platform, status, evidenceExpiryDate, message);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse webfinger response", e);
        }
    }

    private static String normalizeIdentifier(String identifier) {
        String value = requireNonBlank(identifier, "Missing identifier");
        return value.startsWith("+") ? value.substring(1) : value;
    }

    private static String text(JsonNode node) {
        String value = node.asText(null);
        return (value == null || value.isBlank()) ? null : value;
    }
}
