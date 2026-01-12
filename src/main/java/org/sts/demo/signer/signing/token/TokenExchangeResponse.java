package org.sts.demo.signer.signing.token;

public record TokenExchangeResponse(
        String accessToken,
        String tokenType,
        String expiresIn,
        String idToken,
        String raw
) {}
