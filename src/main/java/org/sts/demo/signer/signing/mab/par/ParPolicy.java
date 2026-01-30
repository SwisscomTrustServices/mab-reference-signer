package org.sts.demo.signer.signing.mab.par;

import org.openapi.mab.model.CreateParRequest;
import org.openapi.mab.model.CreateParRequestClaims;
import org.openapi.mab.model.CreateParRequestLoginHint;
import org.sts.demo.signer.signing.domain.HashAlgorithm;
import org.sts.demo.signer.signing.domain.SigningJourney;

public record ParPolicy (
        CreateParRequest.ScopeEnum scope,
        CreateParRequestClaims.CredentialIDEnum credentialId,
        CreateParRequestLoginHint.NamespaceEnum namespace,
        HashAlgorithm hashAlgorithm
) {
    static ParPolicy policyFor(SigningJourney journey) {
        return switch (journey) {
            case FAST_TRACK -> new ParPolicy(
                    CreateParRequest.ScopeEnum.SIGN,
                    CreateParRequestClaims.CredentialIDEnum.ADVANCED4,
                    CreateParRequestLoginHint.NamespaceEnum.PWDOTP,
                    HashAlgorithm.SHA256);
            case QUALIFIED -> new ParPolicy(
                    CreateParRequest.ScopeEnum.IDENT,
                    CreateParRequestClaims.CredentialIDEnum.QUALIFIED4,
                    null,
                    HashAlgorithm.SHA256);
            case TELCO_ID -> new ParPolicy(
                    CreateParRequest.ScopeEnum.SIGN,
                    CreateParRequestClaims.CredentialIDEnum.ADVANCED4,
                    null,
                    HashAlgorithm.SHA256);
        };
    }
}
