import React, { useState } from 'react';
import { GameEngine } from '../../game/GameEngine';
import { ActiveSlotId } from '../../types/game';
import { CLASS_ACTIVE_SKILLS } from '../../game/skillsData';
import { X, Check, Sparkles, Smartphone, Keyboard, BookOpen, Compass } from 'lucide-react';

interface SkillAssignModalProps {
  engine: GameEngine;
  onClose: () => void;
  onRefresh: () => void;
}

export const SkillAssignModal: React.FC<SkillAssignModalProps> = ({
  engine,
  onClose,
  onRefresh,
}) => {
  const [selectedSlot, setSelectedSlot] = useState<ActiveSlotId>('M2');
  const classSkills = CLASS_ACTIVE_SKILLS[engine.playerClass] || [];

  const handleAssign = (skillId: string) => {
    engine.assignSkillToSlot(selectedSlot, skillId);
    onRefresh();
  };

  const slotLabels: Record<ActiveSlotId, { name: string; keyHint: string; desc: string }> = {
    M1: { name: 'Slot Mouse 1', keyHint: 'Tasto Sinistro / Touch', desc: 'Attacco Primario' },
    M2: { name: 'Slot Mouse 2', keyHint: 'Tasto Destro (M2)', desc: 'Abilità Rapida' },
    Q: { name: 'Slot Tasto Q', keyHint: 'Tasto Q / Bottone Q', desc: 'Abilità 1' },
    W: { name: 'Slot Tasto W', keyHint: 'Tasto W / Bottone W', desc: 'Abilità 2' },
    E: { name: 'Slot Tasto E', keyHint: 'Tasto E / Bottone E', desc: 'Abilità 3' },
    R: { name: 'Slot Tasto R', keyHint: 'Tasto R / Ultimate', desc: 'Abilità Pesante / Finale' },
  };

  const currentSkillForSelectedSlot = engine.getSkillDefinition(engine.skillLoadout[selectedSlot]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-2 sm:p-4 select-none animate-in fade-in duration-200">
      <div className="parchment-window w-full max-w-4xl max-h-[92vh] overflow-y-auto rounded-2xl p-4 sm:p-6 text-[#2b1c0c] shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b-2 border-[#8c6239]/40 pb-3 mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-[#dec89b] border-2 border-[#7a4e21] flex items-center justify-center shadow-inner">
              <BookOpen className="w-5 h-5 text-[#69390f]" />
            </div>
            <div>
              <h2 className="font-cinzel text-lg sm:text-xl font-black text-[#381e09] tracking-wide flex items-center gap-2">
                <span>GRIMORIO DELLE ABILITÀ & ASSEGNAZIONE TASTI</span>
              </h2>
              <div className="text-[11px] text-[#694625] italic font-serif">
                Configura i poteri per mouse (M1, M2), tastiera (Q, W, E, R) e comandi touch mobili.
              </div>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-[#ebd9b4] hover:bg-[#d8be8d] border border-[#8c6032] flex items-center justify-center text-[#4a2e12] transition-colors shadow"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Hotkey Guide Notice */}
        <div className="parchment-card rounded-xl p-3 mb-4 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs border border-[#bfa57b]">
          <div className="flex items-center gap-2 text-[#4a2e12]">
            <Keyboard className="w-4 h-4 text-[#854d0e]" />
            <span>
              <strong>Comandi Browser / PC:</strong> Attacca con <strong>Mouse 1</strong>, lancia poteri con <strong>Mouse 2 (Tasto Destro)</strong> e coi tasti <strong>Q, W, E, R</strong>. Spazio per lo scatto.
            </span>
          </div>
          <div className="flex items-center gap-2 text-[#0369a1] bg-[#e0f2fe] px-2.5 py-1 rounded-md border border-[#7dd3fc]">
            <Smartphone className="w-4 h-4" />
            <span className="font-semibold">
              <strong>Touch Cellulare:</strong> Tastierino d'azione visibile a schermo!
            </span>
          </div>
        </div>

        {/* 1. Slots Selector Bar */}
        <div className="mb-5">
          <div className="text-xs font-cinzel font-bold text-[#5c3a1e] mb-2 uppercase tracking-wider flex items-center gap-1.5">
            <Compass className="w-3.5 h-3.5 text-[#854d0e]" />
            1. Seleziona lo Slot d'Azione da Configurare:
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-6 gap-2">
            {(['M1', 'M2', 'Q', 'W', 'E', 'R'] as ActiveSlotId[]).map((slotId) => {
              const assignedSkillId = engine.skillLoadout[slotId];
              const skill = engine.getSkillDefinition(assignedSkillId);
              const isSelected = selectedSlot === slotId;

              return (
                <div
                  key={slotId}
                  onClick={() => setSelectedSlot(slotId)}
                  className={`p-2.5 rounded-xl border-2 cursor-pointer flex flex-col items-center justify-between transition-all ${
                    isSelected
                      ? 'parchment-slot-active scale-105 shadow-md'
                      : 'parchment-slot'
                  }`}
                >
                  <span className="text-[10px] font-black uppercase text-[#854d0e] font-cinzel">
                    {slotId}
                  </span>
                  <div className="text-2xl my-1">{skill ? skill.icon : '❓'}</div>
                  <div className="text-[10px] font-bold text-center truncate max-w-full text-[#381e09]">
                    {skill ? skill.name : 'Non assegnato'}
                  </div>
                  <span className="text-[8px] text-[#785226] mt-0.5">
                    {slotLabels[slotId].keyHint}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* 2. Available Skills List to assign to selectedSlot */}
        <div>
          <div className="text-xs font-cinzel font-bold text-[#5c3a1e] mb-2.5 uppercase tracking-wider flex items-center justify-between">
            <span>
              2. Scegli l'Abilità per <strong>{slotLabels[selectedSlot].name}</strong>:
            </span>
            {currentSkillForSelectedSlot && (
              <span className="text-[#854d0e] font-bold bg-[#f5ecd5] px-2 py-0.5 rounded border border-[#cbaf83]">
                Attuale: {currentSkillForSelectedSlot.icon} {currentSkillForSelectedSlot.name}
              </span>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {classSkills.map((skill) => {
              const isAssignedToThisSlot = engine.skillLoadout[selectedSlot] === skill.id;
              const assignedSlot = (Object.keys(engine.skillLoadout) as ActiveSlotId[]).find(
                (k) => engine.skillLoadout[k] === skill.id
              );

              return (
                <div
                  key={skill.id}
                  onClick={() => handleAssign(skill.id)}
                  className={`p-3.5 rounded-xl border-2 cursor-pointer flex items-start gap-3 transition-all ${
                    isAssignedToThisSlot
                      ? 'bg-[#f0fdf4] border-2 border-emerald-600 shadow-md'
                      : 'parchment-card hover:border-[#854d0e]'
                  }`}
                >
                  <div
                    className="w-12 h-12 rounded-xl flex items-center justify-center text-2xl border-2 shadow-inner"
                    style={{ backgroundColor: `${skill.color}20`, borderColor: skill.color }}
                  >
                    {skill.icon}
                  </div>

                  <div className="flex-1">
                    <div className="flex items-center justify-between mb-1">
                      <div className="flex items-center gap-1.5">
                        <h4 className="font-cinzel font-bold text-sm text-[#2b1706]">
                          {skill.name}
                        </h4>
                        {skill.isUltimate && (
                          <span className="text-[9px] font-black uppercase px-1.5 py-0.5 rounded bg-purple-100 text-purple-900 border border-purple-300">
                            ULTIMATE
                          </span>
                        )}
                      </div>

                      {isAssignedToThisSlot ? (
                        <span className="flex items-center gap-1 text-[10px] font-bold text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded border border-emerald-400">
                          <Check className="w-3 h-3" /> ASSEGNATO A {selectedSlot}
                        </span>
                      ) : assignedSlot ? (
                        <span className="text-[9px] text-[#854d0e] font-semibold bg-[#f5ebd5] px-1.5 py-0.5 rounded border border-[#cbaf83]">
                          In Slot {assignedSlot}
                        </span>
                      ) : null}
                    </div>

                    <p className="text-xs text-[#523820] mb-2 leading-relaxed font-serif italic">
                      "{skill.desc}"
                    </p>

                    <div className="flex items-center gap-3 text-[10px] text-[#694625] font-mono bg-[#eeddbb]/60 px-2 py-1 rounded border border-[#cbb28b]">
                      <span>Ricarica: <strong className="text-[#0369a1]">{skill.cooldown}s</strong></span>
                      <span>Marea: <strong className="text-[#0891b2]">{skill.tideCost}</strong></span>
                      <span>Danno: <strong className="text-[#b91c1c]">{skill.damageMult}x</strong></span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};
