package org.sts.demo.signer.signing.mab.par;

import org.openapi.mab.model.CreateParRequest;
import org.openapi.mab.model.CreateParRequestClaims;
import org.openapi.mab.model.CreateParRequestLoginHint;
import org.springframework.stereotype.Component;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.oidc.util.OidcRandoms;

import java.net.URI;
import java.util.UUID;

@Component
public class ParRequestFactory {
    private final QtspProperties props;

    public ParRequestFactory(QtspProperties props) {
        this.props = props;
    }

    public ParStartContext buildDemoRequest(CreateParRequestClaims claims) {
        String state = OidcRandoms.state();
        String nonce = OidcRandoms.nonce();
        String clientSessionId = UUID.randomUUID().toString();

        CreateParRequest req = new CreateParRequest()
                .clientId(props.getClient().getClientId())
                .clientSecret(props.getClient().getClientSecret())
                .redirectUri(URI.create(props.getClient().getRedirectUri().toString()))
                .state(state)
                .clientSessionId(clientSessionId)
                .scope(CreateParRequest.ScopeEnum.SIGN)
                .claims(claims)
                .identMethods(null)
                .loginHint(new CreateParRequestLoginHint().namespace(CreateParRequestLoginHint.NamespaceEnum.PWDOTP));

        return new ParStartContext(state, nonce, clientSessionId, req);
    }
}