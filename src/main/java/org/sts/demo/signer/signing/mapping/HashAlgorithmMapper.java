package org.sts.demo.signer.signing.mapping;

import org.openapi.etsi.model.EtsiSignRequestDocumentDigests;
import org.openapi.mab.model.CreateParRequestClaims;

public final class HashAlgorithmMapper {
    private HashAlgorithmMapper() {}

    public static EtsiSignRequestDocumentDigests.HashAlgorithmOIDEnum toEtsi(HashAlgorithm alg) {
        return switch (alg) {
            case SHA256 -> EtsiSignRequestDocumentDigests.HashAlgorithmOIDEnum._1;
            case SHA384 -> EtsiSignRequestDocumentDigests.HashAlgorithmOIDEnum._2;
            case SHA512 -> EtsiSignRequestDocumentDigests.HashAlgorithmOIDEnum._3;
        };
    }

    public static CreateParRequestClaims.HashAlgorithmOIDEnum toMab(HashAlgorithm alg) {
        return switch (alg) {
            case SHA256 -> CreateParRequestClaims.HashAlgorithmOIDEnum._1;
            case SHA384 -> CreateParRequestClaims.HashAlgorithmOIDEnum._2;
            case SHA512 -> CreateParRequestClaims.HashAlgorithmOIDEnum._3;
        };
    }
}
