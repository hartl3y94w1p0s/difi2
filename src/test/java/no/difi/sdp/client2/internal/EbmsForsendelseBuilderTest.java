package no.difi.sdp.client2.internal;

import no.difi.sdp.client2.ObjectMother;
import no.difi.sdp.client2.domain.AktoerOrganisasjonsnummer;
import no.difi.sdp.client2.domain.Avsender;
import no.difi.sdp.client2.domain.Databehandler;
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
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class EbmsForsendelseBuilderTest {

    private EbmsForsendelseBuilder sut;

    @Before
    public void set_up() {
        sut = new EbmsForsendelseBuilder();
    }

    @Test
    public void bygg_minimalt_request() {
        Databehandler databehandler = Databehandler.builder(AktoerOrganisasjonsnummer.of("991825827").forfremTilDatabehandler(), ObjectMother.selvsignertNoekkelpar()).build();
        Mottaker mottaker = Mottaker.builder("01129955131", "postkasseadresse", mottakerSertifikat(), Organisasjonsnummer.of("984661185")).build();
        DigitalPost digitalpost = DigitalPost.builder(mottaker, "Ikke-sensitiv tittel").build();
        Dokument dokument = Dokument.builder("Sensitiv tittel", "filnavn", new ByteArrayInputStream("hei".getBytes())).build();
        Dokumentpakke dokumentpakke = Dokumentpakke.builder(dokument).build();

        Avsender avsender = Avsender.builder(ObjectMother.avsenderOrganisasjonsnummer()).build();

        Forsendelse forsendelse = Forsendelse.digital(avsender, digitalpost, dokumentpakke).build();

        EbmsForsendelse ebmsForsendelse = sut.buildEbmsForsendelse(databehandler, Organisasjonsnummer.of("984661185"), forsendelse).entity;

        assertThat(ebmsForsendelse.getAvsender().orgnr.getOrganisasjonsnummerMedLandkode(), equalTo("9908:991825827"));
        assertThat(ebmsForsendelse.getDokumentpakke().getContentType(), equalTo("application/cms"));
    }

    @Test
    public void korrekt_mpc() {
        Databehandler databehandler = Databehandler.builder(AktoerOrganisasjonsnummer.of("991825827").forfremTilDatabehandler(), ObjectMother.selvsignertNoekkelpar()).build();
        Mottaker mottaker = Mottaker.builder("01129955131", "postkasseadresse", mottakerSertifikat(), Organisasjonsnummer.of("984661185")).build();
        DigitalPost digitalpost = DigitalPost.builder(mottaker, "Ikke-sensitiv tittel").build();

        Avsender avsender = Avsender.builder(ObjectMother.avsenderOrganisasjonsnummer()).build();

        Forsendelse forsendelse = Forsendelse.digital(avsender, digitalpost, ObjectMother.dokumentpakke()).mpcId("mpcId").prioritet(Prioritet.PRIORITERT).build();

        EbmsForsendelse ebmsForsendelse = sut.buildEbmsForsendelse(databehandler, Organisasjonsnummer.of("984661185"), forsendelse).entity;

        assertThat(ebmsForsendelse.prioritet, equalTo(EbmsOutgoingMessage.Prioritet.PRIORITERT));
        assertThat(ebmsForsendelse.mpcId, equalTo("mpcId"));
    }
}