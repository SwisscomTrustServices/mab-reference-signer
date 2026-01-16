package org.sts.demo.signer.oidc.discovery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OidcDiscoveryLoose(
        String issuer,
        @JsonProperty("authorization_endpoint") String authorizationEndpoint,
        @JsonProperty("token_endpoint") String tokenEndpoint,
        @JsonProperty("pushed_authorization_request_endpoint") String pushedAuthorizationRequestEndpoint
) {
    public String toLogString() {
        return "issuer=" + issuer +
                ", authz=" + authorizationEndpoint +
                ", token=" + tokenEndpoint +
                ", par=" + pushedAuthorizationRequestEndpoint;
    }
}
