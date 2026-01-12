package org.sts.demo.signer.signing;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SigningSessionStore {
    private final ConcurrentHashMap<String, SigningSession> sessions = new ConcurrentHashMap<>();

    public void put(SigningSession s) {
        sessions.put(s.state(), s);
    }

    public SigningSession get(String state) {
        return sessions.get(state);
    }
}
