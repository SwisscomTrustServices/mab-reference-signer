package org.sts.demo.signer.signing.domain;

import org.springframework.stereotype.Component;

import static org.sts.demo.signer.signing.util.ValidationUtils.requireNonBlank;

@Component
public class SigningSessionValidator {
    private final SigningSessionStore sessions;

    public SigningSessionValidator(SigningSessionStore sessions) {
        this.sessions = sessions;
    }

    public SigningSession validateAndTake(String state, String nonce) {
        requireNonBlank(state, "Missing state");
        requireNonBlank(nonce, "Missing nonce");
        SigningSession session = sessions.remove(state);
        if (session == null) throw new IllegalArgumentException("Unknown/expired state");
        if (!nonce.equals(session.nonce())) throw new IllegalArgumentException("Invalid nonce for state");
        return session;
    }

    public SigningSession validateIfPresent(String state, String nonce) {
        requireNonBlank(state, "Missing state");
        requireNonBlank(nonce, "Missing nonce");
        SigningSession session = sessions.get(state);
        if (session == null) return null;
        if (!nonce.equals(session.nonce())) throw new IllegalArgumentException("Invalid nonce for state");
        return session;
    }
}
