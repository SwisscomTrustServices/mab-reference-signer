package org.sts.demo.signer.signing;

import org.openapi.mab.model.AuthorizationCodeTokenRequest;
import org.openapi.mab.model.OauthTokenRequest;
import org.openapi.mab.model.TokenResponse;
import org.springframework.stereotype.Service;
import org.sts.demo.signer.signing.domain.SigningSession;
import org.sts.demo.signer.signing.domain.SigningSessionStore;
import org.sts.demo.signer.signing.domain.SigningSessionValidator;
import org.sts.demo.signer.signing.mab.token.TokenClient;
import org.sts.demo.signer.signing.mab.token.TokenRequestFactory;
import org.sts.demo.signer.web.dto.CibaTokenPollResponse;
import org.sts.demo.signer.web.dto.CibaTokenPollRequest;
import org.sts.demo.signer.web.dto.TokenExchangeRequest;
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

    public Mono<CibaTokenPollResponse> pollCibaToken(CibaTokenPollRequest in) {
        return Mono.defer(() -> {
            SigningSession session = resolveSessionForCibaPolling(in);
            UUID authReqId = parseUuid(requireNonBlank(in.authReqId(), "Missing authReqId"));
            OauthTokenRequest req = tokenRequestFactory.buildCiba(authReqId);

            return tokenClient.pollCibaToken(req)
                    .map(tr -> new CibaTokenPollResponse(
                            "READY",
                            toCibaTokenExchangeResponse(session, tr.getAccessToken()),
                            CIBA_POLL_INTERVAL_SECONDS,
                            null
                    ))
                    .onErrorResume(TokenClient.CibaAuthorizationPendingException.class,
                            ex -> Mono.just(new CibaTokenPollResponse(
                                    "PENDING",
                                    null,
                                    CIBA_POLL_INTERVAL_SECONDS,
                                    "authorization_pending"
                            )));
        });
    }

    private SigningSession resolveSessionForCibaPolling(CibaTokenPollRequest in) {
        String state = in.state();
        String nonce = in.nonce();

        boolean hasState = state != null && !state.isBlank();
        boolean hasNonce = nonce != null && !nonce.isBlank();
        if (!hasState && !hasNonce) {
            return null;
        }
        if (!hasState || !hasNonce) {
            throw new IllegalArgumentException("Both state and nonce are required when one is provided");
        }
        return signingSessionValidator.validateIfPresent(state, nonce);
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid authReqId", e);
        }
    }

    private static TokenExchangeResponse toTokenExchangeResponse(String sadJwt, TokenResponse tr) {
        return new TokenExchangeResponse(
                redactJwt(sadJwt),
                tr.getTokenType(),
                tr.getExpiresIn(),
                tr.getScope().getValue()
        );
    }

    private TokenExchangeResponse toCibaTokenExchangeResponse(SigningSession session, String sadJwtRaw) {
        String sadJwt = requireNonBlank(sadJwtRaw, "CIBA token response missing access_token");
        if (session != null) {
            sessions.remove(session.state());
            sessions.put(session.withSadJwt(sadJwt));
        }
        return new TokenExchangeResponse(redactJwt(sadJwt), null, null, null);
    }
}