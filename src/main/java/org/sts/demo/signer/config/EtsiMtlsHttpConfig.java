package org.sts.demo.signer.config;

import io.netty.handler.ssl.SslContext;
import org.openapi.etsi.api.EtsiApi;
import org.openapi.etsi.invoker.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.File;

import static org.sts.demo.signer.config.mtls.NettySslContexts.mtlsClientTls12;
import static org.sts.demo.signer.config.mtls.PemMaterialLoader.toTempFile;

@Configuration
public class EtsiMtlsHttpConfig {

    private static final Logger log = LoggerFactory.getLogger(EtsiMtlsHttpConfig.class);

    @Bean(name = "etsiMtlsWebClient")
    WebClient etsiMtlsWebClient(
            QtspProperties props
    ) throws Exception {

        File certFile = toTempFile(props.getMtls().getClientCert(), "qtsp-cert", ".pem");
        File keyFile = toTempFile(props.getMtls().getClientKey(), "qtsp-key", ".key");

        SslContext nettySslContext = mtlsClientTls12(certFile, keyFile);

        HttpClient httpClient = HttpClient.create()
                .secure(ssl -> ssl.sslContext(nettySslContext));

        // IMPORTANT: baseUrl must be the *mTLS* host for ETSI
        return WebClient.builder()
                .filter((req, next) -> {
                    log.debug("[ETSI-mTLS bean] {} {}", req.method(), req.url());
                    return next.exchange(req);
                })
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean(name = "etsiMtlsApiClient")
    ApiClient etsiMtlsApiClient(@Qualifier("etsiMtlsWebClient") WebClient wc,
                                QtspProperties props) {
        ApiClient apiClient = new ApiClient(wc);
        apiClient.setBasePath(props.getMtls().getBaseUrl().toString()); // adjust if ETSI has different basePath
        apiClient.addDefaultHeader("Accept", "application/json");
        return apiClient;
    }

    @Bean(name = "etsiMtlsApi")
    EtsiApi etsiMtlsApi(@Qualifier("etsiMtlsApiClient") ApiClient apiClient) {
        return new EtsiApi(apiClient);
    }
}
