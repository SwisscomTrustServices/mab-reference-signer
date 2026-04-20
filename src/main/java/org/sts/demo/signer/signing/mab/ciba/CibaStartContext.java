package org.sts.demo.signer.signing.mab.ciba;

import org.sts.demo.signer.signing.domain.CredentialId;
import org.sts.demo.signer.signing.domain.HashAlgorithm;

public record CibaStartContext(
        String state,
        String nonce,
        CibaRequestPayload request,
        HashAlgorithm hashAlgorithm,
        CredentialId credentialId
) {}