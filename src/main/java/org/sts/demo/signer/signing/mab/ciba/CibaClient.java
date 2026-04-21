package org.sts.demo.signer.signing.mab.ciba;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openapi.mab.invoker.ApiClient;
import org.openapi.mab.model.OauthAuthenticationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;
import reactor.core.publisher.Mono;

import static org.sts.demo.signer.signing.util.FormDataConverter.prettyPrint;
import static org.sts.demo.signer.signing.util.FormDataConverter.toFormData;

@Component
public class CibaClient {
    private static final Logger log = LoggerFactory.getLogger(CibaClient.class);

    private final ObjectMapper objectMapper;
    private final ApiClient apiClient;
    private final WebClient mtlsWebClient;
    private final OidcEndpoints endpoints;

    public CibaClient(
            @Qualifier("qtspMtlsWebClient") WebClient mtlsWebClient,
            @Qualifier("qtspMtlsApiClient") ApiClient apiClient,
            OidcEndpoints endpoints,
            ObjectMapper objectMapper
    ) {
        this.mtlsWebClient = mtlsWebClient;
        this.apiClient = apiClient;
        this.endpoints = endpoints;
        this.objectMapper = objectMapper;
    }

    public Mono<OauthAuthenticationResponse> authenticate(CibaRequestPayload req) {
        MultiValueMap<String, String> form = toFormData(apiClient.getObjectMapper(), req);

        log.info("CIBA Auth form payload={}", prettyPrint(objectMapper, form));
        log.info("CIBA Auth POST {}", endpoints.cibaAuthUri());

        return mtlsWebClient.post()
                .uri(endpoints.cibaAuthUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(form))
                .exchangeToMono(resp -> {
                    int status = resp.statusCode().value();
                    if (status >= 200 && status < 300) {
                        return resp.bodyToMono(OauthAuthenticationResponse.class)
                                .doOnNext(r -> log.info("CIBA Auth success status={} body={}", status, r));
                    }
                    return resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> {
                                log.warn("CIBA Auth failed status={} body={}", status, body);
                                return Mono.error(new IllegalStateException("CIBA Auth failed: " + status + " body=" + body));
                            });
                });
    }
}