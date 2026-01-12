package org.sts.demo.signer.config.mtls;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import java.io.File;

public final class NettySslContexts {
    private NettySslContexts() {}

    public static SslContext mtlsClientTls12(File certPem, File keyPem) throws Exception {
        return SslContextBuilder.forClient()
                .protocols("TLSv1.2")
                .keyManager(certPem, keyPem, null)
                .build();
    }
}
