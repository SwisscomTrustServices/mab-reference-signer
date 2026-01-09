package org.sts.demo.signer.OidcDiscovery;

import org.springframework.stereotype.Component;

@Component
public class OidcEndpoints {
    private final OidcDiscoveryCache cache;

    public OidcEndpoints(OidcDiscoveryCache cache) { this.cache = cache; }

    public String parUri() {
        String url = cache.get().pushed_authorization_request_endpoint();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("OIDC discovery missing pushed_authorization_request_endpoint");
        }
        return url;
    }
    public String tokenUrl() {
        return cache.get().token_endpoint();
    }
}
