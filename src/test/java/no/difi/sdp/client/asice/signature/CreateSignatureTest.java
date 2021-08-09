package no.difi.sdp.client.asice.signature;

import no.difi.sdp.client.ObjectMother;
import no.difi.sdp.client.asice.AsicEAttachable;
import no.difi.sdp.client.asice.Jaxb;
import no.difi.sdp.client.asice.Signature;
import no.difi.sdp.client.domain.Noekkelpar;
import org.etsi.uri._01903.v1_3.DataObjectFormat;
import org.etsi.uri._01903.v1_3.DigestAlgAndValueType;
import org.etsi.uri._01903.v1_3.QualifyingProperties;
import org.etsi.uri._01903.v1_3.SignedDataObjectProperties;
import org.etsi.uri._01903.v1_3.SigningCertificate;
import org.etsi.uri._2918.v1_2.XAdESSignatures;
import org.junit.Before;
import org.junit.Test;
import org.w3.xmldsig.*;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.List;

import static java.util.Arrays.asList;
import static org.fest.assertions.api.Assertions.assertThat;

public class CreateSignatureTest {

    private CreateSignature sut;

    /**
     * SHA256 hash of "hoveddokument-innhold"
     */
    private byte[] expectedHovedDokumentHash = new byte[] { 93, -36, 99, 92, -27, 39, 21, 31, 33, -127, 30, 77, 6, 49, 92, -48, -114, -61, -100, -126, -64, -70, 70, -38, 67, 93, -126, 62, -125, -7, -115, 123 };

    @Before
    public void setUp() throws Exception {
        sut = new CreateSignature();
    }

    @Test
    public void test_generated_signatures() {
        Noekkelpar noekkelpar = ObjectMother.noekkelpar();
        List<AsicEAttachable> files = asList(
                file("hoveddokument.pdf", "hoveddokument-innhold".getBytes(), "application/pdf"),
                file("manifest.xml", "manifest-innhold".getBytes(), "application/xml")
        );

        Signature signature = sut.createSignature(noekkelpar, files);
        XAdESSignatures xAdESSignatures = Jaxb.unmarshal(new StreamSource(new ByteArrayInputStream(signature.getBytes())), XAdESSignatures.class);

        assertThat(xAdESSignatures.getSignatures()).hasSize(1);
        org.w3.xmldsig.Signature dSignature = xAdESSignatures.getSignatures().get(0);
        verify_signed_info(dSignature.getSignedInfo());
        assertThat(dSignature.getSignatureValue()).isNotNull();
        assertThat(dSignature.getKeyInfo()).isNotNull();
    }

    @Test
    public void test_xades_signed_properties() {
        Noekkelpar noekkelpar = ObjectMother.noekkelpar();
        List<AsicEAttachable> files = asList(
                file("hoveddokument.pdf", "hoveddokument-innhold".getBytes(), "application/pdf"),
                file("manifest.xml", "manifest-innhold".getBytes(), "application/xml")
        );

        Signature signature = sut.createSignature(noekkelpar, files);
        XAdESSignatures xAdESSignatures = Jaxb.unmarshal(new StreamSource(new ByteArrayInputStream(signature.getBytes())), XAdESSignatures.class);
        org.w3.xmldsig.Object object = xAdESSignatures.getSignatures().get(0).getObjects().get(0);

        QualifyingProperties xadesProperties = (QualifyingProperties) object.getContent().get(0);
        SigningCertificate signingCertificate = xadesProperties.getSignedProperties().getSignedSignatureProperties().getSigningCertificate();
        verify_signing_certificate(signingCertificate);

        SignedDataObjectProperties signedDataObjectProperties = xadesProperties.getSignedProperties().getSignedDataObjectProperties();
        verify_signed_data_object_properties(signedDataObjectProperties);
    }

    private void verify_signed_data_object_properties(SignedDataObjectProperties signedDataObjectProperties) {
        assertThat(signedDataObjectProperties.getDataObjectFormats()).hasSize(2); // One per file
        DataObjectFormat hoveddokumentDataObjectFormat = signedDataObjectProperties.getDataObjectFormats().get(0);
        assertThat(hoveddokumentDataObjectFormat.getObjectReference()).isEqualTo("hoveddokument.pdf");
        assertThat(hoveddokumentDataObjectFormat.getMimeType()).isEqualTo("application/pdf");

        DataObjectFormat manifestDataObjectFormat = signedDataObjectProperties.getDataObjectFormats().get(1);
        assertThat(manifestDataObjectFormat.getObjectReference()).isEqualTo("manifest.xml");
        assertThat(manifestDataObjectFormat.getMimeType()).isEqualTo("application/xml");
    }

    private void verify_signing_certificate(SigningCertificate signingCertificate) {
        assertThat(signingCertificate.getCerts()).hasSize(1);

        DigestAlgAndValueType certDigest = signingCertificate.getCerts().get(0).getCertDigest();
        assertThat(certDigest.getDigestMethod().getAlgorithm()).isEqualTo("http://www.w3.org/2000/09/xmldsig#sha1");
        assertThat(certDigest.getDigestValue()).hasSize(20); // SHA1 is 160 bits => 20 bytes

        X509IssuerSerialType issuerSerial = signingCertificate.getCerts().get(0).getIssuerSerial();
        assertThat(issuerSerial.getX509IssuerName()).isEqualTo("CN=Avsender, OU=Avsender, O=Avsender, L=Oslo, ST=NO, C=NO");
        assertThat(issuerSerial.getX509SerialNumber()).isEqualTo(new BigInteger("589725471"));
    }

    private void verify_signed_info(SignedInfo signedInfo) {
        assertThat(signedInfo.getCanonicalizationMethod().getAlgorithm()).isEqualTo("http://www.w3.org/2006/12/xml-c14n11");
        assertThat(signedInfo.getSignatureMethod().getAlgorithm()).isEqualTo("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");

        List<Reference> references = signedInfo.getReferences();
        assertThat(references).hasSize(3);
        assert_hovedokument_reference(references.get(0));
        assertThat(references.get(1).getURI()).isEqualTo("manifest.xml");
        verify_signed_properties_reference(references.get(2));
    }

    private void verify_signed_properties_reference(Reference signedPropertiesReference) {
        assertThat(signedPropertiesReference.getURI()).isEqualTo("#SignedProperties");
        assertThat(signedPropertiesReference.getType()).isEqualTo("http://uri.etsi.org/01903#SignedProperties");
        assertThat(signedPropertiesReference.getDigestMethod().getAlgorithm()).isEqualTo("http://www.w3.org/2001/04/xmlenc#sha256");
        assertThat(signedPropertiesReference.getDigestValue()).hasSize(32); // SHA256 is 256 bits => 32 bytes
        assertThat(signedPropertiesReference.getTransforms().getTransforms().get(0).getAlgorithm()).isEqualTo("http://www.w3.org/TR/2001/REC-xml-c14n-20010315");
    }

    private void assert_hovedokument_reference(Reference hovedDokumentReference) {
        assertThat(hovedDokumentReference.getURI()).isEqualTo("hoveddokument.pdf");
        assertThat(hovedDokumentReference.getDigestValue()).isEqualTo(expectedHovedDokumentHash);
        assertThat(hovedDokumentReference.getDigestMethod().getAlgorithm()).isEqualTo("http://www.w3.org/2001/04/xmlenc#sha256");
    }

    private AsicEAttachable file(final String fileName, final byte[] contents, final String mimeType) {
        return new AsicEAttachable() {
            public String getFileName() { return fileName; }
            public byte[] getBytes() { return contents; }
            public String getMimeType() { return mimeType; }
        };
    }

}
