package org.sts.demo.signer.oidc.discovery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OidcDiscoveryLoose(
        String issuer,
        @JsonProperty("authorization_endpoint") String authorizationEndpoint,
        @JsonProperty("token_endpoint") String tokenEndpoint,
        @JsonProperty("pushed_authorization_request_endpoint") String pushedAuthorizationRequestEndpoint,
        @JsonProperty("jwks_uri") String jwksUri,
        @JsonProperty("backchannel_token_delivery_modes_supported") List<String> backchannelTokenDeliveryModesSupported
) {
    public String toLogString() {
        return "issuer=" + issuer +
                ", authz=" + authorizationEndpoint +
                ", token=" + tokenEndpoint +
                ", par=" + pushedAuthorizationRequestEndpoint;
    }
}
