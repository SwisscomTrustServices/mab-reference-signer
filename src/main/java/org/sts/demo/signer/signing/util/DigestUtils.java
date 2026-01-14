package org.sts.demo.signer.signing.util;

import java.security.MessageDigest;
import java.util.Base64;

public final class DigestUtils {

    private DigestUtils() {}

    public static String sha256Base64(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 digest failed", e);
        }
    }
}
