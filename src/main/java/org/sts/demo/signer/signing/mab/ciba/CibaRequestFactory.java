package org.sts.demo.signer.signing.mab.ciba;

import org.openapi.mab.model.OauthAuthenticationRequest;
import org.springframework.stereotype.Component;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.signing.domain.Namespace;
import org.sts.demo.signer.oidc.tac.TacHashResolver;
import org.sts.demo.signer.signing.util.StateNonceGenerator;
import org.sts.demo.signer.signing.domain.SigningJourney;
import org.sts.demo.signer.signing.mab.AuthPolicy;
import org.sts.demo.signer.signing.util.MabJwtFactory;

import static org.sts.demo.signer.signing.mab.AuthPolicy.policyFor;

@Component
public class CibaRequestFactory {
    private final QtspProperties props;
    private final MabJwtFactory jwtFactory;
    private final TacHashResolver tacHashResolver;

    public static final String TAC_ZERTES_EN_PDF = "zertes_en.pdf";
    public static final String TAC_EIDAS_EN_PDF = "eidas_en.pdf";
    public static final String DOCUMENT_LABEL = "Document-1";

    public CibaRequestFactory(MabJwtFactory jwtFactory,
                              QtspProperties props,
                              TacHashResolver tacHashResolver) {
        this.props = props;
        this.jwtFactory = jwtFactory;
        this.tacHashResolver = tacHashResolver;
    }

    private OauthAuthenticationRequest buildBase(String identifier, Namespace namespace) {
        String loginHintToken = jwtFactory.createLoginHintToken(
                identifier,
                namespace.getValue(),
                props.getCiba().getJwtSharedSecret()
        );
        return new OauthAuthenticationRequest()
                .clientId(props.getClient().getClientId())
                .clientSecret(props.getClient().getClientSecret())
                .loginHintToken(loginHintToken);
    }

    public CibaStartContext buildIdent(SigningJourney journey, String identifier) {
        AuthPolicy authPolicy = policyFor(journey);

        OauthAuthenticationRequest authReq = buildBase(identifier, authPolicy.namespace());
        authReq.identMethodType(OauthAuthenticationRequest.IdentMethodTypeEnum.fromValue(
                authPolicy.identMethodType().getValue()
        ));
        authReq.tacLanguage(OauthAuthenticationRequest.TacLanguageEnum.EN);
        authReq.tacHashZertes(tacHashResolver.resolveSha256(TAC_ZERTES_EN_PDF));
        authReq.tacHashEidas(tacHashResolver.resolveSha256(TAC_EIDAS_EN_PDF));

        return buildCtx(authReq, authPolicy);
    }

    public CibaStartContext buildSign(SigningJourney journey, String digestB64, String identifier) {
        AuthPolicy authPolicy = policyFor(journey);

        OauthAuthenticationRequest authReq = buildBase(identifier, authPolicy.namespace());
        String claimsToken = jwtFactory.createClaimsTokenForSign(
                authPolicy.credentialId().getValue(),
                digestB64,
                DOCUMENT_LABEL,
                authPolicy.hashAlgorithm().getOid(),
                props.getCiba().getJwtSharedSecret()
        );
        authReq.claimsToken(claimsToken);

        return buildCtx(authReq, authPolicy);
    }

    private CibaStartContext buildCtx(OauthAuthenticationRequest authReq, AuthPolicy authPolicy) {
        CibaRequestPayload payload = new CibaRequestPayload(
                authReq,
                authPolicy.scopes().stream().map(OauthAuthenticationRequest.ScopeEnum::fromValue).toList()
        );
        String state = StateNonceGenerator.state();
        String nonce = StateNonceGenerator.nonce();

        return new CibaStartContext(state, nonce, payload, authPolicy.hashAlgorithm(), authPolicy.credentialId());
    }
}