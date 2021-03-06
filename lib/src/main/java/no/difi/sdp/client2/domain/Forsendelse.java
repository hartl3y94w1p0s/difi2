package no.difi.sdp.client2.domain;

import no.difi.sdp.client2.domain.digital_post.DigitalPost;
import no.difi.sdp.client2.domain.fysisk_post.FysiskPost;
import no.digipost.api.PMode;

import java.util.UUID;

import static no.difi.sdp.client2.domain.Forsendelse.Type.DIGITAL;
import static no.difi.sdp.client2.domain.Forsendelse.Type.FYSISK;

public class Forsendelse {

	public enum Type {
		DIGITAL(PMode.Action.FORMIDLE_DIGITAL),
        FYSISK(PMode.Action.FORMIDLE_FYSISK);

        public final PMode.Action action;

        Type(PMode.Action action) {
            this.action = action;
        }
    }

	public final Type type;
    private final DigitalPost digitalPost;
    private final FysiskPost fysiskPost;
    private final Dokumentpakke dokumentpakke;
    private final Avsender avsender;
    private String konversasjonsId = UUID.randomUUID().toString();
    private Prioritet prioritet = Prioritet.NORMAL;
    private String spraakkode = "NO";
    private String mpcId;

    private Forsendelse(Avsender avsender, DigitalPost digitalPost, Dokumentpakke dokumentpakke) {
    	this.type = DIGITAL;
        this.avsender = avsender;
        this.digitalPost = digitalPost;
        this.fysiskPost = null;
        this.dokumentpakke = dokumentpakke;
    }

    private Forsendelse(Avsender avsender, FysiskPost fysiskPost, Dokumentpakke dokumentpakke) {
    	this.type = FYSISK;
    	this.avsender = avsender;
    	this.dokumentpakke = dokumentpakke;
    	this.fysiskPost = fysiskPost;
    	this.digitalPost = null;
    }

	public String getKonversasjonsId() {
        return konversasjonsId;
    }

    public DigitalPost getDigitalPost() {
        return digitalPost;
    }

	public FysiskPost getFysiskPost() {
		return fysiskPost;
    }

    public Dokumentpakke getDokumentpakke() {
        return dokumentpakke;
    }

    public Prioritet getPrioritet() {
        return prioritet;
    }

    public String getSpraakkode() {
        return spraakkode;
    }

    public String getMpcId() {
        return mpcId;
    }

    public Avsender getAvsender() {
        return avsender;
    }

    /**
     * @param avsender Ansvarlig avsender av forsendelsen. Dette vil i de aller fleste tilfeller v??re
     *                             den offentlige virksomheten som er ansvarlig for brevet som skal sendes.
     * @param digitalPost Informasjon som brukes av postkasseleverand??r for ?? behandle den digitale posten.
     * @param dokumentpakke Pakke med hoveddokument og evt vedlegg som skal sendes.
     */
    public static Builder digital(Avsender avsender, DigitalPost digitalPost, Dokumentpakke dokumentpakke) {
        return new Builder(avsender, digitalPost, dokumentpakke);
    }

	public static Builder fysisk(Avsender avsender, FysiskPost fysiskPost, Dokumentpakke dokumentpakke) {
	    return new Builder(avsender, fysiskPost, dokumentpakke);
    }

    public static class Builder {

        private final Forsendelse target;
        private boolean built = false;

        private Builder(Avsender avsender, DigitalPost digitalPost, Dokumentpakke dokumentpakke) {
            this.target = new Forsendelse(avsender, digitalPost, dokumentpakke);
        }

        private Builder(Avsender avsender, FysiskPost fysiskPost, Dokumentpakke dokumentpakke) {
            this.target = new Forsendelse(avsender, fysiskPost, dokumentpakke);
        }

        /**
         * Unik ID opprettet og definert i en initiell melding og siden bruk i alle tilh??rende kvitteringer knyttet til den opprinnelige meldingen.
         * Skal v??re unik for en avsender.
         *
         * Standard er {@link java.util.UUID#randomUUID()}}.
         */
        public Builder konversasjonsId(String konversasjonsId) {
            target.konversasjonsId = konversasjonsId;
            return this;
        }

        /**
         * Standard er {@link no.difi.sdp.client2.domain.Prioritet#NORMAL}
         */
        public Builder prioritet(Prioritet prioritet) {
            target.prioritet = prioritet;
            return this;
        }

        /**
         * Spr??kkode i henhold til ISO-639-1 (2 bokstaver). Brukes til ?? informere postkassen om hvilket spr??k som benyttes, slik at varselet om mulig kan vises i riktig spr??kkontekst.
         *
         * Standard er NO.
         */
        public Builder spraakkode(String spraakkode) {
            target.spraakkode = spraakkode;
            return this;
        }

        /**
         * Brukes til ?? skille mellom ulike kvitteringsk??er for samme tekniske avsender. En forsendelse gjort med en
         * MPC Id vil kun dukke opp i kvitteringsk??en med samme MPC Id.
         *
         * Standardverdi er blank MPC Id.
         *
         * @see no.difi.sdp.client2.domain.kvittering.KvitteringForespoersel.Builder#mpcId(String)
         */
        public Builder mpcId(String mpcId) {
            target.mpcId = mpcId;
            return this;
        }

        public Forsendelse build() {
            if (built) throw new IllegalStateException("Can't build twice");
            built = true;
            return target;
        }
    }

	public TekniskMottaker getTekniskMottaker() {
		switch (type) {
    		case DIGITAL: return digitalPost.getMottaker().getMottakersPostkasse();
    		case FYSISK: return fysiskPost.getUtskriftsleverandoer();
    		default: throw new IllegalStateException("Forsendelse av type " + type + " har ikke teknisk mottaker");
		}
    }

}
