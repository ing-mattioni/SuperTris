# Setup Firebase per la modalita' online (v1.3)

La modalita' "Gioca in 2 a distanza" richiede un progetto Firebase (gratuito).
Senza `google-services.json` l'app compila e funziona comunque: la carta
"A distanza" risulta semplicemente disattivata.

## 1. Creare il progetto (una tantum, ~10 minuti)

1. Vai su https://console.firebase.google.com e accedi col tuo account Google.
2. "Aggiungi progetto" → nome `SuperTris` → Google Analytics: **disattivato**
   (non ci serve e semplifica la Data Safety).
3. Nel progetto: "Aggiungi app" → Android:
   - Package name: `it.claudio.supertris`
   - Nickname: `Super Tris`
   - SHA-1: facoltativo per ora (servira' per App Check/Play Integrity)
4. Scarica **google-services.json** e mettilo in `app/google-services.json`.
   Il file puo' stare nel repository: le chiavi che contiene finiscono
   comunque dentro l'APK, la protezione vera sono le Security Rules.

## 2. Abilitare i servizi

Nella console Firebase:

1. **Authentication** → Sign-in method → abilita **Anonimo**.
2. **Firestore Database** → Crea database → modalita' **produzione** →
   location `eur3 (europe-west)`.
3. **Regole**: incolla il contenuto di `firestore.rules` (tab "Regole" →
   pubblica). In alternativa via CLI: `firebase deploy --only firestore:rules`.
4. **TTL** (pulizia stanze scadute): Firestore → "Gestione dati TTL" →
   nuova policy su collection `rooms`, campo `expireAt`.
5. (Consigliato prima della release) **App Check** → registra l'app con
   Play Integrity e aggiungi la dipendenza
   `com.google.firebase:firebase-appcheck-playintegrity`.

## 3. Test locale con l'Emulator Suite (facoltativo ma consigliato)

Richiede Node.js e la CLI Firebase (`npm i -g firebase-tools`).

```powershell
# ATTENZIONE su questa macchina: l'emulatore Firestore e' un processo Java,
# serve il redirect di TMP come per build.bat:
$env:TMP = "C:\superTris\tmp"; $env:TEMP = "C:\superTris\tmp"
firebase emulators:start
```

UI dell'emulatore su http://localhost:4000. Per puntare l'app all'emulatore
(solo build di sviluppo) usare `FirebaseFirestore.useEmulator("10.0.2.2", 8080)`
e `FirebaseAuth.useEmulator("10.0.2.2", 9099)` prima del primo accesso.

## 4. Prima della release v1.3 sul Play Store

- Aggiornare la **Data Safety** (l'app trasmette: uid anonimo, nickname,
  mosse di gioco — non condivisi, cifrati in transito, TTL 24h).
- Aggiornare la **privacy policy** su GitHub Pages (sezione Firebase).
- Vedi `PIANO_V1.3_ONLINE.md` per il quadro completo.
