package org.sts.demo.signer.signing.par;

import org.openapi.mab.model.CreateParRequest;

public record ParStartContext (
        String state,
        String nonce,
        String clientSessionId,
        CreateParRequest request
) {}