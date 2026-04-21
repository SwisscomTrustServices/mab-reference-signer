package org.sts.demo.signer.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ParTokenExchangeRequest(
        @NotBlank String code,
        @NotBlank String state,
        @NotBlank String nonce
) {}