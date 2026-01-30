package org.sts.demo.signer.signing.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedactorTest {

    @Test
    void redactBase64_nullAndBlank_areReturnedAsIs() {
        assertNull(Redactor.redactBase64(null));
        assertEquals("", Redactor.redactBase64(""));
        assertEquals("   ", Redactor.redactBase64("   "));
    }

    @Test
    void redactBase64_shortValue_isFullyRedacted() {
        String shortB64 = "abcdEFGHijklMNOP";
        assertEquals("***redacted***", Redactor.redactBase64(shortB64));
    }

    @Test
    void redactBase64_longValue_keepsStartAndEnd() {
        String b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/=";

        String redacted = Redactor.redactBase64(b64);

        assertTrue(redacted.startsWith("ABCDEFGHIJKL"));
        assertTrue(redacted.endsWith("456789+/="));
        assertTrue(redacted.contains("…"));
    }

    @Test
    void redactBase64_trimsInput() {
        String b64 = "   ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/=   ";
        String redacted = Redactor.redactBase64(b64);

        assertFalse(redacted.startsWith(" "));
        assertFalse(redacted.endsWith(" "));
    }

    @Test
    void pad_returnsSameString_whenAlreadyPadded() {
        String b64 = "abcd"; // length % 4 == 0
        assertEquals(b64, Redactor.pad(b64));
    }

    @Test
    void pad_addsCorrectPadding_whenMissing() {
        assertEquals("abc=", Redactor.pad("abc"));
        assertEquals("ab==", Redactor.pad("ab"));
        assertEquals("a===", Redactor.pad("a"));
    }

    @Test
    void redactJwt_nullAndBlank_areReturnedAsIs() {
        assertNull(Redactor.redactJwt(null));
        assertEquals("", Redactor.redactJwt(""));
        assertEquals("   ", Redactor.redactJwt("   "));
    }

    @Test
    void redactJwt_shortJwt_isFullyRedacted() {
        String shortJwt = "abc.def";
        assertEquals("***redacted***", Redactor.redactJwt(shortJwt));
    }

    @Test
    void redactJwt_longJwt_keepsHeaderAndTail() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
                + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4ifQ."
                + "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        String redacted = Redactor.redactJwt(jwt);

        assertTrue(redacted.startsWith(jwt.substring(0, 16)));
        assertTrue(redacted.endsWith(jwt.substring(jwt.length() - 12)));
        assertTrue(redacted.contains("***redacted***"));
    }

    @Test
    void redactJwt_trimsInput() {
        String jwt = "   abcdefghijklmnopqrstuvwxyz0123456789   ";
        String redacted = Redactor.redactJwt(jwt);

        assertFalse(redacted.startsWith(" "));
        assertFalse(redacted.endsWith(" "));
    }
}