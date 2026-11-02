import React from 'react';
import { GameEngine } from '../game/GameEngine';
import { ActiveModal, CLASSES_DATA } from '../types/game';
import { Volume2, VolumeX, Maximize2, Users, Backpack, Sparkles, Compass, ShieldAlert, Award } from 'lucide-react';
import { AudioHelper } from '../audio/AudioHelper';

interface DiabloHudProps {
  engine: GameEngine;
  activeModal: ActiveModal;
  setActiveModal: (modal: ActiveModal) => void;
  soundEnabled: boolean;
  setSoundEnabled: (enabled: boolean) => void;
  onAttack: () => void;
  onWave: () => void;
  onDash: () => void;
  onPotion: () => void;
  onTownPortal: () => void;
}

export const DiabloHud: React.FC<DiabloHudProps> = ({
  engine,
  activeModal,
  setActiveModal,
  soundEnabled,
  setSoundEnabled,
  onAttack,
  onWave,
  onDash,
  onPotion,
  onTownPortal,
}) => {
  const hpPct = Math.max(0, Math.min(1, engine.hp / engine.getMaxHp()));
  const tidePct = Math.max(0, Math.min(1, engine.tide / engine.getMaxTide()));
  const xpPct = Math.max(0, Math.min(1, engine.xp / engine.xpNext));

  // Find boss if alive
  const boss = engine.enemies.find((e) => e.isBoss && e.hp > 0);

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

  return (
    <div className="pointer-events-none absolute inset-0 flex flex-col justify-between p-3 select-none">
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
            <div className="w-36 h-2 bg-slate-950 rounded-full border border-sky-500/30 overflow-hidden mt-1">
              <div
                className="h-full bg-gradient-to-r from-sky-500 to-blue-400 transition-all duration-200"
                style={{ width: `${xpPct * 100}%` }}
              />
            </div>
          </div>
        </div>

        {/* Zone & Depth */}
        <div className="flex items-center gap-3 bg-slate-900/90 border border-sky-400/40 rounded-xl px-4 py-1.5 shadow-lg backdrop-blur-md text-center">
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
        <div className="flex items-center gap-2">
          {/* Shells */}
          <div className="flex items-center gap-1.5 bg-slate-900/90 border border-amber-500/50 rounded-lg px-2.5 py-1 text-xs font-bold text-amber-400 shadow-md">
            <span>🐚</span>
            <span>{engine.shells}</span>
          </div>

          {/* Abyssal Shards */}
          <div className="flex items-center gap-1.5 bg-slate-900/90 border border-sky-500/50 rounded-lg px-2.5 py-1 text-xs font-bold text-sky-300 shadow-md">
            <span>💎</span>
            <span>{engine.abyssalShards}</span>
          </div>

          {/* Hero Roster Button */}
          <button
            onClick={() => setActiveModal('STASH')}
            title="Archivio Palombari"
            className="p-2 rounded-lg bg-slate-900/90 border border-slate-700 hover:border-sky-400 text-sky-300 transition-colors"
          >
            <Users className="w-4 h-4" />
          </button>

          {/* Sound Toggle */}
          <button
            onClick={toggleSound}
            title={soundEnabled ? 'Disattiva Audio' : 'Attiva Audio'}
            className="p-2 rounded-lg bg-slate-900/90 border border-slate-700 hover:border-sky-400 text-sky-300 transition-colors"
          >
            {soundEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4 text-red-400" />}
          </button>

          {/* Fullscreen Toggle */}
          <button
            onClick={toggleFullscreen}
            title="Schermo Intero"
            className="p-2 rounded-lg bg-slate-900/90 border border-slate-700 hover:border-sky-400 text-sky-300 transition-colors"
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

      {/* --- BOTTOM DIABLO HUD (Globes & Ability Belt) --- */}
      <div className="flex items-end justify-between w-full">
        {/* Left: Red Life Orb (Sfera della Vita) */}
        <div className="pointer-events-auto flex flex-col items-center">
          <div className="relative w-24 h-24 sm:w-28 sm:h-28 rounded-full border-4 border-amber-600/80 bg-slate-950 shadow-[0_0_20px_rgba(220,38,38,0.6)] diablo-globe flex items-center justify-center">
            {/* Fluid fill with slosh */}
            <div
              className="absolute bottom-0 w-full bg-gradient-to-t from-red-950 via-red-600 to-rose-400 transition-all duration-200"
              style={{ height: `${hpPct * 100}%` }}
            />
            {/* Numeric Overlay */}
            <div className="relative z-10 text-center font-cinzel font-black text-white text-xs sm:text-sm drop-shadow-[0_2px_4px_rgba(0,0,0,0.9)]">
              {engine.hp}
              <div className="text-[10px] font-bold text-red-200 uppercase tracking-tight">Salute</div>
            </div>
          </div>
        </div>

        {/* Center: Ability Belt */}
        <div className="pointer-events-auto flex items-center gap-1.5 sm:gap-2.5 bg-slate-950/90 border-2 border-amber-500/70 rounded-2xl p-1.5 sm:p-2 shadow-2xl backdrop-blur-md">
          {/* Attack Button */}
          <button
            onClick={onAttack}
            title="Attacco Primario"
            className="relative flex flex-col items-center justify-center w-12 h-12 sm:w-14 sm:h-14 rounded-xl bg-slate-900 border-2 border-sky-400 hover:border-white shadow-md active:scale-95 transition-all"
          >
            <span className="text-xl sm:text-2xl">
              {engine.playerClass === 'hunter' ? '🏹' : engine.playerClass === 'juggernaut' ? '⚓' : '🔮'}
            </span>
            <span className="text-[8px] font-bold text-sky-300 uppercase mt-0.5">Attacco</span>
          </button>

          {/* Tidal Wave Ability */}
          <button
            onClick={onWave}
            disabled={engine.waveCooldown > 0 || engine.tide < 30}
            title="Onda di Marea (AoE)"
            className={`relative flex flex-col items-center justify-center w-12 h-12 sm:w-14 sm:h-14 rounded-xl bg-slate-900 border-2 ${
              engine.waveCooldown > 0 || engine.tide < 30
                ? 'border-slate-700 opacity-60'
                : 'border-blue-400 hover:border-white'
            } shadow-md active:scale-95 transition-all`}
          >
            <span className="text-xl sm:text-2xl">🌊</span>
            <span className="text-[8px] font-bold text-blue-300 uppercase mt-0.5">Marea</span>
            {engine.waveCooldown > 0 && (
              <div className="absolute inset-0 bg-slate-950/80 rounded-xl flex items-center justify-center font-bold text-xs text-white">
                {engine.waveCooldown.toFixed(1)}s
              </div>
            )}
          </button>

          {/* Dash Ability */}
          <button
            onClick={onDash}
            disabled={engine.dashCooldown > 0}
            title="Scatto Propulsivo"
            className={`relative flex flex-col items-center justify-center w-12 h-12 sm:w-14 sm:h-14 rounded-xl bg-slate-900 border-2 ${
              engine.dashCooldown > 0 ? 'border-slate-700 opacity-60' : 'border-cyan-400 hover:border-white'
            } shadow-md active:scale-95 transition-all`}
          >
            <span className="text-xl sm:text-2xl">💨</span>
            <span className="text-[8px] font-bold text-cyan-300 uppercase mt-0.5">Scatto</span>
            {engine.dashCooldown > 0 && (
              <div className="absolute inset-0 bg-slate-950/80 rounded-xl flex items-center justify-center font-bold text-xs text-white">
                {engine.dashCooldown.toFixed(1)}s
              </div>
            )}
          </button>

          {/* Healing Potion */}
          <button
            onClick={onPotion}
            disabled={engine.potions <= 0 || engine.hp >= engine.getMaxHp()}
            title="Pozione di Cura"
            className={`relative flex flex-col items-center justify-center w-12 h-12 sm:w-14 sm:h-14 rounded-xl bg-slate-900 border-2 ${
              engine.potions <= 0 ? 'border-slate-700 opacity-50' : 'border-emerald-400 hover:border-white'
            } shadow-md active:scale-95 transition-all`}
          >
            <span className="text-xl sm:text-2xl">🧪</span>
            <span className="text-[8px] font-bold text-emerald-300 uppercase mt-0.5">
              Cura ({engine.potions})
            </span>
          </button>

          <div className="w-[1px] h-8 bg-slate-800 mx-0.5" />

          {/* Town Portal (Diablo Town Portal) */}
          {engine.gameState === 'EXPEDITION' ? (
            <button
              onClick={onTownPortal}
              title="Portale Cittadino: Rientra al Santuario"
              className="relative flex flex-col items-center justify-center w-12 h-12 sm:w-14 sm:h-14 rounded-xl bg-slate-900 border-2 border-indigo-400 hover:border-white shadow-md active:scale-95 transition-all"
            >
              <span className="text-xl sm:text-2xl animate-pulse">🌀</span>
              <span className="text-[8px] font-bold text-indigo-300 uppercase mt-0.5">Città</span>
            </button>
          ) : (
            <button
              onClick={() => setActiveModal('WAYPOINT')}
              title="Mappa di Spedizione (Atti I-IV)"
              className="relative flex flex-col items-center justify-center w-12 h-12 sm:w-14 sm:h-14 rounded-xl bg-slate-900 border-2 border-amber-400 hover:border-white shadow-md active:scale-95 transition-all"
            >
              <Compass className="w-5 h-5 text-amber-400" />
              <span className="text-[8px] font-bold text-amber-300 uppercase mt-0.5">Atti</span>
            </button>
          )}

          {/* Inventory Button */}
          <button
            onClick={() => setActiveModal(activeModal === 'INVENTORY' ? null : 'INVENTORY')}
            title="Zaino & Inventario"
            className="relative flex flex-col items-center justify-center w-12 h-12 sm:w-14 sm:h-14 rounded-xl bg-slate-900 border-2 border-amber-400 hover:border-white shadow-md active:scale-95 transition-all"
          >
            <Backpack className="w-5 h-5 text-amber-300" />
            <span className="text-[8px] font-bold text-amber-200 uppercase mt-0.5">Zaino</span>
            {engine.backpack.length > 0 && (
              <span className="absolute -top-1 -right-1 bg-amber-500 text-slate-950 font-black text-[9px] w-4 h-4 rounded-full flex items-center justify-center">
                {engine.backpack.length}
              </span>
            )}
          </button>

          {/* Talents Button */}
          <button
            onClick={() => setActiveModal(activeModal === 'TALENTS' ? null : 'TALENTS')}
            title="Albero Abilità & Talenti"
            className="relative flex flex-col items-center justify-center w-12 h-12 sm:w-14 sm:h-14 rounded-xl bg-slate-900 border-2 border-purple-400 hover:border-white shadow-md active:scale-95 transition-all"
          >
            <Sparkles className="w-5 h-5 text-purple-300" />
            <span className="text-[8px] font-bold text-purple-200 uppercase mt-0.5">Talenti</span>
            {engine.skillPoints > 0 && (
              <span className="absolute -top-1 -right-1 bg-purple-500 text-white font-black text-[9px] w-4 h-4 rounded-full flex items-center justify-center animate-bounce">
                {engine.skillPoints}
              </span>
            )}
          </button>
        </div>

        {/* Right: Blue Tide Orb (Sfera della Marea) */}
        <div className="pointer-events-auto flex flex-col items-center">
          <div className="relative w-24 h-24 sm:w-28 sm:h-28 rounded-full border-4 border-cyan-600/80 bg-slate-950 shadow-[0_0_20px_rgba(6,182,212,0.6)] diablo-globe flex items-center justify-center">
            {/* Fluid fill */}
            <div
              className="absolute bottom-0 w-full bg-gradient-to-t from-blue-950 via-cyan-600 to-sky-400 transition-all duration-200"
              style={{ height: `${tidePct * 100}%` }}
            />
            {/* Numeric Overlay */}
            <div className="relative z-10 text-center font-cinzel font-black text-white text-xs sm:text-sm drop-shadow-[0_2px_4px_rgba(0,0,0,0.9)]">
              {Math.round(engine.tide)}
              <div className="text-[10px] font-bold text-cyan-200 uppercase tracking-tight">Marea</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
