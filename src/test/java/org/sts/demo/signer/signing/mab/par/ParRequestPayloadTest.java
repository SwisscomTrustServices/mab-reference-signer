package org.sts.demo.signer.signing.mab.par;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.openapi.mab.model.CreateParRequest;
import org.openapi.mab.model.CreateParRequestClaims;
import org.openapi.mab.model.CreateParRequestLoginHint;
import org.sts.demo.signer.signing.util.JsonNullPruner;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ParRequestPayloadTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void serializesUnwrappedGeneratedFieldsAndJoinedScope() {
        CreateParRequest base = new CreateParRequest()
                .clientId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .clientSecret(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .redirectUri(URI.create("https://example.invalid/cb"))
                .state("state-1")
                .clientSessionId("sess-1")
                .claims(new CreateParRequestClaims())
                .loginHint(null)
                .identMethods(null)
                .scope(null);

        ParRequestPayload payload = new ParRequestPayload(
                base,
                List.of(CreateParRequest.ScopeEnum.IDENT, CreateParRequest.ScopeEnum.SIGN)
        );

        ObjectNode json = MAPPER.valueToTree(payload);
        JsonNullPruner.pruneNulls(json);

        assertEquals("ident sign", json.get("scope").asText());
        assertEquals("state-1", json.get("state").asText());
        assertEquals("sess-1", json.get("client_session_id").asText());
        assertTrue(json.has("client_id"));
        assertTrue(json.has("client_secret"));
        assertFalse(json.has("login_hint"), "login_hint should be omitted when null");
    }

    @Test
    void serializesLoginHintOnlyWhenSet() {
        CreateParRequest base = new CreateParRequest()
                .clientId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .clientSecret(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .redirectUri(URI.create("https://example.invalid/cb"))
                .claims(new CreateParRequestClaims())
                .loginHint(new CreateParRequestLoginHint().namespace(CreateParRequestLoginHint.NamespaceEnum.PWDOTP));

        ParRequestPayload payload = new ParRequestPayload(
                base,
                List.of(CreateParRequest.ScopeEnum.SIGN)
        );

        ObjectNode json = MAPPER.valueToTree(payload);
        JsonNullPruner.pruneNulls(json);

        assertTrue(json.has("login_hint"));
        assertEquals("PWDOTP", json.get("login_hint").get("namespace").asText());
    }
}

