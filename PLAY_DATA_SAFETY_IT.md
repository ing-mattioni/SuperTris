# Play Console Data Safety

Base attuale del progetto `Super Tris`.

Inferenza dal codice al 2026-07-06 (v1.2.2):
- permessi Bluetooth/Nearby dichiarati SOLO per la modalita' "due telefoni
  vicini" (Nearby Connections): BLUETOOTH_SCAN e NEARBY_WIFI_DEVICES con
  flag `neverForLocation`, localizzazione legacy solo su Android 12 e
  precedenti come richiesto dalla piattaforma
- la comunicazione multiplayer avviene esclusivamente tra i due dispositivi
  (peer-to-peer locale), nessun server coinvolto
- nessuna raccolta dati verso server visibile nel codice
- salvataggio locale della partita tramite DataStore sul dispositivo
- nessun account utente, analytics, advertising o login implementato

## Dichiarazione suggerita in Play Console

- Il'app raccoglie dati personali? `No`
- L'app condivide dati con terze parti? `No`
- I dati sono elaborati solo sul dispositivo? `Sì`
- È richiesta una privacy policy? `Sì`, comunque necessaria per il Data safety form

## Note operative

- Se in futuro aggiungi analytics, crash reporting, ads, login, cloud save o servizi AI remoti, questa dichiarazione andrà aggiornata.
- Verifica sempre il manifest finale e le librerie incluse nella release prima della pubblicazione.