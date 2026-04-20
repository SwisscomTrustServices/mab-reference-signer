package org.sts.demo.signer.signing;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.util.Store;
import org.openapi.etsi.model.EtsiSignRequest;
import org.openapi.etsi.model.EtsiSignResponse;
import org.springframework.stereotype.Service;
import org.sts.demo.signer.web.dto.EtsiSignStartRequest;
import org.sts.demo.signer.web.dto.EtsiSignStartResponse;
import org.sts.demo.signer.signing.domain.SigningSession;
import org.sts.demo.signer.signing.domain.SigningSessionValidator;
import org.sts.demo.signer.signing.etsi.EtsiResponseMapper;
import org.sts.demo.signer.signing.etsi.EtsiSignClient;
import org.sts.demo.signer.signing.etsi.EtsiSignRequestFactory;
import org.sts.demo.signer.signing.pdf.PdfLtvEnhancer;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

@Service
public class EtsiSignService {

    private final EtsiSignRequestFactory etsiSignRequestFactory;
    private final EtsiSignClient etsiSignClient;
    private final SigningSessionValidator signingSessionValidator;

    public EtsiSignService(EtsiSignRequestFactory etsiSignRequestFactory,
                           EtsiSignClient etsiSignClient,
                           SigningSessionValidator signingSessionValidator) {
        this.etsiSignRequestFactory = etsiSignRequestFactory;
        this.etsiSignClient = etsiSignClient;
        this.signingSessionValidator = signingSessionValidator;
    }

    public Mono<EtsiSignStartResponse> signEtsi(EtsiSignStartRequest in) {
        return Mono.usingWhen(
                Mono.fromCallable(() -> signingSessionValidator.validateAndTake(in.state(), in.nonce()))
                        .subscribeOn(Schedulers.boundedElastic()),
                signingSession -> {
                    signingSession.requireSadJwt();
                    EtsiSignRequest req = etsiSignRequestFactory.build(signingSession);

                    return etsiSignClient.sign(req)
                            .flatMap(resp -> Mono.fromCallable(() -> buildUiResponseWithLtv(signingSession, resp))
                                    .subscribeOn(Schedulers.boundedElastic()));
                },
                signingSession -> Mono.fromRunnable(() -> signingSession.document().close())
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    private EtsiSignStartResponse buildUiResponseWithLtv(SigningSession signingSession, EtsiSignResponse resp) throws Exception {
        byte[] cms = EtsiResponseMapper.extractCms(resp);

        byte[] signedPdf = signingSession.document().embedCms(cms);
        List<byte[]> certsDer = extractCertsDerFromCms(cms);
        var vi = resp.getValidationInfo();
        List<byte[]> ocspsDer = vi.getOcsp().stream().map(Base64.getDecoder()::decode).toList();
        List<byte[]> crlsDer = vi.getCrl().stream().map(Base64.getDecoder()::decode).toList();

        byte[] ltvPdf = PdfLtvEnhancer.addLtDss(signedPdf, certsDer, ocspsDer, crlsDer);
        return EtsiResponseMapper.toUiResponse(resp, cms.length, ltvPdf);
    }

    static List<byte[]> extractCertsDerFromCms(byte[] cms) throws Exception {
        CMSSignedData sd = new CMSSignedData(cms);

        Store<X509CertificateHolder> store = sd.getCertificates();
        Collection<X509CertificateHolder> certs = store.getMatches(null);

        List<byte[]> out = new ArrayList<>();
        for (X509CertificateHolder h : certs) {
            out.add(h.getEncoded()); // DER
        }
        return out;
    }
}