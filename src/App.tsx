import React, { useState, useEffect, useRef, useReducer } from 'react';
import { GameEngine } from './game/GameEngine';
import { ActiveModal } from './types/game';
import { GameCanvas } from './components/GameCanvas';
import { DiabloHud } from './components/DiabloHud';
import { CharacterSelectModal } from './components/modals/CharacterSelectModal';
import { CharacterCreateModal } from './components/modals/CharacterCreateModal';
import { InventoryModal } from './components/modals/InventoryModal';
import { TalentsModal } from './components/modals/TalentsModal';
import { BlacksmithModal } from './components/modals/BlacksmithModal';
import { ShopModal } from './components/modals/ShopModal';
import { StashModal } from './components/modals/StashModal';
import { SageModal } from './components/modals/SageModal';
import { WaypointModal } from './components/modals/WaypointModal';
import { GameOverModal } from './components/modals/GameOverModal';
import { VictoryModal } from './components/modals/VictoryModal';
import { AudioHelper } from './audio/AudioHelper';

export const App: React.FC = () => {
  const [, forceUpdate] = useReducer((x) => x + 1, 0);
  const engineRef = useRef<GameEngine | null>(null);

  if (!engineRef.current) {
    engineRef.current = new GameEngine();
  }
  const engine = engineRef.current;

  const [activeModal, setActiveModal] = useState<ActiveModal>(null);
  const [selectedSlotForCreate, setSelectedSlotForCreate] = useState<number>(0);
  const [soundEnabled, setSoundEnabled] = useState<boolean>(true);

  // Sync state ticks for UI reactivity
  useEffect(() => {
    const timer = setInterval(() => {
      forceUpdate();
    }, 150);
    return () => clearInterval(timer);
  }, []);

  // Keyboard Shortcuts (Diablo Hotkeys: 1/Space=Attack, 2/Q=Wave, 3/E=Dash, 4/R=Potion, T=TownPortal, I=Inventory, K=Talents)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ignore if typing in text input
      if ((e.target as HTMLElement).tagName === 'INPUT') return;

      if (e.key === 'Escape') {
        setActiveModal(null);
        return;
      }

      if (engine.gameState === 'EXPEDITION') {
        if (e.code === 'Space' || e.key === '1') {
          e.preventDefault();
          engine.performAttack();
          forceUpdate();
        } else if (e.key === 'q' || e.key === 'Q' || e.key === '2') {
          e.preventDefault();
          engine.performWave();
          forceUpdate();
        } else if (e.key === 'e' || e.key === 'E' || e.key === '3') {
          e.preventDefault();
          engine.performDash();
          forceUpdate();
        } else if (e.key === 'r' || e.key === 'R' || e.key === '4') {
          e.preventDefault();
          engine.usePotion();
          forceUpdate();
        } else if (e.key === 't' || e.key === 'T') {
          e.preventDefault();
          engine.openTownPortal();
          forceUpdate();
        }
      }

      if (e.key === 'i' || e.key === 'I' || e.key === 'b' || e.key === 'B') {
        e.preventDefault();
        setActiveModal((prev) => (prev === 'INVENTORY' ? null : 'INVENTORY'));
      } else if (e.key === 'k' || e.key === 'K') {
        e.preventDefault();
        setActiveModal((prev) => (prev === 'TALENTS' ? null : 'TALENTS'));
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [engine]);

  // Actions
  const handleAttack = () => {
    engine.performAttack();
    forceUpdate();
  };

  const handleWave = () => {
    engine.performWave();
    forceUpdate();
  };

  const handleDash = () => {
    engine.performDash();
    forceUpdate();
  };

  const handlePotion = () => {
    engine.usePotion();
    forceUpdate();
  };

  const handleTownPortal = () => {
    engine.openTownPortal();
    forceUpdate();
  };

  const handleSelectSlot = (idx: number) => {
    if (engine.selectCharacter(idx)) {
      forceUpdate();
    }
  };

  const handleCreateNewSlot = (idx: number) => {
    setSelectedSlotForCreate(idx);
    engine.gameState = 'CHAR_CREATE';
    forceUpdate();
  };

  const handleCharacterCreated = () => {
    forceUpdate();
  };

  const handleStartExpedition = (actNum: number) => {
    setActiveModal(null);
    engine.startActExpedition(actNum);
    forceUpdate();
  };

  const handleReturnToHub = () => {
    engine.gameOver = false;
    engine.victory = false;
    engine.enterHub();
    forceUpdate();
  };

  const handleContinueEndless = () => {
    engine.victory = false;
    engine.startActExpedition(5);
    forceUpdate();
  };

  return (
    <main className="relative w-screen h-screen overflow-hidden bg-slate-950 font-sans">
      {/* 60 FPS HTML5 Canvas */}
      <GameCanvas
        engine={engine}
        onOpenStation={(station) => setActiveModal(station)}
      />

      {/* Diablo HUD (Life & Tide Orbs, Belt, Top Status Bar) */}
      {engine.gameState !== 'CHAR_SELECT' && engine.gameState !== 'CHAR_CREATE' && (
        <DiabloHud
          engine={engine}
          activeModal={activeModal}
          setActiveModal={setActiveModal}
          soundEnabled={soundEnabled}
          setSoundEnabled={setSoundEnabled}
          onAttack={handleAttack}
          onWave={handleWave}
          onDash={handleDash}
          onPotion={handlePotion}
          onTownPortal={handleTownPortal}
        />
      )}

      {/* MODALS */}
      {engine.gameState === 'CHAR_SELECT' && (
        <CharacterSelectModal
          engine={engine}
          onSelectSlot={handleSelectSlot}
          onCreateNewSlot={handleCreateNewSlot}
          onRefresh={forceUpdate}
        />
      )}

      {engine.gameState === 'CHAR_CREATE' && (
        <CharacterCreateModal
          engine={engine}
          slotIndex={selectedSlotForCreate}
          onCreated={handleCharacterCreated}
          onCancel={() => {
            engine.gameState = 'CHAR_SELECT';
            forceUpdate();
          }}
        />
      )}

      {activeModal === 'INVENTORY' && (
        <InventoryModal
          engine={engine}
          onClose={() => setActiveModal(null)}
          onRefresh={forceUpdate}
        />
      )}

      {activeModal === 'TALENTS' && (
        <TalentsModal
          engine={engine}
          onClose={() => setActiveModal(null)}
          onRefresh={forceUpdate}
        />
      )}

      {activeModal === 'BLACKSMITH' && (
        <BlacksmithModal
          engine={engine}
          onClose={() => setActiveModal(null)}
          onRefresh={forceUpdate}
        />
      )}

      {activeModal === 'SHOP' && (
        <ShopModal
          engine={engine}
          onClose={() => setActiveModal(null)}
          onRefresh={forceUpdate}
        />
      )}

      {activeModal === 'STASH' && (
        <StashModal
          engine={engine}
          onClose={() => setActiveModal(null)}
          onRefresh={forceUpdate}
        />
      )}

      {activeModal === 'SAGE' && (
        <SageModal
          engine={engine}
          onClose={() => setActiveModal(null)}
        />
      )}

      {activeModal === 'WAYPOINT' && (
        <WaypointModal
          engine={engine}
          onClose={() => setActiveModal(null)}
          onStartExpedition={handleStartExpedition}
        />
      )}

      {engine.gameOver && (
        <GameOverModal
          engine={engine}
          onReturnToHub={handleReturnToHub}
        />
      )}

      {engine.victory && (
        <VictoryModal
          engine={engine}
          onReturnToHub={handleReturnToHub}
          onContinueEndless={handleContinueEndless}
        />
      )}
    </main>
  );
};

export default App;
