package org.sts.demo.signer.oidc.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class OidcRandoms {
    private static final SecureRandom RNG = new SecureRandom();

    private OidcRandoms() {}

    public static String state() { return randomBase64Url(16); }
    public static String nonce() { return randomBase64Url(16); }
    public static String pkceVerifier() { return randomBase64Url(32); }

    private static String randomBase64Url(int bytes) {
        byte[] b = new byte[bytes];
        RNG.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}