package org.sts.demo.signer.signing.etsi;

import org.openapi.etsi.model.EtsiSignResponse;
import org.sts.demo.signer.signing.api.EtsiSignStartResponse;

import java.util.Base64;

import static org.sts.demo.signer.signing.util.Redactor.redactBase64;

public final class EtsiResponseMapper {
    private EtsiResponseMapper() {}

    public static EtsiSignStartResponse toUiResponse(EtsiSignResponse response, int cmsLen, byte[] signedPdf) {
        if (response == null) throw new IllegalArgumentException("ETSI response is null");
        if (signedPdf == null || signedPdf.length == 0) throw new IllegalArgumentException("signedPdf is empty");

        String cmsB64 = firstSignatureObjectB64(response);
        String signedPdfB64 = Base64.getEncoder().encodeToString(signedPdf);

        return new EtsiSignStartResponse(
                response.getResponseID(),
                redactBase64(cmsB64),
                cmsLen,
                signedPdfB64
        );
    }

    public static byte[] extractCms(EtsiSignResponse response) {
        if (response == null) throw new IllegalArgumentException("ETSI response is null");
        String b64 = firstSignatureObjectB64(response);
        return decodeB64Flexible(b64);
    }

    private static String firstSignatureObjectB64(EtsiSignResponse response) {
        if (response.getSignatureObject().isEmpty()) {
            throw new IllegalStateException("ETSI response missing SignatureObject");
        }
        String cmsB64 = response.getSignatureObject().getFirst();
        if (cmsB64 == null || cmsB64.isBlank()) {
            throw new IllegalStateException("ETSI SignatureObject[0] is empty");
        }
        return cmsB64;
    }

    private static byte[] decodeB64Flexible(String b64) {
        String s = b64.trim();

        boolean urlSafe = (s.indexOf('-') >= 0) || (s.indexOf('_') >= 0);

        int pad = (4 - (s.length() % 4)) % 4;
        if (pad != 0) s = s + "===".substring(0, pad);

        try {
            return (urlSafe ? Base64.getUrlDecoder() : Base64.getDecoder()).decode(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ETSI SignatureObject[0]" + " is not valid base64/base64url", e);
        }
    }
}
