package org.sts.demo.signer.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CibaTokenPollRequest(
        @NotBlank String authReqId,
        @NotBlank String state,
        @NotBlank String nonce
) {}

