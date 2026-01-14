package org.sts.demo.signer.config;

import org.openapi.mab.api.OidcApi;
import org.openapi.mab.invoker.ApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class MabPublicHttpConfig {

    @Bean(name = "qtspPublicWebClient")
    WebClient qtspPublicWebClient() {
        return WebClient.builder().build();
    }

    @Bean(name = "qtspPublicApiClient")
    public ApiClient qtspPublicApiClient(WebClient qtspPublicWebClient) {
        ApiClient apiClient = new ApiClient(qtspPublicWebClient);
        apiClient.addDefaultHeader("Accept", "application/json");
        return apiClient;
    }

    @Bean(name = "qtspPublicOidcApi")
    public OidcApi qtspPublicOidcApi(ApiClient qtspPublicApiClient) {
        return new OidcApi(qtspPublicApiClient);
    }
}
