package org.sts.demo.signer.signing.domain;

public enum Namespace {
    MSISDN("msisdn"),
    MSA("MSA"),
    STS("STS"),
    MSAINT("MSAINT"),
    FUT("FUT"),
    PWDOTP("PWDOTP"),
    WBAUTHN("WBAUTHN");

    private final String value;

    Namespace(String value) { this.value = value; }

    public String getValue() { return value; }
}