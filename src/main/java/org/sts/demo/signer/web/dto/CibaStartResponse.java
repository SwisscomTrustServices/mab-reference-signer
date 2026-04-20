package org.sts.demo.signer.web.dto;

import java.math.BigDecimal;

public record CibaStartResponse(
        String state,
        String nonce,
        String authReqId,
        BigDecimal expiresIn,
        BigDecimal interval,
        String identProcessData
) {}

