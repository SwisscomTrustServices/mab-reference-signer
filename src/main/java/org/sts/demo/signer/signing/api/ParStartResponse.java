package org.sts.demo.signer.signing.api;

public record ParStartResponse(
        String redirectUrl,
        String state,
        String nonce,
        String clientSessionId
) {}
