package org.sts.demo.signer.signing.token;

public record TokenExchangeRequest(
        String code,
        String state,
        String nonce
) {}