package org.sts.demo.signer.signing.mab.par;

import org.openapi.mab.model.CreateParRequest;
import org.openapi.mab.model.CreateParRequestClaims;
import org.openapi.mab.model.CreateParRequestLoginHint;
import org.sts.demo.signer.signing.domain.HashAlgorithm;
import org.sts.demo.signer.signing.domain.SigningJourney;

import java.util.List;

public record ParPolicy (
        List<CreateParRequest.ScopeEnum> scopes,
        CreateParRequestClaims.CredentialIDEnum credentialId,
        CreateParRequestLoginHint.NamespaceEnum namespace,
        HashAlgorithm hashAlgorithm
) {
    static ParPolicy policyFor(SigningJourney journey) {
        return switch (journey) {
            case FAST_TRACK -> new ParPolicy(
                    List.of(CreateParRequest.ScopeEnum.SIGN),
                    CreateParRequestClaims.CredentialIDEnum.ADVANCED4,
                    CreateParRequestLoginHint.NamespaceEnum.PWDOTP,
                    HashAlgorithm.SHA256);
            case QUALIFIED -> new ParPolicy(
                    List.of(CreateParRequest.ScopeEnum.IDENT, CreateParRequest.ScopeEnum.SIGN),
                    CreateParRequestClaims.CredentialIDEnum.QUALIFIED4,
                    null,
                    HashAlgorithm.SHA256);
        };
    }
}
