package org.sts.demo.signer.signing;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.util.Store;
import org.openapi.etsi.model.EtsiSignRequest;
import org.openapi.etsi.model.EtsiSignResponse;
import org.openapi.mab.model.AuthorizationCodeTokenRequest;
import org.openapi.mab.model.TokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;
import org.sts.demo.signer.signing.api.*;
import org.sts.demo.signer.signing.domain.*;
import org.sts.demo.signer.signing.etsi.EtsiResponseMapper;
import org.sts.demo.signer.signing.etsi.EtsiSignClient;
import org.sts.demo.signer.signing.etsi.EtsiSignRequestFactory;
import org.sts.demo.signer.signing.mab.par.ParClient;
import org.sts.demo.signer.signing.mab.par.ParRequestFactory;
import org.sts.demo.signer.signing.mab.par.ParStartContext;
import org.sts.demo.signer.signing.mab.token.TokenClient;
import org.sts.demo.signer.signing.mab.token.TokenRequestFactory;
import org.sts.demo.signer.signing.pdf.PdfLtvEnhancer;
import org.sts.demo.signer.signing.pdf.PdfSigningContext;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

import static org.sts.demo.signer.signing.util.DigestUtils.sha256Base64;
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
        requirePdf(pdf);
        return readPdfBytes(pdf)
                .flatMap(pdfBytes ->
                        Mono.fromCallable(() -> PdfSigningContext.prepare(pdfBytes))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(pdfSigningContext -> {
                                    try {
                                        byte[] toHash = pdfSigningContext.getContentToSign();
                                        String digestB64 = sha256Base64(toHash);
                                        var ctx = parRequestFactory.build(journey, digestB64);
                                        return parClient.send(ctx.request())
                                                .map(par -> {
                                                    persistInitialSession(ctx, pdfSigningContext, digestB64);
                                                    return toParStartResponse(ctx, par.getRequestUri());
                                                })
                                                .doOnError(e -> pdfSigningContext.close())
                                                .doOnCancel(pdfSigningContext::close);
                                    } catch (Exception e) {
                                        pdfSigningContext.close();
                                        return Mono.error(e);
                                    }
                                })
                );
    }

    public Mono<TokenExchangeResponse> exchangeAuthCodeForAccessToken(TokenExchangeRequest in) {
        return Mono.fromCallable(() -> signingSessionValidator.validateAndTake(in.state(), in.nonce()))
                .subscribeOn(Schedulers.boundedElastic())
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
        return Mono.usingWhen(
                Mono.fromCallable(() -> signingSessionValidator.validateAndTake(in.state(), in.nonce()))
                        .subscribeOn(Schedulers.boundedElastic()),
                signingSession -> {
                    signingSession.requireSadJwt();
                    EtsiSignRequest req = etsiSignRequestFactory.build(signingSession);

                    return etsiSignClient.sign(req)
                            .flatMap(resp -> Mono.fromCallable(() -> buildUiResponseWithLtv(signingSession, resp))
                                    .subscribeOn(Schedulers.boundedElastic()));
                },
                signingSession -> Mono.fromRunnable(() -> signingSession.document().close())
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    private EtsiSignStartResponse buildUiResponseWithLtv(SigningSession signingSession, EtsiSignResponse resp) throws Exception {
        byte[] cms = EtsiResponseMapper.extractCms(resp);

        byte[] signedPdf = signingSession.document().embedCms(cms);
        List<byte[]> certsDer = extractCertsDerFromCms(cms);
        var vi = resp.getValidationInfo();
        List<byte[]> ocspsDer = vi.getOcsp().stream().map(Base64.getDecoder()::decode).toList();
        List<byte[]> crlsDer = vi.getCrl().stream().map(Base64.getDecoder()::decode).toList();

        byte[] ltvPdf = PdfLtvEnhancer.addLtDss(signedPdf, certsDer, ocspsDer, crlsDer);
        return EtsiResponseMapper.toUiResponse(resp, cms.length, ltvPdf);
    }

    private static void requirePdf(MultipartFile pdf) {
        Objects.requireNonNull(pdf, "Missing PDF");
        if (pdf.isEmpty()) {
            throw new IllegalArgumentException("PDF is empty");
        }
    }

    private static Mono<byte[]> readPdfBytes(MultipartFile pdf) {
        return Mono.fromCallable(pdf::getBytes)
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(e -> new IllegalArgumentException("Failed to read PDF", e));
    }

    private void persistInitialSession(ParStartContext ctx, DocumentSigningContext document, String digestB64) {
        sessions.put(new SigningSession(
                ctx.state(),
                ctx.nonce(),
                digestB64,
                ctx.hashAlgorithm(),
                null, // sadJwt will be set after token exchange
                document
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

    static List<byte[]> extractCertsDerFromCms(byte[] cms) throws Exception {
        CMSSignedData sd = new CMSSignedData(cms);

        Store<X509CertificateHolder> store = sd.getCertificates();
        Collection<X509CertificateHolder> certs = store.getMatches(null);

        List<byte[]> out = new ArrayList<>();
        for (X509CertificateHolder h : certs) {
            out.add(h.getEncoded()); // DER
        }
        return out;
    }
}