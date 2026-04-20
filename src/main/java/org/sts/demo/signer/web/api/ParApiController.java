package org.sts.demo.signer.web.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.sts.demo.signer.signing.ParStartService;
import org.sts.demo.signer.signing.TokenService;
import org.sts.demo.signer.web.dto.ParStartResponse;
import org.sts.demo.signer.signing.domain.SigningJourney;
import org.sts.demo.signer.web.dto.TokenExchangeRequest;
import org.sts.demo.signer.web.dto.TokenExchangeResponse;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class ParApiController {
    private final ParStartService parStartService;
    private final TokenService tokenService;

    public ParApiController(ParStartService parStartService, TokenService tokenService) {
        this.parStartService = parStartService;
        this.tokenService = tokenService;
    }

    @PostMapping(
            path="/par/auth",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ParStartResponse> par(
            @RequestPart("pdf") MultipartFile pdf,
            @RequestParam("journey") SigningJourney journey
    ) {
        return parStartService.startParAuth(pdf, journey);
    }

    @PostMapping(
            path = "/par/token",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<TokenExchangeResponse> token(@RequestBody TokenExchangeRequest req) {
        return tokenService.exchangeAuthCodeForAccessToken(req);
    }
}
