package no.difi.sdp.client2.smoke;

import no.difi.sdp.client2.KlientKonfigurasjon;
import no.difi.sdp.client2.ObjectMother;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.Databehandler;
import no.difi.sdp.client2.domain.Forsendelse;
import no.difi.sdp.client2.domain.Miljo;
import no.difi.sdp.client2.domain.Noekkelpar;
import no.difi.sdp.client2.domain.Prioritet;
import no.difi.sdp.client2.domain.kvittering.ForretningsKvittering;
import no.difi.sdp.client2.domain.kvittering.KvitteringForespoersel;
import no.difi.sdp.client2.domain.kvittering.LeveringsKvittering;
import no.digipost.api.representations.Organisasjonsnummer;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import static java.lang.System.out;
import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SmokeTestHelper {

    private static final String VIRKSOMHETSSERTIFIKAT_PASSWORD_ENVIRONMENT_VARIABLE = "virksomhetssertifikat_passord";
    private static final String VIRKSOMHETSSERTIFIKAT_ALIAS_ENVIRONMENT_VARIABLE = "virksomhetssertifikat_alias";
    private static final String VIRKSOMHETSSERTIFIKAT_PATH_ENVIRONMENT_VARIABLE = "virksomhetssertifikat_sti";
    private static String virksomhetssertifikatPasswordValue = System.getenv(VIRKSOMHETSSERTIFIKAT_PASSWORD_ENVIRONMENT_VARIABLE);
    private static String virksomhetssertifikatAliasValue = System.getenv(VIRKSOMHETSSERTIFIKAT_ALIAS_ENVIRONMENT_VARIABLE);
    private static String virksomhetssertifikatPathValue = System.getenv(VIRKSOMHETSSERTIFIKAT_PATH_ENVIRONMENT_VARIABLE);

    private final SikkerDigitalPostKlient _klient;
    private final String _mpcId;
    private Forsendelse _forsendelse;
    private ForretningsKvittering _forretningskvittering;

    public SmokeTestHelper(Miljo miljo) {
        KeyStore virksomhetssertifikat = getVirksomhetssertifikat();
        Organisasjonsnummer databehandlerOrgnr = getOrganisasjonsnummerFraSertifikat(virksomhetssertifikat);
        Databehandler databehandler = ObjectMother.databehandlerMedSertifikat(databehandlerOrgnr, createDatabehandlerNoekkelparFromCertificate(virksomhetssertifikat));
        KlientKonfigurasjon klientKonfigurasjon = KlientKonfigurasjon.builder(miljo).build();
        _klient = new SikkerDigitalPostKlient(databehandler, klientKonfigurasjon);
        _mpcId = UUID.randomUUID().toString();

    }

    private static Noekkelpar createDatabehandlerNoekkelparFromCertificate(KeyStore databehandlerCertificate) {
        //        return Noekkelpar.fraKeyStore(keyStore, virksomhetssertifikatAliasValue, virksomhetssertifikatPasswordValue);
        return Noekkelpar.fraKeyStoreUtenTrustStore(databehandlerCertificate, virksomhetssertifikatAliasValue, virksomhetssertifikatPasswordValue);
    }

    private static KeyStore getVirksomhetssertifikat() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(virksomhetssertifikatPathValue), virksomhetssertifikatPasswordValue.toCharArray());
            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Fant ikke virksomhetssertifikat på sti '%s'. Eksporter environmentvariabel '%s' til virksomhetssertifikatet.", virksomhetssertifikatPathValue, VIRKSOMHETSSERTIFIKAT_PATH_ENVIRONMENT_VARIABLE), e);
        }
    }

    private static Organisasjonsnummer getOrganisasjonsnummerFraSertifikat(KeyStore keyStore) {
        try {
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(virksomhetssertifikatAliasValue);
            if (cert == null) {
                throw new RuntimeException(String.format("Klarte ikke hente ut virksomhetssertifikatet fra keystoren med alias '%s'", virksomhetssertifikatAliasValue));
            }
            X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
            RDN serialnumber = x500name.getRDNs(BCStyle.SN)[0];
            return Organisasjonsnummer.of(IETFUtils.valueToString(serialnumber.getFirst().getValue()));
        } catch (CertificateEncodingException e) {
            throw new RuntimeException("Klarte ikke hente ut organisasjonsnummer fra sertifikatet.", e);
        } catch (KeyStoreException e) {
            throw new RuntimeException("Klarte ikke hente ut virksomhetssertifikatet fra keystoren.", e);
        }
    }

    public SmokeTestHelper create_digital_forsendelse() {
        Forsendelse forsendelse = null;
        try {
            forsendelse = ObjectMother.forsendelse(_mpcId, new ClassPathResource("/test.pdf").getInputStream());
        } catch (IOException e) {
            fail("klarte ikke åpne hoveddokument.");
        }

        _forsendelse = forsendelse;

        return this;
    }

    public SmokeTestHelper send() {
        assertState(_forsendelse);

        _klient.send(_forsendelse);

        return this;
    }

    public SmokeTestHelper fetch_receipt() {
        KvitteringForespoersel kvitteringForespoersel = KvitteringForespoersel.builder(Prioritet.PRIORITERT).mpcId(_mpcId).build();
        ForretningsKvittering forretningsKvittering = null;

        try {
            sleep(2000);

            for (int i = 0; i < 10; i++) {
                forretningsKvittering = _klient.hentKvittering(kvitteringForespoersel);

                if (forretningsKvittering != null) {
                    out.println("Kvittering!");
                    out.println(String.format("%s: %s, %s, %s, %s", forretningsKvittering.getClass().getSimpleName(), forretningsKvittering.getKonversasjonsId(), forretningsKvittering.getReferanseTilMeldingId(), forretningsKvittering.getTidspunkt(), forretningsKvittering));
                    assertThat(forretningsKvittering.getKonversasjonsId(), not(isEmptyString()));
                    assertThat(forretningsKvittering.getReferanseTilMeldingId(), not(isEmptyString()));
                    assertThat(forretningsKvittering.getTidspunkt(), notNullValue());
                    assertThat(forretningsKvittering, instanceOf(LeveringsKvittering.class));

                    _klient.bekreft(forretningsKvittering);
                    break;
                } else {
                    out.println("Ingen kvittering");
                    sleep(1000);
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        _forretningskvittering = forretningsKvittering;

        return this;
    }

    public SmokeTestHelper expect_receipt_to_be_leveringskvittering() {
        assertState(_forretningskvittering);

        Assert.assertThat(_forretningskvittering, Matchers.instanceOf(LeveringsKvittering.class));

        return this;
    }

    public SmokeTestHelper confirm_receipt() {
        _klient.bekreft(_forretningskvittering);

        return this;
    }

    private void assertState(Object object) {
        if (object == null) {
            throw new IllegalStateException("Requires gradually built state. Make sure you use functions in the correct order.");
        }
    }

}
