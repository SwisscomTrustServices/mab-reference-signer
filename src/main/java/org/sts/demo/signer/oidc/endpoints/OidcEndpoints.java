package org.sts.demo.signer.oidc.endpoints;

import org.springframework.stereotype.Component;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.oidc.discovery.OidcDiscoveryCache;

import java.net.URI;

import static org.sts.demo.signer.signing.util.ValidationUtils.requireNonBlank;

@Component
public class OidcEndpoints {
    private final OidcDiscoveryCache cache;
    private final QtspProperties props;

    public OidcEndpoints(
            OidcDiscoveryCache cache, QtspProperties props) {
        this.cache = cache;
        this.props = props;
    }

    public String parUri() {
        return requireNonBlank(cache.get().pushedAuthorizationRequestEndpoint(),
                "OIDC discovery missing pushed_authorization_request_endpoint");
    }

    public String authorizationUri() {
        return requireNonBlank(cache.get().authorizationEndpoint(),
                "OIDC discovery missing authorization_endpoint");
    }

    public String tokenUri() {
        String url = requireNonBlank(cache.get().tokenEndpoint(),
                "OIDC discovery missing token_endpoint");
        return mtlsUri(URI.create(url).getPath());
    }

    public String cibaAuthUri() {
        return mtlsUri("/api/auth/realms/broker/protocol/openid-connect/oauth-authorize");
    }

    public String cibaTokenUri() {
        return mtlsUri("/api/auth/realms/broker/protocol/openid-connect/oauth-token");
    }

    public String tacUri() {
        return requireNonBlank(cache.get().termsAndConditionsEndpoint(),
                "OIDC discovery missing terms_and_conditions_endpoint");
    }

    public String webfingerUri() {
        String url = requireNonBlank(cache.get().resolvedWebfingerEndpoint(),
                "OIDC discovery missing webfinger_endpoint");
        return mtlsUri(URI.create(url).getPath());
    }

    private String mtlsUri(String path) {
        return props.getMtls().getBaseUrl().toString().replaceAll("/+$", "") + path;
    }
}

