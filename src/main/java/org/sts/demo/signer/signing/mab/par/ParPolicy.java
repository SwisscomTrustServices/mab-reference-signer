package org.sts.demo.signer.signing.mab.par;

import org.openapi.mab.model.CreateParRequest;
import org.openapi.mab.model.CreateParRequestClaims;
import org.openapi.mab.model.CreateParRequestLoginHint;

public record ParPolicy (
        CreateParRequest.ScopeEnum scope,
        CreateParRequestClaims.CredentialIDEnum credentialId,
        CreateParRequestLoginHint.NamespaceEnum namespaceOrNull
) {
    static ParPolicy policyFor(String cred) {
        return switch (cred) {
            case "ADVANCED" -> new ParPolicy(
                    CreateParRequest.ScopeEnum.SIGN,
                    CreateParRequestClaims.CredentialIDEnum.ADVANCED4,
                    CreateParRequestLoginHint.NamespaceEnum.PWDOTP);
            case "QUALIFIED" -> new ParPolicy(
                    CreateParRequest.ScopeEnum.IDENT,
                    CreateParRequestClaims.CredentialIDEnum.QUALIFIED4,
                    null);
            default -> throw new IllegalStateException("Unexpected value: " + cred);
        };
    }
}
