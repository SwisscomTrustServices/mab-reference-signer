package org.sts.demo.signer.signing.domain;

public record SigningSession (
    String state,
    String nonce,
    String digestB64,
    HashAlgorithm hashAlgOid
) {}
