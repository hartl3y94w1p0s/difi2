package no.difi.sdp.client2.smoketest;

import no.difi.sdp.client2.KlientKonfigurasjon;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import no.difi.sdp.client2.domain.Behandlingsansvarlig;
import no.difi.sdp.client2.domain.Dokument;
import no.difi.sdp.client2.domain.Dokumentpakke;
import no.difi.sdp.client2.domain.Forsendelse;
import no.difi.sdp.client2.domain.Mottaker;
import no.difi.sdp.client2.domain.Noekkelpar;
import no.difi.sdp.client2.domain.Prioritet;
import no.difi.sdp.client2.domain.TekniskAvsender;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.kvittering.ForretningsKvittering;
import no.difi.sdp.client2.domain.kvittering.KvitteringForespoersel;
import no.difi.sdp.client2.domain.kvittering.LeveringsKvittering;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class SikkerDigitalPostKlientST {

    private static SikkerDigitalPostKlient sikkerDigitalPostKlient;
    private static String MpcId;
    private static String OrganizationNumberFromCertificate;
    private static KeyStore keyStore;

    private static final String VIRKSOMHETSSERTIFIKAT_PASSWORD_ENVIRONMENT_VARIABLE = "virksomhetssertifikat_passord";
    private static String virksomhetssertifikatPasswordValue = System.getenv(VIRKSOMHETSSERTIFIKAT_PASSWORD_ENVIRONMENT_VARIABLE);

    private static final String VIRKSOMHETSSERTIFIKAT_ALIAS_ENVIRONMENT_VARIABLE = "virksomhetssertifikat_alias";
    private static String virksomhetssertifikatAliasValue = System.getenv(VIRKSOMHETSSERTIFIKAT_ALIAS_ENVIRONMENT_VARIABLE);

    private static final String VIRKSOMHETSSERTIFIKAT_PATH_ENVIRONMENT_VARIABLE = "virksomhetssertifikat_sti";
    private static String virksomhetssertifikatPathValue = System.getenv(VIRKSOMHETSSERTIFIKAT_PATH_ENVIRONMENT_VARIABLE);

    @BeforeClass
    public static void setUp() {
        verifyEnvironmentVariables();

        keyStore = getVirksomhetssertifikat();
        MpcId = UUID.randomUUID().toString();
        OrganizationNumberFromCertificate = getOrganizationNumberFromCertificate();

        KlientKonfigurasjon klientKonfigurasjon = KlientKonfigurasjon.builder()
                .meldingsformidlerRoot("https://qaoffentlig.meldingsformidler.digipost.no/api/ebms")
                .connectionTimeout(20, TimeUnit.SECONDS)
                .httpRequestInterceptors(new HttpRequestInterceptor() {
                    @Override
                    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                        System.out.println("Utgående request!");
//                        String s = EntityUtils.toString(((HttpPost)request).getEntity(), "UTF-8");

                    }
                })
                .httpResponseInterceptors(new HttpResponseInterceptor() {
                    @Override
                    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
                        System.out.println("Innkommende request!");
//                        String s = EntityUtils.toString(response.getEntity(), "UTF-8");
                    }
                })
                .build();

        TekniskAvsender avsender = ObjectMother.tekniskAvsenderMedSertifikat(OrganizationNumberFromCertificate, avsenderNoekkelpar());

        sikkerDigitalPostKlient = new SikkerDigitalPostKlient(avsender, klientKonfigurasjon);
    }

    private static void verifyEnvironmentVariables() {
        throwIfEnvironmentVariableNotSet("sti", virksomhetssertifikatPathValue);
        throwIfEnvironmentVariableNotSet("alias", virksomhetssertifikatAliasValue);
        throwIfEnvironmentVariableNotSet("passord", virksomhetssertifikatPasswordValue);
    }

    private static void throwIfEnvironmentVariableNotSet(String variabel, String value) {
        String oppsett = "For å kjøre smoketestene må det brukes et gyldig virksomhetssertifikat. \n" +
                "1) Sett environmentvariabel '" + VIRKSOMHETSSERTIFIKAT_PATH_ENVIRONMENT_VARIABLE + "' til full sti til virksomhetsssertifikatet. \n" +
                "2) Sett environmentvariabel '" + VIRKSOMHETSSERTIFIKAT_ALIAS_ENVIRONMENT_VARIABLE + "' til aliaset (siste avsnitt, første del før komma): \n" +
                "       keytool -list -keystore VIRKSOMHETSSERTIFIKAT.p12 -storetype pkcs12 \n" +
                "3) Sett environmentvariabel '" + VIRKSOMHETSSERTIFIKAT_PASSWORD_ENVIRONMENT_VARIABLE + "' til passordet til virksomhetssertifikatet. \n";

        if (value == null) {
            throw new RuntimeException(String.format("Finner ikke %s til virksomhetssertifikat. \n %s", variabel, oppsett));
        }
    }

    private static Noekkelpar avsenderNoekkelpar() {
        return Noekkelpar.fraKeyStoreUtenTrustStore(keyStore, virksomhetssertifikatAliasValue, virksomhetssertifikatPasswordValue);
    }

    private static String getOrganizationNumberFromCertificate() {
        try {
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(virksomhetssertifikatAliasValue);
            if (cert == null) {
                throw new RuntimeException(String.format("Klarte ikke hente ut virksomhetssertifikatet fra keystoren med alias '%s'", virksomhetssertifikatAliasValue));
            }
            X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
            RDN serialnumber = x500name.getRDNs(BCStyle.SN)[0];
            return IETFUtils.valueToString(serialnumber.getFirst().getValue());
        } catch (CertificateEncodingException e) {
            throw new RuntimeException("Klarte ikke hente ut organisasjonsnummer fra sertifikatet.", e);
        } catch (KeyStoreException e) {
            throw new RuntimeException("Klarte ikke hente ut virksomhetssertifikatet fra keystoren.", e);
        }
    }

    private static KeyStore getVirksomhetssertifikat() {

        try {
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(virksomhetssertifikatPathValue), virksomhetssertifikatPasswordValue.toCharArray());
            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException("Kunne ikke initiere keystoren. Legg virksomhetssertifikatet.p12 i src/smoke-test/resources/. ", e);
        }
    }

    @Test
    public void send_digital_forsendelse_og_hent_kvittering() throws InterruptedException {
        Forsendelse forsendelse = null;
        try {
            forsendelse = ObjectMother.forsendelse(OrganizationNumberFromCertificate, MpcId, new ClassPathResource("/test.pdf").getInputStream());
        } catch (IOException e) {
            fail("klarte ikke åpne hoveddokument.");
        }

        sikkerDigitalPostKlient.send(forsendelse);
        ForretningsKvittering forretningsKvittering = getForretningsKvittering(sikkerDigitalPostKlient);
        sikkerDigitalPostKlient.bekreft(forretningsKvittering);
        assertThat(forretningsKvittering != null).isTrue();
    }

    @Test
    public void send_digital_forsendelse_med_databehandler_og_hent_kvittering() throws InterruptedException {
        Dokument hovedDokument = null;
        try {
            hovedDokument = Dokument.builder("Sensitiv brevtittel", "faktura.pdf", new ClassPathResource("/test.pdf").getInputStream())
                    .mimeType("application/pdf")
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Dokumentpakke dokumentpakke = Dokumentpakke.builder(hovedDokument).build();

        Behandlingsansvarlig behandlingsansvarlig = new Behandlingsansvarlig(OrganizationNumberFromCertificate);
        Mottaker mottaker = Mottaker.builder("01013300001", "ove.jonsen#6K5A", no.difi.sdp.client2.ObjectMother.mottakerSertifikat(), "984661185").build();
        DigitalPost digitalPost = DigitalPost.builder(mottaker, "IkkeSensitivTittel").build();
        Forsendelse forsendelse = Forsendelse.digital(behandlingsansvarlig, digitalPost, dokumentpakke).mpcId(MpcId).prioritet(Prioritet.PRIORITERT).build();

        KlientKonfigurasjon klientKonfigurasjon = KlientKonfigurasjon.builder()
                .meldingsformidlerRoot("https://qaoffentlig.meldingsformidler.digipost.no/api/ebms")
                .connectionTimeout(40, TimeUnit.SECONDS).build();

        SikkerDigitalPostKlient sikkerDigitalPostKlient = new SikkerDigitalPostKlient(ObjectMother.tekniskAvsenderMedSertifikat(OrganizationNumberFromCertificate, avsenderNoekkelpar()), klientKonfigurasjon);

        sikkerDigitalPostKlient.send(forsendelse);
        ForretningsKvittering forretningsKvittering = getForretningsKvittering(sikkerDigitalPostKlient);
        sikkerDigitalPostKlient.bekreft(forretningsKvittering);

    }

    private ForretningsKvittering getForretningsKvittering(SikkerDigitalPostKlient sikkerDigitalPostKlient) throws InterruptedException {
        KvitteringForespoersel kvitteringForespoersel = KvitteringForespoersel.builder(Prioritet.PRIORITERT).mpcId(MpcId).build();
        ForretningsKvittering forretningsKvittering = null;
        sleep(1000);//wait 1 sec until first try.
        for (int i = 0; i < 10; i++) {
            forretningsKvittering = sikkerDigitalPostKlient.hentKvittering(kvitteringForespoersel);

            if (forretningsKvittering != null) {
                System.out.println("Kvittering!");
                System.out.println(String.format("%s: %s, %s, %s, %s", forretningsKvittering.getClass().getSimpleName(), forretningsKvittering.kvitteringsinfo.konversasjonsId, forretningsKvittering.kvitteringsinfo.referanseTilMeldingId, forretningsKvittering.kvitteringsinfo.tidspunkt, forretningsKvittering));
                assertThat(forretningsKvittering.kvitteringsinfo.konversasjonsId).isNotEmpty();
                assertThat(forretningsKvittering.kvitteringsinfo.referanseTilMeldingId).isNotEmpty();
                assertThat(forretningsKvittering.kvitteringsinfo.tidspunkt).isNotNull();
                assertThat(forretningsKvittering).isInstanceOf(LeveringsKvittering.class);

                sikkerDigitalPostKlient.bekreft(forretningsKvittering);
                break;
            } else {
                System.out.println("Ingen kvittering");
                sleep(1000);
            }
        }
        return forretningsKvittering;
    }
}
