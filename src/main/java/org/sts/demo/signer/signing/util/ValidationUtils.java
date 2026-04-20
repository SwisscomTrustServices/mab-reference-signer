package org.sts.demo.signer.signing.util;

public final class ValidationUtils {
    private ValidationUtils() {}

    public static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}

