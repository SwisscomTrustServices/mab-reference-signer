package org.sts.demo.signer.signing.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public final class JsonNullPruner {

    private JsonNullPruner() {}

    public static void pruneNulls(JsonNode node) {
        if (node == null) return;

        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            List<String> nullFields = new java.util.ArrayList<>();
            obj.fields().forEachRemaining(entry -> {
                if (entry.getValue().isNull()) {
                    nullFields.add(entry.getKey());
                } else {
                    pruneNulls(entry.getValue());
                }
            });
            nullFields.forEach(obj::remove);

        } else if (node.isArray()) {
            for (JsonNode child : node) {
                pruneNulls(child);
            }
        }
    }
}
