package org.sts.demo.signer.OidcDiscovery;

import org.openapi.model.ParResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.sts.demo.signer.MabService;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private static final Logger log =
            LoggerFactory.getLogger(DebugController.class);

    private final OidcDiscoveryClient discovery;
    private final MabService mabService;

    public DebugController(OidcDiscoveryClient discovery,
                           MabService mabService) {
        this.discovery = discovery;
        this.mabService = mabService;
    }

    @GetMapping("/oidc")
    public Object oidc() {
        return discovery.getConfig();
    }

    @GetMapping("/par")
    public void par() throws Exception {
        ParResponse response = mabService.pushPar().block();
        assert response != null;
        log.info(response.getRequestUri());
    }
}
