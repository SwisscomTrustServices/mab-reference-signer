package org.sts.demo.signer.signing.pdf;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class PdfLtvEnhancerTest {

    @Test
    void addLtDss_shouldAddDssAndVriForLastSignature() throws Exception {
        byte[] signedPdf = readResource("/pdfs/signed.pdf");
        assertNotNull(signedPdf);
        assertTrue(signedPdf.length > 1000);

        List<byte[]> certsDer = List.of(new byte[]{0x30, 0x03, 0x02, 0x01, 0x01}); // dummy DER-ish bytes
        List<byte[]> ocspsDer = List.of(new byte[]{0x30, 0x03, 0x02, 0x01, 0x02});
        List<byte[]> crlsDer  = List.of(new byte[]{0x30, 0x03, 0x02, 0x01, 0x03});

        byte[] out = PdfLtvEnhancer.addLtDss(signedPdf, certsDer, ocspsDer, crlsDer);

        assertNotNull(out);
        assertTrue(out.length > signedPdf.length, "Output should grow (incremental update appended)");

        try (PDDocument doc = PDDocument.load(out)) {

            COSDictionary catalog = doc.getDocumentCatalog().getCOSObject();
            COSDictionary dss = asDict(catalog.getDictionaryObject(COSName.getPDFName("DSS")));
            assertNotNull(dss, "Catalog must contain /DSS after LTV enrichment");

            assertNotNull(asArray(dss.getDictionaryObject(COSName.getPDFName("Certs"))), "DSS must contain /Certs");
            assertNotNull(asArray(dss.getDictionaryObject(COSName.getPDFName("OCSPs"))), "DSS must contain /OCSPs");
            assertNotNull(asArray(dss.getDictionaryObject(COSName.getPDFName("CRLs"))), "DSS must contain /CRLs");
            COSDictionary vri = asDict(dss.getDictionaryObject(COSName.getPDFName("VRI")));
            assertNotNull(vri, "DSS must contain /VRI");

            List<PDSignature> sigs = doc.getSignatureDictionaries();
            assertFalse(sigs.isEmpty(), "Test PDF must contain at least one signature");
            PDSignature lastSig = sigs.getLast();

            byte[] contents = lastSig.getContents(out);
            assertNotNull(contents);
            assertTrue(contents.length > 0);

            String vriKeyHex = sha1Hex(contents).toUpperCase(Locale.ROOT);
            COSName vriKey = COSName.getPDFName(vriKeyHex);

            COSDictionary perSig = asDict(vri.getDictionaryObject(vriKey));
            assertNotNull(perSig, "VRI must contain entry keyed by SHA-1(/Contents)");

            assertNotNull(asArray(perSig.getDictionaryObject(COSName.getPDFName("Cert"))), "VRI entry must have /Cert");
            assertNotNull(asArray(perSig.getDictionaryObject(COSName.getPDFName("OCSP"))), "VRI entry must have /OCSP");
        }

        String tail = new String(out, Math.max(0, out.length - 4096), Math.min(4096, out.length), java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(tail.contains("/Prev") || tail.contains("startxref"), "Expected incremental trailer/xref near EOF");
    }

    private static byte[] readResource(String path) throws Exception {
        try (InputStream is = PdfLtvEnhancerTest.class.getResourceAsStream(path)) {
            assertNotNull(is, "Missing test resource: " + path);
            return is.readAllBytes();
        }
    }

    private static COSDictionary asDict(COSBase b) { return (b instanceof COSDictionary) ? (COSDictionary) b : null; }
    private static COSArray asArray(COSBase b) { return (b instanceof COSArray) ? (COSArray) b : null; }

    private static String sha1Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] dig = md.digest(data);
        StringBuilder sb = new StringBuilder(dig.length * 2);
        for (byte x : dig) sb.append(String.format("%02x", x));
        return sb.toString();
    }
}