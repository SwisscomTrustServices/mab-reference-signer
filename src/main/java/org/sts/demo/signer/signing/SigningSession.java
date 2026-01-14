package org.sts.demo.signer.signing;

import org.sts.demo.signer.signing.mapping.HashAlgorithm;

public record SigningSession (
    String state,
    String nonce,
    String clientSessionId,
    String pdfName,
    String digestB64,
    HashAlgorithm hashAlgOid
) {}
