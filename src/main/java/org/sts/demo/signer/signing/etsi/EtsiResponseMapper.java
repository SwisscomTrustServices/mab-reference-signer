package org.sts.demo.signer.signing.etsi;

import org.openapi.etsi.model.EtsiSignResponse;
import org.sts.demo.signer.signing.api.EtsiSignStartResponse;

import java.util.Base64;

import static org.sts.demo.signer.signing.util.Redactor.redactBase64;

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
                cms.length
        );
    }
}
