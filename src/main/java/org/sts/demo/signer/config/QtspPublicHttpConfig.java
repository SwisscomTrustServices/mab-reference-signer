package org.sts.demo.signer.config;

import org.openapi.api.OidcApi;
import org.openapi.invoker.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.sts.demo.signer.QtspProperties;

@Configuration
public class QtspPublicHttpConfig {

    private static final Logger log = LoggerFactory.getLogger(QtspPublicHttpConfig.class);

    @Bean
    WebClient qtspPublicWebClient(QtspProperties props) {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .build();
    }

    @Bean
    public ApiClient qtspPublicApiClient(
            WebClient qtspPublicWebClient,
            QtspProperties props
    ) {
        ApiClient apiClient = new ApiClient(qtspPublicWebClient);
        apiClient.setBasePath(props.getBaseUrl());
        apiClient.addDefaultHeader("Accept", "application/json");

        return apiClient;
    }

    @Bean
    public OidcApi oidcApi(ApiClient qtspPublicApiClient) {
        return new OidcApi(qtspPublicApiClient);
    }
}
