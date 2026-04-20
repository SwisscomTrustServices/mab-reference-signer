package org.sts.demo.signer.oidc.discovery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OidcDiscoveryLoose(
        String issuer,
        @JsonProperty("authorization_endpoint") String authorizationEndpoint,
        @JsonProperty("token_endpoint") String tokenEndpoint,
        @JsonProperty("pushed_authorization_request_endpoint") String pushedAuthorizationRequestEndpoint,
        @JsonProperty("terms_and_conditions_endpoint") String termsAndConditionsEndpoint,
        @JsonProperty("webfinger_endpoint") String webfingerEndpoint,
        @JsonProperty("mtls_endpoint_aliases") MtlsEndpointAliases mtlsEndpointAliases
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MtlsEndpointAliases(
            @JsonProperty("webfinger_endpoint") String webfingerEndpoint
    ) {}

    public String resolvedWebfingerEndpoint() {
        if (webfingerEndpoint != null && !webfingerEndpoint.isBlank()) {
            return webfingerEndpoint;
        }
        if (mtlsEndpointAliases != null && mtlsEndpointAliases.webfingerEndpoint() != null
                && !mtlsEndpointAliases.webfingerEndpoint().isBlank()) {
            return mtlsEndpointAliases.webfingerEndpoint();
        }
        return null;
    }

    public String toLogString() {
        return "issuer=" + issuer +
                ", authz=" + authorizationEndpoint +
                ", token=" + tokenEndpoint +
                ", par=" + pushedAuthorizationRequestEndpoint +
                ", terms=" + termsAndConditionsEndpoint +
                ", webfinger=" + resolvedWebfingerEndpoint();
    }
}
