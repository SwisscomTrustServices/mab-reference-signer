package org.sts.demo.signer.config.mtls;

import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.file.Files;

public final class PemMaterialLoader {
    private PemMaterialLoader() {}

    public static File toTempFile(Resource resource, String prefix, String suffix) throws IOException {
        File tmp = Files.createTempFile(prefix, suffix).toFile();
        tmp.deleteOnExit();

        try (InputStream in = resource.getInputStream();
             OutputStream out = new FileOutputStream(tmp)) {
            in.transferTo(out);
        }
        return tmp;
    }
}
