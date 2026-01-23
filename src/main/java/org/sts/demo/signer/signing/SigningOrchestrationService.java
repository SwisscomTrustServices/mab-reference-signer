package org.sts.demo.signer.signing;

import org.openapi.etsi.model.EtsiSignRequest;
import org.openapi.mab.model.AuthorizationCodeTokenRequest;
import org.openapi.mab.model.TokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;
import org.sts.demo.signer.signing.api.*;
import org.sts.demo.signer.signing.domain.SigningJourney;
import org.sts.demo.signer.signing.domain.SigningSession;
import org.sts.demo.signer.signing.domain.SigningSessionStore;
import org.sts.demo.signer.signing.domain.SigningSessionValidator;
import org.sts.demo.signer.signing.etsi.EtsiResponseMapper;
import org.sts.demo.signer.signing.etsi.EtsiSignClient;
import org.sts.demo.signer.signing.etsi.EtsiSignRequestFactory;
import org.sts.demo.signer.signing.mab.par.ParClient;
import org.sts.demo.signer.signing.mab.par.ParRequestFactory;
import org.sts.demo.signer.signing.mab.par.ParStartContext;
import org.sts.demo.signer.signing.mab.token.TokenClient;
import org.sts.demo.signer.signing.mab.token.TokenRequestFactory;
import org.sts.demo.signer.signing.util.DigestUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.sts.demo.signer.signing.util.Redactor.redactJwt;

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

    public Mono<ParStartResponse> pushPar(MultipartFile pdf, SigningJourney journey) {
        return Mono.defer(() -> {
            requirePdf(pdf);
            return readPdfBytes(pdf)
                    .map(DigestUtils::sha256Base64)
                    .map(digestB64 -> parRequestFactory.buildDemoRequest(journey, digestB64))
                    .flatMap(ctx -> parClient.send(ctx.request())
                                    .map(par -> {
                                        persistInitialSession(ctx);
                                        return toParStartResponse(ctx, par.getRequestUri());
                                    })
                    );
        });
    }

    public Mono<TokenExchangeResponse> exchangeAuthCodeForAccessToken(TokenExchangeRequest in) {
        return Mono.fromCallable(() -> signingSessionValidator.validateAndGet(in.state(), in.nonce()))
                        .flatMap(session -> {
                            String code = requireNonBlank(in.code(), "Missing authorization code");
                            AuthorizationCodeTokenRequest authReq = tokenRequestFactory.buildAuthCode(code);
                            return tokenClient.exchange(authReq)
                                    .map(tr -> {
                                        String sadJwt = requireNonBlank(tr.getAccessToken(), "Token response missing access_token");
                                        sessions.put(session.withSadJwt(sadJwt));
                                        return toTokenExchangeResponse(sadJwt, tr);
                                    });
                        });
    }

    public Mono<EtsiSignStartResponse> signEtsi(EtsiSignStartRequest in) {
        return Mono.fromCallable(() -> signingSessionValidator.validateAndGet(in.state(), in.nonce()))
                        .flatMap(session -> {
                            session.requireSadJwt();
                            EtsiSignRequest req = etsiSignRequestFactory.build(session);
                            return etsiSignClient.sign(req).map(EtsiResponseMapper::toEmbedResponse);
                        });
    }

    private static void requirePdf(MultipartFile pdf) {
        if (pdf == null || pdf.isEmpty()) {
            throw new IllegalArgumentException("Missing PDF");
        }
    }

    private static Mono<byte[]> readPdfBytes(MultipartFile pdf) {
        return Mono.fromCallable(pdf::getBytes)
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(e -> new IllegalArgumentException("Failed to read PDF", e));
    }

    private void persistInitialSession(ParStartContext ctx) {
        sessions.put(new SigningSession(
                ctx.state(),
                ctx.nonce(),
                ctx.digestB64(),
                ctx.hashAlg(),
                null // sadJwt will be set after token exchange
        ));
    }

    private ParStartResponse toParStartResponse(ParStartContext ctx, String requestUri) {
        String authorizationUrl = UriComponentsBuilder
                .fromUriString(endpoints.authorizationUri())
                .queryParam("client_id", props.getClient().getClientId().toString())
                .queryParam("redirect_uri", props.getClient().getRedirectUri())
                .queryParam("request_uri", requestUri)
                .queryParam("state", ctx.state())
                .queryParam("nonce", ctx.nonce())
                .queryParam("response_type", "code")
                .build(true)
                .toUriString();

        return new ParStartResponse(
                authorizationUrl,
                ctx.state(),
                ctx.nonce()
        );
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private static TokenExchangeResponse toTokenExchangeResponse(String sadJwt, TokenResponse tr) {
        return new TokenExchangeResponse(
                redactJwt(sadJwt),
                tr.getTokenType(),
                tr.getExpiresIn(),
                tr.getScope().getValue()
        );
    }
}