@echo off
rem Wrapper di build per questa macchina (Windows + endpoint security aziendale).
rem
rem PROBLEMA: l'EDR blocca i file socket AF_UNIX dentro %TEMP% dell'utente.
rem Il JDK moderno (17+) crea li' i socket interni dei Selector NIO, quindi
rem Gradle fallisce con "java.io.IOException: Unable to establish loopback connection".
rem
rem SOLUZIONE: reindirizzare TMP/TEMP a una cartella locale al progetto
rem (non bloccata) prima di lanciare Gradle. Tutti i processi figli
rem (daemon Gradle, daemon Kotlin, worker di test) ereditano la variabile.
rem
rem USO:  build.bat :app:assembleDebug
rem       build.bat :app:testDebugUnitTest
rem       build.bat :app:bundleRelease
setlocal
set TMP=%~dp0.tmp-build
set TEMP=%~dp0.tmp-build
if not exist "%TMP%" mkdir "%TMP%"
call "%~dp0gradlew.bat" %*
endlocal
