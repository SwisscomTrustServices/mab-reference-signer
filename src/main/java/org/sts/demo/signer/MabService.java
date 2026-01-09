package org.sts.demo.signer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openapi.invoker.ApiClient;
import org.openapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.sts.demo.signer.OidcDiscovery.OidcEndpoints;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class MabService {

    private static final Logger log =
            LoggerFactory.getLogger(MabService.class);

    private final WebClient qtspMtlsWebClient;
    private final ApiClient qtspMtlsApiClient;
    private final OidcEndpoints oidcEndpoints;
    private final QtspProperties props;

    public MabService(
            @Qualifier("qtspMtlsWebClient") WebClient qtspMtlsWebClient,
            @Qualifier("qtspMtlsApiClient") ApiClient qtspMtlsApiClient,
            OidcEndpoints oidcEndpoints,
            QtspProperties props
    ) {
        this.qtspMtlsWebClient = qtspMtlsWebClient;
        this.qtspMtlsApiClient = qtspMtlsApiClient;
        this.oidcEndpoints = oidcEndpoints;
        this.props = props;
    }

    public Mono<ParResponse> pushPar() throws Exception {

        CreateParRequestClaims claims = new CreateParRequestClaims();
        claims.setCredentialID(CreateParRequestClaims.CredentialIDEnum.ADVANCED4);

        List<CreateParRequestClaimsDocumentDigestsInner> digests =
                List.of(
                        new CreateParRequestClaimsDocumentDigestsInner()
                                .hash(sha384Base64("BASE64_OR_HEX_HASH_VALUE".getBytes()))
                                .label("Document-1")
                );
        claims.setDocumentDigests(digests);
        claims.setHashAlgorithmOID(CreateParRequestClaims.HashAlgorithmOIDEnum._2);

        CreateParRequest req = new CreateParRequest()
                .clientId(props.getClient().getClientId())
                .clientSecret(props.getClient().getClientSecret())
                .redirectUri(URI.create(props.getClient().getRedirectUri()))
                .state(String.valueOf(UUID.randomUUID()))
                .clientSessionId(String.valueOf(UUID.randomUUID()))
                .scope(CreateParRequest.ScopeEnum.SIGN)
                .claims(claims);

        log.debug("Sending par request: {}", req);
        return parPruned(req);
    }

    private Mono<ParResponse> parPruned(CreateParRequest req) {
        // 1) Convert DTO -> JSON
        ObjectNode json = qtspMtlsApiClient.getObjectMapper().valueToTree(req);

        // 2) Prune nulls recursively (removes login_hint:null etc.)
        pruneNulls(json);
        log.debug("POST {} (pruned)", oidcEndpoints.parUri());

        // 3) Send pruned JSON using mTLS WebClient

        return qtspMtlsWebClient.post()
                .uri(oidcEndpoints.parUri())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .retrieve()
                .bodyToMono(ParResponse.class);
    }

    private static String sha384Base64(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-384");
        byte[] digest = md.digest(data);
        return Base64.getEncoder().encodeToString(digest);
    }

    private static void pruneNulls(JsonNode node) {
        if (node == null) return;
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.fieldNames().forEachRemaining(fn -> {
                JsonNode child = obj.get(fn);
                if (child == null || child.isNull()) {
                    obj.remove(fn);
                } else {
                    pruneNulls(child);
                }
            });
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (JsonNode child : arr) pruneNulls(child);
        }
    }
}
