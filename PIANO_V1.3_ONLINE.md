# Piano di sviluppo v1.3 — "Gioca in 2 a distanza" (online via Firebase)

Stato: **pianificato, non iniziato**. Prerequisito: v1.2 pubblicata e stabile.

## Obiettivo

Terza modalita' a 2 giocatori: partite via internet, ognuno a casa propria.
Modello "stanza privata con codice": chi crea la partita riceve un codice
(es. `TRIS-A7K2`), lo manda all'amico su WhatsApp, l'amico lo inserisce e si
gioca. Niente matchmaking pubblico, niente classifiche, niente account: fuori
scope per la v1.3.

## Scelta tecnologica

**Cloud Firestore + Firebase Anonymous Auth**, piano gratuito (Spark).

Perche' Firestore e non alternative:
- gioco a turni = payload minuscoli e bassa frequenza: i listener realtime di
  Firestore (latenza 100-500 ms) sono piu' che sufficienti;
- Anonymous Auth da' un uid senza chiedere registrazioni all'utente;
- Security Rules lato server senza scrivere un backend;
- TTL nativo sui documenti per la pulizia delle stanze scadute (niente Cloud
  Functions, che richiederebbero il piano a pagamento Blaze);
- piano gratuito: 50k letture/20k scritture al giorno. Una partita completa
  costa ~80 scritture e ~160 letture: reggerebbe centinaia di partite al
  giorno a costo zero. Nessuna carta di credito richiesta.

Alternative scartate: WebRTC P2P (servirebbe comunque un server di signaling),
server WebSocket proprio (hosting e manutenzione), Realtime Database (valido,
ma Firestore ha regole di sicurezza migliori e TTL nativo).

## Il vantaggio della v1.2

L'architettura Nearby e' riutilizzabile quasi 1:1, perche' il principio e' lo
stesso: **le regole sono deterministiche, si scambiano solo le mosse**.
- l'host sorteggia simboli e primo turno (come `GameStart`);
- ogni mossa e' indicizzata con `moveCount` (stesso anti-desync di `MoveMsg`);
- `GameScreen` e' gia' disaccoppiata e gestisce gia' `opponentName`, blocco
  del turno, rivincita, dialog di disconnessione.

Differenza chiave: con Firestore non serve un protocollo di messaggi — il
documento della stanza E' lo stato condiviso, e i client lo osservano.

## Modello dati (Firestore)

```
rooms/{roomCode}:
  status: "waiting" | "playing" | "finished"
  hostUid, guestUid          // da Anonymous Auth
  hostName, guestName        // nickname scelti dai giocatori
  hostMark, firstTurn        // sorteggio dell'host alla partenza
  moves: [ {i, micro, cell}, ... ]   // array nel documento, append via transazione
  rematch: { host: bool, guest: bool, round: int }
  lastSeen: { host: ts, guest: ts } // heartbeat presenza (~30s)
  protocolVersion: int
  expireAt: timestamp        // TTL Firestore: stanza eliminata dopo 24h di inattivita'
```

- Codice stanza: 5 caratteri da alfabeto non ambiguo (niente 0/O/1/I) ≈ 24M
  combinazioni; creazione con precondizione "non esiste" e retry.
- Mossa: transazione che verifica `index == moves.length` → niente race.
- Ripresa partita: lo stato si ricostruisce dall'array `moves` → si puo'
  chiudere l'app e rientrare nella stanza finche' non scade.

## Security Rules (concetto)

- lettura/scrittura solo per `hostUid`/`guestUid` (il guest puo' leggere una
  stanza `waiting` per unirsi, mai elencare le stanze);
- join valido solo se `guestUid == null`;
- mossa valida solo se: sei un partecipante, e' il tuo turno (parita' indice
  rispetto a `firstTurn`/`hostMark`), `index` progressivo;
- la validazione completa delle regole di gioco resta client-side (entrambi i
  client verificano ogni mossa con `SuperTrisRules`): tra amici basta, e le
  rules bloccano comunque scritture fuori turno.
- **App Check** (Play Integrity) attivo per rifiutare client non-app.

## Modifiche al progetto

- `GameMode.ONLINE` (l'enum e la UI sono gia' predisposti per estensione).
- Nuovo modulo client: `net/OnlineRoomClient` (create/join/observe/move/
  rematch/leave) che espone eventi nella stessa forma di `NearbyTransport` →
  valutare un'interfaccia comune `GameTransport` per condividere la logica
  del ViewModel con Nearby.
- UI: la scelta "Gioca in 2" guadagna la terza carta "A distanza"; nuova
  `OnlineLobbyScreen` (crea → codice + tasto condividi / unisciti → campo
  codice), stessi stili della lobby Nearby.
- **Nickname**: online serve un nome umano (non il modello del telefono).
  Campo nickname al primo uso, salvato in DataStore, riusato anche da Nearby.
- Gradle: plugin `google-services`, `firebase-bom`, `firebase-firestore`,
  `firebase-auth`, `firebase-appcheck-playintegrity`. Peso APK: +1-1,5 MB.
- Manifest: solo permesso `INTERNET` (nessun prompt runtime).
- `google-services.json` in `app/`: puo' stare nel repo pubblico (le chiavi
  che contiene finiscono comunque nell'APK; la protezione vera sono Rules +
  App Check).

## Impatti Play Store (da non dimenticare)

- **Data Safety da rifare**: l'app iniziera' a trasmettere dati — identificatore
  anonimo (uid), nickname, mosse di gioco. Non condivisi con terze parti,
  cifrati in transito, eliminati automaticamente (TTL 24h). Va dichiarato.
- **Privacy policy da aggiornare** (pagina GitHub Pages): sezione su Firebase,
  auth anonima, dati memorizzati e retention.

## Fasi di lavoro (stima ~5-6 sessioni)

| Fase | Contenuto | Stima |
|---|---|---|
| 0 | Progetto Firebase, dipendenze, App Check, Emulator Suite locale | 0,5 |
| 1 | `GameMode.ONLINE`, `OnlineRoomClient`, auth anonima, nickname | 1 |
| 2 | Security Rules + TTL, test con emulatore | 1 |
| 3 | Lobby online: crea/condividi codice, unisciti, stati di attesa/errore | 1 |
| 4 | Partita: ViewModel (riuso pattern Nearby), ripresa partita, heartbeat presenza, rivincita | 1 |
| 5 | Data Safety + privacy policy, test su reti diverse (Wi-Fi vs 4G), edge case (codice errato, stanza piena/scaduta, doppio join), release v1.3.0 | 1 |

Nota macchina di sviluppo: il **Firestore Emulator e' un processo Java** →
su questo PC va lanciato con TMP reindirizzato (stesso workaround di
`build.bat`), altrimenti fallisce per il blocco EDR sui socket in %TEMP%.

## Fuori scope v1.3 (candidati v1.4+)

- Partite asincrone con notifica push "e' il tuo turno" (richiede FCM +
  Cloud Functions → piano Blaze);
- matchmaking pubblico / classifica / cronologia partite;
- chat o emoji in partita.

## Rischi e mitigazioni

- **Limiti free tier**: piu' che sufficienti per uso tra amici; se l'app
  decolla, passaggio a Blaze con budget alert (i costi restano centesimi).
- **Codici stanza indovinabili**: 24M combinazioni + join solo su slot libero
  + TTL 24h → rischio trascurabile.
- **Versioni app diverse**: campo `protocolVersion` nella stanza, come per
  Nearby.
- **Abbandoni**: heartbeat + banner "l'avversario sembra offline"; la stanza
  resta valida 24h, quindi si puo' anche riprendere piu' tardi.
