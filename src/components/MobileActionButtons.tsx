import React from 'react';
import { GameEngine } from '../game/GameEngine';
import { ActiveSlotId } from '../types/game';
import { Sliders, Sparkles } from 'lucide-react';

interface MobileActionButtonsProps {
  engine: GameEngine;
  onOpenSkillAssign: () => void;
  onTriggerSlot: (slot: ActiveSlotId) => void;
  onDash: () => void;
  onPotion: () => void;
}

export const MobileActionButtons: React.FC<MobileActionButtonsProps> = ({
  engine,
  onOpenSkillAssign,
  onTriggerSlot,
  onDash,
  onPotion,
}) => {
  const slots: ActiveSlotId[] = ['M1', 'M2', 'Q', 'W', 'E', 'R'];

  return (
    <div className="pointer-events-none absolute inset-x-0 bottom-24 sm:bottom-28 px-3 flex items-end justify-between select-none z-20">
      {/* Left Touch Controls: Quick Utility (Potion, Dash, Config) */}
      <div className="pointer-events-auto flex flex-col items-center gap-2">
        {/* Customize Skills Button */}
        <button
          onClick={onOpenSkillAssign}
          title="Personalizza Tasti & Abilità"
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-slate-900/90 border border-sky-400 text-sky-300 text-xs font-cinzel font-bold shadow-lg backdrop-blur-md active:scale-95 transition-all"
        >
          <Sliders className="w-3.5 h-3.5" />
          <span>Configura Tasti</span>
        </button>

        <div className="flex items-center gap-2">
          {/* Potion Button */}
          <button
            onClick={onPotion}
            disabled={engine.potions <= 0 || engine.hp >= engine.getMaxHp()}
            className="w-12 h-12 rounded-full bg-slate-900/90 border-2 border-emerald-400 text-xl shadow-lg flex flex-col items-center justify-center active:scale-95 disabled:opacity-50"
          >
            <span>🧪</span>
            <span className="text-[9px] font-bold text-emerald-300 -mt-1">
              x{engine.potions}
            </span>
          </button>

          {/* Dash Button */}
          <button
            onClick={onDash}
            disabled={engine.dashCooldown > 0}
            className="w-12 h-12 rounded-full bg-slate-900/90 border-2 border-cyan-400 text-xl shadow-lg flex items-center justify-center active:scale-95 disabled:opacity-50 relative"
          >
            <span>💨</span>
            {engine.dashCooldown > 0 && (
              <div className="absolute inset-0 rounded-full bg-black/70 flex items-center justify-center text-[10px] font-bold text-white">
                {engine.dashCooldown.toFixed(1)}s
              </div>
            )}
          </button>
        </div>
      </div>

      {/* Right Touch Controls: Customizable Action Buttons Grid (M1, M2, Q, W, E, R) */}
      <div className="pointer-events-auto flex flex-col items-end gap-1.5">
        <div className="grid grid-cols-3 gap-1.5">
          {slots.map((slotId) => {
            const skillId = engine.skillLoadout[slotId];
            const skill = engine.getSkillDefinition(skillId);
            const cd = engine.slotCooldowns[slotId];
            const hasTide = skill ? engine.tide >= skill.tideCost : true;
            const isReady = cd <= 0 && hasTide;

            return (
              <button
                key={slotId}
                onClick={() => onTriggerSlot(slotId)}
                disabled={!isReady}
                className={`relative w-12 h-12 sm:w-14 sm:h-14 rounded-2xl flex flex-col items-center justify-center shadow-xl border-2 backdrop-blur-md transition-all active:scale-90 ${
                  isReady
                    ? 'bg-slate-900/90 hover:bg-slate-800'
                    : 'bg-slate-950/90 opacity-60'
                }`}
                style={{
                  borderColor: skill ? skill.color : '#38bdf8',
                  boxShadow: isReady && skill ? `0 0 10px ${skill.color}40` : undefined,
                }}
              >
                <span className="text-xl sm:text-2xl">{skill ? skill.icon : '❓'}</span>
                <span className="text-[9px] font-black uppercase text-amber-300 font-cinzel -mt-0.5">
                  {slotId}
                </span>

                {/* Cooldown Overlay */}
                {cd > 0 && (
                  <div className="absolute inset-0 rounded-2xl bg-slate-950/85 flex items-center justify-center font-mono font-bold text-xs text-white">
                    {cd.toFixed(1)}s
                  </div>
                )}

                {/* Insufficient Tide */}
                {!hasTide && cd <= 0 && (
                  <div className="absolute inset-0 rounded-2xl bg-cyan-950/70 flex items-center justify-center text-[8px] font-bold text-cyan-300 text-center px-0.5">
                    No Marea
                  </div>
                )}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};
