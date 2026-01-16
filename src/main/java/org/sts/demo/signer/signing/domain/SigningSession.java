package org.sts.demo.signer.signing.domain;

public record SigningSession (
    String state,
    String nonce,
    String digestB64,
    HashAlgorithm hashAlg,
    String sadJwt
) {
    public SigningSession withSadJwt(String sadJwt) {
        if (sadJwt == null || sadJwt.isBlank()) throw new IllegalArgumentException("sadJwt is required");
        return new SigningSession(state, nonce, digestB64, hashAlg, sadJwt.trim());
    }

    public void requireSadJwt() {
        if (sadJwt == null || sadJwt.isBlank()) {
            throw new IllegalStateException("No SAD JWT in session - run token exchange first");
        }
    }
}
