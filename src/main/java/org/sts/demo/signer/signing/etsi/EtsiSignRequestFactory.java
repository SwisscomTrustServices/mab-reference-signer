package org.sts.demo.signer.signing.etsi;

import org.openapi.etsi.model.EtsiSignRequest;
import org.openapi.etsi.model.EtsiSignRequestDocumentDigests;
import org.springframework.stereotype.Component;
import org.sts.demo.signer.signing.domain.SigningSession;

import java.util.List;

@Component
public class EtsiSignRequestFactory {

    public EtsiSignRequest build(SigningSession session) {
        EtsiSignRequestDocumentDigests digests = new EtsiSignRequestDocumentDigests()
                .hashAlgorithmOID(session.hashAlgorithm().toEtsi())
                .hashes(List.of(session.digestB64()));

        return new EtsiSignRequest()
                .SAD(session.sadJwt())
                .documentDigests(digests)
                .credentialID(session.credentialId().toEtsi())
                .signatureFormat(EtsiSignRequest.SignatureFormatEnum.P);
    }
}
