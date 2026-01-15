package org.sts.demo.signer.web.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.sts.demo.signer.signing.SigningOrchestrationService;
import org.sts.demo.signer.signing.api.ParStartResponse;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class ParApiController {
    private final SigningOrchestrationService signing;

    public ParApiController(SigningOrchestrationService signing) {
        this.signing = signing;
    }

    @PostMapping(
            path="/par",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ParStartResponse> par(
            @RequestPart("pdf") MultipartFile pdf,
            @RequestPart("credentialId") String credentialId
    ) {
        return signing.pushPar(pdf, credentialId);
    }
}
