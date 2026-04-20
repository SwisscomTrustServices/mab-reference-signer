package org.sts.demo.signer.signing.domain;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SigningSessionStore {
    private final ConcurrentHashMap<String, SigningSession> sessions = new ConcurrentHashMap<>();

    public void put(SigningSession s) {
        SigningSession prev = sessions.putIfAbsent(s.state(), s);
        if (prev != null) {
            throw new IllegalStateException("Signing session already exists for state");
        }
    }

    public SigningSession remove(String state) {
        return sessions.remove(state);
    }

    public SigningSession get(String state) {
        return sessions.get(state);
    }

    public void putInitial(String state,
                           String nonce,
                           String digestB64,
                           HashAlgorithm hashAlgorithm,
                           CredentialId credentialId,
                           DocumentSigningContext document) {
        put(new SigningSession(
                state,
                nonce,
                digestB64,
                hashAlgorithm,
                credentialId,
                null,
                document
        ));
    }
}
