import React, { useState } from 'react';
import { GameEngine } from '../game/GameEngine';
import { ActiveModal, CLASSES_DATA, ActiveSlotId } from '../types/game';
import {
  Volume2,
  VolumeX,
  Maximize2,
  Users,
  Backpack,
  Sparkles,
  Compass,
  ShieldAlert,
  Sliders,
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  Circle,
  Lock,
  Unlock,
  Scroll,
} from 'lucide-react';
import { AudioHelper } from '../audio/AudioHelper';

interface DiabloHudProps {
  engine: GameEngine;
  activeModal: ActiveModal;
  setActiveModal: (modal: ActiveModal) => void;
  soundEnabled: boolean;
  setSoundEnabled: (enabled: boolean) => void;
  onAttack: () => void;
  onWave?: () => void;
  onDash: () => void;
  onPotion: () => void;
  onTownPortal: () => void;
  onTriggerSlot?: (slot: ActiveSlotId) => void;
  onOpenSkillAssign?: () => void;
  isTouchMode?: boolean;
}

export const DiabloHud: React.FC<DiabloHudProps> = ({
  engine,
  activeModal,
  setActiveModal,
  soundEnabled,
  setSoundEnabled,
  onAttack,
  onDash,
  onPotion,
  onTownPortal,
  onTriggerSlot,
  onOpenSkillAssign,
  isTouchMode = false,
}) => {
  const [questTrackerOpen, setQuestTrackerOpen] = useState(true);

  const hpPct = Math.max(0, Math.min(1, engine.hp / engine.getMaxHp()));
  const tidePct = Math.max(0, Math.min(1, engine.tide / engine.getMaxTide()));
  const xpPct = Math.max(0, Math.min(1, engine.xp / engine.xpNext));

  // Find boss if alive
  const boss = engine.enemies.find((e) => e.isBoss && e.hp > 0);

  // Active Act info for Quest Tracker
  const currentAct = engine.acts.find((a) => a.number === engine.currentAct);

  const toggleSound = () => {
    const next = !soundEnabled;
    setSoundEnabled(next);
    AudioHelper.setSoundEnabled(next);
  };

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
    } else {
      document.exitFullscreen().catch(() => {});
    }
  };

  const handleSlotClick = (slot: ActiveSlotId) => {
    if (onTriggerSlot) {
      onTriggerSlot(slot);
    } else {
      engine.triggerSlot(slot);
    }
  };

  const openSkillAssign = () => {
    if (onOpenSkillAssign) {
      onOpenSkillAssign();
    } else {
      setActiveModal('SKILL_ASSIGN');
    }
  };

  const activeSlots: { id: ActiveSlotId; keyHint: string; label: string }[] = [
    { id: 'M1', keyHint: 'M1', label: 'Primario' },
    { id: 'M2', keyHint: 'M2', label: 'Rapido' },
    { id: 'Q', keyHint: 'Q', label: 'Abilità' },
    { id: 'W', keyHint: 'W', label: 'Abilità' },
    { id: 'E', keyHint: 'E', label: 'Abilità' },
    { id: 'R', keyHint: 'R', label: 'Ultimate' },
  ];

  return (
    <div className="pointer-events-none absolute inset-0 flex flex-col justify-between p-2 sm:p-3 select-none">
      {/* --- TOP STATUS BAR --- */}
      <div className="pointer-events-auto flex items-center justify-between gap-2">
        {/* Hero Banner & Level */}
        <div className="flex items-center gap-3 bg-slate-900/90 border-2 border-sky-400/70 rounded-xl px-3 py-1.5 shadow-xl backdrop-blur-md">
          <div className="relative">
            <img
              src={CLASSES_DATA[engine.playerClass].portrait}
              alt="Class"
              className="w-10 h-10 rounded-lg border-2 border-sky-300 object-cover bg-slate-800"
            />
            <div className="absolute -bottom-1 -right-1 bg-amber-500 text-slate-950 font-black text-[10px] px-1 rounded-sm border border-white">
              Lv.{engine.level}
            </div>
          </div>
          <div className="flex flex-col">
            <div className="flex items-center gap-2">
              <span className="font-cinzel font-bold text-sm text-sky-200">
                {engine.playerName}
              </span>
              <span className="text-[10px] uppercase font-bold px-1.5 py-0.5 rounded bg-sky-950 text-sky-400 border border-sky-600/40">
                {CLASSES_DATA[engine.playerClass].name}
              </span>
            </div>
            {/* XP Bar */}
            <div className="w-32 sm:w-36 h-2 bg-slate-950 rounded-full border border-sky-500/30 overflow-hidden mt-1">
              <div
                className="h-full bg-gradient-to-r from-sky-500 to-blue-400 transition-all duration-200"
                style={{ width: `${xpPct * 100}%` }}
              />
            </div>
          </div>
        </div>

        {/* Zone & Depth */}
        <div className="hidden lg:flex items-center gap-3 bg-slate-900/90 border border-sky-400/40 rounded-xl px-4 py-1.5 shadow-lg backdrop-blur-md text-center">
          <Compass className="w-4 h-4 text-sky-400 animate-spin-slow" />
          <div className="flex flex-col">
            <span className="font-cinzel text-xs font-bold text-sky-200">
              {engine.gameState === 'HUB' ? 'Santuario del Nautilus' : engine.zoneName}
            </span>
            <span className="text-[10px] font-mono text-cyan-400">
              {engine.gameState === 'HUB' ? 'Hub Sicuro' : `Profondità: -${engine.depthMeters}m`}
            </span>
          </div>
        </div>

        {/* Currencies & Controls */}
        <div className="flex items-center gap-1 sm:gap-1.5">
          {/* Shells */}
          <div className="flex items-center gap-1 bg-slate-900/90 border border-amber-500/50 rounded-lg px-2 py-1 text-xs font-bold text-amber-400 shadow-md">
            <span>🐚</span>
            <span>{engine.shells}</span>
          </div>

          {/* Abyssal Shards */}
          <div className="flex items-center gap-1 bg-slate-900/90 border border-sky-500/50 rounded-lg px-2 py-1 text-xs font-bold text-sky-300 shadow-md">
            <span>💎</span>
            <span>{engine.abyssalShards}</span>
          </div>

          {/* Inventory Shortcut (Always accessible on Mobile/Desktop) */}
          <button
            onClick={() => setActiveModal(activeModal === 'INVENTORY' ? null : 'INVENTORY')}
            title="Zaino & Confronto Equipaggiamento (I)"
            className="relative flex items-center gap-1 px-2 py-1.5 rounded-lg bg-slate-900/90 border border-amber-400/80 hover:border-amber-300 text-amber-200 transition-colors text-xs font-cinzel font-bold shadow-md"
          >
            <Backpack className="w-3.5 h-3.5 text-amber-400" />
            <span className="hidden sm:inline">Zaino</span>
            {engine.backpack.length > 0 && (
              <span className="bg-amber-500 text-slate-950 font-black text-[9px] px-1 rounded-full">
                {engine.backpack.length}
              </span>
            )}
          </button>

          {/* Talents Shortcut */}
          <button
            onClick={() => setActiveModal(activeModal === 'TALENTS' ? null : 'TALENTS')}
            title="Albero Abilità di Classe (K)"
            className="relative flex items-center gap-1 px-2 py-1.5 rounded-lg bg-slate-900/90 border border-purple-400/80 hover:border-purple-300 text-purple-200 transition-colors text-xs font-cinzel font-bold shadow-md"
          >
            <Sparkles className="w-3.5 h-3.5 text-purple-400" />
            <span className="hidden sm:inline">Talenti</span>
            {engine.skillPoints > 0 && (
              <span className="bg-purple-500 text-white font-black text-[9px] px-1 rounded-full animate-pulse">
                {engine.skillPoints}
              </span>
            )}
          </button>

          {/* Skill Customizer Button (Top Bar shortcut) */}
          <button
            onClick={openSkillAssign}
            title="Configura Abilità e Tasti (S)"
            className="flex items-center gap-1 px-2 py-1.5 rounded-lg bg-slate-900/90 border border-sky-400/70 hover:border-sky-300 text-sky-300 hover:text-white transition-colors text-xs font-cinzel font-bold shadow-md"
          >
            <Sliders className="w-3.5 h-3.5 text-sky-400" />
            <span className="hidden md:inline">Abilità</span>
          </button>

          {/* Town Portal or Waypoint Map */}
          {engine.gameState === 'EXPEDITION' ? (
            <button
              onClick={onTownPortal}
              title="Portale Cittadino: Torna al Santuario (T)"
              className="flex items-center gap-1 px-2 py-1.5 rounded-lg bg-slate-900/90 border border-indigo-400/80 hover:border-indigo-300 text-indigo-200 transition-colors text-xs font-cinzel font-bold shadow-md"
            >
              <span>🌀</span>
              <span className="hidden sm:inline">Città</span>
            </button>
          ) : (
            <button
              onClick={() => setActiveModal('WAYPOINT')}
              title="Mappa di Spedizione (Atti I-IV)"
              className="flex items-center gap-1 px-2 py-1.5 rounded-lg bg-slate-900/90 border border-amber-400/80 hover:border-amber-300 text-amber-300 transition-colors text-xs font-cinzel font-bold shadow-md"
            >
              <Compass className="w-3.5 h-3.5 text-amber-400" />
              <span className="hidden sm:inline">Atti</span>
            </button>
          )}

          {/* Hero Roster / Switch Hero Button */}
          <button
            onClick={() => {
              engine.gameState = 'CHAR_SELECT';
              setActiveModal(null);
            }}
            title="Archivio Palombari (Cambia Eroe)"
            className="p-1.5 sm:p-2 rounded-lg bg-slate-900/90 border border-slate-700 hover:border-sky-400 text-sky-300 transition-colors"
          >
            <Users className="w-4 h-4" />
          </button>

          {/* Sound Toggle */}
          <button
            onClick={toggleSound}
            title={soundEnabled ? 'Disattiva Audio' : 'Attiva Audio'}
            className="p-1.5 sm:p-2 rounded-lg bg-slate-900/90 border border-slate-700 hover:border-sky-400 text-sky-300 transition-colors"
          >
            {soundEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4 text-red-400" />}
          </button>

          {/* Fullscreen Toggle */}
          <button
            onClick={toggleFullscreen}
            title="Schermo Intero"
            className="p-1.5 sm:p-2 rounded-lg bg-slate-900/90 border border-slate-700 hover:border-sky-400 text-sky-300 transition-colors"
          >
            <Maximize2 className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* --- BOSS HEALTH BAR (Diablo Ornate Banner) --- */}
      {boss && (
        <div className="pointer-events-auto mx-auto w-full max-w-lg mt-2 flex flex-col items-center">
          <div className="flex items-center gap-2 mb-1">
            <ShieldAlert className="w-4 h-4 text-red-500 animate-pulse" />
            <span className="font-cinzel text-sm font-black text-red-400 uppercase tracking-widest drop-shadow-[0_2px_4px_rgba(0,0,0,0.8)]">
              {boss.name}
            </span>
          </div>
          <div className="w-full h-4 bg-slate-950/90 border-2 border-red-600 rounded-full overflow-hidden shadow-[0_0_15px_rgba(239,68,68,0.5)] p-0.5">
            <div
              className="h-full bg-gradient-to-r from-red-700 via-red-500 to-amber-500 rounded-full transition-all duration-150"
              style={{ width: `${Math.max(0, Math.min(100, (boss.hp / boss.maxHp) * 100))}%` }}
            />
          </div>
          <div className="text-[10px] font-mono text-red-300 mt-0.5">
            {boss.hp} / {boss.maxHp} PV
          </div>
        </div>
      )}

      {/* --- STORY QUEST TRACKER (Diablo / Nautical Parchment Quest Log) --- */}
      {engine.gameState === 'EXPEDITION' && currentAct && (
        <div className="pointer-events-auto absolute top-16 left-3 w-64 sm:w-72 parchment-window rounded-xl shadow-2xl overflow-hidden transition-all duration-300 border-2 border-[#8c6239]">
          {/* Tracker Header */}
          <div
            onClick={() => setQuestTrackerOpen(!questTrackerOpen)}
            className="flex items-center justify-between px-3 py-2 bg-[#ecd9b2] border-b-2 border-[#8c6239]/40 cursor-pointer hover:bg-[#dfc89d]"
          >
            <div className="flex items-center gap-1.5">
              <Scroll className="w-4 h-4 text-[#854d0e]" />
              <span className="font-cinzel font-bold text-xs text-[#381e09] uppercase tracking-wide truncate">
                {currentAct.title}
              </span>
            </div>
            <button className="text-[#694625] hover:text-[#2b1706]">
              {questTrackerOpen ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
            </button>
          </div>

          {/* Tracker Body */}
          {questTrackerOpen && (
            <div className="p-3 text-xs space-y-2">
              <div className="text-[11px] font-medium text-[#694625] italic border-l-2 border-[#854d0e] pl-2 font-serif">
                {currentAct.subtitle}
              </div>

              {/* Objectives List */}
              <div className="space-y-1.5 mt-2">
                {currentAct.objectives.map((obj) => (
                  <div
                    key={obj.id}
                    className={`flex items-start gap-2 p-1.5 rounded-lg border ${
                      obj.completed
                        ? 'bg-[#f0fdf4] border-emerald-400 text-emerald-900'
                        : 'bg-[#fffaf0] border-[#cbb28b] text-[#2b1706]'
                    }`}
                  >
                    {obj.completed ? (
                      <CheckCircle2 className="w-4 h-4 text-emerald-700 flex-shrink-0 mt-0.5" />
                    ) : (
                      <Circle className="w-4 h-4 text-[#854d0e] flex-shrink-0 mt-0.5" />
                    )}
                    <div className="flex-1">
                      <div className={`font-semibold ${obj.completed ? 'line-through text-emerald-800' : 'text-[#2b1706]'}`}>
                        {obj.title}
                      </div>
                      <div className="flex items-center justify-between text-[10px] text-[#694625] mt-0.5">
                        <span className="font-serif italic">{obj.desc}</span>
                        <span className="font-mono font-bold text-[#854d0e] ml-1">
                          {obj.currentCount} / {obj.targetCount}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* Gate Barrier Status */}
              <div
                className={`flex items-center gap-2 p-2 rounded-lg border text-[11px] font-bold ${
                  currentAct.gateOpened
                    ? 'bg-[#e0f2fe] border-[#38bdf8] text-[#0369a1]'
                    : 'bg-[#fef3c7] border-[#d97706] text-[#92400e]'
                }`}
              >
                {currentAct.gateOpened ? (
                  <>
                    <Unlock className="w-4 h-4 text-[#0284c7] flex-shrink-0 animate-bounce" />
                    <span>Barriera Disattivata: L'Arena del Boss è accessibile a Sud!</span>
                  </>
                ) : (
                  <>
                    <Lock className="w-4 h-4 text-[#b45309] flex-shrink-0" />
                    <span>Barriera Abissale Attiva: Risolvi gli obiettivi per sbloccarla.</span>
                  </>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {/* --- BOTTOM DIABLO HUD (Globes & Ability Belt) --- */}
      <div className="flex items-end justify-between w-full">
        {/* Left: Red Life Orb (Sfera della Vita) */}
        <div className="pointer-events-auto flex flex-col items-center">
          <div className="relative w-20 h-20 sm:w-28 sm:h-28 rounded-full border-4 border-amber-600/80 bg-slate-950 shadow-[0_0_20px_rgba(220,38,38,0.6)] diablo-globe flex items-center justify-center">
            {/* Fluid fill with slosh */}
            <div
              className="absolute bottom-0 w-full bg-gradient-to-t from-red-950 via-red-600 to-rose-400 transition-all duration-200"
              style={{ height: `${hpPct * 100}%` }}
            />
            {/* Numeric Overlay */}
            <div className="relative z-10 text-center font-cinzel font-black text-white text-xs sm:text-sm drop-shadow-[0_2px_4px_rgba(0,0,0,0.9)]">
              {engine.hp}
              <div className="text-[9px] sm:text-[10px] font-bold text-red-200 uppercase tracking-tight">Salute</div>
            </div>
          </div>
        </div>

        {/* Center: Customizable Ability Belt (M1, M2, Q, W, E, R, Dash, Potion, Hotkeys) */}
        <div
          className={`pointer-events-auto ${
            isTouchMode && engine.gameState === 'EXPEDITION' ? 'hidden' : 'hidden md:flex'
          } items-center gap-1.5 sm:gap-2 bg-slate-950/95 border-2 border-amber-500/70 rounded-2xl p-1.5 sm:p-2 shadow-2xl backdrop-blur-md`}
        >
          {/* Active Skills (M1, M2, Q, W, E, R) */}
          {activeSlots.map(({ id: slotId, keyHint, label }) => {
            const skillId = engine.skillLoadout[slotId];
            const skill = engine.getSkillDefinition(skillId);
            const cd = engine.slotCooldowns[slotId];
            const hasTide = skill ? engine.tide >= skill.tideCost : true;
            const isReady = cd <= 0 && hasTide;

            return (
              <button
                key={slotId}
                onClick={() => handleSlotClick(slotId)}
                title={skill ? `${skill.name} (${keyHint})\n${skill.desc}` : keyHint}
                className={`relative flex flex-col items-center justify-center w-12 h-12 sm:w-14 sm:h-14 rounded-xl bg-slate-900 border-2 shadow-md active:scale-95 transition-all overflow-hidden ${
                  slotId === 'R'
                    ? 'border-amber-400 shadow-[0_0_8px_rgba(251,191,36,0.5)]'
                    : slotId === 'M1'
                    ? 'border-sky-400'
                    : 'border-blue-400'
                } ${!isReady ? 'opacity-60' : 'hover:border-white'}`}
                style={{
                  borderColor: skill?.color,
                }}
              >
                {/* Hotkey Badge */}
                <div className="absolute top-0.5 left-1 font-mono font-black text-[9px] text-amber-300 drop-shadow">
                  {keyHint}
                </div>

                {/* Tide Cost Badge */}
                {skill && skill.tideCost > 0 && (
                  <div className="absolute top-0.5 right-1 text-[8px] font-bold text-cyan-300">
                    {skill.tideCost}
                  </div>
                )}

                {/* Icon */}
                <span className="text-xl sm:text-2xl mt-1">
                  {skill ? skill.icon : '✨'}
                </span>

                {/* Skill Name */}
                <span className="text-[7.5px] sm:text-[8px] font-bold text-sky-200 uppercase truncate max-w-[48px]">
                  {skill ? skill.name.split(' ')[0] : label}
                </span>

                {/* Cooldown Overlay */}
                {cd > 0 && (
                  <div className="absolute inset-0 bg-slate-950/85 rounded-xl flex items-center justify-center font-bold text-xs text-white">
                    {cd.toFixed(1)}s
                  </div>
                )}
              </button>
            );
          })}

          <div className="w-[1px] h-8 bg-slate-800 mx-0.5" />

          {/* Dash Ability (Space) */}
          <button
            onClick={onDash}
            disabled={engine.dashCooldown > 0}
            title="Scatto Propulsivo (Spazio)"
            className={`relative flex flex-col items-center justify-center w-12 h-12 sm:w-14 sm:h-14 rounded-xl bg-slate-900 border-2 ${
              engine.dashCooldown > 0 ? 'border-slate-700 opacity-60' : 'border-cyan-400 hover:border-white'
            } shadow-md active:scale-95 transition-all`}
          >
            <div className="absolute top-0.5 left-1 font-mono font-black text-[9px] text-cyan-300">
              Spazio
            </div>
            <span className="text-xl sm:text-2xl mt-1">💨</span>
            <span className="text-[8px] font-bold text-cyan-300 uppercase">Scatto</span>
            {engine.dashCooldown > 0 && (
              <div className="absolute inset-0 bg-slate-950/80 rounded-xl flex items-center justify-center font-bold text-xs text-white">
                {engine.dashCooldown.toFixed(1)}s
              </div>
            )}
          </button>

          {/* Healing Potion (1 / F) */}
          <button
            onClick={onPotion}
            disabled={engine.potions <= 0 || engine.hp >= engine.getMaxHp()}
            title="Pozione di Cura (Tasto 1 o F)"
            className={`relative flex flex-col items-center justify-center w-12 h-12 sm:w-14 sm:h-14 rounded-xl bg-slate-900 border-2 ${
              engine.potions <= 0 ? 'border-slate-700 opacity-50' : 'border-emerald-400 hover:border-white'
            } shadow-md active:scale-95 transition-all`}
          >
            <div className="absolute top-0.5 left-1 font-mono font-black text-[9px] text-emerald-300">
              1/F
            </div>
            <span className="text-xl sm:text-2xl mt-1">🧪</span>
            <span className="text-[8px] font-bold text-emerald-300 uppercase">
              x{engine.potions}
            </span>
          </button>

          <div className="w-[1px] h-8 bg-slate-800 mx-0.5" />

          {/* Open Skill Customizer / Keybinds Modal */}
          <button
            onClick={openSkillAssign}
            title="Personalizza Tasti & Assegna Poteri (S)"
            className="relative flex flex-col items-center justify-center w-11 h-12 sm:w-12 sm:h-14 rounded-xl bg-slate-900 border-2 border-sky-400 hover:border-white shadow-md active:scale-95 transition-all"
          >
            <Sliders className="w-4 h-4 text-sky-300" />
            <span className="text-[8px] font-bold text-sky-200 uppercase mt-0.5">Tasti</span>
          </button>

          {/* Town Portal (Diablo Town Portal) */}
          {engine.gameState === 'EXPEDITION' ? (
            <button
              onClick={onTownPortal}
              title="Portale Cittadino: Rientra al Santuario (T)"
              className="relative flex flex-col items-center justify-center w-11 h-12 sm:w-12 sm:h-14 rounded-xl bg-slate-900 border-2 border-indigo-400 hover:border-white shadow-md active:scale-95 transition-all"
            >
              <div className="absolute top-0.5 left-1 font-mono font-black text-[9px] text-indigo-300">
                T
              </div>
              <span className="text-lg sm:text-xl animate-pulse mt-1">🌀</span>
              <span className="text-[8px] font-bold text-indigo-300 uppercase">Città</span>
            </button>
          ) : (
            <button
              onClick={() => setActiveModal('WAYPOINT')}
              title="Mappa di Spedizione (Atti I-IV)"
              className="relative flex flex-col items-center justify-center w-11 h-12 sm:w-12 sm:h-14 rounded-xl bg-slate-900 border-2 border-amber-400 hover:border-white shadow-md active:scale-95 transition-all"
            >
              <Compass className="w-4 h-4 text-amber-400" />
              <span className="text-[8px] font-bold text-amber-300 uppercase mt-0.5">Atti</span>
            </button>
          )}

          {/* Inventory Button */}
          <button
            onClick={() => setActiveModal(activeModal === 'INVENTORY' ? null : 'INVENTORY')}
            title="Zaino & Inventario (I)"
            className="relative flex flex-col items-center justify-center w-11 h-12 sm:w-12 sm:h-14 rounded-xl bg-slate-900 border-2 border-amber-400 hover:border-white shadow-md active:scale-95 transition-all"
          >
            <div className="absolute top-0.5 left-1 font-mono font-black text-[9px] text-amber-300">
              I
            </div>
            <Backpack className="w-4 h-4 text-amber-300 mt-1" />
            <span className="text-[8px] font-bold text-amber-200 uppercase">Zaino</span>
            {engine.backpack.length > 0 && (
              <span className="absolute -top-1 -right-1 bg-amber-500 text-slate-950 font-black text-[9px] w-4 h-4 rounded-full flex items-center justify-center">
                {engine.backpack.length}
              </span>
            )}
          </button>

          {/* Talents Button */}
          <button
            onClick={() => setActiveModal(activeModal === 'TALENTS' ? null : 'TALENTS')}
            title="Albero Abilità & Talenti (K)"
            className="relative flex flex-col items-center justify-center w-11 h-12 sm:w-12 sm:h-14 rounded-xl bg-slate-900 border-2 border-purple-400 hover:border-white shadow-md active:scale-95 transition-all"
          >
            <div className="absolute top-0.5 left-1 font-mono font-black text-[9px] text-purple-300">
              K
            </div>
            <Sparkles className="w-4 h-4 text-purple-300 mt-1" />
            <span className="text-[8px] font-bold text-purple-200 uppercase">Talenti</span>
            {engine.skillPoints > 0 && (
              <span className="absolute -top-1 -right-1 bg-purple-500 text-white font-black text-[9px] w-4 h-4 rounded-full flex items-center justify-center animate-bounce">
                {engine.skillPoints}
              </span>
            )}
          </button>
        </div>

        {/* Right: Blue Tide Orb (Sfera della Marea) */}
        <div className="pointer-events-auto flex flex-col items-center">
          <div className="relative w-20 h-20 sm:w-28 sm:h-28 rounded-full border-4 border-cyan-600/80 bg-slate-950 shadow-[0_0_20px_rgba(6,182,212,0.6)] diablo-globe flex items-center justify-center">
            {/* Fluid fill */}
            <div
              className="absolute bottom-0 w-full bg-gradient-to-t from-blue-950 via-cyan-600 to-sky-400 transition-all duration-200"
              style={{ height: `${tidePct * 100}%` }}
            />
            {/* Numeric Overlay */}
            <div className="relative z-10 text-center font-cinzel font-black text-white text-xs sm:text-sm drop-shadow-[0_2px_4px_rgba(0,0,0,0.9)]">
              {Math.round(engine.tide)}
              <div className="text-[9px] sm:text-[10px] font-bold text-cyan-200 uppercase tracking-tight">Marea</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
