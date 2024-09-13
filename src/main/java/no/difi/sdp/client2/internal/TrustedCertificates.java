package no.difi.sdp.client2.internal;

import no.difi.sdp.client2.domain.exceptions.SertifikatException;
import no.digipost.security.cert.Trust;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.difi.sdp.client2.internal.Environment.PRODUCTION;
import static no.difi.sdp.client2.internal.Environment.TEST;
import static no.digipost.security.DigipostSecurity.readCertificate;


public class TrustedCertificates {

    public static Trust createTrust(Environment environment) {
        return new Trust(
                getTrustedRootCertificates(environment),
                getTrustedIntermediateCertificates(environment)
        );
    }

    private static Stream<X509Certificate> getTrustedRootCertificates(Environment environment) {
        Stream.Builder<X509Certificate> trustedCertificates = Stream.builder();

        switch (environment) {
            case PRODUCTION:
                // Buypass gyldig 2010 - 2040 - C=NO, O=Buypass AS-983163327, CN=Buypass Class 3 Root CA
                trustedCertificates.add(readCertificate("certificates/prod/BPClass3RootCA.cer"));
                // commfides gyldig 2011 - 2024 - CN=CPN RootCA SHA256 Class 3, OU=Commfides Trust Environment (c) 2011 Commfides Norge AS, O=Commfides Norge AS - 988 312 495, C=NO
                trustedCertificates.add(readCertificate("certificates/prod/commfides_root_ca.cer"));
                break;
            case TEST:
                // Buypass gyldig 2010 - 2040 - C=NO, O=Buypass AS-983163327, CN=Buypass Class 3 Test4 Root CA
                trustedCertificates.add(readCertificate("certificates/test/Buypass_Class_3_Test4_Root_CA.cer"));
                // Buypass gyldig 2012 - 2022 - CN=CPN Root SHA256 CA - TEST, OU=Commfides Trust Environment(C) TEST 2010 Commfides Norge AS, OU=CPN TEST - For authorized use only, OU=CPN Primary Certificate Authority TEST, O=Commfides Norge AS - 988 312 495, C=NO
                trustedCertificates.add(readCertificate("certificates/test/commfides_test_root_ca.cer"));
                break;
            default:
                throw getInvalidEnvironmentException(environment);
        }

        return trustedCertificates.build();
    }

    private static Stream<X509Certificate> getTrustedIntermediateCertificates(Environment environment) {
        Stream.Builder<X509Certificate> trustedCertificates = Stream.builder();

        switch (environment) {
            case PRODUCTION:
                //2012-2032
                trustedCertificates.add(readCertificate("certificates/prod/BPClass3CA3.cer"));
                //2011-2025
                trustedCertificates.add(readCertificate("certificates/prod/commfides_ca.cer"));
                break;
            case TEST:
                //2012-2032
                trustedCertificates.add(readCertificate("certificates/test/Buypass_Class_3_Test4_CA_3.cer"));
                //2012-2022
                trustedCertificates.add(readCertificate("certificates/test/commfides_test_ca.cer"));
                break;
            default:
                throw getInvalidEnvironmentException(environment);
        }

        return trustedCertificates.build();
    }

    private static IllegalStateException getInvalidEnvironmentException(Environment environment) {
        String exceptionDescription = MessageFormat.format("The environment {0} is not supported for trusted certificates.", environment);
        return new IllegalStateException(exceptionDescription);
    }

    public static KeyStore getTrustStore() {
        KeyStore trustStore;

        try {
            trustStore = KeyStore.getInstance("JCEKS");
            trustStore.load(null, "".toCharArray());
        } catch (Exception e) {
            throw new SertifikatException("Oppretting av tom keystore feilet. Grunnen er " + e.toString());
        }

        try {
            addCertificatesToTrustStore(getTrustedRootCertificates(PRODUCTION), trustStore);
            addCertificatesToTrustStore(getTrustedIntermediateCertificates(PRODUCTION), trustStore);
            addCertificatesToTrustStore(getTrustedRootCertificates(TEST), trustStore);
            addCertificatesToTrustStore(getTrustedIntermediateCertificates(TEST), trustStore);
        } catch (KeyStoreException e) {
            throw new SertifikatException("Klarte ikke å legge til sertifikat til trust store. Grunnen er " + e.toString());
        }

        return trustStore;
    }

    public static void addCertificatesToTrustStore(Stream<X509Certificate> certificates, KeyStore trustStore) throws KeyStoreException {
        for (X509Certificate cert : certificates.collect(Collectors.toList())) {
            String uniqueCertificateAlias = cert.getSerialNumber().toString() + Math.random();
            trustStore.setCertificateEntry(uniqueCertificateAlias, cert);
        }
    }
}
