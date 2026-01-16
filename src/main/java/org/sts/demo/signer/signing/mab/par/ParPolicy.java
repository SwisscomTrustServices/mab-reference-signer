package org.sts.demo.signer.signing.mab.par;

import org.openapi.mab.model.CreateParRequest;
import org.openapi.mab.model.CreateParRequestClaims;
import org.openapi.mab.model.CreateParRequestLoginHint;
import org.sts.demo.signer.signing.domain.SigningJourney;

public record ParPolicy (
        CreateParRequest.ScopeEnum scope,
        CreateParRequestClaims.CredentialIDEnum credentialId,
        CreateParRequestLoginHint.NamespaceEnum namespace
) {
    static ParPolicy policyFor(SigningJourney journey) {
        return switch (journey) {
            case FAST_TRACK -> new ParPolicy(
                    CreateParRequest.ScopeEnum.SIGN,
                    CreateParRequestClaims.CredentialIDEnum.ADVANCED4,
                    CreateParRequestLoginHint.NamespaceEnum.PWDOTP);
            case QUALIFIED -> new ParPolicy(
                    CreateParRequest.ScopeEnum.IDENT,
                    CreateParRequestClaims.CredentialIDEnum.QUALIFIED4,
                    null);
            case TELCO_ID -> new ParPolicy(
                    CreateParRequest.ScopeEnum.SIGN,
                    CreateParRequestClaims.CredentialIDEnum.ADVANCED4,
                    null);
        };
    }
}
