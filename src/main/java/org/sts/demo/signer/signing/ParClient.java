package org.sts.demo.signer.signing;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openapi.invoker.ApiClient;
import org.openapi.model.CreateParRequest;
import org.openapi.model.ParResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;
import org.sts.demo.signer.json.JsonNullPruner;
import reactor.core.publisher.Mono;

@Component
public class ParClient {
    private static final Logger log = LoggerFactory.getLogger(ParClient.class);

    private final WebClient mtlsWebClient;
    private final ApiClient apiClient;
    private final OidcEndpoints endpoints;

    public ParClient(
            @Qualifier("qtspMtlsWebClient") WebClient mtlsWebClient,
            @Qualifier("qtspMtlsApiClient") ApiClient apiClient,
            OidcEndpoints endpoints
    ) {
        this.mtlsWebClient = mtlsWebClient;
        this.apiClient = apiClient;
        this.endpoints = endpoints;
    }

    public Mono<ParResponse> send(CreateParRequest req) {
        ObjectNode json = apiClient.getObjectMapper().valueToTree(req);
        JsonNullPruner.pruneNulls(json);

        log.info("PAR POST {} clientSessionId={}", endpoints.parUri(), req.getClientSessionId());

        return mtlsWebClient.post()
                .uri(endpoints.parUri())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchangeToMono(resp -> {
                    int status = resp.statusCode().value();
                    if (status >= 200 && status < 300) {
                        return resp.bodyToMono(ParResponse.class)
                                .doOnNext(r -> log.info("PAR success status={}", status));
                    } else {
                        return resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    String truncated = body.length() > 500 ? body.substring(0, 500) + "…" : body;
                                    log.warn("PAR failed status={} body={}", status, truncated);
                                    return Mono.error(new IllegalStateException("PAR failed: " + status));
                                });
                    }
                });
    }
}
