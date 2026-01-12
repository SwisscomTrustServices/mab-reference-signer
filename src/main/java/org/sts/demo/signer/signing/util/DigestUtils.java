package org.sts.demo.signer.signing.util;

import java.security.MessageDigest;
import java.util.Base64;

public final class DigestUtils {

    private DigestUtils() {}

    public static String sha384Base64(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-384");
            byte[] digest = md.digest(data);
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-384 digest failed", e);
        }
    }
}
