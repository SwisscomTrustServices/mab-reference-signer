package org.sts.demo.signer.signing.api;

public record TokenExchangeRequest(
        String code,
        String state,
        String nonce
) {}