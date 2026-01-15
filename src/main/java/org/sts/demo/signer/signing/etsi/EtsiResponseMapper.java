package org.sts.demo.signer.signing.etsi;

import org.openapi.etsi.model.EtsiSignResponse;
import org.sts.demo.signer.signing.api.EtsiSignStartResponse;

import java.security.MessageDigest;
import java.util.Base64;

public final class EtsiResponseMapper {
    private EtsiResponseMapper() {}

    public static EtsiSignStartResponse toEmbedResponse(EtsiSignResponse r) {
        if (r == null) throw new IllegalArgumentException("ETSI response is null");
        if (r.getSignatureObject().isEmpty()) {
            throw new IllegalStateException("ETSI response missing SignatureObject");
        }

        String cmsB64 = r.getSignatureObject().getFirst();
        if (cmsB64 == null || cmsB64.isBlank()) {
            throw new IllegalStateException("ETSI SignatureObject[0] is empty");
        }

        byte[] cms;
        try {
            cms = Base64.getDecoder().decode(cmsB64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ETSI SignatureObject[0] is not valid base64", e);
        }

        return new EtsiSignStartResponse(
                r.getResponseID(),
                redactBase64(cmsB64),
                cms.length,
                sha256Base64(cms)
        );
    }

    private static String redactBase64(String b64) {
        // keep a little prefix/suffix for debugging; never return full CMS to the browser
        int keep = 12;
        String s = b64.trim();
        if (s.length() <= keep * 2) return "***redacted***";
        return s.substring(0, keep) + "…***redacted***…" + s.substring(s.length() - keep);
    }

    private static String sha256Base64(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(data));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 failed", e);
        }
    }
}
