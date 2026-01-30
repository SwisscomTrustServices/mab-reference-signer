package org.sts.demo.signer.signing.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonNullPrunerTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void pruneNulls_nullInput_noop() {
        // should not throw
        JsonNullPruner.pruneNulls(null);
    }

    @Test
    void pruneNulls_removesNullFieldsFromObject() throws Exception {
        ObjectNode node = (ObjectNode) MAPPER.readTree("""
            {
              "a": 1,
              "b": null,
              "c": "text"
            }
        """);

        JsonNullPruner.pruneNulls(node);

        assertTrue(node.has("a"));
        assertTrue(node.has("c"));
        assertFalse(node.has("b"));
    }

    @Test
    void pruneNulls_removesNestedNullFields() throws Exception {
        ObjectNode node = (ObjectNode) MAPPER.readTree("""
            {
              "a": {
                "x": 1,
                "y": null
              },
              "b": null
            }
        """);

        JsonNullPruner.pruneNulls(node);

        assertTrue(node.has("a"));
        assertFalse(node.has("b"));

        JsonNode a = node.get("a");
        assertTrue(a.has("x"));
        assertFalse(a.has("y"));
    }

    @Test
    void pruneNulls_handlesArraysWithObjects() throws Exception {
        ObjectNode node = (ObjectNode) MAPPER.readTree("""
            {
              "arr": [
                { "a": 1, "b": null },
                { "c": null, "d": 2 }
              ]
            }
        """);

        JsonNullPruner.pruneNulls(node);

        JsonNode arr = node.get("arr");
        assertEquals(2, arr.size());

        assertTrue(arr.get(0).has("a"));
        assertFalse(arr.get(0).has("b"));

        assertTrue(arr.get(1).has("d"));
        assertFalse(arr.get(1).has("c"));
    }

    @Test
    void pruneNulls_keepsNullsInsideArrays() throws Exception {
        ObjectNode node = (ObjectNode) MAPPER.readTree("""
            {
              "arr": [1, null, 2]
            }
        """);

        JsonNullPruner.pruneNulls(node);

        JsonNode arr = node.get("arr");
        assertEquals(3, arr.size());
        assertTrue(arr.get(1).isNull());
    }

    @Test
    void pruneNulls_doesNotRemoveEmptyObjects() throws Exception {
        ObjectNode node = (ObjectNode) MAPPER.readTree("""
            {
              "a": {
                "x": null
              }
            }
        """);

        JsonNullPruner.pruneNulls(node);

        assertTrue(node.has("a"));
        assertTrue(node.get("a").isObject());
        assertEquals(0, node.get("a").size());
    }
}