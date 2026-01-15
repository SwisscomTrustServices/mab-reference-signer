package org.sts.demo.signer.signing.mab.par;

import org.openapi.mab.model.CreateParRequest;
import org.openapi.mab.model.CreateParRequestClaims;
import org.openapi.mab.model.CreateParRequestClaimsDocumentDigestsInner;
import org.openapi.mab.model.CreateParRequestLoginHint;
import org.springframework.stereotype.Component;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.oidc.util.OidcRandoms;
import org.sts.demo.signer.signing.domain.HashAlgorithm;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.sts.demo.signer.signing.mab.par.ParPolicy.policyFor;

@Component
public class ParRequestFactory {
    private final QtspProperties props;

    private static final HashAlgorithm HASH_ALGORITHM = HashAlgorithm.SHA256;

    public ParRequestFactory(QtspProperties props) {
        this.props = props;
    }

    public ParStartContext buildDemoRequest(String credentialId, String digestB64) {
        ParPolicy policy = policyFor(credentialId);

        CreateParRequestClaims claims = new CreateParRequestClaims();
        claims.setCredentialID(policy.credentialId());
        claims.setHashAlgorithmOID(HASH_ALGORITHM.toMab());
        claims.setDocumentDigests(List.of(
                new CreateParRequestClaimsDocumentDigestsInner()
                        .hash(digestB64)
                        .label("Document-1")
        ));

        String state = OidcRandoms.state();
        String nonce = OidcRandoms.nonce();
        String clientSessionId = UUID.randomUUID().toString();

        CreateParRequest req = new CreateParRequest()
                .clientId(props.getClient().getClientId())
                .clientSecret(props.getClient().getClientSecret())
                .redirectUri(URI.create(props.getClient().getRedirectUri().toString()))
                .state(state)
                .clientSessionId(clientSessionId)
                .scope(policy.scope())
                .claims(claims)
                .identMethods(null);

        if (policy.namespaceOrNull() != null) {
            req.setLoginHint(new CreateParRequestLoginHint().namespace(policy.namespaceOrNull()));
        }

        return new ParStartContext(state, nonce, digestB64, req, HASH_ALGORITHM);
    }
}