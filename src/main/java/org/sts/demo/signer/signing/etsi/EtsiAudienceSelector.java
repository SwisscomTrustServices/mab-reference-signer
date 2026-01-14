package org.sts.demo.signer.signing.etsi;

import org.sts.demo.signer.signing.util.JwtAudiences;

import java.net.URI;

public final class EtsiAudienceSelector {
    private EtsiAudienceSelector() {}

    public static URI pickEtsiBaseUri(String accessToken) {
        return JwtAudiences.aud(accessToken).stream()
                .filter(a -> a.startsWith("https://"))
                .filter(a -> a.contains("etsi") || a.contains("sign")) // adapt to your reality
                .findFirst()
                .map(a -> URI.create(a.replaceAll("/+$", "")))
                .orElseThrow(() -> new IllegalStateException("No ETSI audience URL in access_token aud"));
    }
}
