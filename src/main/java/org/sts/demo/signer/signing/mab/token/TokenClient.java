package org.sts.demo.signer.signing.mab.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openapi.mab.invoker.ApiClient;
import org.openapi.mab.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;
import org.sts.demo.signer.signing.util.JsonNullPruner;
import reactor.core.publisher.Mono;

import static org.sts.demo.signer.signing.util.FormDataConverter.prettyPrint;
import static org.sts.demo.signer.signing.util.FormDataConverter.toFormData;

@Component
public class TokenClient {

    private static final Logger log = LoggerFactory.getLogger(TokenClient.class);

    private final ObjectMapper objectMapper;
    private final WebClient mtlsClient;
    private final ApiClient apiClient;
    private final OidcEndpoints endpoints;

    public TokenClient(
            @Qualifier("qtspMtlsWebClient") WebClient mtlsClient,
            @Qualifier("qtspMtlsApiClient") ApiClient apiClient,
            OidcEndpoints endpoints,
            ObjectMapper objectMapper) {
        this.mtlsClient = mtlsClient;
        this.apiClient = apiClient;
        this.endpoints = endpoints;
        this.objectMapper = objectMapper;
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

    public Mono<OauthTokenSignResponse> poll(OauthTokenRequest req) {
        MultiValueMap<String, String> form = toFormData(apiClient.getObjectMapper(), req);
        final String prettyForm = prettyPrint(objectMapper, form);

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
                                .doOnNext(body -> log.info("CIBA Token response status={} body={}", status, body))
                                .flatMap(body -> classifyCibaTokenResponse(status, body));
                    });
        });
    }

    private Mono<OauthTokenSignResponse> classifyCibaTokenResponse(int status, String body) {
        if (status >= 200 && status < 300) {
            return parseSuccessResponse(status, body);
        }
        return parseErrorResponse(status, body);
    }

    private Mono<OauthTokenSignResponse> parseSuccessResponse(int status, String body) {
        try {
            OauthTokenSignResponse ok = apiClient.getObjectMapper().readValue(body, OauthTokenSignResponse.class);
            log.info("CIBA token success status={}", status);
            return Mono.just(ok);
        } catch (Exception e) {
            return Mono.error(new IllegalStateException("CIBA token response unparseable, status=" + status + " body=" + body, e));
        }
    }

    private <T> Mono<T> parseErrorResponse(int status, String body) {
        try {
            OauthTokenErrorResponse err = apiClient.getObjectMapper().readValue(body, OauthTokenErrorResponse.class);
            if (err.getError() == OauthTokenErrorResponse.ErrorEnum.AUTHORIZATION_PENDING) {
                return Mono.error(new CibaAuthorizationPendingException("authorization_pending"));
            }
            String code = err.getError().getValue();
            String desc = err.getErrorDescription() == null ? "" : err.getErrorDescription();
            return Mono.error(new IllegalStateException("CIBA token failed: " + status + " error=" + code + " desc=" + desc));
        } catch (Exception e) {
            return Mono.error(new IllegalStateException("CIBA token failed: " + status + " body=" + body, e));
        }
    }

    public static class CibaAuthorizationPendingException extends RuntimeException {
        public CibaAuthorizationPendingException(String message) {
            super(message);
        }
    }
}
