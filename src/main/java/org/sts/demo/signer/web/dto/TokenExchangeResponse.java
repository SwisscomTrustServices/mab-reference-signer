package org.sts.demo.signer.web.dto;

public record TokenExchangeResponse(
        String accessToken,
        String tokenType,
        String expiresIn,
        String scope
) {}
