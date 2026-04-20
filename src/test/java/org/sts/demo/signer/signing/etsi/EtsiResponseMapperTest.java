package org.sts.demo.signer.signing.etsi;

import org.junit.jupiter.api.Test;
import org.openapi.etsi.model.EtsiSignResponse;
import org.openapi.etsi.model.EtsiSignResponseValidationInfo;
import org.sts.demo.signer.web.dto.EtsiSignStartResponse;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EtsiResponseMapperTest {

    @Test
    void extractCms_shouldDecodeBase64() {
        byte[] cms = new byte[] {1,2,3,4};
        String b64 = Base64.getEncoder().encodeToString(cms);

        EtsiSignResponse r = new EtsiSignResponse()
                .validationInfo(new EtsiSignResponseValidationInfo())
                .responseID(UUID.randomUUID())
                .addSignatureObjectItem(b64);

        byte[] out = EtsiResponseMapper.extractCms(r);
        assertArrayEquals(cms, out);
    }

    @Test
    void toUiResponse_shouldIncludePdfBase64AndCmsSize() {
        byte[] cms = new byte[] {9,9,9};
        String b64 = Base64.getEncoder().encodeToString(cms);

        byte[] signedPdf = "%PDF-1.7\n".getBytes();

        EtsiSignResponse r = new EtsiSignResponse()
                .validationInfo(new EtsiSignResponseValidationInfo())
                .responseID(UUID.randomUUID())
                .addSignatureObjectItem(b64);

        EtsiSignStartResponse ui = EtsiResponseMapper.toUiResponse(r, cms.length, signedPdf);

        assertEquals(3, ui.cmsBytes());
        assertNotNull(ui.signedPdf());
        assertTrue(ui.signedPdf().startsWith("JVBERi0"), "should be base64 of %PDF...");
    }
}