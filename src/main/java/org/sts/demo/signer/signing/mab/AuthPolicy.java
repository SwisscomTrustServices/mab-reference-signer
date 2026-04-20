package org.sts.demo.signer.signing.mab;

import org.openapi.mab.model.CreateParRequest;
import org.openapi.mab.model.CreateParRequestIdentMethodsInner;
import org.openapi.mab.model.CreateParRequestLoginHint;
import org.openapi.mab.model.OauthAuthenticationRequest;
import org.sts.demo.signer.signing.domain.CredentialId;
import org.sts.demo.signer.signing.domain.HashAlgorithm;
import org.sts.demo.signer.signing.domain.SigningJourney;

import java.util.List;

public record AuthPolicy(
        List<String> scopes,
        CredentialId credentialId,
        CreateParRequestLoginHint.NamespaceEnum namespace,
        CreateParRequestIdentMethodsInner.TypeEnum identMethodType,
        HashAlgorithm hashAlgorithm
) {
    public static AuthPolicy policyFor(SigningJourney journey) {
        return switch (journey) {
            case PAR_AES_FAST_TRACK -> new AuthPolicy(
                    List.of(CreateParRequest.ScopeEnum.SIGN.getValue()),
                    CredentialId.ADVANCED4,
                    CreateParRequestLoginHint.NamespaceEnum.PWDOTP,
                    null,
                    HashAlgorithm.SHA256);
            case PAR_QES_REP -> new AuthPolicy(
                    List.of(CreateParRequest.ScopeEnum.SIGN.getValue(), CreateParRequest.ScopeEnum.IDENT.getValue()),
                    CredentialId.QUALIFIED4,
                    CreateParRequestLoginHint.NamespaceEnum.WBAUTHN,
                    CreateParRequestIdentMethodsInner.TypeEnum.FIDENTITY,
                    HashAlgorithm.SHA256);
            case CIBA_AES_IDENT -> new AuthPolicy(
                    List.of(OauthAuthenticationRequest.ScopeEnum.IDENT.getValue()),
                    CredentialId.ADVANCED4,
                    CreateParRequestLoginHint.NamespaceEnum.MSISDN,
                    CreateParRequestIdentMethodsInner.TypeEnum.FIDENTITY,
                    HashAlgorithm.SHA256);
            case CIBA_QES_IDENT -> new AuthPolicy(
                    List.of(OauthAuthenticationRequest.ScopeEnum.IDENT.getValue()),
                    CredentialId.QUALIFIED4,
                    CreateParRequestLoginHint.NamespaceEnum.MSISDN,
                    CreateParRequestIdentMethodsInner.TypeEnum.FIDENTITY,
                    HashAlgorithm.SHA256);
            case CIBA_AES_SIGN -> new AuthPolicy(
                    List.of(OauthAuthenticationRequest.ScopeEnum.SIGN.getValue()),
                    CredentialId.ADVANCED4,
                    CreateParRequestLoginHint.NamespaceEnum.MSISDN,
                    null,
                    HashAlgorithm.SHA256);
            case CIBA_QES_SIGN -> new AuthPolicy(
                    List.of(OauthAuthenticationRequest.ScopeEnum.SIGN.getValue()),
                    CredentialId.QUALIFIED4,
                    CreateParRequestLoginHint.NamespaceEnum.MSISDN,
                    null,
                    HashAlgorithm.SHA256);
        };
    }
}
