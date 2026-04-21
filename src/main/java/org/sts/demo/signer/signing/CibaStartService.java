package org.sts.demo.signer.signing;

import org.openapi.mab.model.OauthAuthenticationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.sts.demo.signer.web.dto.CibaStartResponse;
import org.sts.demo.signer.signing.domain.SigningJourney;
import org.sts.demo.signer.signing.domain.SigningSessionStore;
import org.sts.demo.signer.signing.mab.ciba.CibaClient;
import org.sts.demo.signer.signing.mab.ciba.CibaRequestFactory;
import org.sts.demo.signer.signing.mab.ciba.CibaStartContext;
import org.sts.demo.signer.signing.pdf.PdfSigningContext;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.regex.Pattern;

import static org.sts.demo.signer.signing.mab.AuthPolicy.policyFor;
import static org.sts.demo.signer.signing.util.DigestUtils.sha256Base64;
import static org.sts.demo.signer.signing.util.PdfInputUtils.readPdfBytes;
import static org.sts.demo.signer.signing.util.PdfInputUtils.requirePdf;
import static org.sts.demo.signer.signing.util.ValidationUtils.requireNonBlank;

@Service
public class CibaStartService {

    private final CibaRequestFactory cibaRequestFactory;
    private final CibaClient cibaClient;
    private final SigningSessionStore sessions;

    public static final String SCOPE_SIGN = "sign";
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^\\+?[1-9]\\d{6,15}$");

    public CibaStartService(CibaRequestFactory cibaRequestFactory,
                            CibaClient cibaClient,
                            SigningSessionStore sessions) {
        this.cibaRequestFactory = cibaRequestFactory;
        this.cibaClient = cibaClient;
        this.sessions = sessions;
    }

    public Mono<CibaStartResponse> startCibaAuth(MultipartFile pdf, SigningJourney journey, String identifier) {
        String trimmedIdentifier = requireNonBlank(identifier, "Missing CIBA identifier");
        if (!IDENTIFIER_PATTERN.matcher(trimmedIdentifier).matches()) {
            return Mono.error(new IllegalArgumentException("Invalid CIBA identifier format (expected E.164-like number)"));
        }
        if (containsSignScope(journey)) {
            return startCibaSignAuth(pdf, journey, trimmedIdentifier);
        }
        return startCibaIdentAuth(journey, trimmedIdentifier);
    }

    public Mono<CibaStartResponse> startCibaIdentAuth(SigningJourney journey, String identifier) {
        var ctx = cibaRequestFactory.buildIdent(journey, identifier);
        return cibaClient.authenticate(ctx.request())
                .map(resp -> {
                    sessions.putInitial(
                            ctx.state(),
                            ctx.nonce(),
                            null,
                            null,
                            null,
                            null
                    );
                    return toCibaAuthStartResponse(ctx, resp);
                });
    }

    public Mono<CibaStartResponse> startCibaSignAuth(MultipartFile pdf, SigningJourney journey, String identifier) {
        requirePdf(pdf);
        return readPdfBytes(pdf)
                .flatMap(pdfBytes ->
                        Mono.fromCallable(() -> PdfSigningContext.prepare(pdfBytes))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(pdfSigningContext -> {
                                    try {
                                        byte[] toHash = pdfSigningContext.getContentToSign();
                                        String digestB64 = sha256Base64(toHash);
                                        var ctx = cibaRequestFactory.buildSign(journey, digestB64, identifier);
                                        return cibaClient.authenticate(ctx.request())
                                                .map(resp -> {
                                                    sessions.putInitial(
                                                            ctx.state(),
                                                            ctx.nonce(),
                                                            digestB64,
                                                            ctx.hashAlgorithm(),
                                                            ctx.credentialId(),
                                                            pdfSigningContext
                                                    );
                                                    return toCibaAuthStartResponse(ctx, resp);
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

    private static boolean containsSignScope(SigningJourney journey) {
        return policyFor(journey).scopes().contains(SCOPE_SIGN);
    }

    private static CibaStartResponse toCibaAuthStartResponse(CibaStartContext ctx, OauthAuthenticationResponse resp) {
        return new CibaStartResponse(
                requireNonBlank(resp.getAuthReqId().toString(), "CIBA response missing auth_req_id"),
                ctx.state(),
                ctx.nonce(),
                resp.getIdentProcessData()
        );
    }
}