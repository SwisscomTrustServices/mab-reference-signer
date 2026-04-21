package org.sts.demo.signer.signing;

import org.openapi.mab.model.AuthorizationCodeTokenRequest;
import org.openapi.mab.model.OauthTokenRequest;
import org.openapi.mab.model.TokenResponse;
import org.springframework.stereotype.Service;
import org.sts.demo.signer.signing.domain.SigningSessionStore;
import org.sts.demo.signer.signing.domain.SigningSessionValidator;
import org.sts.demo.signer.signing.mab.token.TokenClient;
import org.sts.demo.signer.signing.mab.token.TokenRequestFactory;
import org.sts.demo.signer.web.dto.CibaTokenPollRequest;
import org.sts.demo.signer.web.dto.CibaTokenPollResponse;
import org.sts.demo.signer.web.dto.ParTokenExchangeRequest;
import org.sts.demo.signer.web.dto.TokenExchangeResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

import static org.sts.demo.signer.signing.util.Redactor.redactJwt;
import static org.sts.demo.signer.signing.util.ValidationUtils.requireNonBlank;

@Service
public class TokenService {

    private final SigningSessionStore sessions;
    private final TokenRequestFactory tokenRequestFactory;
    private final TokenClient tokenClient;
    private final SigningSessionValidator signingSessionValidator;

    private static final int CIBA_POLL_INTERVAL_SECONDS = 5;

    public TokenService(SigningSessionStore sessions,
                        TokenRequestFactory tokenRequestFactory,
                        TokenClient tokenClient,
                        SigningSessionValidator signingSessionValidator) {
        this.sessions = sessions;
        this.tokenRequestFactory = tokenRequestFactory;
        this.tokenClient = tokenClient;
        this.signingSessionValidator = signingSessionValidator;
    }

    public Mono<TokenExchangeResponse> exchangeAuthCodeForAccessToken(ParTokenExchangeRequest in) {
        return Mono.fromCallable(() -> signingSessionValidator.validateAndTake(in.state(), in.nonce()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(session -> {
                    String code = requireNonBlank(in.code(), "Missing authorization code");
                    AuthorizationCodeTokenRequest authReq = tokenRequestFactory.buildTokenExchangeRequest(code);
                    return tokenClient.exchange(authReq)
                            .map(tokenResponse -> {
                                String sadJwt = requireNonBlank(tokenResponse.getAccessToken(), "Token response missing access_token");
                                sessions.put(session.withSadJwt(sadJwt));
                                return toTokenExchangeResponse(sadJwt, tokenResponse);
                            });
                });
    }

    public Mono<CibaTokenPollResponse> pollCibaToken(CibaTokenPollRequest in) {
        return Mono.fromCallable(() -> signingSessionValidator.validate(in.state(), in.nonce()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(session -> {
                    UUID authReqId = UUID.fromString(requireNonBlank(in.authReqId(), "Missing authReqId"));
                    OauthTokenRequest req = tokenRequestFactory.buildTokenPollingRequest(authReqId);
                    return tokenClient.pollCibaToken(req)
                            .map(tokenSignResponse -> {
                                String sadJwt = requireNonBlank(tokenSignResponse.getAccessToken(), "Token response missing access_token");
                                sessions.put(session.withSadJwt(sadJwt));
                                return toCibaTokenPollResponse(sadJwt);
                            })
                            .onErrorResume(TokenClient.CibaAuthorizationPendingException.class,
                                    ex -> Mono.just(new CibaTokenPollResponse(
                                            "PENDING",
                                            null,
                                            CIBA_POLL_INTERVAL_SECONDS,
                                            "authorization_pending"
                                    )));
                });
    }

    private static TokenExchangeResponse toTokenExchangeResponse(String sadJwt, TokenResponse tr) {
        return new TokenExchangeResponse(
                redactJwt(sadJwt),
                tr.getTokenType(),
                tr.getExpiresIn(),
                tr.getScope().getValue()
        );
    }

    private CibaTokenPollResponse toCibaTokenPollResponse(String sadJwt) {
        return new CibaTokenPollResponse(
                "READY",
                redactJwt(sadJwt),
                CIBA_POLL_INTERVAL_SECONDS,
                null
        );
    }
}