package org.sts.demo.signer.signing.mab.par;

import org.sts.demo.signer.signing.domain.HashAlgorithm;

public record ParStartContext (
        String state,
        String nonce,
        ParRequestPayload request,
        HashAlgorithm hashAlgorithm
) {}