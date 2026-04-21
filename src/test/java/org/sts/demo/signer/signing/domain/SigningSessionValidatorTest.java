package org.sts.demo.signer.signing.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SigningSessionValidatorTest {

    @Test
    void validateAndTake_shouldRemoveAndReturnSession() {
        SigningSessionStore store = new SigningSessionStore();
        SigningSessionValidator v = new SigningSessionValidator(store);

        var s = new SigningSession("stateA", "nonceA", "digest", HashAlgorithm.SHA256, CredentialId.ADVANCED4, "sad", new NoopDoc());
        store.put(s);

        SigningSession taken = v.validateAndTake("stateA", "nonceA");
        assertEquals("stateA", taken.state());

        assertNull(store.remove("stateA"), "state should be consumed (single-use)");
    }

    @Test
    void validateAndTake_wrongNonce_shouldNotConsumeState() {
        SigningSessionStore store = new SigningSessionStore();
        SigningSessionValidator v = new SigningSessionValidator(store);

        var s = new SigningSession("stateB", "nonceB", "digest", HashAlgorithm.SHA256, CredentialId.ADVANCED4, "sad", new NoopDoc());
        store.put(s);

        assertThrows(IllegalArgumentException.class, () -> v.validateAndTake("stateB", "WRONG"));

        assertNotNull(store.get("stateB"), "state should not be consumed on wrong nonce");
    }

    @Test
    void validate_shouldThrowForMissingState() {
        SigningSessionStore store = new SigningSessionStore();
        SigningSessionValidator v = new SigningSessionValidator(store);

        assertThrows(IllegalArgumentException.class, () -> v.validate("unknown", "nonce"));
    }

    @Test
    void validate_shouldNotConsumeState() {
        SigningSessionStore store = new SigningSessionStore();
        SigningSessionValidator v = new SigningSessionValidator(store);

        var s = new SigningSession("stateC", "nonceC", "digest", HashAlgorithm.SHA256, CredentialId.ADVANCED4, null, new NoopDoc());
        store.put(s);

        SigningSession lookedUp = v.validate("stateC", "nonceC");
        assertEquals("stateC", lookedUp.state());

        assertNotNull(store.remove("stateC"), "state should remain available for later steps");
    }

    static final class NoopDoc implements DocumentSigningContext {
        @Override public byte[] getContentToSign() { return new byte[0]; }
        @Override public byte[] embedCms(byte[] cmsSignature) { return new byte[0]; }
        @Override public void close() {}
    }
}