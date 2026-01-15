package org.sts.demo.signer.signing.mab.par;

import org.openapi.mab.model.CreateParRequest;
import org.sts.demo.signer.signing.domain.HashAlgorithm;

public record ParStartContext (
        String state,
        String nonce,
        String digestB64,
        CreateParRequest request,
        HashAlgorithm hashAlg
) {}