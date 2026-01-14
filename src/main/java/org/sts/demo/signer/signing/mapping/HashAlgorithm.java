package org.sts.demo.signer.signing.mapping;

public enum HashAlgorithm {
    SHA256("2.16.840.1.101.3.4.2.1"),
    SHA384("2.16.840.1.101.3.4.2.2"),
    SHA512("2.16.840.1.101.3.4.2.3");

    private final String oid;

    HashAlgorithm(String oid) {
        this.oid = oid;
    }

    public String oid() {
        return oid;
    }
}
