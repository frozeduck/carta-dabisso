# Carta d’Abisso

Prototipo originale di action RPG subacqueo con scenari e personaggi di carta disegnati in Canvas. Tre aree collegate, combattimenti in tempo reale, combo di arpione, marea, scatto invulnerabile, cure, bottino, scelte di potenziamento e un boss finale. Tutti i testi sono in italiano.

**[Scarica l’APK Android v0.1.0](https://github.com/frozeduck/carta-dabisso/releases/download/v0.1.0/carta-dabisso-0.1.0-debug.apk)** · [Release e checksum](https://github.com/frozeduck/carta-dabisso/releases/latest)

L’app funziona offline e richiede Android 8.0 o successivo. Scaricare l’APK dal telefono, aprirlo e, se richiesto, consentire al browser di installare app da questa fonte. È una build debug firmata per il testing. Firma, contenuto del pacchetto e asset offline sono stati verificati; manca ancora la prova su un dispositivo Android reale. La partita non viene salvata chiudendo l’app.

Il progetto risiede su Debian in `/opt/docker/carta-dabisso`.

APK Android offline: `npm run android:debug`. Il pacchetto firmato di testing viene scritto in `downloads/carta-dabisso-0.1.0-debug.apk`. Richiede Android 8.0 o successivo e usa lo schermo verticale. Gli asset sono inclusi nel pacchetto, senza font online o permesso Internet. La chiave debug locale va conservata per installare gli aggiornamenti senza disinstallare l’app. `node tests/browser.mjs --android` verifica gli asset Android nel browser con rete disattivata, incluso il layout touch a 360 px; non sostituisce una prova sul dispositivo Android.

La build riusa JDK 21, Android SDK 36 in `/usr/lib/android-sdk` e la cache Gradle presenti su Debian. Su un altro host impostare `android/local.properties` con il percorso del proprio SDK; per il primo download delle dipendenze omettere `--offline` dallo script. L’APK viene verificato con `scripts/check-apk.py`.

Download in LAN: `docker compose up -d`, quindi http://192.168.1.200:8142/. APK diretto: http://192.168.1.200:8142/carta-dabisso-0.1.0-debug.apk.

Avvio della versione browser locale: `npm start`, quindi aprire http://127.0.0.1:8143. Verifica della logica: `npm test`. Nessuna dipendenza npm o build richiesta per la versione browser. Il test browser `node tests/browser.mjs` usa il server sulla porta 8143 e la copia Playwright già installata su questo host.

Comandi: WASD o frecce per muoversi, mouse o J per attaccare, E per marea, spazio per scatto, Q per cura, Esc per pausa. Su touchscreen: joystick sinistro e pulsanti a destra. Toccare il fondale per attaccare in una direzione. Le aree rosse anticipano gli attacchi nemici; le maree cancellano i proiettili vicini. Dopo aver sconfitto tutti i nemici, attraversare l’arco luminoso a est.

Il prototipo dura circa 5–10 minuti. Non salva i progressi tra le visite. Il suono sintetizzato è facoltativo. I font di Google sono facoltativi e hanno fallback locali; il gioco funziona senza risorse grafiche o librerie esterne.

`dist/` contiene tutti i file pubblicabili. `.openai/hosting.json` identifica il Site privato. I personaggi e gli scenari sono originali.
