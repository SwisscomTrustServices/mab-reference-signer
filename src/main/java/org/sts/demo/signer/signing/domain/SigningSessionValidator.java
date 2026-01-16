package org.sts.demo.signer.signing.domain;

import org.springframework.stereotype.Component;

@Component
public class SigningSessionValidator {
    private final SigningSessionStore sessions;

    public SigningSessionValidator(SigningSessionStore sessions) {
        this.sessions = sessions;
    }

    public SigningSession validateAndGet(String state, String nonce) {
        requireNonBlank(state, "Missing state");
        requireNonBlank(nonce, "Missing nonce");
        SigningSession session = sessions.get(state);
        if (session == null) {
            throw new IllegalArgumentException("Unknown/expired state");
        }
        if (!nonce.equals(session.nonce())) {
            throw new IllegalArgumentException("Invalid nonce for state");
        }
        return session;
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }
}
