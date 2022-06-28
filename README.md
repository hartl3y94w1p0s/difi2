Sikker Digital Post Javaklient
==============================

Dette er en Java-klient for sending av sikker digital post for det offentlige.
Formålet er å forenkle integrasjonen som må utføres av avsendervirksomheter.
For mer informasjon om sikker digital post se http://begrep.difi.no/SikkerDigitalPost/.

Forutsetninger
--------------

For å starte sending av digital post må følgende være på plass:

* Avsender må være registrert hos Meldingsformidler
* Avsender må være registrert hos postkassene
* Avsender må ha et gyldig virksomhetssertifikat

Tekniske krav:

* Java 1.6 eller nyere
* (Maven for å hente ned sikker-digital-post-klient)

Sertifikater
------------

For å bruke klienten må det settes opp en keystore med et gyldig virksomhetssertifikat. Keystoren må inneholde sertifikatkjeden helt opp til rot-CAen for sertifikatutstederen.
En PKCS#12-fil fra en sertifikatutsteder vil normalt inneholde alle de nødvendige sertifikatene.

Bruk Java Keytool for å opprette et keystore fra en PKCS#12-fil (.p12):

```bash
keytool -importkeystore -srckeystore pcks12-fil.p12 -srcstoretype pkcs12 -destkeystore min-keystore.jce -deststoretype jceks
```

Eksempelkode
------------

Det er satt opp et <a href="https://github.com/digipost/sikker-digital-post-test-sender">eksempelprosjekt</a> som viser bruk av hele klienten til å gjennomføre sending av brev og henting av kvitteringer.

Hva ligger i klientbiblioteket
------------------------------

* Bygge meldinger som inneholder EBMS, StandardBusinessDocument, ASIC-E dokumentpakke
* Sende meldinger:
    * Sende digital post
    * Hente kvittering
    * Bekrefte kvittering


Hvordan ta dette i bruk
-----------------------

Når vi får releaset til Maven Central kan artifakten lastes ned derfra.
I øyeblikket har vi kun fått ut en SNAPSHOT versjon ute i Maven central snapshot repo, se https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide#SonatypeOSSMavenRepositoryUsageGuide-4.MavenRepositories.

Legg til følgende i POM:

```xml
<dependency>
    <groupId>no.difi.sdp</groupId>
    <artifactId>sikker-digital-post-java-klient</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Debugging
---------

***Merk: innstillingene under er ikke anbefalt i produksjonsmiljøer.***

Den underliggende http-klienten har støtte for å logge meldingene som sendes over nettverket. Sett `org.apache.http.wire` til `debug` eller lavere for å slå på denne loggingen.

Biblioteket har innebygd støtte for å outputte den genererte ASiC-E Dokumentpakken til disk for debug-formål:

```java
try {
    File tempFile = File.createTempFile("dokumentpakke", "debug");
    CreateASiCE.debug_writeArchiveToDisk(tempFile);
    System.out.println(tempFile);
} catch (IOException e) {
    throw new RuntimeException("Kunne ikke lagre dokumentpakke", e);
}
```


Spørsmål
------------------

Registrer gjerne spørsmål og kommentarer under <a href="issues">Issues</a>.
