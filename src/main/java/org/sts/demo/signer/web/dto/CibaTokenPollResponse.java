package org.sts.demo.signer.web.dto;

public record CibaTokenPollResponse(
        String status,
        String accessToken,
        Integer nextPollInSec,
        String message
) {}

