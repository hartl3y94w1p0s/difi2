package no.difi.sdp.client2.internal;

import no.difi.sdp.client2.ObjectMother;
import no.difi.sdp.client2.domain.Behandlingsansvarlig;
import no.difi.sdp.client2.domain.TekniskAvsender;
import no.difi.sdp.client2.domain.Dokument;
import no.difi.sdp.client2.domain.Dokumentpakke;
import no.difi.sdp.client2.domain.Forsendelse;
import no.difi.sdp.client2.domain.Mottaker;
import no.difi.sdp.client2.domain.Prioritet;
import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.digipost.api.representations.EbmsForsendelse;
import no.digipost.api.representations.EbmsOutgoingMessage;
import no.digipost.api.representations.Organisasjonsnummer;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static no.difi.sdp.client2.ObjectMother.mottakerSertifikat;
import static org.fest.assertions.api.Assertions.assertThat;

public class EbmsForsendelseBuilderTest {

    private EbmsForsendelseBuilder sut;

    @Before
    public void set_up() {
        sut = new EbmsForsendelseBuilder();
    }

    @Test
    public void bygg_minimalt_request() {
        TekniskAvsender avsender = TekniskAvsender.builder(Organisasjonsnummer.of("991825827"), ObjectMother.noekkelpar()).build();
        Mottaker mottaker = Mottaker.builder("01129955131", "postkasseadresse", mottakerSertifikat(), Organisasjonsnummer.of("984661185")).build();
        DigitalPost digitalpost = DigitalPost.builder(mottaker, "Ikke-sensitiv tittel").build();
        Dokument dokument = Dokument.builder("Sensitiv tittel", "filnavn", new ByteArrayInputStream("hei".getBytes())).build();
        Dokumentpakke dokumentpakke = Dokumentpakke.builder(dokument).build();
        Behandlingsansvarlig behandlingsansvarlig = Behandlingsansvarlig.builder("936796702").build();
        Forsendelse forsendelse = Forsendelse.digital(behandlingsansvarlig, digitalpost, dokumentpakke).build();

        EbmsForsendelse ebmsForsendelse = sut.buildEbmsForsendelse(avsender, Organisasjonsnummer.of("984661185"), forsendelse);

        assertThat(ebmsForsendelse.getAvsender().orgnr.getOrganisasjonsnummerMedLandkode()).isEqualTo("9908:991825827");
        assertThat(ebmsForsendelse.getDokumentpakke().getContentType()).isEqualTo("application/cms");
    }

    @Test
    public void korrekt_mpc() {
        TekniskAvsender avsender = TekniskAvsender.builder(Organisasjonsnummer.of("991825827"), ObjectMother.noekkelpar()).build();
        Mottaker mottaker = Mottaker.builder("01129955131", "postkasseadresse", mottakerSertifikat(), Organisasjonsnummer.of("984661185")).build();
        DigitalPost digitalpost = DigitalPost.builder(mottaker, "Ikke-sensitiv tittel").build();
        Behandlingsansvarlig behandlingsansvarlig = Behandlingsansvarlig.builder("991825827").build();
        Forsendelse forsendelse = Forsendelse.digital(behandlingsansvarlig, digitalpost, ObjectMother.dokumentpakke()).mpcId("mpcId").prioritet(Prioritet.PRIORITERT).build();

        EbmsForsendelse ebmsForsendelse = sut.buildEbmsForsendelse(avsender, Organisasjonsnummer.of("984661185"), forsendelse);

        assertThat(ebmsForsendelse.prioritet).isEqualTo(EbmsOutgoingMessage.Prioritet.PRIORITERT);
        assertThat(ebmsForsendelse.mpcId).isEqualTo("mpcId");
    }
}