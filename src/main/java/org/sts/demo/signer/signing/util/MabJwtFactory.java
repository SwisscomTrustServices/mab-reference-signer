package org.sts.demo.signer.signing.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MabJwtFactory {
    private final ObjectMapper objectMapper;

    public MabJwtFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String createLoginHintToken(String identifier, String namespace, String secret) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("identifier is required");
        }
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace is required");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("identifier", identifier);
        payload.put("namespace", namespace);
        return createHs256Jwt(payload, secret);
    }

    public String createClaimsTokenForSign(
            String credentialId,
            String base64Digest,
            String label,
            String hashAlgorithmOid,
            String secret) {
        if (base64Digest == null || base64Digest.isBlank()) {
            throw new IllegalArgumentException("base64Digest is required");
        }
        if (hashAlgorithmOid == null || hashAlgorithmOid.isBlank()) {
            throw new IllegalArgumentException("hashAlgorithmOid is required");
        }

        Map<String, Object> payload = new LinkedHashMap<>();

        if (credentialId != null && !credentialId.isBlank()) {
            payload.put("credentialID", credentialId);
        }

        Map<String, Object> digestEntry = new LinkedHashMap<>();
        digestEntry.put("hash", base64Digest);
        if (label != null && !label.isBlank()) {
            digestEntry.put("label", label);
        }

        payload.put("documentDigests", List.of(digestEntry));
        payload.put("hashAlgorithmOID", hashAlgorithmOid);

        return createHs256Jwt(payload, secret);
    }

    private String createHs256Jwt(Map<String, Object> payload, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("secret is required");
        }
        try {
            Map<String, Object> header = Map.of(
                    "alg", "HS256",
                    "typ", "JWT"
            );

            String encodedHeader = base64Url(toJson(header));
            String encodedPayload = base64Url(toJson(payload));
            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = hmacSha256(signingInput, secret);

            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create JWT", e);
        }
    }

    private String toJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }
}
