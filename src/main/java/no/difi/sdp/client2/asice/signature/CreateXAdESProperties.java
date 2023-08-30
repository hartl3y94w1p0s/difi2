package no.difi.sdp.client2.asice.signature;

import no.difi.sdp.client2.asice.AsicEAttachable;
import no.difi.sdp.client2.domain.Sertifikat;
import no.difi.sdp.client2.domain.exceptions.XmlKonfigurasjonException;
import org.etsi.uri._01903.v1_3.CertIDType;
import org.etsi.uri._01903.v1_3.DataObjectFormat;
import org.etsi.uri._01903.v1_3.DigestAlgAndValueType;
import org.etsi.uri._01903.v1_3.QualifyingProperties;
import org.etsi.uri._01903.v1_3.SignedDataObjectProperties;
import org.etsi.uri._01903.v1_3.SignedProperties;
import org.etsi.uri._01903.v1_3.SignedSignatureProperties;
import org.etsi.uri._01903.v1_3.SigningCertificate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.w3.xmldsig.X509IssuerSerialType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.transform.dom.DOMResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.codec.digest.DigestUtils.sha1;

class CreateXAdESProperties {

    private final org.w3.xmldsig.DigestMethod sha1DigestMethod = new org.w3.xmldsig.DigestMethod(emptyList(), DigestMethod.SHA1);

    private static Jaxb2Marshaller marshaller;

    static {
        marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(QualifyingProperties.class);
    }

    public Document createPropertiesToSign(List<AsicEAttachable> files, Sertifikat sertifikat) {
        X509Certificate certificate = sertifikat.getX509Certificate();
        byte[] certificateDigestValue = sha1(sertifikat.getEncoded());

        DigestAlgAndValueType certificateDigest = new DigestAlgAndValueType(sha1DigestMethod, certificateDigestValue);
        X509IssuerSerialType certificateIssuer = new X509IssuerSerialType(certificate.getIssuerDN().getName(), certificate.getSerialNumber());
        SigningCertificate signingCertificate = new SigningCertificate(singletonList(new CertIDType(certificateDigest, certificateIssuer, null)));

        DateTime now = DateTime.now(DateTimeZone.UTC);
        SignedSignatureProperties signedSignatureProperties = new SignedSignatureProperties(now, signingCertificate, null, null, null, null);
        SignedDataObjectProperties signedDataObjectProperties = new SignedDataObjectProperties(dataObjectFormats(files), null, null, null, null);
        SignedProperties signedProperties = new SignedProperties(signedSignatureProperties, signedDataObjectProperties, "SignedProperties");
        QualifyingProperties qualifyingProperties = new QualifyingProperties(signedProperties, null, "#Signature", null);

        DOMResult domResult = new DOMResult();
        marshaller.marshal(qualifyingProperties, domResult);
        Document document = (Document) domResult.getNode();

        // Explicitly mark the SignedProperties Id as an Document ID attribute, so that it will be eligble as a reference for signature.
        // If not, it will not be treated as something to sign.
        markAsIdProperty(document, "SignedProperties", "Id");

        return document;
    }

    private List<DataObjectFormat> dataObjectFormats(List<AsicEAttachable> files) {
        List<DataObjectFormat> result = new ArrayList<DataObjectFormat>();
        for (int i = 0; i < files.size(); i++) {
            String signatureElementIdReference = format("#ID_%s", i);
            result.add(new DataObjectFormat(null, null, files.get(i).getMimeType(), null, signatureElementIdReference));
        }
        return result;
    }

    private void markAsIdProperty(Document document, final String elementName, String property) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            Element idElement = (Element) xPath.evaluate("//*[local-name()='" + elementName + "']", document, XPathConstants.NODE);
            idElement.setIdAttribute(property, true);

        } catch (XPathExpressionException e) {
            throw new XmlKonfigurasjonException("XPath på generert XML feilet.", e);
        }
    }
}
