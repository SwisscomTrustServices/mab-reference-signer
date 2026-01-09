package org.sts.demo.signer.OidcDiscovery;

import org.springframework.stereotype.Service;

@Service
public class OidcDiscoveryClient {
    private final OidcDiscoveryCache cache;

    public OidcDiscoveryClient(OidcDiscoveryCache cache) {
        this.cache = cache;
    }

    public OidcDiscoveryLoose getConfig() {
        return cache.get();
    }
}
