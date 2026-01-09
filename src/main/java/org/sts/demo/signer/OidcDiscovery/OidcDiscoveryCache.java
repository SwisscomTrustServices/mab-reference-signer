package org.sts.demo.signer.OidcDiscovery;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class OidcDiscoveryCache {

    private static final Logger log =
            LoggerFactory.getLogger(OidcDiscoveryCache.class);

    private final WebClient publicClient;
    private final String discoveryPath;

    private final AtomicReference<OidcDiscoveryLoose> cached = new AtomicReference<>();

    public OidcDiscoveryCache(
            WebClient qtspPublicWebClient,
            @Value("${qtsp.oidc.discovery-path}") String discoveryPath
    ) {
        this.publicClient = qtspPublicWebClient;
        this.discoveryPath = discoveryPath;
    }

    @PostConstruct
    public void init() {
        log.info("Fetching OIDC discovery at startup …");
        refresh();
        log.info("OIDC discovery loaded: issuer={}", cached.get().issuer());
    }

    public OidcDiscoveryLoose get() {
        OidcDiscoveryLoose v = cached.get();
        return Objects.requireNonNull(v, "OIDC discovery not loaded");
    }

    public synchronized void refresh() {
        OidcDiscoveryLoose v = publicClient.get()
                .uri(discoveryPath)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(OidcDiscoveryLoose.class)
                .block();

        cached.set(Objects.requireNonNull(v, "OIDC discovery returned null"));
    }
}
