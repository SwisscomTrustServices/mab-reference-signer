package org.sts.demo.signer.web.dto;

import jakarta.validation.constraints.NotBlank;

public record EtsiSignStartRequest (
        @NotBlank String state,
        @NotBlank String nonce
) {}
