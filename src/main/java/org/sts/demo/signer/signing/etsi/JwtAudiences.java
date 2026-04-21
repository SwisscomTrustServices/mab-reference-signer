package org.sts.demo.signer.signing.etsi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class JwtAudiences {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtAudiences() {}

    public static List<String> aud(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) throw new IllegalArgumentException("Not a JWT");

        byte[] jsonBytes = Base64.getUrlDecoder().decode(padBase64(parts[1]));
        JsonNode payload;
        try {
            payload = MAPPER.readTree(jsonBytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse JWT payload", e);
        }

        JsonNode aud = payload.get("aud");
        if (aud == null || aud.isNull()) return List.of();

        if (aud.isTextual()) return List.of(aud.asText());
        if (aud.isArray()) {
            List<String> out = new ArrayList<>();
            aud.forEach(n -> { if (n.isTextual()) out.add(n.asText()); });
            return out;
        }
        return List.of();
    }

    private static String padBase64(String b64) {
        int mod = b64.length() % 4;
        return mod == 0 ? b64 : b64 + "====".substring(mod);
    }
}
