package org.sts.demo.signer.signing.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.MultiValueMap;

public final class FormPrettyPrinter {
    private FormPrettyPrinter() {}

    public static String prettyPrint(ObjectMapper objectMapper, MultiValueMap<String, String> form) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(form);
        } catch (Exception e) {
            return form.toString();
        }
    }
}

