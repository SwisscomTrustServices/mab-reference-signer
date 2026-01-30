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
}
