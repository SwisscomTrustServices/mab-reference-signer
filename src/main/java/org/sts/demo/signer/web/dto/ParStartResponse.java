package org.sts.demo.signer.web.dto;

public record ParStartResponse(
        String authorizationUrl,
        String state,
        String nonce
) {}
