# Carta d’Abisso

ARPG subacqueo in **React + TypeScript + Vite + HTML5 Canvas a 60 FPS**, progettato con dinamiche e feeling **Diablo-like** e stile artistico **Paper Mario Pop-Up**.

---

## Caratteristiche di Gioco

- **3 Classi di Eroi Distinte con Identità Permanente**:
  - *Cacciatore dei Mari*: Specialista a distanza con fucile ad arpione balistico, frecce perforanti e multitiro a ventaglio.
  - *Custode dei Fondali*: Colosso corazzato da mischia con campana imbullonata e fendenti ad ancora ad arco con contraccolpo e scaling difensivo.
  - *Mistico dell'Abisso*: Stregone delle correnti con scettro corallino e perla di marea; sfere a guida fluida (homing orbs) che rigenerano Marea.
- **Archivio Palombari (3 Slot Salvataggio)**:
  - Gestione fino a 3 personaggi indipendenti salvati in memoria locale persistente con anteprima statistiche, livello, muta, conchiglie e progressione atti.
- **Santuario "L'Avamposto del Nautilus" (Hub Cittadino)**:
  - Mappa santuario con fondale a dune sabbiose e cuciture artigianali Paper Mario.
  - Mosaico a spirale del Nautilus e piazza centrale in ciottoli antichi.
  - **Mastro Astice (Fabbro)**: Potenziamento equipaggiamento fino a +5 (+15% per livello), Smantellamento bottino in Frammenti Abissali e Forgiatura di oggetti Rari e Abissali.
  - **Mastro Paguro (Alchimista / Bottega)**: Pozioni di linfa marina, Benedizioni della Marea e Forzieri Misteriosi.
  - **Cassa di Spedizione (Stash)**: 16 slot di deposito sicuro e permanente tra le immersioni.
  - **Anziana Tartaruga (Archivio di Storia)**: Cronache delle profondità, stato avanzamento missioni e taglie sui boss.
  - **Portale di Spedizione (Waypoint)**: Mappa per selezionare e immergersi direttamente negli Atti sbloccati.
  - **Portale Cittadino (Diablo Town Portal)**: Tasto dedicato per teletrasportarsi all'istante all'Avamposto e portale di ritorno per tornare al dungeon.
- **Progressione a 4 Atti & Endless Dive**:
  - *Atto I: La Barriera Silenziosa (-220m)* — Boss: *Granchio Gigante Corazzato*.
  - *Atto II: La Fossa delle Tenebre (-680m)* — Boss: *Murena Zannuta Primordiale*.
  - *Atto III: La Dorsale Vulcanica (-1450m)* — Boss: *Rana Pescatrice Abissale*.
  - *Atto IV: L'Abisso Infinito (-2400m)* — Boss: *Leviatano Primordiale*.
  - *Endless Dive*: Discesa infinita con moltiplicatori scalati di vita e danno e forzieri sommersi colmi di bottino Abissale.
- **Albero Abilità & Talenti a Conduzione Energetica**:
  - Due rami per ciascuna classe con progressione Tier I → Tier II → Tier III.
  - Tasto di reset talenti (Respec per 25 conchiglie).
- **HUD Diablo-like**:
  - Sfera della Vita (Rossa) e Sfera della Marea (Blu) con rifrazione e fluidi dinamici.
  - Cintura delle abilità centrale (Attacco, Marea AoE, Scatto, Pozione, Portale, Zaino, Talenti).
- **Juice & Sintesi Audio Procedurale**:
  - Sintetizzatore Web Audio API a bassa latenza (arpioni, bolle, ondate, colpi pesanti, rintocchi di loot in base alla rarità).
  - Screen shake basato su trauma quadratico, hit-stop sui colpi critici, raggi di rarità verticali e numeri fluttuanti in stile fumetto.

---

## Tecnologie

- **React 19** & **TypeScript**
- **Vite 6**
- **HTML5 Canvas 2D Rendering a 60 FPS**
- **Tailwind CSS 4**
- **Web Audio API**
