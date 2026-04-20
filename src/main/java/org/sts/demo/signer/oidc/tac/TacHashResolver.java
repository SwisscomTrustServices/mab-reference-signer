package org.sts.demo.signer.oidc.tac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;

import java.time.Duration;

@Component
public class TacHashResolver {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient publicClient;
    private final OidcEndpoints endpoints;

    public TacHashResolver(@Qualifier("qtspPublicWebClient") WebClient publicClient,
                           OidcEndpoints endpoints) {
        this.publicClient = publicClient;
        this.endpoints = endpoints;
    }

    public String resolveSha256(String documentKey) {
        try {
            String body = publicClient.get()
                    .uri(endpoints.tacUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (body == null || body.isBlank()) {
                throw new IllegalStateException("Empty TAC response from " + endpoints.tacUri());
            }

            JsonNode root = MAPPER.readTree(body);
            String hash = root.path(documentKey).path("sha256").asText(null);

            if (hash == null || hash.isBlank()) {
                throw new IllegalArgumentException("Missing sha256 for TAC key " + documentKey);
            }
            return hash;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load TAC index from " + endpoints.tacUri(), e);
        }
    }
}

