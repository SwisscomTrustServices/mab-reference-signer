package org.sts.demo.signer.signing.pdf;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.sts.demo.signer.signing.domain.DocumentSigningContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PdfSigningContext implements DocumentSigningContext {
    private final PDDocument document;
    private final ByteArrayOutputStream incrementalOut;
    private final ExternalSigningSupport signingSupport;
    private final byte[] contentToSign;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private static final int DEFAULT_SIGNATURE_SIZE = 50000;

    private PdfSigningContext(PDDocument document,
                                   ByteArrayOutputStream incrementalOut,
                                   ExternalSigningSupport signingSupport,
                                   byte[] contentToSign) {
        this.document = document;
        this.incrementalOut = incrementalOut;
        this.signingSupport = signingSupport;
        this.contentToSign = contentToSign;
    }

    @Override
    public byte[] getContentToSign() {
        return contentToSign;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { document.close(); } catch (Exception ignored) {}
        try { incrementalOut.close(); } catch (Exception ignored) {}
    }

    public static PdfSigningContext prepare(byte[] pdfBytes) throws IOException {
        PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdfBytes));
        try {
            PDSignature sig = new PDSignature();
            Calendar signDate = Calendar.getInstance();
            signDate.add(Calendar.MINUTE, 3);

            sig.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            sig.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);
            sig.setSignDate(signDate);
            sig.setName("Demo Signer");
            sig.setReason("Demo Signature");

            SignatureOptions options = new SignatureOptions();
            options.setPreferredSignatureSize(DEFAULT_SIGNATURE_SIZE);

            doc.addSignature(sig, options);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ExternalSigningSupport ext = doc.saveIncrementalForExternalSigning(baos);

            byte[] contentToSign;
            try (InputStream is = ext.getContent()) {
                contentToSign = IOUtils.toByteArray(is);
            } finally {
                options.close();
            }

            return new PdfSigningContext(doc, baos, ext, contentToSign);
        }
        catch (Exception e) {
            try { doc.close(); } catch (Exception ignored) {}
            throw e;
        }
    }

    @Override
    public byte[] embedCms(byte[] cmsSignature) throws IOException {
        if (closed.get()) throw new IllegalStateException("PdfSigningContext is closed");
        signingSupport.setSignature(cmsSignature);
        byte[] out = incrementalOut.toByteArray();
        close();
        return out;
    }
}

