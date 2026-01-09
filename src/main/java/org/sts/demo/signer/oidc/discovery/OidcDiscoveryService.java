package org.sts.demo.signer.oidc.discovery;

import org.springframework.stereotype.Service;
import org.sts.demo.signer.oidc.model.OidcDiscoveryLoose;

@Service
public class OidcDiscoveryService {
    private final OidcDiscoveryCache cache;

    public OidcDiscoveryService(OidcDiscoveryCache cache) {
        this.cache = cache;
    }

    public OidcDiscoveryLoose getConfig() {
        return cache.get();
    }
}
