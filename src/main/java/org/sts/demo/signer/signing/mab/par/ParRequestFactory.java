package org.sts.demo.signer.signing.mab.par;

import org.openapi.mab.model.CreateParRequest;
import org.openapi.mab.model.CreateParRequestClaims;
import org.openapi.mab.model.CreateParRequestClaimsDocumentDigestsInner;
import org.openapi.mab.model.CreateParRequestLoginHint;
import org.springframework.stereotype.Component;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.oidc.util.OidcRandoms;
import org.sts.demo.signer.signing.domain.SigningJourney;

import java.util.List;
import java.util.UUID;

import static org.sts.demo.signer.signing.mab.par.ParPolicy.policyFor;

@Component
public class ParRequestFactory {
    private final QtspProperties props;

    public ParRequestFactory(QtspProperties props) {
        this.props = props;
    }

    public ParStartContext build(SigningJourney journey, String digestB64) {
        ParPolicy policy = policyFor(journey);

        CreateParRequestClaims claims = new CreateParRequestClaims();
        claims.setCredentialID(policy.credentialId());
        claims.setHashAlgorithmOID(policy.hashAlgorithm().toMab());
        claims.setDocumentDigests(List.of(
                new CreateParRequestClaimsDocumentDigestsInner()
                        .hash(digestB64)
                        .label("Document-1")
        ));

        String state = OidcRandoms.state();
        String nonce = OidcRandoms.nonce();
        String clientSessionId = UUID.randomUUID().toString();

        CreateParRequest base = new CreateParRequest()
                .clientId(props.getClient().getClientId())
                .clientSecret(props.getClient().getClientSecret())
                .redirectUri(props.getClient().getRedirectUri())
                .state(state)
                .clientSessionId(clientSessionId)
                .scope(null)
                .claims(claims)
                .identMethods(null);

        if (policy.namespace() != null) {
            base.setLoginHint(new CreateParRequestLoginHint().namespace(policy.namespace()));
        }

        ParRequestPayload req = new ParRequestPayload(base, policy.scopes());
        return new ParStartContext(state, nonce, req, policy.hashAlgorithm());
    }
}