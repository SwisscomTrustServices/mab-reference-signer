package org.sts.demo.signer.signing.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public final class FormDataConverter {

    private FormDataConverter() {}

    public static MultiValueMap<String, String> toFormData(ObjectMapper mapper, Object request) {
        ObjectNode json = mapper.valueToTree(request);
        JsonNullPruner.pruneNulls(json);
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        json.fields().forEachRemaining(entry ->
                form.add(entry.getKey(), entry.getValue().asText()));
        return form;
    }

    public static String prettyPrint(ObjectMapper mapper, MultiValueMap<String, String> form) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(form);
        } catch (Exception e) {
            return form.toString();
        }
    }
}
