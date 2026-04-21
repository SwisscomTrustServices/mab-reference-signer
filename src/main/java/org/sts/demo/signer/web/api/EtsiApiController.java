package org.sts.demo.signer.web.api;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sts.demo.signer.signing.EtsiSignService;
import org.sts.demo.signer.web.dto.EtsiSignStartRequest;
import org.sts.demo.signer.web.dto.EtsiSignStartResponse;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class EtsiApiController {
    private final EtsiSignService etsiSignService;

    public EtsiApiController(EtsiSignService etsiSignService) {
        this.etsiSignService = etsiSignService;
    }

    @PostMapping(
            path = "/etsi/sign",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<EtsiSignStartResponse> etsiSign(@Valid @RequestBody EtsiSignStartRequest req) {
        return etsiSignService.signEtsi(req);
    }
}
