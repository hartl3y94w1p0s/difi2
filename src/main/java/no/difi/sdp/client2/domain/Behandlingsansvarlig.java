package no.difi.sdp.client2.domain;

/**
 * Behandlingsansvarlig som beskrevet i <a href="http://begrep.difi.no/SikkerDigitalPost/forretningslag/Aktorer">oversikten over aktører</a>.
 */
public class Behandlingsansvarlig {

    private final String organisasjonsnummer;
    private String avsenderIdentifikator;
    private String fakturaReferanse;

    public Behandlingsansvarlig(String organisasjonsnummer) {
        this.organisasjonsnummer = organisasjonsnummer;
    }

    public String getAvsenderIdentifikator() {
        return avsenderIdentifikator;
    }

    public String getFakturaReferanse() {
        return fakturaReferanse;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public static Builder builder(String organisasjonsnummer) {
        return new Builder(organisasjonsnummer);
    }

    public static class Builder {

        private final Behandlingsansvarlig target;
        private boolean built = false;

        private Builder(String organisasjonsnummer) {
            target = new Behandlingsansvarlig(organisasjonsnummer);
        }

        public Builder fakturaReferanse(String fakturaReferanse) {
            target.fakturaReferanse = fakturaReferanse;
            return this;
        }

        /**
         * Brukes for å identifisere en ansvarlig enhet innen for en virksomhet. Benyttes dersom det er behov for å skille mellom ulike enheter hos avsender.
         *
         * @param avsenderIdentifikator Identifikator som er tildelt av Sentralforvalter ved tilkobling til tjenesten.
         */
        public Builder avsenderIdentifikator(String avsenderIdentifikator) {
            target.avsenderIdentifikator = avsenderIdentifikator;
            return this;
        }

        public Behandlingsansvarlig build() {
            if (built) throw new IllegalStateException("Can't build twice");
            built = true;
            return this.target;
        }
    }

}