package org.sts.demo.signer.OidcDiscovery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OidcDiscoveryLoose(
        String issuer,
        String authorization_endpoint,
        String token_endpoint,
        String pushed_authorization_request_endpoint,
        String jwks_uri,
        List<String> backchannel_token_delivery_modes_supported
) {}
