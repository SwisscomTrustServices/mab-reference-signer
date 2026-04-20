package org.sts.demo.signer.signing.mab.ciba;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.openapi.mab.model.OauthAuthenticationRequest;

import java.util.List;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("unused")
public final class CibaRequestPayload {
    private final OauthAuthenticationRequest base;
    private final List<OauthAuthenticationRequest.ScopeEnum> scopes;

    public CibaRequestPayload(OauthAuthenticationRequest base,
                              List<OauthAuthenticationRequest.ScopeEnum> scopes) {
        this.base = base;
        this.scopes = scopes;
    }

    @JsonUnwrapped
    public OauthAuthenticationRequest getBase() { return base; }

    @JsonProperty("scope")
    public String getScope() {
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("CIBA scopes are required");
        }
        return scopes.stream().map(OauthAuthenticationRequest.ScopeEnum::getValue).collect(Collectors.joining(" "));
    }
}

