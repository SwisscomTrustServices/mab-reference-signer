package org.sts.demo.signer.signing.etsi;

import java.net.URI;
import java.util.List;

public final class EtsiAudienceSelector {
    private EtsiAudienceSelector() {}

    public static URI pickEtsiBaseUri(String sadJwt) {
        List<String> auds = JwtAudiences.aud(sadJwt);
        if (auds.isEmpty()) {
            throw new IllegalStateException("No aud in SAD JWT");
        }
        if (auds.size() != 1) {
            throw new IllegalStateException("Expected exactly 1 aud, got " + auds.size());
        }
        return URI.create(auds.getFirst());
    }
}
