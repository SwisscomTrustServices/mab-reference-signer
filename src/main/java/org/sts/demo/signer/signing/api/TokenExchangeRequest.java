package org.sts.demo.signer.signing.api;

import jakarta.validation.constraints.NotBlank;

public record TokenExchangeRequest(
        @NotBlank String code,
        @NotBlank String state,
        @NotBlank String nonce
) {}