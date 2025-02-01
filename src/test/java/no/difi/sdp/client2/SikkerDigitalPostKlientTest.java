package no.difi.sdp.client2;

import no.difi.sdp.client2.domain.Databehandler;
import no.difi.sdp.client2.domain.Miljo;
import no.difi.sdp.client2.domain.exceptions.SendIOException;
import no.difi.sdp.client2.domain.exceptions.SertifikatException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static no.difi.sdp.client2.ObjectMother.databehandler;
import static no.difi.sdp.client2.ObjectMother.forsendelse;
import static no.difi.sdp.client2.domain.exceptions.SendException.AntattSkyldig.UKJENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class SikkerDigitalPostKlientTest {

    private static final URI lokalTimeoutUrl = URI.create("http://10.255.255.1");

    @Test
    public void handles_connection_timeouts() {
        @SuppressWarnings("deprecation")
        KlientKonfigurasjon klientKonfigurasjon = KlientKonfigurasjon.builder(lokalTimeoutUrl)
                .connectionTimeout(1, TimeUnit.MILLISECONDS)
                .build();

        SikkerDigitalPostKlient postklient = new SikkerDigitalPostKlient(databehandler(), klientKonfigurasjon);

        try {
            postklient.send(forsendelse());
            fail("Should fail");
        } catch (SendIOException e) {
            assertThat(e.getAntattSkyldig(), equalTo(UKJENT));
        }
    }

    @Test
    public void calls_http_interceptors() {
        final StringBuilder interceptorString = new StringBuilder();

        @SuppressWarnings("deprecation")
        KlientKonfigurasjon klientKonfigurasjon = KlientKonfigurasjon.builder(lokalTimeoutUrl)
                .connectionTimeout(1, TimeUnit.MILLISECONDS)
                .httpRequestInterceptors(
                        (request, context) -> interceptorString.append("First interceptor called"),
                        (request, context) -> interceptorString.append(", and second too!"))
                .build();

        SikkerDigitalPostKlient postklient = new SikkerDigitalPostKlient(databehandler(), klientKonfigurasjon);

        try {
            postklient.send(forsendelse());
            fail("Fails");
        } catch (SendIOException e) {
            assertThat(interceptorString.toString(), equalTo("First interceptor called, and second too!"));
        }
    }

    @Test
    public void calls_certificate_validator_on_init() {
        Databehandler databehandlerWithTestCertificate = databehandler();
        KlientKonfigurasjon konfigurasjon = KlientKonfigurasjon.builder(Miljo.PRODUKSJON).build();

        assertThrows(SertifikatException.class, () -> new SikkerDigitalPostKlient(databehandlerWithTestCertificate, konfigurasjon));
    }

}
