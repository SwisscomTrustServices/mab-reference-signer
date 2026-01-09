package org.sts.demo.signer.config;

import org.openapi.api.OidcApi;
import org.openapi.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class QtspPublicHttpConfig {
    @Bean
    WebClient qtspPublicWebClient(@Value("${qtsp.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public ApiClient qtspPublicApiClient(
            WebClient qtspPublicWebClient,
            @Value("${qtsp.base-url}") String baseUrl
    ) {
        ApiClient apiClient = new ApiClient(qtspPublicWebClient);
        apiClient.setBasePath(baseUrl);

        apiClient.addDefaultHeader("Accept", "application/json");

        return apiClient;
    }

    @Bean
    public OidcApi oidcApi(ApiClient qtspPublicApiClient) {
        return new OidcApi(qtspPublicApiClient);
    }
}
