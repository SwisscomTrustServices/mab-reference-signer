package org.sts.demo.signer.web.dto;

import java.util.UUID;

public record EtsiSignStartResponse(
        UUID responseId,
        String cmsBase64Redacted,
        int cmsBytes,
        String signedPdf
) {}
