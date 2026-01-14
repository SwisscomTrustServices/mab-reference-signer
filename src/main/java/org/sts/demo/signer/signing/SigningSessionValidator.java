package org.sts.demo.signer.signing;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SigningSessionValidator {
    private final SigningSessionStore sessions;

    public SigningSessionValidator(SigningSessionStore sessions) {
        this.sessions = sessions;
    }

    public Mono<SigningSession> validate(String state, String nonce) {
        if (state == null || state.isBlank()) {
            return Mono.error(new IllegalArgumentException("Missing state"));
        }

        SigningSession session = sessions.get(state);
        if (session == null) {
            return Mono.error(new IllegalArgumentException("Unknown/expired state"));
        }

        if (nonce == null || nonce.isBlank()) {
            return Mono.error(new IllegalArgumentException("Missing nonce"));
        }

        if (!nonce.equals(session.nonce())) {
            return Mono.error(new IllegalArgumentException("Invalid nonce for state"));
        }

        return Mono.just(session);
    }
}
