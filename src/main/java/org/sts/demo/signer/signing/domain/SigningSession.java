package org.sts.demo.signer.signing.domain;

public record SigningSession (
    String state,
    String nonce,
    String digestB64,
    HashAlgorithm hashAlgorithm,
    CredentialId credentialId,
    String sadJwt,
    DocumentSigningContext document
) {
    public SigningSession withSadJwt(String sadJwt) {
        if (sadJwt == null || sadJwt.isBlank()) throw new IllegalArgumentException("sadJwt is required");
        return new SigningSession(state, nonce, digestB64, hashAlgorithm, credentialId, sadJwt.trim(), document);
    }

    public void requireSadJwt() {
        if (sadJwt == null || sadJwt.isBlank()) {
            throw new IllegalStateException("No SAD JWT in session - run token exchange first");
        }
    }

    public SigningSession {
        if (state == null || state.isBlank()) throw new IllegalArgumentException("state is required");
        if (nonce == null || nonce.isBlank()) throw new IllegalArgumentException("nonce is required");
    }
}
