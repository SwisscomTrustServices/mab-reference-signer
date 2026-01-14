package org.sts.demo.signer.oidc.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class OidcRandoms {
    private static final SecureRandom RNG = new SecureRandom();

    private OidcRandoms() {}

    public static String state() { return randomBase64Url(); }
    public static String nonce() { return randomBase64Url(); }

    private static String randomBase64Url() {
        byte[] b = new byte[16];
        RNG.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}