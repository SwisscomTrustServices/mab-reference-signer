package org.sts.demo.signer.signing.api;

import org.openapi.etsi.model.EtsiSignResponse;

public record EtsiSignStartResponse(
        EtsiSignResponse etsiResponse
) {}
