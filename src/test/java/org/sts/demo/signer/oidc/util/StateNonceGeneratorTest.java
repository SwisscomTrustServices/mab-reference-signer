package org.sts.demo.signer.oidc.util;

import org.junit.jupiter.api.Test;
import org.sts.demo.signer.signing.util.StateNonceGenerator;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class StateNonceGeneratorTest {

    private static final Pattern BASE64URL_NO_PADDING =
            Pattern.compile("^[A-Za-z0-9_-]+$");

    @Test
    void state_isBase64UrlNoPadding_andDecodesTo16Bytes() {
        String s = StateNonceGenerator.state();

        assertNotNull(s);
        assertFalse(s.isEmpty());
        assertFalse(s.contains("="), "must not contain padding '='");
        assertTrue(BASE64URL_NO_PADDING.matcher(s).matches(),
                "must be base64url chars only");

        byte[] decoded = Base64.getUrlDecoder().decode(s);
        assertEquals(16, decoded.length, "must decode to 16 bytes");
    }

    @Test
    void nonce_isBase64UrlNoPadding_andDecodesTo16Bytes() {
        String n = StateNonceGenerator.nonce();

        assertNotNull(n);
        assertFalse(n.isEmpty());
        assertFalse(n.contains("="), "must not contain padding '='");
        assertTrue(BASE64URL_NO_PADDING.matcher(n).matches(),
                "must be base64url chars only");

        byte[] decoded = Base64.getUrlDecoder().decode(n);
        assertEquals(16, decoded.length, "must decode to 16 bytes");
    }

    @Test
    void state_isVeryLikelyUnique_overManyGenerations() {
        // Not a proof, but a strong regression guard.
        int samples = 10_000;

        Set<String> seen = new HashSet<>(samples);
        for (int i = 0; i < samples; i++) {
            String s = StateNonceGenerator.state();
            assertTrue(seen.add(s), "duplicate state at iteration " + i);
        }
    }

    @Test
    void nonce_isVeryLikelyUnique_overManyGenerations() {
        int samples = 10_000;

        Set<String> seen = new HashSet<>(samples);
        for (int i = 0; i < samples; i++) {
            String n = StateNonceGenerator.nonce();
            assertTrue(seen.add(n), "duplicate nonce at iteration " + i);
        }
    }

    @Test
    void stateAndNonce_areDifferentMostOfTheTime() {
        // Extremely low probability of failing unless something breaks.
        String state = StateNonceGenerator.state();
        String nonce = StateNonceGenerator.nonce();
        assertNotEquals(state, nonce, "state and nonce should almost never be equal");
    }

}