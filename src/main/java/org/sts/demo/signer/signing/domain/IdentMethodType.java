package org.sts.demo.signer.signing.domain;

public enum IdentMethodType {
    IDNOW_VIDEO("idnow-video"),
    INTRUM_VIDEO("intrum-video"),
    INTRUM_AUTO("intrum-auto"),
    FIDENTITY("fidentity"),
    FIDENTITY_NFC("fidentity_nfc"),
    FIDENTITY_AUTO("fidentity_auto"),
    SUMSUB_VIDEO("sumsub-video"),
    SUMSUB_AUTO("sumsub-auto");

    private final String value;

    IdentMethodType(String value) { this.value = value; }

    public String getValue() { return value; }
}