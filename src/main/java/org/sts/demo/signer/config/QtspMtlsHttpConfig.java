package org.sts.demo.signer.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.openapi.api.OidcApi;
import org.openapi.invoker.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.*;

@Configuration
public class QtspMtlsHttpConfig {

    private static final Logger log =
            LoggerFactory.getLogger(QtspMtlsHttpConfig.class);

    @Bean(name = "qtspMtlsWebClient")
    @Qualifier("qtspMtlsWebClient")
    WebClient qtspMtlsWebClient(
            @Value("${qtsp.base-url-auth}") String baseUrl
    ) throws Exception {

        File certFile = copyClasspathToTempFile("test-client.pem", "qtsp-cert", ".pem");
        File keyFile  = copyClasspathToTempFile("test-client.key", "qtsp-key", ".key");

        SslContext nettySslContext = SslContextBuilder.forClient()
                .protocols("TLSv1.2")
                .keyManager(certFile, keyFile, null) // key password if encrypted
                .build();

        HttpClient httpClient = HttpClient.create()
                .wiretap(true)
                .secure(ssl -> ssl.sslContext(nettySslContext));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .filter((req, next) -> {
                    log.info("[QTSP-mTLS bean] {} {}", req.method(), req.url());
                    return next.exchange(req);
                })
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private static File copyClasspathToTempFile(String resource, String prefix, String suffix) throws IOException {
        try (InputStream in = new ClassPathResource(resource).getInputStream()) {
            File tmp = File.createTempFile(prefix, suffix);
            tmp.deleteOnExit();
            try (OutputStream out = new FileOutputStream(tmp)) {
                in.transferTo(out);
            }
            return tmp;
        }
    }

    @Bean(name = "qtspMtlsApiClient")
    @Qualifier("qtspMtlsApiClient")
    ApiClient qtspMtlsApiClient(
            @Qualifier("qtspMtlsWebClient") WebClient mtlsWebClient,
            @Value("${qtsp.base-url-auth}") String baseUrl
    ) {
        var apiClient = new ApiClient(mtlsWebClient);
        apiClient.setBasePath(baseUrl);
        apiClient.addDefaultHeader("Accept", "application/json");

        // ✅ important: affects the OpenAPI-generated client serialization
        apiClient.getObjectMapper().setDefaultPropertyInclusion(
                JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_EMPTY)
        );

        // prove it
        log.info("ApiClient ObjectMapper inclusion = {}",
                apiClient.getObjectMapper().getSerializationConfig().getDefaultPropertyInclusion());

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