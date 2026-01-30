package org.sts.demo.signer.signing.util;

public final class Redactor {

    private Redactor() {}

    public static String redactBase64(String b64) {
        if (b64 == null || b64.isBlank()) return b64;
        String s = b64.trim();
        return s.length() <= 24 ? "***redacted***" : s.substring(0, 12) + "…" + s.substring(s.length() - 12);
    }

    public static String pad(String b64) {
        int mod = b64.length() % 4;
        return mod == 0 ? b64 : b64 + "====".substring(mod);
    }

    public static String redactJwt(String jwt) {
        if (jwt == null || jwt.isBlank()) return jwt;
        String s = jwt.trim();
        int keepStart = Math.min(16, s.length());
        int keepEnd = Math.min(12, s.length() - keepStart);
        if (s.length() <= keepStart + keepEnd) return "***redacted***";
        return s.substring(0, keepStart) + "…***redacted***…" + s.substring(s.length() - keepEnd);
    }
}
