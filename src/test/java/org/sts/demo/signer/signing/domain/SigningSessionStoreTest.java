package org.sts.demo.signer.signing.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SigningSessionStoreTest {

    @Test
    void putIfAbsent_shouldStore() {
        SigningSessionStore store = new SigningSessionStore();
        SigningSession s = new SigningSession("state1", "nonce1", "digest", HashAlgorithm.SHA256, null, new NoopDoc());
        store.put(s);
        SigningSession removed = store.remove("state1");
        assertNotNull(removed);
        assertEquals("nonce1", removed.nonce());
    }

    static final class NoopDoc implements DocumentSigningContext {
        @Override public byte[] getContentToSign() { return new byte[0]; }
        @Override public byte[] embedCms(byte[] cmsSignature) { return new byte[0]; }
        @Override public void close() {}
    }
}