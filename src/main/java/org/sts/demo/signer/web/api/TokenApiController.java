package org.sts.demo.signer.web.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sts.demo.signer.signing.SigningOrchestrationService;
import org.sts.demo.signer.signing.token.TokenExchangeRequest;
import org.sts.demo.signer.signing.token.TokenExchangeResponse;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class TokenApiController {
    private final SigningOrchestrationService signing;

    public TokenApiController(SigningOrchestrationService signing) {
        this.signing = signing;
    }

    @PostMapping(
            path = "/token",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<TokenExchangeResponse> token(@RequestBody TokenExchangeRequest req) {
        return signing.exchangeAuthCodeForAccessToken(req);
    }
}
