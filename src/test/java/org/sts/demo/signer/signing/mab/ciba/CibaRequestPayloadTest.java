package org.sts.demo.signer.signing.mab.ciba;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.openapi.mab.model.OauthAuthenticationRequest;
import org.sts.demo.signer.signing.util.JsonNullPruner;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CibaRequestPayloadTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void serializesUnwrappedGeneratedFieldsAndJoinedScope() {
        OauthAuthenticationRequest base = new OauthAuthenticationRequest()
                .clientId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .clientSecret(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .claimsToken("claims-token")
                .scope(null);

        CibaRequestPayload payload = new CibaRequestPayload(
                base,
                List.of(OauthAuthenticationRequest.ScopeEnum.IDENT, OauthAuthenticationRequest.ScopeEnum.SIGN)
        );

        ObjectNode json = MAPPER.valueToTree(payload);
        JsonNullPruner.pruneNulls(json);

        assertEquals("ident sign", json.get("scope").asText());
        assertTrue(json.has("client_id"));
        assertTrue(json.has("client_secret"));
        assertEquals("claims-token", json.get("claims_token").asText());
    }

    @Test
    void getScope_shouldRejectMissingScopes() {
        CibaRequestPayload payload = new CibaRequestPayload(
                new OauthAuthenticationRequest(),
                List.of()
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, payload::getScope);
        assertTrue(ex.getMessage().contains("required"));
    }
}

