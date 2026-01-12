package org.sts.demo.signer.signing.token;

import org.openapi.mab.model.AuthorizationCodeTokenRequest;
import org.sts.demo.signer.signing.SigningSession;

public record TokenContext (
        SigningSession session,
        AuthorizationCodeTokenRequest request
){}
