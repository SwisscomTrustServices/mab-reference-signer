package org.sts.demo.signer.signing;

import org.openapi.etsi.model.EtsiSignRequest;
import org.openapi.mab.model.AuthorizationCodeTokenRequest;
import org.openapi.mab.model.CreateParRequestClaims;
import org.openapi.mab.model.CreateParRequestClaimsDocumentDigestsInner;
import org.openapi.mab.model.TokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;
import org.sts.demo.signer.signing.etsi.EtsiSignClient;
import org.sts.demo.signer.signing.etsi.EtsiSignRequestFactory;
import org.sts.demo.signer.signing.etsi.EtsiSignStartRequest;
import org.sts.demo.signer.signing.etsi.EtsiSignStartResponse;
import org.sts.demo.signer.signing.par.ParClient;
import org.sts.demo.signer.signing.par.ParRequestFactory;
import org.sts.demo.signer.signing.par.ParStartResponse;
import org.sts.demo.signer.signing.token.TokenClient;
import org.sts.demo.signer.signing.token.TokenExchangeRequest;
import org.sts.demo.signer.signing.token.TokenExchangeResponse;
import org.sts.demo.signer.signing.token.TokenRequestFactory;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.sts.demo.signer.signing.mapping.HashAlgorithmMapper.fromMab;
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

    public Mono<ParStartResponse> pushPar(MultipartFile pdf) {
        return Mono.defer(() -> {
            if (pdf == null || pdf.isEmpty()) {
                return Mono.error(new IllegalArgumentException("Missing PDF"));
            }

            return Mono.fromCallable(pdf::getBytes)
                    .onErrorMap(e -> new IllegalArgumentException("Failed to read PDF", e))
                    .map(bytes -> {
                        String digestB64 = sha256Base64(bytes);

                        CreateParRequestClaims claims = new CreateParRequestClaims();
                        claims.setCredentialID(CreateParRequestClaims.CredentialIDEnum.ADVANCED4);
                        claims.setHashAlgorithmOID(CreateParRequestClaims.HashAlgorithmOIDEnum._1);
                        claims.setDocumentDigests(List.of(
                                new CreateParRequestClaimsDocumentDigestsInner()
                                        .hash(digestB64)
                                        .label("Document-1")
                        ));

                        return parRequestFactory.buildDemoRequest(claims); // ParStartContext
                    })
                    .flatMap(ctx -> parClient.send(ctx.request())
                            .flatMap(par -> {
                                var reqClaims = ctx.request().getClaims();
                                if (reqClaims == null) {
                                    return Mono.error(new IllegalStateException("PAR claims missing"));
                                }
                                var alg = reqClaims.getHashAlgorithmOID();
                                if (alg == null) {
                                    return Mono.error(new IllegalStateException("hashAlgorithmOID missing"));
                                }
                                var digests = reqClaims.getDocumentDigests();
                                if (digests == null || digests.isEmpty()) {
                                    return Mono.error(new IllegalStateException("documentDigests missing"));
                                }
                                var firstDigest = digests.getFirst();
                                if (firstDigest == null || firstDigest.getHash().isBlank()) {
                                    return Mono.error(new IllegalStateException("first documentDigest hash missing"));
                                }

                                sessions.put(new SigningSession(
                                        ctx.state(),
                                        ctx.nonce(),
                                        ctx.clientSessionId(),
                                        pdf.getOriginalFilename(),
                                        firstDigest.getHash(),
                                        fromMab(alg)
                                ));

                                String redirectUrl = UriComponentsBuilder
                                        .fromUriString(endpoints.authorizationUri())
                                        .queryParam("client_id", props.getClient().getClientId().toString())
                                        .queryParam("redirect_uri", props.getClient().getRedirectUri())
                                        .queryParam("request_uri", par.getRequestUri())
                                        .queryParam("state", ctx.state())
                                        .queryParam("nonce", ctx.nonce())
                                        .queryParam("response_type", "code")
                                        .build(true)
                                        .toUriString();

                                return Mono.just(new ParStartResponse(
                                        redirectUrl, ctx.state(), ctx.nonce(), ctx.clientSessionId()
                                ));
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

                            // 1) code -> access token
                            AuthorizationCodeTokenRequest authReq = tokenRequestFactory.buildAuthCode(in.code());

                            return tokenClient.exchange(authReq)
                                    .map(this::toTokenExchangeResponse);
                        })
        );
    }

    private TokenExchangeResponse toTokenExchangeResponse(TokenResponse tr) {
        return new TokenExchangeResponse(
                tr.getAccessToken(),
                tr.getTokenType(),
                tr.getExpiresIn(),
                tr.getScope().getValue(),
                tr.getRefreshToken()
        );
    }

    public Mono<EtsiSignStartResponse> signEtsi(EtsiSignStartRequest in) {
        return Mono.defer(() ->
                signingSessionValidator.validate(in.state(), in.nonce())
                        .flatMap(session -> {
                            if (in.sadJwt() == null || in.sadJwt().isBlank()) {
                                return Mono.error(new IllegalArgumentException("Missing SAD JWT"));
                            }
                            EtsiSignRequest req = etsiSignRequestFactory.build(session);

                            return etsiSignClient.sign(in.sadJwt(), req)   // adjust to your real method signature
                                    .map(EtsiSignStartResponse::new);
                        })
        );
    }
}