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
.\gradlew.bat :app:bundleRelease --no-daemon
```

Output atteso:

- `app\build\outputs\bundle\release\app-release.aab`

## Nota Play Store

Il progetto ora usa `targetSdk = 36` e `compileSdk = 36`.
Prima di pubblicare, verifica sempre i requisiti Play Store correnti.