package org.sts.demo.signer.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.openapi.api.OidcApi;
import org.openapi.invoker.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.sts.demo.signer.QtspProperties;
import reactor.netty.http.client.HttpClient;

import java.io.*;

@Configuration
public class QtspMtlsHttpConfig {

    private static final Logger log =
            LoggerFactory.getLogger(QtspMtlsHttpConfig.class);

    @Bean(name = "qtspMtlsWebClient")
    @Qualifier("qtspMtlsWebClient")
    WebClient qtspMtlsWebClient(
            QtspProperties props,
            ResourceLoader resourceLoader
    ) throws Exception {

        Resource certResource = resourceLoader.getResource(props.getMtls().getClientCert());
        Resource keyResource = resourceLoader.getResource(props.getMtls().getClientKey());

        File certFile = copyToTempFile(certResource, "qtsp-cert", ".pem");
        File keyFile = copyToTempFile(keyResource, "qtsp-key", ".key");

        SslContext nettySslContext = SslContextBuilder.forClient()
                .protocols("TLSv1.2")
                .keyManager(certFile, keyFile, null) // key password if encrypted
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(ssl -> ssl.sslContext(nettySslContext));

        return WebClient.builder()
                .baseUrl(props.getMtls().getBaseUrlAuth())
                .filter((req, next) -> {
                    log.info("[QTSP-mTLS bean] {} {}", req.method(), req.url());
                    return next.exchange(req);
                })
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private static File copyToTempFile(Resource resource, String prefix, String suffix) throws IOException {
        File tmp = File.createTempFile(prefix, suffix);
        tmp.deleteOnExit();
        try (InputStream in = resource.getInputStream();
             OutputStream out = new FileOutputStream(tmp)) {
            in.transferTo(out);
        }
        return tmp;
    }

    @Bean(name = "qtspMtlsApiClient")
    @Qualifier("qtspMtlsApiClient")
    ApiClient qtspMtlsApiClient(
            @Qualifier("qtspMtlsWebClient") WebClient mtlsWebClient,
            QtspProperties props
    ) {
        var apiClient = new ApiClient(mtlsWebClient);
        apiClient.setBasePath(props.getMtls().getBaseUrlAuth());
        apiClient.addDefaultHeader("Accept", "application/json");

        return apiClient;
    }

    @Bean(name = "qtspMtlsOidcApi")
    @Qualifier("qtspMtlsOidcApi")
    OidcApi qtspMtlsOidcApi(
            @Qualifier("qtspMtlsApiClient") ApiClient apiClient
    ) {
        return new OidcApi(apiClient);
    }
}