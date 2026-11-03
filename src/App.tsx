import React, { useState, useEffect, useRef, useReducer } from 'react';
import { GameEngine } from './game/GameEngine';
import { ActiveModal, ActiveSlotId } from './types/game';
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
import { SkillAssignModal } from './components/modals/SkillAssignModal';
import { MobileActionButtons } from './components/MobileActionButtons';
import { StoryDialogueBanner } from './components/StoryDialogueBanner';
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

  // Keyboard Shortcuts for Desktop:
  // Mouse 1 = Attack (Primary)
  // Mouse 2 = M2 (Rapid secondary ability)
  // Q, W, E, R = Assigned active skills
  // Space = Dash
  // 1 or F = Potion
  // S = Custom Skill Assign Modal
  // T = Town Portal
  // I or B = Inventory
  // K = Talents
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ignore if typing in text input
      if ((e.target as HTMLElement).tagName === 'INPUT') return;

      if (e.key === 'Escape') {
        setActiveModal(null);
        return;
      }

      if (engine.gameState === 'EXPEDITION') {
        // Abilities Q, W, E, R
        if (e.key === 'q' || e.key === 'Q') {
          e.preventDefault();
          engine.performQ();
          forceUpdate();
        } else if (e.key === 'w' || e.key === 'W') {
          e.preventDefault();
          engine.performW();
          forceUpdate();
        } else if (e.key === 'e' || e.key === 'E') {
          e.preventDefault();
          engine.performE();
          forceUpdate();
        } else if (e.key === 'r' || e.key === 'R') {
          e.preventDefault();
          engine.performR();
          forceUpdate();
        } else if (e.code === 'Space') {
          e.preventDefault();
          engine.performDash();
          forceUpdate();
        } else if (e.key === '1' || e.key === 'f' || e.key === 'F') {
          e.preventDefault();
          engine.usePotion();
          forceUpdate();
        } else if (e.key === 't' || e.key === 'T') {
          e.preventDefault();
          engine.openTownPortal();
          forceUpdate();
        }
      }

      // Hotkeys accessible anywhere
      if (e.key === 's' || e.key === 'S') {
        e.preventDefault();
        setActiveModal((prev) => (prev === 'SKILL_ASSIGN' ? null : 'SKILL_ASSIGN'));
      } else if (e.key === 'i' || e.key === 'I' || e.key === 'b' || e.key === 'B') {
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

  const handleTriggerSlot = (slot: ActiveSlotId) => {
    engine.triggerSlot(slot);
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

  const isTouchOrAndroid =
    typeof window !== 'undefined' &&
    ('ontouchstart' in window ||
      navigator.maxTouchPoints > 0 ||
      Boolean((window as unknown as { AndroidBridge?: unknown }).AndroidBridge));

  useEffect(() => {
    const win = window as unknown as { __handleAndroidBack?: () => boolean };
    win.__handleAndroidBack = () => {
      if (activeModal) {
        setActiveModal(null);
        return true;
      }
      if (engine.currentDialogue) {
        engine.currentDialogue = null;
        forceUpdate();
        return true;
      }
      if (engine.gameState === 'EXPEDITION') {
        engine.openTownPortal();
        forceUpdate();
        return true;
      }
      return false;
    };
    return () => {
      delete win.__handleAndroidBack;
    };
  }, [activeModal, engine]);

  return (
    <main className="relative w-screen h-screen overflow-hidden bg-slate-950 font-sans">
      {/* 60 FPS HTML5 Canvas */}
      <GameCanvas
        engine={engine}
        onOpenStation={(station) => setActiveModal(station)}
      />

      {/* Story Narrative Dialogue Banner (Story Progression) */}
      {engine.currentDialogue && (
        <StoryDialogueBanner
          dialogue={engine.currentDialogue}
          onDismiss={() => {
            engine.currentDialogue = null;
            forceUpdate();
          }}
        />
      )}

      {/* Mobile Touch Action Buttons (Customizable on phone/touch & Android APK) */}
      {engine.gameState === 'EXPEDITION' && !activeModal && !engine.gameOver && !engine.victory && (
        <div className={isTouchOrAndroid ? 'block' : 'md:hidden'}>
          <MobileActionButtons
            engine={engine}
            onOpenSkillAssign={() => setActiveModal('SKILL_ASSIGN')}
            onTriggerSlot={handleTriggerSlot}
            onDash={handleDash}
            onPotion={handlePotion}
          />
        </div>
      )}

      {/* Diablo HUD (Life & Tide Orbs, Belt, Top Status Bar, Quest Tracker) */}
      {engine.gameState !== 'CHAR_SELECT' && engine.gameState !== 'CHAR_CREATE' && !engine.gameOver && !engine.victory && (
        <DiabloHud
          engine={engine}
          activeModal={activeModal}
          setActiveModal={setActiveModal}
          soundEnabled={soundEnabled}
          setSoundEnabled={setSoundEnabled}
          onAttack={handleAttack}
          onDash={handleDash}
          onPotion={handlePotion}
          onTownPortal={handleTownPortal}
          onTriggerSlot={handleTriggerSlot}
          onOpenSkillAssign={() => setActiveModal('SKILL_ASSIGN')}
          isTouchMode={isTouchOrAndroid}
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

      {activeModal === 'SKILL_ASSIGN' && (
        <SkillAssignModal
          engine={engine}
          onClose={() => setActiveModal(null)}
          onRefresh={forceUpdate}
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
