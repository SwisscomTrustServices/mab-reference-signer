package org.sts.demo.signer.web.dev;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sts.demo.signer.oidc.discovery.OidcDiscoveryCache;
import org.sts.demo.signer.oidc.discovery.OidcDiscoveryLoose;

import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/debug")
public class DebugController {

    private final OidcDiscoveryCache discovery;

    public DebugController(OidcDiscoveryCache discovery) {
        this.discovery = discovery;
    }

    @GetMapping("/oidc")
    public ResponseEntity<?> oidc() {
        OidcDiscoveryLoose cfg = discovery.get();

        return ResponseEntity.ok(Map.of(
                "issuer", cfg.issuer(),
                "authorization_endpoint", cfg.authorizationEndpoint(),
                "token_endpoint", cfg.tokenEndpoint(),
                "pushed_authorization_request_endpoint", cfg.pushedAuthorizationRequestEndpoint(),
                "jwks_uri", cfg.jwksUri()
        ));
    }
}
