# Release Play Store

## Signing locale

I file di signing reali non sono nel repository:

- `keystore.properties`
- `.signing/supertris-upload.jks`
- `.signing/release-keystore-info.txt`

Per ricreare la configurazione da zero:

1. Copia `keystore.properties.example` in `keystore.properties`.
2. Inserisci alias e password reali.
3. Genera o ripristina il file `.signing/supertris-upload.jks`.

## Build del bundle

Da PowerShell nella root del progetto:

```powershell
.\gradlew.bat :app:bundleRelease
```

Su macchine dove la build fallisce con "Unable to establish loopback connection"
(endpoint security che blocca i socket in %TEMP%), usare invece:

```powershell
.\build.bat :app:bundleRelease
```

Output atteso:

- `app\build\outputs\bundle\release\app-release.aab`

## Nota Play Store

Il progetto ora usa `targetSdk = 36` e `compileSdk = 36`.
Prima di pubblicare, verifica sempre i requisiti Play Store correnti.

## Nota permessi (dalla v1.2)

La modalita' "su due telefoni vicini" usa Nearby Connections e dichiara i
permessi Bluetooth/NEARBY_WIFI_DEVICES (con `neverForLocation`) piu' i
permessi legacy di localizzazione solo per Android 12 e precedenti.
Nella scheda Data Safety: nessun dato raccolto ne' condiviso, la
comunicazione avviene solo tra i due dispositivi.