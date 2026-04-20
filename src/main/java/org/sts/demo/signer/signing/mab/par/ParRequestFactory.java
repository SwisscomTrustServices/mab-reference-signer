package org.sts.demo.signer.signing.mab.par;

import org.openapi.mab.model.*;
import org.springframework.stereotype.Component;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.signing.util.StateNonceGenerator;
import org.sts.demo.signer.signing.domain.SigningJourney;
import org.sts.demo.signer.signing.mab.AuthPolicy;

import java.util.List;
import java.util.UUID;

import static org.sts.demo.signer.signing.mab.AuthPolicy.policyFor;

@Component
public class ParRequestFactory {
    private final QtspProperties props;

    public ParRequestFactory(QtspProperties props) {
        this.props = props;
    }

    public ParStartContext build(SigningJourney journey, String digestB64) {
        AuthPolicy authPolicy = policyFor(journey);

        CreateParRequestClaims claims = new CreateParRequestClaims();
        claims.setCredentialID(authPolicy.credentialId().toMab());
        claims.setHashAlgorithmOID(authPolicy.hashAlgorithm().toMab());
        claims.setDocumentDigests(List.of(
                new CreateParRequestClaimsDocumentDigestsInner()
                        .hash(digestB64)
                        .label("Document-1")
        ));

        String state = StateNonceGenerator.state();
        String nonce = StateNonceGenerator.nonce();
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

        if (authPolicy.identMethodType() != null) {
            base.setIdentMethods(List.of(
                    new CreateParRequestIdentMethodsInner()
                            .type(authPolicy.identMethodType())
            ));
        }

        if (authPolicy.namespace() != null) {
            base.setLoginHint(new CreateParRequestLoginHint().namespace(authPolicy.namespace()));
        }

        List<CreateParRequest.ScopeEnum> scopes = authPolicy.scopes().stream()
                .map(CreateParRequest.ScopeEnum::fromValue)
                .toList();

        ParRequestPayload req = new ParRequestPayload(base, scopes);
        return new ParStartContext(state, nonce, req, authPolicy.hashAlgorithm(), authPolicy.credentialId());
    }
}