# Super Tris (Android)

App Android (Kotlin + Jetpack Compose) per giocare a "Super Tic Tac Toe" in italiano.

## Note (v1.2.1)
- Player vs AI (facile/medio/difficile)
- **Gioca in 2 — sullo stesso telefono** (pass-and-play)
- **Gioca in 2 — su due telefoni vicini** via Google Nearby Connections:
  connessione diretta Bluetooth/Wi-Fi, senza internet ne' router. Un telefono
  "Crea partita", l'altro "Cerca partite"; conferma con codice su entrambi.
  Richiede Google Play Services e i permessi Bluetooth/dispositivi vicini.
- Schermata **"Come si gioca"** con le regole spiegate e un esempio visivo
  della "regola d'oro" (la cella scelta manda l'avversario nella griglia
  corrispondente)
- Salvataggio automatico dopo ogni mossa ("Continua", solo partite locali)
- Simbolo (X/O) e chi inizia random a ogni nuova partita
- Indicazione a schermo della griglia in cui giocare (obbligata o libera)

Sviluppi successivi (online, statistiche, cronologia, notifiche) sono su rami
dedicati: vedi [PIANO_V1.3_ONLINE.md](PIANO_V1.3_ONLINE.md) e i branch
`versione-1.3` / `versione-1.4` / `versione-1.5`.

## Requisiti di build
- JDK 17 o superiore
- Android SDK con platform 36 (il percorso va in `local.properties`: `sdk.dir=...`)
- Toolchain: Gradle 8.11.1 (wrapper), AGP 8.9.2, Kotlin 2.1.20

## Compilare

```powershell
.\gradlew.bat :app:assembleDebug
```

APK di debug (installabile subito su qualsiasi dispositivo con "origini sconosciute" abilitate):
`app\build\outputs\apk\debug\app-debug.apk`

### Macchine con endpoint security aziendale

Se la build fallisce con `java.io.IOException: Unable to establish loopback connection`,
l'EDR sta bloccando i file socket AF_UNIX che il JDK crea in `%TEMP%`.
Usa lo script wrapper che reindirizza TMP a una cartella del progetto:

```powershell
.\build.bat :app:assembleDebug
```

## Installare su un dispositivo

1. Abilita "Opzioni sviluppatore" e "Debug USB" sul telefono.
2. Collega il telefono e lancia:

```powershell
%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe install app\build\outputs\apk\debug\app-debug.apk
```

In alternativa copia `app-debug.apk` sul telefono e aprilo dal file manager.

## Release Play Store

Vedi [PLAY_RELEASE.md](PLAY_RELEASE.md).
