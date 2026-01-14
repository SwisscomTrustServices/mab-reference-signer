package org.sts.demo.signer.signing.mab.token;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openapi.mab.invoker.ApiClient;
import org.openapi.mab.model.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;
import org.sts.demo.signer.signing.util.JsonNullPruner;
import reactor.core.publisher.Mono;

@Component
public class TokenClient {

    private static final Logger log = LoggerFactory.getLogger(TokenClient.class);

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

    public Mono<TokenResponse> exchange(Object req) {
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
                                String truncated = body.length() > 500 ? body.substring(0, 500) + "…" : body;
                                log.warn("Token failed status={} body={}", status, truncated);
                                return Mono.error(new IllegalStateException("Token failed: " + status));
                            });
                });
    }
}
