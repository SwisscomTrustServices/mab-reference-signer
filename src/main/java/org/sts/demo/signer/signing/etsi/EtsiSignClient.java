package org.sts.demo.signer.signing.etsi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openapi.etsi.invoker.ApiClient;
import org.openapi.etsi.model.EtsiSignRequest;
import org.openapi.etsi.model.EtsiSignResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.sts.demo.signer.signing.util.JsonNullPruner;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.sts.demo.signer.signing.etsi.EtsiAudienceSelector.pickEtsiBaseUri;

@Service
public class EtsiSignClient {
    private static final Logger log = LoggerFactory.getLogger(EtsiSignClient.class);

    private final WebClient mtlsWebClient;
    private final ApiClient apiClient;

    public EtsiSignClient(
            @Qualifier("etsiMtlsWebClient") WebClient mtlsWebClient,
            @Qualifier("etsiMtlsApiClient") ApiClient apiClient
    ) {
        this.mtlsWebClient = mtlsWebClient;
        this.apiClient = apiClient;
    }

    public Mono<EtsiSignResponse> sign(EtsiSignRequest req) {
        ObjectNode json = apiClient.getObjectMapper().valueToTree(req);
        JsonNullPruner.pruneNulls(json);

        URI signUri = pickEtsiBaseUri(req.getSAD());
        log.info("ETSI request payload={}", json.toPrettyString());
        log.info("SIGN POST {}", signUri);

        return mtlsWebClient.post()
                .uri(signUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchangeToMono(resp -> {
                    int status = resp.statusCode().value();
                    if (status >= 200 && status < 300) {
                        return resp.bodyToMono(EtsiSignResponse.class)
                                .doOnNext(r -> log.info("ETSI success status={}", status));
                    }
                    return resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> {
                                log.warn("ETSI failed status={} body={}", status, body);
                                return Mono.error(new IllegalStateException("ETSI failed: " + status));
                            });
                });
    }
}
