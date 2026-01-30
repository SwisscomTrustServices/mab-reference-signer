package org.sts.demo.signer.signing.api;

import java.util.UUID;

public record EtsiSignStartResponse(
        UUID responseId,
        String cmsBase64Redacted,
        int cmsBytes,
        String signedPdf
) {}
