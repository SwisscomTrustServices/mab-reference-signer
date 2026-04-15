package org.sts.demo.signer.signing.mab.par;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.openapi.mab.model.CreateParRequest;

import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("unused")
public final class ParRequestPayload {
    private final CreateParRequest base;
    private final List<CreateParRequest.ScopeEnum> scopes;

    public ParRequestPayload(CreateParRequest base,
                             List<CreateParRequest.ScopeEnum> scopes) {
        this.base = base;
        this.scopes = scopes;
    }

    @JsonUnwrapped
    public CreateParRequest getBase() { return base; }

    @JsonProperty("scope")
    public String getScope() {
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("PAR scopes are required");
        }
        return scopes.stream().map(CreateParRequest.ScopeEnum::getValue).collect(Collectors.joining(" "));
    }
}

