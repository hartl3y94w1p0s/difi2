package no.difi.sdp.client.internal;

import no.difi.sdp.client.asice.CreateASiCE;
import no.difi.sdp.client.domain.Avsender;
import no.difi.sdp.client.domain.Forsendelse;
import no.difi.sdp.client.domain.Sertifikat;
import no.difi.sdp.client.domain.exceptions.RuntimeIOException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class CreateDokumentpakke {

    private final CreateASiCE createASiCE;
    private final CreateCryptographicMessageSyntax createCMS;

    public CreateDokumentpakke() {
        createASiCE = new CreateASiCE();
        createCMS = new CreateCryptographicMessageSyntax();
    }

    public InputStream createDokumentpakke(Avsender avsender, Forsendelse forsendelse) {
        InputStream asicStream = createASiCE.createStream(avsender, forsendelse);

        byte[] bytes;
        try {
            bytes = IOUtils.toByteArray(asicStream);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }

        Sertifikat mottakerSertifikat = forsendelse.getDigitalPost().getMottaker().getSertifikat();

        return createCMS.createCMS(bytes, mottakerSertifikat);
    }

}
