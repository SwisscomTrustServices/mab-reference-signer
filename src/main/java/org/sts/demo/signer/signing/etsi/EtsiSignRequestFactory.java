package org.sts.demo.signer.signing.etsi;

import org.openapi.etsi.model.EtsiSignRequest;
import org.openapi.etsi.model.EtsiSignRequestDocumentDigests;
import org.springframework.stereotype.Component;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.signing.domain.SigningSession;

import java.util.List;

@Component
public class EtsiSignRequestFactory {

    QtspProperties qtspProperties;

    public EtsiSignRequestFactory(QtspProperties qtspProperties) {
        this.qtspProperties = qtspProperties;
    }

    public EtsiSignRequest build(SigningSession session) {
        EtsiSignRequestDocumentDigests digests = new EtsiSignRequestDocumentDigests()
                .hashAlgorithmOID(session.hashAlgOid().toEtsi())
                .hashes(List.of(session.digestB64()));

        return new EtsiSignRequest()
                .documentDigests(digests)
                .credentialID(EtsiSignRequest.CredentialIDEnum.ADVANCED4)
                .signatureFormat(EtsiSignRequest.SignatureFormatEnum.P);
    }
}
