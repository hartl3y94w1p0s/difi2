package no.difi.sdp.client2.domain.fysisk_post;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static no.difi.sdp.client2.util.Convenience.onlyNonNull;

/**
 * Adresse som skrives på konvolutt for sending av fysisk post. Bruk
 * {@link KonvoluttAdresse#build(String) KonvoluttAdresse.build(navn)} og en av
 * de påfølgende metodene på returnert {@link Builder} for å spesifisere adressen:
 *
 * <ul>
 *   <li>{@link Builder#iNorge(String, String, String, String, String) iNorge(..)}</li>
 *   <li>{@link Builder#iUtlandet(String, String, String, String, Landkode) iUtlandet(.., Landkode)} (foretrukket for utenlandske adresser)</li>
 *   <li>eller ev. {@link Builder#iUtlandet(String, String, String, String, String) iUtlandet(..)} dersom man ikke har landkode tilgjengelig.</li>
 * </ul>
 */
public class KonvoluttAdresse {

	public enum Type { NORSK, UTENLANDSK }

	private Type type;
	private String navn;
	private List<String> adresselinjer;

	private String postnummer;
	private String poststed;

	private String landkode;
	private String land;

	public static KonvoluttAdresse.Builder build(String mottakersNavn) {
		return new Builder(mottakersNavn);
	}

	public boolean er(Type type) {
		return this.type == type;
    }

	public Type getType() {
		return type;
    }

	public String getNavn() {
		return navn;
	}

	public List<String> getAdresselinjer() {
		return unmodifiableList(adresselinjer);
	}

	public String getLandkode() {
		return landkode;
	}

	public String getLand() {
		return land;
	}

	public String getPostnummer() {
		return postnummer;
	}

	public String getPoststed() {
		return poststed;
	}






	/**
	 * Builder for å opprette {@link KonvoluttAdresse}.
	 */
	public static final class Builder {

		private final KonvoluttAdresse postadresse;
		private boolean built = false;

		private Builder(String mottakersNavn) {
			postadresse = new KonvoluttAdresse();
			postadresse.navn = mottakersNavn;
		}


		/**
		 * Lag norsk postadresse for fysisk post.
		 *
		 * @param adresselinje1
		 * @param adresselinje2 (valgfri)
		 * @param adresselinje3 (valgfri)
		 * @param postnummer
		 * @param poststed
		 * @return
		 */
		public Builder iNorge(String adresselinje1, String adresselinje2, String adresselinje3, String postnummer, String poststed) {
			postadresse.type = Type.NORSK;
			postadresse.adresselinjer = onlyNonNull(adresselinje1, adresselinje2, adresselinje3);
			postadresse.postnummer = postnummer;
			postadresse.poststed = poststed;
			return this;
		}



		/**
		 * Lag utenlandsk postadresse for fysisk post.
		 * <strong>Denne metoden er den prefererte måten å angi utenlandsk fysisk postadresse.</strong>
		 * Se for øvrig dokumentasjon på
		 * <a href="http://begrep.difi.no/SikkerDigitalPost/begrep/FysiskPostadresse">
		 *   http://begrep.difi.no/SikkerDigitalPost/begrep/FysiskPostadresse
		 * </a>
		 *
		 * @param adresselinje1 Første adresselinje
		 * @param adresselinje2 Andre adresselinje (valgfri)
		 * @param adresselinje3 Tredje adresselinje (valgfri)
		 * @param adresselinje4 Fjerde adresselinje (valgfri)
		 * @param landkode <a href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO_3166-1_alpha-2</a> landkode.
		 *
		 * @return builder. Kall {@link #build()} for å få en {@link KonvoluttAdresse}.
		 *
		 * @see Landkoder
		 * @see Landkoder.Predefinert
		 * @see Landkoder#landkode(String)
		 */
		public Builder iUtlandet(String adresselinje1, String adresselinje2, String adresselinje3, String adresselinje4, Landkode landkode) {
			return iUtlandet(adresselinje1, adresselinje2, adresselinje3, adresselinje4, null, landkode);
		}



		/**
		 * Lag utenlandsk postadresse for fysisk post. Denne metoden kan brukes dersom avsender ikke har
		 * mulighet til å benytte landkode. <strong>Det anbefales å bruke
		 * {@link #iUtlandet(String, String, String, String, Landkode)} i stedet</strong>, hvor man angir en
		 * utvetydig {@link Landkode}.
		 *
		 * @param adresselinje1 Første adresselinje
		 * @param adresselinje2 Andre adresselinje (valgfri)
		 * @param adresselinje3 Tredje adresselinje (valgfri)
		 * @param adresselinje4 Fjerde adresselinje (valgfri)
		 * @param land postadressens land.
		 *
		 * @return builder. Kall {@link #build()} for å få en {@link KonvoluttAdresse}.
		 *
		 */
		public Builder iUtlandet(String adresselinje1, String adresselinje2, String adresselinje3, String adresselinje4, String land) {
			return iUtlandet(adresselinje1, adresselinje2, adresselinje3, adresselinje4, land, null);
		}


		private Builder iUtlandet(String adresselinje1, String adresselinje2, String adresselinje3, String adresselinje4, String land, Landkode landkode) {
			postadresse.type = Type.UTENLANDSK;
			postadresse.adresselinjer = onlyNonNull(adresselinje1, adresselinje2, adresselinje3, adresselinje4);
			postadresse.land = land;
			postadresse.landkode = landkode != null ? landkode.getKode() : null;
			return this;
		}


		/**
		 * @return ferdig {@link KonvoluttAdresse}
		 */
		public KonvoluttAdresse build() {
			if (built) throw new IllegalStateException("Can't build twice");
            built = true;
            return postadresse;
		}
	}


}
