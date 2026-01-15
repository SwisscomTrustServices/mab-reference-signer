package org.sts.demo.signer.web.api;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sts.demo.signer.signing.SigningOrchestrationService;
import org.sts.demo.signer.signing.api.EtsiSignStartRequest;
import org.sts.demo.signer.signing.api.EtsiSignStartResponse;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class EtsiApiController {
    private final SigningOrchestrationService signing;

    public EtsiApiController(SigningOrchestrationService signing) {
        this.signing = signing;
    }

    @PostMapping(
            path = "/etsi/sign",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<EtsiSignStartResponse> sign(@Valid @RequestBody EtsiSignStartRequest req) {
        return signing.signEtsi(req);
    }
}
