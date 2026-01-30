package org.sts.demo.signer.signing.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DigestUtilsTest {

    @Test
    void sha256Base64_knownValue() {
        byte[] input = "hello world".getBytes(StandardCharsets.UTF_8);
        String digest = DigestUtils.sha256Base64(input);
        assertEquals(
                "uU0nuZNNPgilLlLX2n2r+sSE7+N6U4DukIj3rOLvzek=",
                digest
        );
    }

    @Test
    void sha256Base64_emptyArray() {
        byte[] input = new byte[0];
        String digest = DigestUtils.sha256Base64(input);
        assertEquals(
                "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=",
                digest
        );
    }

    @Test
    void sha256Base64_nullInput_throwsIae() {
        assertThrows(IllegalArgumentException.class, () -> DigestUtils.sha256Base64(null));
    }
}