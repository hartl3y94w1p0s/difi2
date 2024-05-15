package no.difi.sdp.client2.internal;

import no.difi.sdp.client2.ExceptionMapper;
import no.difi.sdp.client2.KlientKonfigurasjon;
import no.difi.sdp.client2.domain.Databehandler;
import no.difi.sdp.client2.domain.exceptions.KonfigurasjonException;
import no.difi.sdp.client2.domain.exceptions.SendException;
import no.difi.sdp.client2.domain.exceptions.XmlValideringException;
import no.digipost.api.MessageSender;
import no.digipost.api.interceptors.KeyStoreInfo;
import no.digipost.api.interceptors.TransactionLogClientInterceptor;
import no.digipost.api.interceptors.WsSecurityInterceptor;
import no.digipost.api.representations.EbmsAktoer;
import no.digipost.api.representations.EbmsApplikasjonsKvittering;
import no.digipost.api.representations.EbmsForsendelse;
import no.digipost.api.representations.EbmsPullRequest;
import no.digipost.api.representations.KanBekreftesSomBehandletKvittering;
import no.digipost.api.representations.Organisasjonsnummer;
import no.digipost.api.representations.TransportKvittering;
import no.digipost.api.xml.Schemas;
import org.apache.http.HttpRequestInterceptor;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.client.support.interceptor.PayloadValidatingInterceptor;
import org.springframework.ws.context.MessageContext;
import org.xml.sax.SAXParseException;

import java.util.Arrays;
import java.util.function.Supplier;

import static no.difi.sdp.client2.domain.exceptions.SendException.AntattSkyldig.KLIENT;
import static no.difi.sdp.client2.domain.exceptions.SendException.AntattSkyldig.SERVER;
import static no.difi.sdp.client2.domain.exceptions.SendException.AntattSkyldig.UKJENT;

public class DigipostMessageSenderFacade {

    private final MessageSender messageSender;
    private ExceptionMapper exceptionMapper = new ExceptionMapper();


    public DigipostMessageSenderFacade(final Databehandler databehandler, final KlientKonfigurasjon klientKonfigurasjon) {
        KeyStoreInfo keyStoreInfo = databehandler.noekkelpar.getKeyStoreInfo();
        WsSecurityInterceptor wsSecurityInterceptor = new WsSecurityInterceptor(keyStoreInfo, new UserFriendlyWsSecurityExceptionMapper());
        wsSecurityInterceptor.afterPropertiesSet();

        MessageSender.Builder messageSenderBuilder = MessageSender.create(klientKonfigurasjon.getMeldingsformidlerRoot(),
                keyStoreInfo,
                wsSecurityInterceptor,
                EbmsAktoer.avsender(Organisasjonsnummer.of(databehandler.organisasjonsnummer.getOrganisasjonsnummer())),
                EbmsAktoer.meldingsformidler(klientKonfigurasjon.getMeldingsformidlerOrganisasjon()))
                .withConnectTimeout((int) klientKonfigurasjon.getConnectTimeoutInMillis())
                .withSocketTimeout((int) klientKonfigurasjon.getSocketTimeoutInMillis())
                .withConnectionRequestTimeout((int) klientKonfigurasjon.getConnectionRequestTimeoutInMillis())
                .withDefaultMaxPerRoute(klientKonfigurasjon.getMaxConnectionPoolSize())
                .withMaxTotal(klientKonfigurasjon.getMaxConnectionPoolSize());

        if (klientKonfigurasjon.useProxy()) {
            messageSenderBuilder.withHttpProxy(klientKonfigurasjon.getProxyHost(), klientKonfigurasjon.getProxyPort(), klientKonfigurasjon.getProxyScheme());
        }

        // Legg til http request interceptors fra konfigurasjon pluss vår egen.
        HttpRequestInterceptor[] httpRequestInterceptors = Arrays.copyOf(klientKonfigurasjon.getHttpRequestInterceptors(), klientKonfigurasjon.getHttpRequestInterceptors().length + 1);
        httpRequestInterceptors[httpRequestInterceptors.length - 1] = new AddClientVersionInterceptor();
        messageSenderBuilder.withHttpRequestInterceptors(httpRequestInterceptors);

        messageSenderBuilder.withHttpResponseInterceptors(klientKonfigurasjon.getHttpResponseInterceptors());

        messageSenderBuilder.withMeldingInterceptorBefore(TransactionLogClientInterceptor.class, payloadValidatingInterceptor());

        for (ClientInterceptor clientInterceptor : klientKonfigurasjon.getSoapInterceptors()) {
            // TransactionLogClientInterceptoren bør alltid ligge ytterst for å sikre riktig transaksjonslogging (i tilfelle en custom interceptor modifiserer requestet)
            messageSenderBuilder.withMeldingInterceptorBefore(TransactionLogClientInterceptor.class, clientInterceptor);
        }

        messageSender = messageSenderBuilder.build();
    }

    public TransportKvittering send(final EbmsForsendelse ebmsForsendelse) {
        return performRequest(() -> messageSender.send(ebmsForsendelse));
    }

    public EbmsApplikasjonsKvittering hentKvittering(final EbmsPullRequest ebmsPullRequest) {
        return performRequest(() -> messageSender.hentKvittering(ebmsPullRequest));
    }

    public EbmsApplikasjonsKvittering hentKvittering(final EbmsPullRequest ebmsPullRequest, final KanBekreftesSomBehandletKvittering applikasjonsKvittering) {
        return performRequest(() -> messageSender.hentKvittering(ebmsPullRequest, applikasjonsKvittering));
    }

    public void bekreft(final KanBekreftesSomBehandletKvittering kanBekreftesSomBehandletKvittering) {
        performRequest(() -> messageSender.bekreft(kanBekreftesSomBehandletKvittering));

    }

    private void performRequest(final Runnable request) {
        this.performRequest(() -> {
            request.run();
            return null;
        });
    }

    private <T> T performRequest(final Supplier<T> request) throws SendException {
        try {
            return request.get();
        } catch (RuntimeException e) {
            RuntimeException mappedException = exceptionMapper.mapException(e);
            if (mappedException != null) {
                throw mappedException;
            }

            throw new SendException("An unhandled exception occured while performing request", UKJENT, e);
        }
    }

    public void setExceptionMapper(final ExceptionMapper exceptionMapper) {
        this.exceptionMapper = exceptionMapper;
    }

    protected ClientInterceptor payloadValidatingInterceptor() {
        try {
            PayloadValidatingInterceptor payloadValidatingInterceptor = new PayloadValidatingInterceptor() {
                @Override
                protected boolean handleRequestValidationErrors(final MessageContext messageContext, final SAXParseException[] errors) {
                    if (messageContext.hasResponse()) {
                        // Feil i responsen, sannsynligvis serveren sin skyld
                        throw new XmlValideringException("XML validation errors in response from server", errors, SERVER);
                    } else {
                        throw new XmlValideringException("XML validation errors in request. Maybe some fields are not being set or are set with null values?", errors, KLIENT);
                    }

                }
            };
            payloadValidatingInterceptor.setSchemas(Schemas.allSchemaResources());
            payloadValidatingInterceptor.setValidateRequest(true);
            // TODO: Responsevalidering skal skrus på når vi er sikre på at MF og postkassene leverer skikkelige responser
            payloadValidatingInterceptor.setValidateResponse(false);
            payloadValidatingInterceptor.afterPropertiesSet();
            return payloadValidatingInterceptor;
        } catch (Exception e) {
            throw new KonfigurasjonException("Unable to initialize payload validating interecptor", e);
        }
    }

}
