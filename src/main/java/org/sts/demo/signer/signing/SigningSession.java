package org.sts.demo.signer.signing;

public record SigningSession (
    String state,
    String nonce,
    String clientSessionId,
    String pdfName
) {}
