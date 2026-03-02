package org.sts.demo.signer.signing.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;

class PdfSigningContextTest {

    private static byte[] minimalPdfBytes() throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void prepare_returnsNonEmptyContentToSign() throws Exception {
        byte[] inputPdf = minimalPdfBytes();

        try (PdfSigningContext ctx = PdfSigningContext.prepare(inputPdf)) {
            byte[] toSign = ctx.getContentToSign();
            assertNotNull(toSign);
            assertTrue(toSign.length > 0, "contentToSign must not be empty");
        }
    }

    @Test
    void embedCms_returnsLoadablePdfWithExpectedSignatureFields_andClosesContext() throws Exception {
        byte[] inputPdf = minimalPdfBytes();

        PdfSigningContext ctx = PdfSigningContext.prepare(inputPdf);

        // Dummy CMS bytes: PDFBox doesn't cryptographically validate CMS here; it just embeds.
        byte[] dummyCms = new byte[] { 0x30, 0x03, 0x02, 0x01, 0x00 };

        byte[] signedPdf = ctx.embedCms(dummyCms);
        assertNotNull(signedPdf);
        assertTrue(signedPdf.length > inputPdf.length, "signed PDF should be larger than the original");

        // Context should be closed after embedCms()
        assertThrows(IllegalStateException.class, () -> ctx.embedCms(dummyCms));

        try (PDDocument outDoc = PDDocument.load(signedPdf)) {
            var sigs = outDoc.getSignatureDictionaries();
            assertEquals(1, sigs.size(), "expected exactly one signature");

            PDSignature sig = sigs.get(0);

            // PDSignature#getFilter / getSubFilter return String
            assertEquals(PDSignature.FILTER_ADOBE_PPKLITE.getName(), sig.getFilter());
            assertEquals(PDSignature.SUBFILTER_ETSI_CADES_DETACHED.getName(), sig.getSubFilter());

            assertEquals("Demo Signer", sig.getName());
            assertEquals("Demo Signature", sig.getReason());

            Calendar signDate = sig.getSignDate();
            assertNotNull(signDate, "signDate must be set");
        }
    }

    @Test
    void close_isIdempotent_and_embedCmsAfterCloseThrows() throws Exception {
        byte[] inputPdf = minimalPdfBytes();

        PdfSigningContext ctx = PdfSigningContext.prepare(inputPdf);

        assertDoesNotThrow(ctx::close);
        assertDoesNotThrow(ctx::close, "close() should be idempotent");

        assertThrows(IllegalStateException.class, () -> ctx.embedCms(new byte[] {0x01, 0x02}));
    }

}