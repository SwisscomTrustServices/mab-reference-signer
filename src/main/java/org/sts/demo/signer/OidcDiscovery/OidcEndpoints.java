package org.sts.demo.signer.OidcDiscovery;

import org.springframework.stereotype.Component;

@Component
public class OidcEndpoints {
    private final OidcDiscoveryCache cache;

    public OidcEndpoints(OidcDiscoveryCache cache) { this.cache = cache; }

    public String patUrl() {
        return cache.get().pushed_authorization_request_endpoint();
    }

    public String tokenUrl() {
        return cache.get().token_endpoint();
    }
}
