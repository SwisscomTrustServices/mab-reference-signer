package org.sts.demo.signer.signing.par;

public record ParStartResponse(
        String redirectUrl,
        String state,
        String nonce,
        String clientSessionId
) {}
