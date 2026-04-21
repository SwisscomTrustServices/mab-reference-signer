package org.sts.demo.signer.signing.mab;

import org.sts.demo.signer.signing.domain.*;

import java.util.List;

public record AuthPolicy(
        List<String> scopes,
        CredentialId credentialId,
        Namespace namespace,
        IdentMethodType identMethodType,
        HashAlgorithm hashAlgorithm
) {
    public static AuthPolicy policyFor(SigningJourney journey) {
        return switch (journey) {
            case PAR_AES_FAST_TRACK -> new AuthPolicy(
                    List.of("sign"),
                    CredentialId.ADVANCED4,
                    Namespace.PWDOTP,
                    null,
                    HashAlgorithm.SHA256);
            case PAR_QES_REP -> new AuthPolicy(
                    List.of("sign", "ident"),
                    CredentialId.QUALIFIED4,
                    Namespace.WBAUTHN,
                    IdentMethodType.FIDENTITY,
                    HashAlgorithm.SHA256);
            case CIBA_AES_IDENT -> new AuthPolicy(
                    List.of("ident"),
                    CredentialId.ADVANCED4,
                    Namespace.MSISDN,
                    IdentMethodType.FIDENTITY,
                    HashAlgorithm.SHA256);
            case CIBA_QES_IDENT -> new AuthPolicy(
                    List.of("ident"),
                    CredentialId.QUALIFIED4,
                    Namespace.MSISDN,
                    IdentMethodType.FIDENTITY,
                    HashAlgorithm.SHA256);
            case CIBA_AES_SIGN -> new AuthPolicy(
                    List.of("sign"),
                    CredentialId.ADVANCED4,
                    Namespace.MSISDN,
                    null,
                    HashAlgorithm.SHA256);
            case CIBA_QES_SIGN -> new AuthPolicy(
                    List.of("sign"),
                    CredentialId.QUALIFIED4,
                    Namespace.MSISDN,
                    null,
                    HashAlgorithm.SHA256);
        };
    }
}
