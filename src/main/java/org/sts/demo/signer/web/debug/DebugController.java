package org.sts.demo.signer.web.debug;

import org.openapi.model.ParResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sts.demo.signer.oidc.discovery.OidcDiscoveryService;
import org.sts.demo.signer.oidc.model.OidcDiscoveryLoose;
import org.sts.demo.signer.signing.SigningOrchestrationService;

import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/debug")
public class DebugController {

    private final OidcDiscoveryService discovery;
    private final SigningOrchestrationService signingOrchestrationService;

    public DebugController(OidcDiscoveryService discovery,
                           SigningOrchestrationService signingOrchestrationService) {
        this.discovery = discovery;
        this.signingOrchestrationService = signingOrchestrationService;
    }

    @GetMapping("/oidc")
    public ResponseEntity<?> oidc() {
        OidcDiscoveryLoose cfg = discovery.getConfig();

        return ResponseEntity.ok(Map.of(
                "issuer", cfg.issuer(),
                "authorization_endpoint", cfg.authorizationEndpoint(),
                "token_endpoint", cfg.tokenEndpoint(),
                "pushed_authorization_request_endpoint", cfg.pushedAuthorizationRequestEndpoint(),
                "jwks_uri", cfg.jwksUri()
        ));
    }

    @PostMapping("/par")
    public ResponseEntity<?> par() throws Exception {
        ParResponse resp = signingOrchestrationService.pushPar().block(); // OK for dev+MVC
        String requestUri = resp != null ? resp.getRequestUri() : null;
        String requestUriShort = requestUri == null
                ? null
                : (requestUri.length() > 40 ? requestUri.substring(0, 40) + "…" : requestUri);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "request_uri", requestUriShort,
                "expires_in", resp != null ? resp.getExpiresIn() : null
        ));
    }
}
