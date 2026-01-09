package org.sts.demo.signer;

import org.openapi.api.OidcApi;
import org.openapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Service
public class MabService {

    private static final Logger log =
            LoggerFactory.getLogger(MabService.class);

    private final OidcApi oidcApi;
    private final UUID clientId;
    private final UUID clientSecret;
    private final URI redirectUri;

    public MabService(
            @Qualifier("qtspMtlsOidcApi") OidcApi oidcApi,
            @Value("${qtsp.client.client-id}") String clientId,
            @Value("${qtsp.client.client-secret}") String clientSecret,
            @Value("${qtsp.redirectUri}") String redirectUri

    ) {
        this.oidcApi = oidcApi;
        this.clientId = UUID.fromString(clientId);
        this.clientSecret = UUID.fromString(clientSecret);
        this.redirectUri = URI.create(redirectUri);
    }

    public Mono<ParResponse> pushPar() {

        CreateParRequestClaims claims = new CreateParRequestClaims();
        claims.setCredentialID(CreateParRequestClaims.CredentialIDEnum.ADVANCED4);

        List<CreateParRequestClaimsDocumentDigestsInner> digests =
                List.of(
                        new CreateParRequestClaimsDocumentDigestsInner()
                                .hash("BASE64_OR_HEX_HASH_VALUE")
                                .label("Document-1")
                );
        claims.setDocumentDigests(digests);
        claims.setHashAlgorithmOID(CreateParRequestClaims.HashAlgorithmOIDEnum._2);

        CreateParRequest req = new CreateParRequest()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri(redirectUri)
                .state(String.valueOf(UUID.randomUUID()))
                .clientSessionId(String.valueOf(UUID.randomUUID()))
                .scope(CreateParRequest.ScopeEnum.SIGN)
                .claims(claims);

        log.info("Sending par request: {}", req);

        return oidcApi.par(req)
                .doOnError(e -> {
                    if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException w) {
                        log.error("PAR HTTP {} body={}", w.getRawStatusCode(), w.getResponseBodyAsString());
                    } else {
                        log.error("PAR failed", e);
                    }
                });
    }
}
