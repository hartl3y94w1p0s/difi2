package no.difi.sdp.client2.domain.kvittering;

import no.difi.sdp.client2.domain.kvittering.KvitteringsInfo.Builder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;


public class KvitteringsInfoTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void builder_initializes() throws Exception {
        String konversasjonsid = "konversasjonsid";
        String referanse = "referanse";
        Instant tidspunkt = Instant.now();

        KvitteringsInfo kvitteringsInfo = KvitteringsInfo.builder()
                .konversasjonsId(konversasjonsid)
                .referanseTilMeldingId(referanse)
                .tidspunkt(tidspunkt).build();

        assertThat(kvitteringsInfo.getKonversasjonsId(), equalTo(konversasjonsid));
        assertThat(kvitteringsInfo.getReferanseTilMeldingId(), equalTo(referanse));
        assertThat(kvitteringsInfo.getTidspunkt(), equalTo(tidspunkt));
    }


    @Test
    public void builder_fails_on_konversasjonsid_not_initialized() throws Exception {
        String referanse = "referanse";
        Instant tidspunkt = Instant.now();

        Builder kvitteringsInfoBuilder = KvitteringsInfo.builder()
                .referanseTilMeldingId(referanse)
                .tidspunkt(tidspunkt);

        thrown.expect(RuntimeException.class);
        kvitteringsInfoBuilder.build();
    }

    @Test
    public void builder_fails_on_referanse_not_initialized() throws Exception {
        String konversasjonsid = "konversasjonsid";
        Instant tidspunkt = Instant.now();

        Builder kvitteringsInfoBuilder = KvitteringsInfo.builder()
                .konversasjonsId(konversasjonsid)
                .tidspunkt(tidspunkt);

        thrown.expect(RuntimeException.class);
        kvitteringsInfoBuilder.build();
    }

    @Test
    public void builder_fails_on_tidspunkt_not_initialized() throws Exception {
        String konversasjonsid = "konversasjonsid";
        String referanse = "referanse";

        Builder kvitteringsInfoBuilder = KvitteringsInfo.builder()
                .konversasjonsId(konversasjonsid)
                .referanseTilMeldingId(referanse);

        thrown.expect(RuntimeException.class);
        kvitteringsInfoBuilder.build();
    }
}