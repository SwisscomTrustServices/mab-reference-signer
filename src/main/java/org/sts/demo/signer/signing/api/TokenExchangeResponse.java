package org.sts.demo.signer.signing.api;

public record TokenExchangeResponse(
        String accessToken,
        String tokenType,
        String expiresIn,
        String scope
) {}
