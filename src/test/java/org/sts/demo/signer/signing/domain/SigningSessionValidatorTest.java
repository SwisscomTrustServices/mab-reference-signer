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
    void validateAndTake_wrongNonce_shouldConsumeStateAnyway() {
        SigningSessionStore store = new SigningSessionStore();
        SigningSessionValidator v = new SigningSessionValidator(store);

        var s = new SigningSession("stateB", "nonceB", "digest", HashAlgorithm.SHA256, CredentialId.ADVANCED4, "sad", new NoopDoc());
        store.put(s);

        assertThrows(IllegalArgumentException.class, () -> v.validateAndTake("stateB", "WRONG"));

        assertNull(store.remove("stateB"), "state should still be consumed to prevent brute-force nonce guessing");
    }

    static final class NoopDoc implements DocumentSigningContext {
        @Override public byte[] getContentToSign() { return new byte[0]; }
        @Override public byte[] embedCms(byte[] cmsSignature) { return new byte[0]; }
        @Override public void close() {}
    }
}