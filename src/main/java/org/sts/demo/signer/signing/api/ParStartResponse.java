package org.sts.demo.signer.signing.api;

public record ParStartResponse(
        String authorizationUrl,
        String state,
        String nonce,
        String clientSessionId
) {}
