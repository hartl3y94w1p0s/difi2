package no.difi.sdp.client.domain;

import java.util.Date;

public class Forsendelse {

    private String konversasjonsId;
    private Prioritet prioritet;
    private MottakerVirksomhet mottakerVirksomhet;
    private Mottaker mottaker;

    private Dokumentpakke dokumentpakke;

    /**
     * Når brevet tilgjengeliggjøres for mottaker. Standard er nå.
     */
    private Date virkningsdato = new Date();

    private boolean aapningskvittering;

    /**
     * Sikkerhetsnivå som kreves for å åpne brevet. Standard er nivå 3 (passord).
     */
    private Sikkerhetsnivaa sikkerhetsnivaa = Sikkerhetsnivaa.NIVAA_3;

    /**
     * Ikke-sensitiv tittel på brevet.
     */
    private String tittel;

    /**
     * Varsler som skal sendes til mottaker av brevet.
     */
    private Varsler varsler;

}
