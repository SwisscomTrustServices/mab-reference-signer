package org.sts.demo.signer.signing;

import org.openapi.etsi.model.EtsiSignRequest;
import org.openapi.mab.model.AuthorizationCodeTokenRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;
import org.sts.demo.signer.signing.api.*;
import org.sts.demo.signer.signing.domain.SigningSession;
import org.sts.demo.signer.signing.domain.SigningSessionStore;
import org.sts.demo.signer.signing.domain.SigningSessionValidator;
import org.sts.demo.signer.signing.etsi.EtsiResponseMapper;
import org.sts.demo.signer.signing.etsi.EtsiSignClient;
import org.sts.demo.signer.signing.etsi.EtsiSignRequestFactory;
import org.sts.demo.signer.signing.mab.par.ParClient;
import org.sts.demo.signer.signing.mab.par.ParRequestFactory;
import org.sts.demo.signer.signing.mab.token.TokenClient;
import org.sts.demo.signer.signing.mab.token.TokenRequestFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.sts.demo.signer.signing.util.DigestUtils.sha256Base64;

@Service
public class SigningOrchestrationService {

    private final ParRequestFactory parRequestFactory;
    private final ParClient parClient;
    private final OidcEndpoints endpoints;
    private final QtspProperties props;
    private final SigningSessionStore sessions;
    private final TokenRequestFactory tokenRequestFactory;
    private final TokenClient tokenClient;
    private final EtsiSignRequestFactory etsiSignRequestFactory;
    private final EtsiSignClient etsiSignClient;
    private final SigningSessionValidator signingSessionValidator;

    public SigningOrchestrationService(ParRequestFactory parRequestFactory,
                                       ParClient parClient,
                                       OidcEndpoints endpoints,
                                       QtspProperties props,
                                       SigningSessionStore sessions,
                                       TokenRequestFactory tokenRequestFactory,
                                       TokenClient tokenClient,
                                       EtsiSignRequestFactory etsiSignRequestFactory,
                                       EtsiSignClient etsiSignClient,
                                       SigningSessionValidator signingSessionValidator) {
        this.parRequestFactory = parRequestFactory;
        this.parClient = parClient;
        this.endpoints = endpoints;
        this.props = props;
        this.sessions = sessions;
        this.tokenRequestFactory = tokenRequestFactory;
        this.tokenClient = tokenClient;
        this.etsiSignRequestFactory = etsiSignRequestFactory;
        this.etsiSignClient = etsiSignClient;
        this.signingSessionValidator = signingSessionValidator;
    }

    public Mono<ParStartResponse> pushPar(MultipartFile pdf, String credentialId) {
        return Mono.defer(() -> {
            if (pdf == null || pdf.isEmpty()) {
                return Mono.error(new IllegalArgumentException("Missing PDF"));
            }

            return Mono.fromCallable(pdf::getBytes)
                    .subscribeOn(Schedulers.boundedElastic())
                    .onErrorMap(e -> new IllegalArgumentException("Failed to read PDF", e))
                    .map(bytes -> {
                        String digestB64 = sha256Base64(bytes);
                        return parRequestFactory.buildDemoRequest(credentialId, digestB64);
                    })
                    .flatMap(ctx ->
                            parClient.send(ctx.request())
                                    .map(par -> {
                                        sessions.put(new SigningSession(
                                                ctx.state(),
                                                ctx.nonce(),
                                                ctx.digestB64(),
                                                ctx.hashAlg(),
                                                null
                                        ));

                                        String redirectUri = UriComponentsBuilder
                                                .fromUriString(endpoints.authorizationUri())
                                                .queryParam("client_id", props.getClient().getClientId().toString())
                                                .queryParam("redirect_uri", props.getClient().getRedirectUri())
                                                .queryParam("request_uri", par.getRequestUri())
                                                .queryParam("state", ctx.state())
                                                .queryParam("nonce", ctx.nonce())
                                                .queryParam("response_type", "code")
                                                .build(true)
                                                .toUriString();

                                        return new ParStartResponse(
                                                redirectUri,
                                                ctx.state(),
                                                ctx.nonce()
                                        );
                                    })
                    );
        });
    }

    public Mono<TokenExchangeResponse> exchangeAuthCodeForAccessToken(TokenExchangeRequest in) {
        return Mono.defer(() ->
                signingSessionValidator.validate(in.state(), in.nonce())
                        .flatMap(session -> {
                            if (in.code() == null || in.code().isBlank()) {
                                return Mono.error(new IllegalArgumentException("Missing authorization code"));
                            }
                            AuthorizationCodeTokenRequest authReq = tokenRequestFactory.buildAuthCode(in.code());

                            return tokenClient.exchange(authReq)
                                    .flatMap(tr -> {
                                        String sadJwt = tr.getAccessToken();
                                        if (sadJwt.isBlank()) {
                                            return Mono.error(new IllegalStateException("Token response missing access_token"));
                                        }

                                        sessions.put(session.withSadJwt(sadJwt));

                                        return Mono.just(new TokenExchangeResponse(
                                                sadJwt.substring(0, 40) + "...***redacted***",
                                                tr.getTokenType(),
                                                tr.getExpiresIn(),
                                                tr.getScope().getValue()
                                        ));
                                    });
                        })
        );
    }

    public Mono<EtsiSignStartResponse> signEtsi(EtsiSignStartRequest in) {
        return Mono.defer(() ->
                signingSessionValidator.validate(in.state(), in.nonce())
                        .flatMap(session -> {
                            EtsiSignRequest req = etsiSignRequestFactory.build(session);

                            return etsiSignClient.sign(req)
                                    .map(EtsiResponseMapper::toEmbedResponse);
                        })
        );
    }
}