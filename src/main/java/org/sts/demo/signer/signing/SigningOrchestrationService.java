package org.sts.demo.signer.signing;

import org.openapi.mab.model.AuthorizationCodeTokenRequest;
import org.openapi.mab.model.CreateParRequestClaims;
import org.openapi.mab.model.CreateParRequestClaimsDocumentDigestsInner;
import org.openapi.mab.model.TokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.sts.demo.signer.config.QtspProperties;
import org.sts.demo.signer.oidc.endpoints.OidcEndpoints;
import org.sts.demo.signer.signing.par.ParClient;
import org.sts.demo.signer.signing.par.ParRequestFactory;
import org.sts.demo.signer.signing.par.ParStartResponse;
import org.sts.demo.signer.signing.token.*;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.sts.demo.signer.signing.util.DigestUtils.sha384Base64;

@Service
public class SigningOrchestrationService {

    private final ParRequestFactory parRequestFactory;
    private final ParClient parClient;
    private final OidcEndpoints endpoints;
    private final QtspProperties props;
    private final SigningSessionStore sessions;
    private final TokenRequestFactory tokenRequestFactory;
    private final TokenClient tokenClient;

    public SigningOrchestrationService(ParRequestFactory parRequestFactory,
                                       ParClient parClient,
                                       OidcEndpoints endpoints,
                                       QtspProperties props,
                                       SigningSessionStore sessions,
                                       TokenRequestFactory tokenRequestFactory,
                                       TokenClient tokenClient) {
        this.parRequestFactory = parRequestFactory;
        this.parClient = parClient;
        this.endpoints = endpoints;
        this.props = props;
        this.sessions = sessions;
        this.tokenRequestFactory = tokenRequestFactory;
        this.tokenClient = tokenClient;
    }

    public Mono<ParStartResponse> pushPar(MultipartFile pdf) {
        return Mono.fromCallable(pdf::getBytes)
                .onErrorMap(e -> new IllegalArgumentException("Failed to read PDF", e))
                .map(bytes -> {
                    String digestB64 = sha384Base64(bytes);

                    CreateParRequestClaims claims = new CreateParRequestClaims();
                    claims.setCredentialID(CreateParRequestClaims.CredentialIDEnum.ADVANCED4);
                    claims.setHashAlgorithmOID(CreateParRequestClaims.HashAlgorithmOIDEnum._2);
                    claims.setDocumentDigests(List.of(
                            new CreateParRequestClaimsDocumentDigestsInner()
                                    .hash(digestB64)
                                    .label("Document-1")
                    ));

                    return parRequestFactory.buildDemoRequest(claims); // ParStartContext
                })
                .flatMap(ctx -> parClient.send(ctx.request())
                        .map(par -> {
                            // store session keyed by state
                            sessions.put(new SigningSession(
                                    ctx.state(), ctx.nonce(), ctx.clientSessionId(), pdf.getOriginalFilename()
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

                            return new ParStartResponse(redirectUrl, ctx.state(), ctx.nonce(), ctx.clientSessionId());
                        })
                );
    }

    public Mono<TokenExchangeResponse> exchangeToken(TokenExchangeRequest in) {
        return Mono.just(in)
                .<TokenContext>handle((req, sink) -> {
                    if (req.code() == null || req.code().isBlank()) {
                        sink.error(new IllegalArgumentException("Missing authorization code"));
                        return;
                    }
                    if (req.state() == null || req.state().isBlank()) {
                        sink.error(new IllegalArgumentException("Missing state"));
                        return;
                    }

                    SigningSession session = sessions.get(req.state());
                    if (session == null) {
                        sink.error(new IllegalArgumentException("Unknown/expired state"));
                        return;
                    }

                    AuthorizationCodeTokenRequest qtspReq = tokenRequestFactory.build(req);
                    sink.next(new TokenContext(session, qtspReq));
                })
                .flatMap(ctx -> tokenClient.exchange(ctx.request())
                        .map(this::toTokenExchangeResponse)
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
}