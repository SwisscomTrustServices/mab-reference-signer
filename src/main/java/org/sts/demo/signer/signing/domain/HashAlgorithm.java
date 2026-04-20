package org.sts.demo.signer.signing.domain;

import org.openapi.etsi.model.EtsiSignRequestDocumentDigests;
import org.openapi.mab.model.CreateParRequestClaims;

public enum HashAlgorithm {
    SHA256(EtsiSignRequestDocumentDigests.HashAlgorithmOIDEnum._1,
            CreateParRequestClaims.HashAlgorithmOIDEnum._1),
    SHA384(EtsiSignRequestDocumentDigests.HashAlgorithmOIDEnum._2,
            CreateParRequestClaims.HashAlgorithmOIDEnum._2),
    SHA512(EtsiSignRequestDocumentDigests.HashAlgorithmOIDEnum._3,
            CreateParRequestClaims.HashAlgorithmOIDEnum._3);

    private final EtsiSignRequestDocumentDigests.HashAlgorithmOIDEnum etsiOid;
    private final CreateParRequestClaims.HashAlgorithmOIDEnum mabOid;

    HashAlgorithm(EtsiSignRequestDocumentDigests.HashAlgorithmOIDEnum etsiOid,
                  CreateParRequestClaims.HashAlgorithmOIDEnum mabOid) {
        this.etsiOid = etsiOid;
        this.mabOid = mabOid;
    }

    public EtsiSignRequestDocumentDigests.HashAlgorithmOIDEnum toEtsi() {
        return etsiOid;
    }

    public CreateParRequestClaims.HashAlgorithmOIDEnum toMab() {
        return mabOid;
    }
}