package org.sts.demo.signer.signing;

import org.openapi.mab.model.CreateParRequest;
import org.openapi.mab.model.CreateParRequestClaims;
import org.openapi.mab.model.CreateParRequestClaimsDocumentDigestsInner;
import org.openapi.mab.model.ParResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.sts.demo.signer.crypto.DigestUtils.sha384Base64;

@Service
public class SigningOrchestrationService {

    private final ParRequestFactory parRequestFactory;
    private final ParClient parClient;

    public SigningOrchestrationService(ParRequestFactory parRequestFactory, ParClient parClient) {
        this.parRequestFactory = parRequestFactory;
        this.parClient = parClient;
    }

    public Mono<ParResponse> pushPar() throws Exception {
        CreateParRequestClaims claims = new CreateParRequestClaims();
        claims.setCredentialID(CreateParRequestClaims.CredentialIDEnum.ADVANCED4);
        claims.setHashAlgorithmOID(CreateParRequestClaims.HashAlgorithmOIDEnum._2);
        claims.setDocumentDigests(List.of(
                new CreateParRequestClaimsDocumentDigestsInner()
                        .hash(sha384Base64("BASE64_OR_HEX_HASH_VALUE".getBytes()))
                        .label("Document-1")
        ));

        CreateParRequest req = parRequestFactory.buildDemoRequest(claims);
        return parClient.send(req);
    }
}