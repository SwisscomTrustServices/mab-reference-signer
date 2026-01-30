package org.sts.demo.signer.signing.domain;

import java.io.IOException;

public interface DocumentSigningContext extends AutoCloseable {
    byte[] getContentToSign();

    byte[] embedCms(byte[] cmsSignature) throws IOException;

    @Override
    void close();
}
