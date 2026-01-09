package org.sts.demo.signer.signing;

import org.openapi.mab.model.CreateParRequest;
import org.openapi.mab.model.CreateParRequestClaims;
import org.springframework.stereotype.Component;
import org.sts.demo.signer.config.QtspProperties;

import java.net.URI;
import java.util.UUID;

@Component
public class ParRequestFactory {
    private final QtspProperties props;

    public ParRequestFactory(QtspProperties props) {
        this.props = props;
    }

    public CreateParRequest buildDemoRequest(CreateParRequestClaims claims) {
        return new CreateParRequest()
                .clientId(props.getClient().getClientId())
                .clientSecret(props.getClient().getClientSecret())
                .redirectUri(URI.create(props.getClient().getRedirectUri().toString()))
                .state(UUID.randomUUID().toString())
                .clientSessionId(UUID.randomUUID().toString())
                .scope(CreateParRequest.ScopeEnum.SIGN)
                .claims(claims);
    }
}
