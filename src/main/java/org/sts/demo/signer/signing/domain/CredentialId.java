package org.sts.demo.signer.signing.domain;

import org.openapi.etsi.model.EtsiSignRequest;
import org.openapi.mab.model.CreateParRequestClaims;

public enum CredentialId {
    ADVANCED4(EtsiSignRequest.CredentialIDEnum.ADVANCED4,
            CreateParRequestClaims.CredentialIDEnum.ADVANCED4),
    ADVANCED4_1_EU(EtsiSignRequest.CredentialIDEnum.ADVANCED4_1_EU,
            CreateParRequestClaims.CredentialIDEnum.ADVANCED4_1_EU),
    QUALIFIED4(EtsiSignRequest.CredentialIDEnum.QUALIFIED4,
            CreateParRequestClaims.CredentialIDEnum.QUALIFIED4),
    QUALIFIED4_1_EU(EtsiSignRequest.CredentialIDEnum.QUALIFIED4_1_EU,
            CreateParRequestClaims.CredentialIDEnum.QUALIFIED4_1_EU);

    private final EtsiSignRequest.CredentialIDEnum etsiCredentialId;
    private final CreateParRequestClaims.CredentialIDEnum mabCredentialId;

    CredentialId(EtsiSignRequest.CredentialIDEnum etsiCredentialId,
                 CreateParRequestClaims.CredentialIDEnum mabCredentialId) {
        this.etsiCredentialId = etsiCredentialId;
        this.mabCredentialId = mabCredentialId;
    }

    public EtsiSignRequest.CredentialIDEnum toEtsi() {
        return etsiCredentialId;
    }

    public CreateParRequestClaims.CredentialIDEnum toMab() {
        return mabCredentialId;
    }
}