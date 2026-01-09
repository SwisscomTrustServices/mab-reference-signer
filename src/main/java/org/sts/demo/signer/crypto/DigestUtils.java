package org.sts.demo.signer.crypto;

import java.security.MessageDigest;
import java.util.Base64;

public final class DigestUtils {

    private DigestUtils() {}

    public static String sha384Base64(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-384");
        byte[] digest = md.digest(data);
        return Base64.getEncoder().encodeToString(digest);
    }
}
