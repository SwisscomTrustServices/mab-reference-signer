package org.sts.demo.signer;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

@ConfigurationProperties(prefix = "qtsp")
public class QtspProperties {
    private String baseUrl;
    private Oidc oidc = new Oidc();
    private Client client = new Client();
    private Mtls mtls = new Mtls();

    public static class Oidc {
        private String discoveryPath;
        public String getDiscoveryPath() { return discoveryPath; }
        public void setDiscoveryPath(String discoveryPath) { this.discoveryPath = discoveryPath; }
    }

    public static class Client {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        public UUID getClientId() { return UUID.fromString(clientId); }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public UUID getClientSecret() { return UUID.fromString(clientSecret); }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    }

    public static class Mtls {
        private String baseUrlAuth;
        private String clientCert;
        private String clientKey;
        public String getBaseUrlAuth() { return baseUrlAuth; }
        public void setBaseUrlAuth(String baseUrlAuth) { this.baseUrlAuth = baseUrlAuth; }
        public String getClientCert() { return clientCert; }
        public void setClientCert(String clientCert) { this.clientCert = clientCert; }
        public String getClientKey() { return clientKey; }
        public void setClientKey(String clientKey) { this.clientKey = clientKey; }
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Oidc getOidc() { return oidc; }
    public void setOidc(Oidc oidc) { this.oidc = oidc; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public Mtls getMtls() { return mtls; }
    public void setMtls(Mtls mtls) { this.mtls = mtls; }
}
