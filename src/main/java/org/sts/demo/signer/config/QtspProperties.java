package org.sts.demo.signer.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.UUID;

@ConfigurationProperties(prefix = "qtsp")
@Validated
public class QtspProperties {

    @Valid @NotNull
    private Oidc oidc = new Oidc();

    @Valid @NotNull
    private Client client = new Client();

    @Valid
    @NotNull
    private Mtls mtls = new Mtls();

    public static class Oidc {
        @NotNull
        private URI discoveryPath;
        public URI getDiscoveryPath() { return discoveryPath; }
        public void setDiscoveryPath(URI discoveryPath) { this.discoveryPath = discoveryPath; }
    }

    public static class Client {
        @NotNull
        private UUID clientId;
        @NotNull
        private UUID clientSecret;
        @NotNull
        private URI redirectUri;

        public UUID getClientId() { return clientId; }
        public void setClientId(UUID clientId) { this.clientId = clientId; }

        public UUID getClientSecret() { return clientSecret; }
        public void setClientSecret(UUID clientSecret) { this.clientSecret = clientSecret; }

        public URI getRedirectUri() { return redirectUri; }
        public void setRedirectUri(URI redirectUri) { this.redirectUri = redirectUri; }
    }

    public static class Mtls {
        @NotNull
        private URI baseUrl;
        @NotNull
        private Resource clientCert;
        @NotNull
        private Resource clientKey;
        public URI getBaseUrl() { return baseUrl; }
        public void setBaseUrl(URI baseUrl) { this.baseUrl = baseUrl; }
        public Resource getClientCert() { return clientCert; }
        public void setClientCert(Resource clientCert) { this.clientCert = clientCert; }
        public Resource getClientKey() { return clientKey; }
        public void setClientKey(Resource clientKey) { this.clientKey = clientKey; }
    }

    public Oidc getOidc() { return oidc; }
    public void setOidc(Oidc oidc) { this.oidc = oidc; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public Mtls getMtls() { return mtls; }
    public void setMtls(Mtls mtls) { this.mtls = mtls; }
}
