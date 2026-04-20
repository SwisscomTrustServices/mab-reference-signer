package org.sts.demo.signer.signing.mab.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openapi.mab.invoker.ApiClient;
import org.openapi.mab.model.AuthorizationCodeTokenRequest;
import org.openapi.mab.model.OauthTokenErrorResponse;
import org.openapi.mab.model.OauthTokenRequest;
import org.openapi.mab.model.OauthTokenSignResponse;
import org.openapi.mab.model.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;
import org.sts.demo.signer.signing.util.JsonNullPruner;
import reactor.core.publisher.Mono;

@Component
public class TokenClient {

    private static final Logger log = LoggerFactory.getLogger(TokenClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient mtlsClient;
    private final ApiClient apiClient;
    private final OidcEndpoints endpoints;

    public TokenClient(
            @Qualifier("qtspMtlsWebClient") WebClient mtlsClient,
            @Qualifier("qtspMtlsApiClient") ApiClient apiClient,
            OidcEndpoints endpoints) {
        this.mtlsClient = mtlsClient;
        this.apiClient = apiClient;
        this.endpoints = endpoints;
    }

    public Mono<TokenResponse> exchange(AuthorizationCodeTokenRequest req) {
        ObjectNode json = apiClient.getObjectMapper().valueToTree(req);
        JsonNullPruner.pruneNulls(json);

        log.info("Token exchange request payload={}", json.toPrettyString());
        log.info("Token POST {}", endpoints.tokenUri());

        return mtlsClient.post()
                .uri(endpoints.tokenUri())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchangeToMono(resp -> {
                    int status = resp.statusCode().value();
                    if (status >= 200 && status < 300) {
                        return resp.bodyToMono(TokenResponse.class)
                                .doOnNext(r -> log.info("Token success status={}", status));
                    }
                    return resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> {
                                log.warn("Token failed status={} body={}", status, body);
                                return Mono.error(new IllegalStateException("Token failed: " + status));
                            });
                });
    }

    public Mono<OauthTokenSignResponse> pollCibaToken(OauthTokenRequest req) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", req.getClientId().toString());
        form.add("client_secret", req.getClientSecret().toString());
        form.add("grant_type", req.getGrantType().getValue());
        form.add("auth_req_id", req.getAuthReqId().toString());

        final String prettyForm = toPrettyForm(form);

        return Mono.defer(() -> {
            log.info("CIBA Token form payload={}", prettyForm);
            log.info("CIBA Token POST {}", endpoints.cibaTokenUri());

            return mtlsClient.post()
                    .uri(endpoints.cibaTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromFormData(form))
                    .exchangeToMono(resp -> {
                        int status = resp.statusCode().value();
                        return resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> classifyCibaTokenResponse(status, body));
                    });
        });
    }

    private Mono<OauthTokenSignResponse> classifyCibaTokenResponse(int status, String body) {
        if (isAuthorizationPendingBody(body)) {
            return Mono.error(new CibaAuthorizationPendingException("authorization_pending"));
        }

        try {
            OauthTokenSignResponse ok = apiClient.getObjectMapper().readValue(body, OauthTokenSignResponse.class);
            String accessToken = ok.getAccessToken();
            if (!accessToken.isBlank()) {
                log.info("CIBA token success status={}", status);
                return Mono.just(ok);
            }
        } catch (Exception ignored) {
            // Parsed below as error body if possible.
        }

        try {
            OauthTokenErrorResponse err = apiClient.getObjectMapper().readValue(body, OauthTokenErrorResponse.class);
            if (err.getError() == OauthTokenErrorResponse.ErrorEnum.AUTHORIZATION_PENDING) {
                return Mono.error(new CibaAuthorizationPendingException("authorization_pending"));
            }
            String code = err.getError().getValue();
            String desc = err.getErrorDescription() == null ? "" : err.getErrorDescription();
            return Mono.error(new IllegalStateException("CIBA token failed: " + status + " error=" + code + " desc=" + desc));
        } catch (Exception parseError) {
            return Mono.error(new IllegalStateException("CIBA token failed: " + status + " body=" + body));
        }
    }

    private static boolean isAuthorizationPendingBody(String body) {
        if (body == null) return false;
        String normalized = body.toLowerCase();
        return normalized.contains("authorization_pending");
    }

    private static String toPrettyForm(MultiValueMap<String, String> form) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(form);
        } catch (Exception e) {
            return form.toString();
        }
    }

    public static class CibaAuthorizationPendingException extends RuntimeException {
        public CibaAuthorizationPendingException(String message) {
            super(message);
        }
    }
}
