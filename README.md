# Carta d’Abisso

Prototipo originale di action RPG subacqueo con scenari e personaggi di carta disegnati in Canvas. Tre aree collegate, combattimenti in tempo reale, combo di arpione, marea, scatto invulnerabile, cure, bottino, scelte di potenziamento e un boss finale. Tutti i testi sono in italiano.

Avvio locale: `npm start`, quindi aprire http://127.0.0.1:8142. Verifica della logica: `npm test`. Nessuna dipendenza npm o build richiesta.

Comandi: WASD o frecce per muoversi, mouse o J per attaccare, E per marea, spazio per scatto, Q per cura, Esc per pausa. Su touchscreen: joystick sinistro e pulsanti a destra. Toccare il fondale per attaccare in una direzione. Le aree rosse anticipano gli attacchi nemici; le maree cancellano i proiettili vicini. Dopo aver sconfitto tutti i nemici, attraversare l’arco luminoso a est.

Il prototipo dura circa 5–10 minuti. Non salva i progressi tra le visite. Il suono sintetizzato è facoltativo. I font di Google sono facoltativi e hanno fallback locali; il gioco funziona senza risorse grafiche o librerie esterne.

`dist/` contiene tutti i file pubblicabili. `.openai/hosting.json` identifica il Site privato. I personaggi e gli scenari sono originali.
