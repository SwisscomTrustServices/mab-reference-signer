package org.sts.demo.signer.oidc.endpoints;

import org.springframework.stereotype.Component;
import org.sts.demo.signer.oidc.discovery.OidcDiscoveryCache;

@Component
public class OidcEndpoints {
    private final OidcDiscoveryCache cache;

    public OidcEndpoints(OidcDiscoveryCache cache) { this.cache = cache; }

    public String parUri() {
        String url = cache.get().pushedAuthorizationRequestEndpoint();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("OIDC discovery missing pushed_authorization_request_endpoint");
        }
        return url;
    }
}
