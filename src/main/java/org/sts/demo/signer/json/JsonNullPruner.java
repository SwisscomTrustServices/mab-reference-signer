package org.sts.demo.signer.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class JsonNullPruner {

    private JsonNullPruner() {}

    public static void pruneNulls(JsonNode node) {
        if (node == null) return;

        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;

            // collect names first -> no ConcurrentModificationException
            List<String> names = new ArrayList<>();
            for (Iterator<String> it = obj.fieldNames(); it.hasNext(); ) {
                names.add(it.next());
            }

            for (String name : names) {
                JsonNode child = obj.get(name);
                if (child == null || child.isNull()) {
                    obj.remove(name);
                } else {
                    pruneNulls(child);
                }
            }

        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (JsonNode child : arr) {
                pruneNulls(child);
            }
        }
    }
}
