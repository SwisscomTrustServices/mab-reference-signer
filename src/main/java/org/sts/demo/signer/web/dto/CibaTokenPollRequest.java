package org.sts.demo.signer.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CibaTokenPollRequest(
        String state,
        String nonce,
        @NotBlank String authReqId
) {}

