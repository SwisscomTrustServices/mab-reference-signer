package org.sts.demo.signer.signing.token;

import org.openapi.mab.model.AuthorizationCodeTokenRequest;
import org.springframework.stereotype.Component;
import org.sts.demo.signer.config.QtspProperties;

@Component
public class TokenRequestFactory {
    private final QtspProperties props;

    public TokenRequestFactory(QtspProperties props) {
        this.props = props;
    }

    public AuthorizationCodeTokenRequest buildAuthCode(String code) {
        return new AuthorizationCodeTokenRequest()
                .clientId(props.getClient().getClientId())
                .clientSecret(props.getClient().getClientSecret())
                .grantType(AuthorizationCodeTokenRequest.GrantTypeEnum.AUTHORIZATION_CODE)
                .code(code);
    }
}
