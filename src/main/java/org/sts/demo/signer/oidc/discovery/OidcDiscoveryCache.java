package org.sts.demo.signer.oidc.discovery;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.oidc.model.OidcDiscoveryLoose;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@Component
public class OidcDiscoveryCache {

    private static final Logger log = LoggerFactory.getLogger(OidcDiscoveryCache.class);

    private final WebClient publicClient;
    private final URI discoveryUri;

    private volatile OidcDiscoveryLoose cached;

    public OidcDiscoveryCache(
            @Qualifier("qtspPublicWebClient") WebClient publicClient,
            QtspProperties props
    ) {
        this.publicClient = publicClient;
        this.discoveryUri = props.getBaseUrl().resolve(props.getOidc().getDiscoveryPath());
    }

    @PostConstruct
    public void init() {
        log.info("Fetching OIDC discovery at startup: {}", discoveryUri);
        refresh(); // fail fast if broken
        log.info("OIDC discovery loaded: {}", cached.toLogString());
    }

    public OidcDiscoveryLoose get() {
        return Objects.requireNonNull(cached, "OIDC discovery not loaded");
    }

    public synchronized void refresh() {
        try {
            OidcDiscoveryLoose v = publicClient.get()
                    .uri(discoveryUri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(OidcDiscoveryLoose.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            this.cached = Objects.requireNonNull(v, "OIDC discovery returned null");
        } catch (Exception e) {
            // make startup failure explicit and readable
            throw new IllegalStateException("Failed to load OIDC discovery from " + discoveryUri, e);
        }
    }
}