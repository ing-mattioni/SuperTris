# Guida alla pubblicazione su Google Play Store

Passo per passo, dalla registrazione come sviluppatore alla pubblicazione di
Super Tris. I materiali citati (AAB, icona, screenshot, testi) sono gia' pronti
nel repository.

## FASE A — Registrazione come sviluppatore

**A1. Account Google.** Usa l'account Google che vuoi legare (per sempre)
all'app.

**A2. Apri il Play Console.** Vai su https://play.google.com/console →
"Crea account sviluppatore" → tipo **account personale** (non "organizzazione",
che richiederebbe un numero D-U-N-S aziendale).

**A3. Paga la quota.** **25 USD una tantum** (pagamento unico, non annuale),
con carta.

**A4. Verifica dell'identita'.** Per gli account personali nuovi Google chiede:
- documento d'identita' (carta/passaporto) da caricare
- indirizzo e recapiti verificati
- l'approvazione puo' richiedere da poche ore ad alcuni giorni

**A5. Requisito tester (IMPORTANTE per account nuovi).** Prima di poter
pubblicare **in produzione**, gli account personali recenti devono:
- fare un **test chiuso** con **almeno 12 tester** iscritti e attivi per
  **14 giorni consecutivi**
- poi richiedere l'accesso alla produzione

E' il vero collo di bottiglia sui tempi. I 12 tester possono essere amici e
parenti (bastano i loro indirizzi Gmail). Il requisito esatto cambia nel tempo:
verificalo sempre nella Console.

## FASE B — Creazione e pubblicazione dell'app

**B1. Crea l'app.** Play Console → "Crea app":
- Nome: `Super Tris`
- Lingua predefinita: `Italiano (it-IT)`
- Tipo: `Gioco`, categoria `Da tavolo`
- Gratuita; accetta le dichiarazioni (norme sviluppatore, export USA)

**B2. Play App Signing.** Impostazione di default, va bene: Google custodisce
la chiave di firma; il nostro `.jks` e' la chiave di **upload**. Niente da
caricare manualmente.

**B3. Primo AAB — Test interno.** Menu **Test → Test interno** → "Crea nuova
release":
- carica `app\build\outputs\bundle\release\app-release.aab` (v1.2.2, gia'
  firmato)
- note di rilascio: copia da `PLAY_RELEASE_NOTES_IT.md`
- aggiungi la tua email come tester e installa dal link per verificare che
  tutto funzioni prima di procedere

**B4. Scheda dello Store** (Crescita → Presenza sullo Store → Scheda
principale):
- titolo, descrizione breve (<=80 caratteri) e completa: da
  `PLAY_STORE_LISTING_IT.md`
- icona 512x512: `play-assets/play-icon-512.png`
- feature graphic 1024x500: `play-assets/feature-graphic-1024x500.png`
- screenshot telefono (min 2, disponibili 5): `play-assets/screenshot-1..5-*.png`
- email assistenza: `ing.mattioni@libero.it`

**B5. Privacy policy.** Incolla l'URL gia' online:
`https://ing-mattioni.github.io/SuperTris/`

**B6. Sicurezza dei dati (Data Safety).** Risposte pronte in
`PLAY_DATA_SAFETY_IT.md`:
- raccogli/condividi dati? **No**
- dati trattati solo sul dispositivo? **Si**
- privacy policy richiesta? **Si** (gia' fornita)

**B7. Classificazione dei contenuti.** Questionario IARC: gioco da tavolo,
nessuna violenza/contenuto sensibile → esito atteso **PEGI 3 / Everyone**.

**B8. Pubblico e contenuti.**
- fascia d'eta' target: consigliato **13+** (evita i requisiti extra del
  programma "Progettato per le famiglie")
- app non pensata per bambini
- nessuna pubblicita' (dichiaralo)

**B9. Altre dichiarazioni.** La checklist della Console chiede: app di governo?
no; app finanziaria? no; contenuti generati dagli utenti? no.

**B10. Test chiuso** (sblocca la produzione per gli account nuovi). Menu
**Test → Test chiuso** → crea release, carica lo stesso AAB, aggiungi una lista
di **>=12 tester** e mandagli il link. Devono installare e tenere l'app per
**14 giorni**. Poi comparira' il pulsante per richiedere l'accesso alla
produzione.

**B11. Produzione.** Soddisfatto il requisito tester: menu **Produzione → Crea
release** → carica l'AAB → invia per la revisione di Google (da poche ore a
qualche giorno). Ad approvazione, l'app e' pubblica sul Play Store.

## Promemoria pratici

- **Backup della chiave di upload**: `keystore.properties` + cartella
  `.signing/`. Non sono nel repository. Senza backup, in caso di perdita del PC
  serve chiedere a Google il reset della chiave di upload. Farlo subito.
- **Ogni aggiornamento** richiede un `versionCode` piu' alto. La v1.2.2 e'
  `versionCode 4`; le prossime saranno `5`, `6`, ...
- **Rigenerare l'AAB**: `build.bat :app:bundleRelease` (su questa macchina
  serve `build.bat`, non `gradlew.bat` diretto — vedi README).
- **Ordine dei canali**: Interno (subito, per te) → Chiuso (i 12 tester,
  requisito) → Produzione.
