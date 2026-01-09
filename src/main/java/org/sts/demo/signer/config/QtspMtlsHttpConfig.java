package org.sts.demo.signer.config;

import io.netty.handler.ssl.SslContext;
import org.openapi.api.OidcApi;
import org.openapi.invoker.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.*;

import static org.sts.demo.signer.crypto.NettySslContexts.mtlsClientTls12;
import static org.sts.demo.signer.crypto.PemMaterialLoader.toTempFile;

@Configuration
public class QtspMtlsHttpConfig {

    private static final Logger log =
            LoggerFactory.getLogger(QtspMtlsHttpConfig.class);

    @Bean(name = "qtspMtlsWebClient")
    WebClient qtspMtlsWebClient(
            QtspProperties props,
            ResourceLoader resourceLoader
    ) throws Exception {

        File certFile = toTempFile(props.getMtls().getClientCert(), "qtsp-cert", ".pem");
        File keyFile = toTempFile(props.getMtls().getClientKey(), "qtsp-key", ".key");

        SslContext nettySslContext = mtlsClientTls12(certFile, keyFile);

        HttpClient httpClient = HttpClient.create()
                .secure(ssl -> ssl.sslContext(nettySslContext));

        return WebClient.builder()
                .baseUrl(props.getMtls().getBaseUrl().toString())
                .filter((req, next) -> {
                    log.debug("[QTSP-mTLS bean] {} {}", req.method(), req.url());
                    return next.exchange(req);
                })
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean(name = "qtspMtlsApiClient")
    ApiClient qtspMtlsApiClient(
            @Qualifier("qtspMtlsWebClient") WebClient mtlsWebClient,
            QtspProperties props
    ) {
        var apiClient = new ApiClient(mtlsWebClient);
        apiClient.setBasePath(props.getMtls().getBaseUrl().toString());
        apiClient.addDefaultHeader("Accept", "application/json");

        return apiClient;
    }

    @Bean(name = "qtspMtlsOidcApi")
    OidcApi qtspMtlsOidcApi(
            @Qualifier("qtspMtlsApiClient") ApiClient apiClient
    ) {
        return new OidcApi(apiClient);
    }
}