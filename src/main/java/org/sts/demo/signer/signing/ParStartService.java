package org.sts.demo.signer.signing;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;
import org.sts.demo.signer.web.dto.ParStartResponse;
import org.sts.demo.signer.signing.domain.SigningJourney;
import org.sts.demo.signer.signing.domain.SigningSessionStore;
import org.sts.demo.signer.signing.mab.par.ParClient;
import org.sts.demo.signer.signing.mab.par.ParRequestFactory;
import org.sts.demo.signer.signing.mab.par.ParStartContext;
import org.sts.demo.signer.signing.pdf.PdfSigningContext;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.sts.demo.signer.signing.util.DigestUtils.sha256Base64;
import static org.sts.demo.signer.signing.util.PdfInputUtils.readPdfBytes;
import static org.sts.demo.signer.signing.util.PdfInputUtils.requirePdf;

@Service
public class ParStartService {

    private final ParRequestFactory parRequestFactory;
    private final ParClient parClient;
    private final OidcEndpoints endpoints;
    private final QtspProperties props;
    private final SigningSessionStore sessions;

    public ParStartService(ParRequestFactory parRequestFactory,
                           ParClient parClient,
                           OidcEndpoints endpoints,
                           QtspProperties props,
                           SigningSessionStore sessions) {
        this.parRequestFactory = parRequestFactory;
        this.parClient = parClient;
        this.endpoints = endpoints;
        this.props = props;
        this.sessions = sessions;
    }

    public Mono<ParStartResponse> startParAuth(MultipartFile pdf, SigningJourney journey) {
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
                                        return parClient.authenticate(ctx.request())
                                                .map(par -> {
                                                    sessions.putInitial(
                                                            ctx.state(),
                                                            ctx.nonce(),
                                                            digestB64,
                                                            ctx.hashAlgorithm(),
                                                            ctx.credentialId(),
                                                            pdfSigningContext
                                                    );
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
}