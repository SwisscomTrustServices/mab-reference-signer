package org.sts.demo.signer.signing.mab.ciba;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openapi.mab.model.OauthAuthenticationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;
import reactor.core.publisher.Mono;

@Component
public class CibaClient {
    private static final Logger log = LoggerFactory.getLogger(CibaClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient mtlsWebClient;
    private final OidcEndpoints endpoints;

    public CibaClient(
            @Qualifier("qtspMtlsWebClient") WebClient mtlsWebClient,
            OidcEndpoints endpoints
    ) {
        this.mtlsWebClient = mtlsWebClient;
        this.endpoints = endpoints;
    }

    public Mono<OauthAuthenticationResponse> authenticate(CibaRequestPayload req) {
        MultiValueMap<String, String> form = toAuthForm(req);

        String prettyForm;
        try {
            prettyForm = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(form);
        } catch (Exception e) {
            prettyForm = form.toString();
        }
        log.info("CIBA Auth form payload={}", prettyForm);
        log.info("CIBA Auth POST {}", endpoints.cibaAuthUri());

        return mtlsWebClient.post()
                .uri(endpoints.cibaAuthUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(form))
                .exchangeToMono(resp -> {
                    int status = resp.statusCode().value();
                    if (status >= 200 && status < 300) {
                        return resp.bodyToMono(OauthAuthenticationResponse.class)
                                .doOnNext(r -> log.info("CIBA Auth success status={} body={}", status, r));
                    } else {
                        return resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.warn("CIBA Auth failed status={} body={}", status, body);
                                    return Mono.error(new IllegalStateException("CIBA Auth failed: " + status + " body=" + body));
                                });
                    }
                });
    }

    private MultiValueMap<String, String> toAuthForm(CibaRequestPayload req) {
        var base = req.getBase();
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        add(form, "client_id", base.getClientId());
        add(form, "client_secret", base.getClientSecret());
        add(form, "ident_method_type", base.getIdentMethodType());
        add(form, "ident_method_parameters", base.getIdentMethodParameters());
        add(form, "ident_method_delegated_account_id", base.getIdentMethodDelegatedAccountId());
        add(form, "ident_method_success_redirect_uri", base.getIdentMethodSuccessRedirectUri());
        add(form, "ident_method_error_redirect_uri", base.getIdentMethodErrorRedirectUri());
        add(form, "tac_hash_zertes", base.getTacHashZertes());
        add(form, "tac_hash_eidas", base.getTacHashEidas());
        add(form, "tac_language", base.getTacLanguage());
        add(form, "claims_token", base.getClaimsToken());
        add(form, "login_hint_token", base.getLoginHintToken());
        add(form, "scope", req.getScope());
        add(form, "token_delivery_mode", base.getTokenDeliveryMode());
        add(form, "callback_url", base.getCallbackUrl());
        add(form, "client_notification_token", base.getClientNotificationToken());

        return form;
    }

    private static void add(MultiValueMap<String, String> form, String key, Object value) {
        if (value != null) {
            form.add(key, value.toString());
        }
    }
}