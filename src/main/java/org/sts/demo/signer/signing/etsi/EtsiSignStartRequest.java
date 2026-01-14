package org.sts.demo.signer.signing.etsi;

import jakarta.validation.constraints.NotBlank;

public record EtsiSignStartRequest (
        @NotBlank String state,
        @NotBlank String nonce,
        @NotBlank String sadJwt
) {}
