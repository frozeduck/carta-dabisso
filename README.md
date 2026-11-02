# Carta d’Abisso

ARPG isometrico subacqueo in **Android nativo** (SurfaceView + Canvas a 60 FPS) in **orientamento orizzontale (Landscape)**, progettato con feeling e dinamiche **Diablo-like** e stile artistico **Paper Mario Pop-Up**.

**[Scarica l’APK Android v0.6.0](downloads/carta-dabisso-0.6.0-debug.apk)** · [Server locale di download](http://192.168.1.200:8142)

---

## Caratteristiche Tecniche e Gameplay (v0.6.0)

- **Art Dedicata per le 3 Classi di Gioco**:
  - *Cacciatore dei Mari*: sprite ad alta risoluzione con fucile ad arpione balistico con mirino e pinne da immersione aerodinamiche.
  - *Custode dei Fondali*: massiccia corazza a campana da palombaro con spallacci imbullonati e colossale ancora in ferro da mischia.
  - *Mistico dell'Abisso*: tonaca cerimoniale abissale con cappuccio intessuto di rune e bastone di corallo sormontato da perla di marea splendente.
- **Menu Iniziale Gestione Multipli Personaggi (Archivio Palombari)**:
  - Fino a 3 slot personaggio indipendenti con salvataggio isolato per livello, classe, scafandro, inventario e progressione.
  - Scheda anteprima eroe con ritratto animato, statistiche, conchiglie, pulsante "GIOCA", "ELIMINA" e "＋ NUOVO EROE".
  - Accesso e cambio eroe in ogni momento dall'Hub del Santuario tramite il tasto `👥 EROI`.
- **Identità di Classe Permanente & Albero Abilità Ridisegnato**:
  - Classe bloccata permanentemente all'atto della creazione (niente switch mid-game nell'albero).
  - Banner di classe dedicato in testata con stemma, livello ed eroe.
  - Condotti idraulici energetici con frecce direzionali tra i Tier (Tier 1 -> Tier 2 -> Tier 3).
  - Medaglioni runici con numerazione romana e pulsante "Azzera Talenti" (Respec per 25 conchiglie).
- **Nuova Estetica del Fondale Marino nell'Hub Santuario Nautilus**:
  - Dune sabbiose dorate con cuciture decorative artigianali Paper Mario.
  - Piazza centrale circolare in ciottoli antichi con decorazione radiale e mosaico a spirale del Nautilus.
  - Percorsi lastricati verso tutte le botteghe, colonne antiche e coralli decorativi sulle quinte.
  - Correzione spaziatura cartigli/badge sopra la testa di Mastro Paguro, Mastro Astice, Cassa e Anziana Tartaruga.
- **Risoluzione Crash Atto 1 & Architettura Thread-Safe**:
  - Blocco unificato di sincronizzazione tra il thread di rendering a 60 FPS (`GameLoopThread`) e il thread UI principale Android (`onTouchEvent`).
  - Iterazione indicizzata priva di `ConcurrentModificationException` per nemici, forzieri sommersi e drop.
- **Mappe Dinamiche ad Atti & Forzieri Sommersi**:
  - Dimensioni mappa scalate per ciascun Atto (da 2400x1400 fino a 2800x1600).
  - 3 settori sequenziali per Atto (Ingresso, Esplorazione con Forziere Sommerso apribile, e Tana del Boss).
- **Progressione a 4 Atti Diablo**:
  - *Atto I: La Barriera Silenziosa (300m)* - Boss: *Granchio Gigante Corazzato*.
  - *Atto II: La Fossa delle Tenebre (850m)* - Boss: *Murena Zannuta Primordiale*.
  - *Atto III: La Dorsale Vulcanica (1450m)* - Boss: *Rana Pescatrice Abissale*.
  - *Atto IV: L'Abisso Infinito (2400m+)* - Boss: *Leviatano Primordiale* & Endless Dive infinito.
- **Schermata di Creazione Personaggio**:
  - Selezione fra 3 classi distinte con attacchi unici.
  - Personalizzazione cromatica muta subacquea con 5 stili (Oceano, Corallo, Oro Abisso, Smeraldo, Ametista).
  - Generatore di nomi marinareschi casuali con dado e visualizzazione modello Paper Mario pop-up.
- **Santuario "L'Avamposto del Nautilus" (Hub Cittadino)**:
  - Mappa santuario non combattiva con telecamera estesa (`1400x800`).
  - **Mastro Astice (Fabbro)**: Smantella equipaggiamenti per Frammenti Abissali e Conchiglie, Potenzia oggetti fino a +5 (+12% danno/difesa/PV per livello), Forgia armi e corazze Rare o Abissali.
  - **Mastro Paguro (Alchimista/Bottega)**: Vendita di pozioni, forzieri misteriosi e Altare delle Reliquie permanenti.
  - **Cassa di Spedizione (Stash a 16 Slot)**: Deposito sicuro e permanente di armi, corazze e reliquie tra le immersioni.
  - **Anziana Tartaruga (Archivio di Storia)**: Diario missioni, cronache abissali e stato avanzamento taglie degli Atti.
  - **Portale di Spedizione (Waypoint)**: Mappa ad acquerello per selezionare l'Atto e immergersi direttamente.
  - **Portale Cittadino (Diablo Town Portal)**: Pulsante HUD dedicato durante la spedizione; teletrasporto istantaneo all'Avamposto e portale di ritorno attivo per rientrare esattamente alle stesse coordinate nel dungeon!
- **Classi di Gioco con Attacchi Primari Distinti**:
  - *Cacciatore dei Mari*: arpione perforante balistico ad altissima velocità e raggio lungo con multishot.
  - *Custode dei Fondali*: fendente melee ad ancora ad arco semicircolare a corto raggio (140px, scaling 1.35x danno + 0.5x difesa, forte contraccolpo).
  - *Mistico dell'Abisso*: globi marini a traiettoria guidata fluida verso il nemico più vicino (homing orbs, recupero +3 Marea a colpo).
- **Architettura Android 100% Nativa**: Nessun vincolo o dipendenza Web/DOM/WebView; ciclo di gioco hardware-accelerated a 60 FPS direttamente su `SurfaceView` e `Canvas` Android in Java (`GameView`, `Game`, `AudioHelper`, `MainActivity`).
- **Supporto Samsung Galaxy Z Flip 7 Fullscreen**: Coordinate virtuali responsive adattate alle proporzioni ultra-wide 22:9 senza bande nere.
- **Orientamento Orizzontale (Landscape)**: Layout a 16:9 (`960x540` virtuale) con scaling automatico e immersive sticky fullscreen.
- **HUD Diablo-like (Senza Emoji)**:
  - **Sfera della Vita (Rossa)** in basso a sinistra con rendering dei fluidi, rifrazione di vetro e lettura numerica PV.
  - **Sfera della Marea (Blu)** in basso a destra con rigenerazione dinamica e livello di energia/mana.
  - **Cintura delle Abilità** centrale con icone vettoriali procedurali:
    - `Arpione` (Attacco primario balistico con proiettili direzionali).
    - `Marea` (Onda d'urto ad anello a 360° che respinge e distrugge i proiettili).
    - `Scatto` (Balzo di propulsione rapida con frame di invulnerabilità).
    - `Pozione` (Fiala di cura curativa immediata).
    - `Zaino / Inventario` (Pannello paperdoll e gestione bottino).
- **Sistema di Navigazione Point & Click**:
  - Tocco singolo o trascinamento continuo per muovere il palombaro.
  - Beacon dorato concentrico a scomparsa sul punto cliccato.
  - Puntamento diretto dei nemici: toccando un bersaglio si attiva l'attacco primario con l'arpione.
- **Loot Drop con Rarity Beams & Inventario**:
  - Raggi di luce verticali colorati dal fondale marino:
    - *Comune* (Ardesia/Bianco)
    - *Magico* (Azzurro cobalto)
    - *Raro* (Oro ambrato)
    - *Abissale* (Ametista/Viola profondo)
  - Pannello inventario in pietra intagliata: slot `Arma`, `Corazza`, `Reliquia` e griglia di 8 slot zaino.
  - Cambiamenti cosmetici in tempo reale sul palombaro: colore scafandro in base alla corazza, lama/tridente in base all'arma ed aura orbitante in base alla reliquia.
- **Game Feel & Juice (gamedev-skills)**:
  - Screen shake basato su trauma quadratico (`trauma^2`).
  - Hit-stop (micro-pause sui colpi critici).
  - Squash & stretch sui modelli colpiti.
  - Numeri di danno fluttuanti con ingrandimento dorato per i colpi critici.
  - Telegrafi visivi circolari rossi ad espansione per gli attacchi pesanti del Boss.
  - Sintesi audio procedurale a bassa latenza su `AudioTrack` PCM a 16-bit.
- **Zero Emoji**: Tutta l'interfaccia, icone, fiale, glifi e mostri sono generati proceduralmente via `android.graphics.Path` e primitive geometriche.

---

## Compilazione e Test

- Esecuzione test unitari nativi: `npm test`
- Compilazione APK di debug firmato: `npm run android:debug`
- Output APK: `downloads/carta-dabisso-0.3.0-debug.apk`
- Verifica APK: `python3 scripts/check-apk.py`
