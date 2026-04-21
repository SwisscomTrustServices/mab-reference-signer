package org.sts.demo.signer.signing.domain;

public enum CredentialId {
    ADVANCED4("OnDemand-Advanced4"),
    ADVANCED4_1_EU("OnDemand-Advanced4.1-EU"),
    QUALIFIED4("OnDemand-Qualified4"),
    QUALIFIED4_1_EU("OnDemand-Qualified4.1-EU");

    private final String value;

    CredentialId(String value) { this.value = value; }

    public String getValue() { return value; }
}