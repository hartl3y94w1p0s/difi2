package no.difi.sdp.client.internal;

import no.difi.begrep.sdp.schema_v10.*;
import no.difi.sdp.client.domain.Prioritet;
import no.difi.sdp.client.domain.kvittering.*;
import no.posten.dpost.offentlig.api.representations.*;

import java.util.Date;

public class KvitteringBuilder {

    public EbmsPullRequest buildEbmsPullRequest(Organisasjonsnummer meldingsformidlerOrgNummer, Prioritet prioritet) {
        EbmsMottaker meldingsformidler = new EbmsMottaker(meldingsformidlerOrgNummer);

        if (prioritet == Prioritet.PRIORITERT) {
            return new EbmsPullRequest(meldingsformidler, EbmsOutgoingMessage.Prioritet.PRIORITERT);
        }

        return new EbmsPullRequest(meldingsformidler);
    }

    public ForretningsKvittering buildForretningsKvittering(EbmsApplikasjonsKvittering applikasjonsKvittering) {
        SimpleStandardBusinessDocument sbd = applikasjonsKvittering.getStandardBusinessDocument();

        if (sbd.erKvittering()) {
            SimpleStandardBusinessDocument.SimpleKvittering kvittering = sbd.getKvittering();
            SDPKvittering sdpKvittering = kvittering.kvittering;

            String konversasjonsId = kvittering.getKonversasjonsId();
            Date tidspunkt = sdpKvittering.getTidspunkt().toDate();

            if (sdpKvittering.getAapning() != null) {
                return AapningsKvittering.builder(tidspunkt, konversasjonsId).build();
            } else if (sdpKvittering.getLevering() != null) {
                return LeveringsKvittering.builder(tidspunkt, konversasjonsId).build();
            } else if (sdpKvittering.getTilbaketrekking() != null) {
                SDPTilbaketrekkingsresultat tilbaketrekking = sdpKvittering.getTilbaketrekking();
                TilbaketrekkingsStatus status = mapTilbaketrekkingsStatus(tilbaketrekking.getStatus());

                return TilbaketrekkingsKvittering.builder(tidspunkt, konversasjonsId, status)
                        .beskrivelse(tilbaketrekking.getBeskrivelse())
                        .build();
            } else if (sdpKvittering.getVarslingfeilet() != null) {
                SDPVarslingfeilet varslingfeilet = sdpKvittering.getVarslingfeilet();
                Varslingskanal varslingskanal = mapVarslingsKanal(varslingfeilet.getVarslingskanal());

                return VarslingFeiletKvittering.builder(tidspunkt, konversasjonsId, varslingskanal)
                        .beskrivelse(varslingfeilet.getBeskrivelse())
                        .build();
            }
        } else if (sbd.erFeil()) {
            //todo: mangler domeneobjekt for feilhåndtering
            return null;
        }
        //todo: proper exception handling
        throw new RuntimeException("Kvittering tilbake fra meldingsformidler var hverken kvittering eller feil.");
    }

    private Varslingskanal mapVarslingsKanal(SDPVarslingskanal varslingskanal) {
        if (varslingskanal == SDPVarslingskanal.EPOST) {
            return Varslingskanal.EPOST;
        }
        return Varslingskanal.SMS;
    }

    private TilbaketrekkingsStatus mapTilbaketrekkingsStatus(SDPTilbaketrekkingsstatus status) {
        if (status == SDPTilbaketrekkingsstatus.OK) {
            return TilbaketrekkingsStatus.OK;
        }
        return TilbaketrekkingsStatus.FEILET;
    }
}
