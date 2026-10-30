# Carta d’Abisso

ARPG isometrico subacqueo in **Android nativo** (SurfaceView + Canvas a 60 FPS) in **orientamento orizzontale (Landscape)**, progettato con feeling e dinamiche **Diablo-like**.

**[Scarica l’APK Android v0.3.0](https://github.com/frozeduck/carta-dabisso/releases/download/v0.3.0/carta-dabisso-0.3.0-debug.apk)** · [Release e checksum](https://github.com/frozeduck/carta-dabisso/releases/latest)

---

## Caratteristiche Tecniche e Gameplay

- **Architettura Android 100% Nativa**: Nessun vincolo o dipendenza Web/DOM/WebView; ciclo di gioco hardware-accelerated a 60 FPS direttamente su `SurfaceView` e `Canvas` Android in Java (`GameView`, `Game`, `AudioHelper`, `MainActivity`).
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
