package org.sts.demo.signer.oidc.endpoints;

import org.springframework.stereotype.Component;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.oidc.discovery.OidcDiscoveryCache;

import java.net.URI;

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
        String url = cache.get().pushedAuthorizationRequestEndpoint();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("OIDC discovery missing pushed_authorization_request_endpoint");
        }
        return url;
    }

    public String authorizationUri() {
        String url = cache.get().authorizationEndpoint();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("OIDC discovery missing authorization_endpoint");
        }
        return url;
    }

    public String tokenUri() {
        String url = cache.get().tokenEndpoint();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("OIDC discovery missing token_endpoint");
        }
        URI discoveredUri = URI.create(url);

        URI mtlsBase = props.getMtls().getBaseUrl();
        return mtlsBase.toString().replaceAll("/+$","") + discoveredUri.getPath();
    }

    public String cibaAuthUri() {
        URI mtlsBase = props.getMtls().getBaseUrl();
        return mtlsBase.toString().replaceAll("/+$", "") +
                "/api/auth/realms/broker/protocol/openid-connect/oauth-authorize";
    }

    public String cibaTokenUri() {
        URI mtlsBase = props.getMtls().getBaseUrl();
        return mtlsBase.toString().replaceAll("/+$", "") +
                "/api/auth/realms/broker/protocol/openid-connect/oauth-token";
    }

    public String tacUri() {
        String url = cache.get().termsAndConditionsEndpoint();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("OIDC discovery missing terms_and_conditions_endpoint");
        }
        return url;
    }

    public String webfingerUri() {
        String url = cache.get().resolvedWebfingerEndpoint();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("OIDC discovery missing webfinger_endpoint");
        }
        URI discoveredUri = URI.create(url);

        URI mtlsBase = props.getMtls().getBaseUrl();
        return mtlsBase.toString().replaceAll("/+$", "") + discoveredUri.getPath();
    }
}
