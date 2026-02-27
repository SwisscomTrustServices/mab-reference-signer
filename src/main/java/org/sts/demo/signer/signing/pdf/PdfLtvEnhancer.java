package org.sts.demo.signer.signing.pdf;

import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class PdfLtvEnhancer {

    private PdfLtvEnhancer() {}

    // DSS + VRI structure keys
    private static final COSName DSS   = COSName.getPDFName("DSS");
    private static final COSName VRI   = COSName.getPDFName("VRI");
    private static final COSName CERTS = COSName.getPDFName("Certs");
    private static final COSName OCSPS = COSName.getPDFName("OCSPs");
    private static final COSName CRLS  = COSName.getPDFName("CRLs");

    // Per-signature VRI entry keys
    private static final COSName CERT  = COSName.getPDFName("Cert");
    private static final COSName CERTS_VRI = COSName.getPDFName("Certs"); // interop hardening
    private static final COSName OCSP  = COSName.getPDFName("OCSP");
    private static final COSName CRL   = COSName.getPDFName("CRL");

    public static byte[] addLtDss(byte[] signedPdf,
                                  List<byte[]> certsDer,
                                  List<byte[]> ocspsDer,
                                  List<byte[]> crlsDer) throws Exception {

        Objects.requireNonNull(signedPdf, "signedPdf");

        List<byte[]> certs = certsDer == null ? List.of() : certsDer;
        List<byte[]> ocsps = ocspsDer == null ? List.of() : ocspsDer;
        List<byte[]> crls  = crlsDer  == null ? List.of() : crlsDer;

        try (PDDocument doc = PDDocument.load(signedPdf)) {

            // Bind VRI to the newest (last) signature.
            List<PDSignature> sigs = doc.getSignatureDictionaries();
            if (sigs.isEmpty()) throw new IllegalStateException("No signatures found in PDF");
            PDSignature sig = sigs.getLast();

            byte[] contents = sig.getContents(signedPdf);
            if (contents == null || contents.length == 0)
                throw new IllegalStateException("Signature /Contents not found / empty");

            COSName vriKey = COSName.getPDFName(sha1Hex(contents).toUpperCase(Locale.ROOT));

            PDDocumentCatalog pdCatalog = doc.getDocumentCatalog();
            COSDictionary catalog = pdCatalog.getCOSObject();

            // DSS dictionary (create if missing)
            COSDictionary dss = asDict(catalog.getDictionaryObject(DSS));
            if (dss == null) {
                dss = new COSDictionary();
                catalog.setItem(DSS, dss);
            }

            COSArray dssCerts = getOrCreateArray(dss, CERTS);
            COSArray dssOcsps = getOrCreateArray(dss, OCSPS);
            COSArray dssCrls  = getOrCreateArray(dss, CRLS);

            COSDictionary vri = asDict(dss.getDictionaryObject(VRI));
            if (vri == null) {
                vri = new COSDictionary();
                dss.setItem(VRI, vri);
            }

            // Add embedded streams and build per-signature arrays referencing those same streams
            COSArray vriCertArr = new COSArray();
            for (byte[] der : certs) {
                if (der == null || der.length == 0) continue;
                COSStream s = createEmbeddedStream(doc, der);
                dssCerts.add(s);
                vriCertArr.add(s);
            }

            COSArray vriOcspArr = new COSArray();
            for (byte[] der : ocsps) {
                if (der == null || der.length == 0) continue;
                COSStream s = createEmbeddedStream(doc, der);
                dssOcsps.add(s);
                vriOcspArr.add(s);
            }

            COSArray vriCrlArr = new COSArray();
            for (byte[] der : crls) {
                if (der == null || der.length == 0) continue;
                COSStream s = createEmbeddedStream(doc, der);
                dssCrls.add(s);
                vriCrlArr.add(s);
            }

            COSDictionary perSig = asDict(vri.getDictionaryObject(vriKey));
            if (perSig == null) perSig = new COSDictionary();

            // Interop hardening: set both /Cert and /Certs
            perSig.setItem(CERT, vriCertArr);
            perSig.setItem(CERTS_VRI, vriCertArr);

            perSig.setItem(OCSP, vriOcspArr);
            if (vriCrlArr.size() > 0) perSig.setItem(CRL, vriCrlArr);

            vri.setItem(vriKey, perSig);

            // Ensure incremental update includes the updated root and DSS graph
            doc.getDocument().getTrailer().setNeedToBeUpdated(true);
            catalog.setNeedToBeUpdated(true);
            dss.setNeedToBeUpdated(true);
            vri.setNeedToBeUpdated(true);
            perSig.setNeedToBeUpdated(true);

            // Debug-only (you can remove once stable)
            // doc.getDocument().setIsXRefStream(false);

            ByteArrayOutputStream out = new ByteArrayOutputStream(signedPdf.length + 128_000);
            doc.saveIncremental(out);
            return out.toByteArray();
        }
    }

    private static COSArray getOrCreateArray(COSDictionary dict, COSName key) {
        COSArray arr = asArray(dict.getDictionaryObject(key));
        if (arr == null) {
            arr = new COSArray();
            dict.setItem(key, arr);
        }
        return arr;
    }

    static COSDictionary asDict(COSBase b) { return (b instanceof COSDictionary) ? (COSDictionary) b : null; }
    static COSArray asArray(COSBase b) { return (b instanceof COSArray) ? (COSArray) b : null; }

    static String sha1Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] dig = md.digest(data);
        StringBuilder sb = new StringBuilder(dig.length * 2);
        for (byte x : dig) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static COSStream createEmbeddedStream(PDDocument doc, byte[] der) throws IOException {
        COSStream stream = doc.getDocument().createCOSStream();
        try (OutputStream os = stream.createOutputStream()) {
            os.write(der);
        }
        return stream;
    }
}