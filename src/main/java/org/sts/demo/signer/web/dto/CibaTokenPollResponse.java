package org.sts.demo.signer.web.dto;

public record CibaTokenPollResponse(
        String status,
        TokenExchangeResponse token,
        Integer nextPollInSec,
        String message
) {}

