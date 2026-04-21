package org.sts.demo.signer.web.api;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.sts.demo.signer.signing.WebfingerService;
import org.sts.demo.signer.signing.CibaStartService;
import org.sts.demo.signer.signing.TokenService;
import org.sts.demo.signer.web.dto.CibaStartResponse;
import org.sts.demo.signer.web.dto.CibaTokenPollResponse;
import org.sts.demo.signer.web.dto.CibaTokenPollRequest;
import org.sts.demo.signer.web.dto.CibaWebfingerResponse;
import org.sts.demo.signer.signing.domain.SigningJourney;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class CibaApiController {
    private final TokenService tokenService;
    private final CibaStartService cibaStartService;
    private final WebfingerService webfingerService;

    public CibaApiController(TokenService tokenService,
                             CibaStartService cibaStartService,
                             WebfingerService webfingerService) {
        this.tokenService = tokenService;
        this.cibaStartService = cibaStartService;
        this.webfingerService = webfingerService;
    }

    @PostMapping(
            path = "/ciba/auth",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<CibaStartResponse> cibaAuth(
            @RequestPart(value = "pdf", required = false) MultipartFile pdf,
            @RequestParam("identifier") String identifier,
            @RequestParam("journey") SigningJourney journey
    ) {
        return cibaStartService.startCibaAuth(pdf, journey, identifier);
    }

    @GetMapping(
            path = "/ciba/webfinger",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<CibaWebfingerResponse> cibaWebfinger(
            @RequestParam("identifier") String identifier,
            @RequestParam("journey") SigningJourney journey
    ) {
        return webfingerService.checkIdentifier(identifier, journey);
    }

    @PostMapping(
            path = "/ciba/token",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<CibaTokenPollResponse> cibaToken(@Valid @RequestBody CibaTokenPollRequest req) {
        return tokenService.pollCibaToken(req);
    }
}