package org.sts.demo.signer.signing.util;

import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;

public final class PdfInputUtils {

    public static void requirePdf(MultipartFile pdf) {
        Objects.requireNonNull(pdf, "Missing PDF");
        if (pdf.isEmpty()) {
            throw new IllegalArgumentException("PDF is empty");
        }
    }

    public static Mono<byte[]> readPdfBytes(MultipartFile pdf) {
        return Mono.fromCallable(pdf::getBytes)
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(e -> new IllegalArgumentException("Failed to read PDF", e));
    }
}