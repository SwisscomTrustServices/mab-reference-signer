package org.sts.demo.signer.signing.mab.token;

import org.openapi.mab.model.AuthorizationCodeTokenRequest;
import org.openapi.mab.model.OauthTokenRequest;
import org.springframework.stereotype.Component;
import org.sts.demo.signer.config.QtspProperties;

import java.util.UUID;

@Component
public class TokenRequestFactory {
    private final QtspProperties props;

    public TokenRequestFactory(QtspProperties props) {
        this.props = props;
    }

    public AuthorizationCodeTokenRequest buildTokenExchangeRequest(String code) {
        return new AuthorizationCodeTokenRequest()
                .clientId(props.getClient().getClientId())
                .clientSecret(props.getClient().getClientSecret())
                .grantType(AuthorizationCodeTokenRequest.GrantTypeEnum.AUTHORIZATION_CODE)
                .code(code);
    }

    public OauthTokenRequest buildTokenPollingRequest(UUID authReqId) {
        return new OauthTokenRequest()
                .clientId(props.getClient().getClientId())
                .clientSecret(props.getClient().getClientSecret())
                .grantType(OauthTokenRequest.GrantTypeEnum.URN_OPENID_PARAMS_GRANT_TYPE_CIBA)
                .authReqId(authReqId);
    }
}
