package org.sts.demo.signer.signing.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtAudiencesTest {

    private static String jwtWithPayload(String json) {
        String header = base64Url("{\"alg\":\"none\"}");
        String payload = base64Url(json);
        return header + "." + payload + ".";
    }

    private static String base64Url(String s) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void aud_singleString() {
        String jwt = jwtWithPayload("""
            { "aud": "https://etsi.sign.endpoint" }
        """);

        List<String> aud = JwtAudiences.aud(jwt);

        assertEquals(List.of("https://etsi.sign.endpoint"), aud);
    }

    @Test
    void aud_arrayOfStrings() {
        String jwt = jwtWithPayload("""
            { "aud": ["aud1", "aud2"] }
        """);

        List<String> aud = JwtAudiences.aud(jwt);

        assertEquals(List.of("aud1", "aud2"), aud);
    }

    @Test
    void aud_arrayWithNonTextualEntries_ignoresThem() {
        String jwt = jwtWithPayload("""
            { "aud": ["aud1", 123, null, true, "aud2"] }
        """);

        List<String> aud = JwtAudiences.aud(jwt);

        assertEquals(List.of("aud1", "aud2"), aud);
    }

    @Test
    void aud_missing_returnsEmptyList() {
        String jwt = jwtWithPayload("""
            { "sub": "user123" }
        """);

        List<String> aud = JwtAudiences.aud(jwt);

        assertTrue(aud.isEmpty());
    }

    @Test
    void aud_null_returnsEmptyList() {
        String jwt = jwtWithPayload("""
            { "aud": null }
        """);

        List<String> aud = JwtAudiences.aud(jwt);

        assertTrue(aud.isEmpty());
    }

    @Test
    void aud_nonTextualNonArray_returnsEmptyList() {
        String jwt = jwtWithPayload("""
            { "aud": 123 }
        """);

        List<String> aud = JwtAudiences.aud(jwt);

        assertTrue(aud.isEmpty());
    }

    @Test
    void malformedJwt_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                JwtAudiences.aud("not-a-jwt")
        );
    }

    @Test
    void invalidBase64Payload_throwsIllegalArgumentException() {
        String jwt = "abc.def@@@.ghi";

        assertThrows(IllegalArgumentException.class, () ->
                JwtAudiences.aud(jwt)
        );
    }

    @Test
    void invalidJsonPayload_throwsIllegalArgumentException() {
        String header = base64Url("{\"alg\":\"none\"}");
        String payload = base64Url("{ not-json ");
        String jwt = header + "." + payload + ".";

        assertThrows(IllegalArgumentException.class, () ->
                JwtAudiences.aud(jwt)
        );
    }
}