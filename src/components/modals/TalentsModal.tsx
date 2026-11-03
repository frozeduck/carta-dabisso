import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { ALL_SKILLS } from '../../game/skillsData';
import { CLASSES_DATA } from '../../types/game';
import { X, Sparkles, RotateCcw, ArrowDown, Check, Compass } from 'lucide-react';

interface TalentsModalProps {
  engine: GameEngine;
  onClose: () => void;
  onRefresh: () => void;
}

export const TalentsModal: React.FC<TalentsModalProps> = ({
  engine,
  onClose,
  onRefresh,
}) => {
  const clz = CLASSES_DATA[engine.playerClass];
  const classSkills = ALL_SKILLS.filter((s) => s.classType === engine.playerClass);
  const branch1 = classSkills.filter((s) => s.branch === 1).sort((a, b) => a.tier - b.tier);
  const branch2 = classSkills.filter((s) => s.branch === 2).sort((a, b) => a.tier - b.tier);

  const handleUnlock = (skillId: string) => {
    if (engine.unlockSkill(skillId)) {
      onRefresh();
    }
  };

  const handleRespec = () => {
    if (engine.shells < 25) {
      alert('Servono almeno 25 conchiglie per azzerare i talenti!');
      return;
    }
    engine.respecSkills();
    onRefresh();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-2 sm:p-4 select-none animate-in fade-in duration-200">
      <div className="parchment-window w-full max-w-4xl max-h-[92vh] overflow-y-auto rounded-2xl p-4 sm:p-6 text-[#2b1c0c] shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b-2 border-[#8c6239]/40 pb-3 mb-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-lg bg-[#e8d7b7] border-2 border-[#7e22ce] p-1 flex items-center justify-center shadow-inner">
              <img
                src={clz.portrait}
                alt={clz.name}
                className="w-full h-full object-contain filter drop-shadow"
              />
            </div>
            <div>
              <h2 className="font-cinzel text-lg sm:text-xl font-black text-[#381e09] tracking-wide flex items-center gap-2">
                <span>TAVOLA DEI TALENTI NAUTICI · {clz.name.toUpperCase()}</span>
                <Compass className="w-4 h-4 text-[#854d0e]" />
              </h2>
              <div className="text-[11px] text-[#694625] italic font-serif">
                Costellazioni di abilità passive permanenti · Grado Palombaro Lv.{engine.level}
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

        {/* Skill Points Banner & Respec */}
        <div className="parchment-panel-inset rounded-xl p-3.5 mb-5 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <Sparkles className="w-5 h-5 text-[#854d0e]" />
            <span className="font-cinzel font-bold text-xs sm:text-sm text-[#4a2e12]">
              Punti Talento Disponibili:
            </span>
            <span className="font-black text-lg text-[#854d0e] px-2.5 py-0.5 rounded bg-[#f5ecd5] border border-[#cbb28b] shadow-inner">
              {engine.skillPoints}
            </span>
          </div>

          <button
            onClick={handleRespec}
            disabled={engine.unlockedSkills.size === 0 || engine.shells < 25}
            className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg parchment-button-neutral text-xs font-bold disabled:opacity-40 transition-colors shadow"
          >
            <RotateCcw className="w-4 h-4 text-[#854d0e]" />
            Azzera Talenti (25 🐚)
          </button>
        </div>

        {/* 2 Branches Grid with Flow Conduits */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {/* Branch 1 */}
          <div className="parchment-card rounded-xl p-4 border-2 border-[#b59868] flex flex-col items-center">
            <h3 className="font-cinzel font-bold text-xs sm:text-sm text-[#0369a1] uppercase tracking-wider mb-4 pb-1 border-b border-[#0369a1]/30 w-full text-center">
              Ramo I: {engine.playerClass === 'hunter' ? 'Balistica Pesante' : engine.playerClass === 'juggernaut' ? 'Bastione di Corallo' : 'Signore delle Tempeste'}
            </h3>

            <div className="w-full flex flex-col items-center gap-2.5">
              {branch1.map((skill, idx) => {
                const isUnlocked = engine.hasSkill(skill.id);
                const canUnlock = engine.canUnlockSkill(skill.id);

                return (
                  <React.Fragment key={skill.id}>
                    {idx > 0 && (
                      <div className="flex flex-col items-center my-0.5 text-[#0369a1]">
                        <ArrowDown className="w-4 h-4" />
                      </div>
                    )}
                    <div
                      className={`w-full p-3 rounded-xl border-2 transition-all ${
                        isUnlocked
                          ? 'bg-[#f0fdf4] border-emerald-600 shadow-md'
                          : canUnlock
                          ? 'parchment-slot-active shadow-sm'
                          : 'bg-[#ebdcb9]/40 border-dashed border-[#b89d75] opacity-55'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2 mb-1.5">
                        <div className="flex items-center gap-1.5">
                          <span className="font-cinzel font-black text-[10px] px-1.5 py-0.5 rounded bg-[#ebd7b2] text-[#69390f] border border-[#a47e4e]">
                            Tier {skill.tier === 1 ? 'I' : skill.tier === 2 ? 'II' : 'III'}
                          </span>
                          <h4 className="font-cinzel font-bold text-xs sm:text-sm text-[#2b1706]">
                            {skill.name}
                          </h4>
                        </div>
                        {isUnlocked ? (
                          <span className="flex items-center gap-1 text-[9px] font-bold text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded border border-emerald-400">
                            <Check className="w-3 h-3" /> ATTIVO
                          </span>
                        ) : canUnlock ? (
                          <button
                            onClick={() => handleUnlock(skill.id)}
                            className="px-2.5 py-1 rounded parchment-button-green text-[10px] font-bold shadow"
                          >
                            SBLOCCA (-1 Pt)
                          </button>
                        ) : (
                          <span className="text-[10px] font-semibold text-[#8c6b4d] italic">
                            Bloccato
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-[#523820] leading-relaxed font-serif italic">
                        "{skill.desc}"
                      </p>
                    </div>
                  </React.Fragment>
                );
              })}
            </div>
          </div>

          {/* Branch 2 */}
          <div className="parchment-card rounded-xl p-4 border-2 border-[#b59868] flex flex-col items-center">
            <h3 className="font-cinzel font-bold text-xs sm:text-sm text-[#854d0e] uppercase tracking-wider mb-4 pb-1 border-b border-[#854d0e]/30 w-full text-center">
              Ramo II: {engine.playerClass === 'hunter' ? 'Agilità Idrodinamica' : engine.playerClass === 'juggernaut' ? 'Maglio Sismico' : 'Drenaggio Oscuro'}
            </h3>

            <div className="w-full flex flex-col items-center gap-2.5">
              {branch2.map((skill, idx) => {
                const isUnlocked = engine.hasSkill(skill.id);
                const canUnlock = engine.canUnlockSkill(skill.id);

                return (
                  <React.Fragment key={skill.id}>
                    {idx > 0 && (
                      <div className="flex flex-col items-center my-0.5 text-[#854d0e]">
                        <ArrowDown className="w-4 h-4" />
                      </div>
                    )}
                    <div
                      className={`w-full p-3 rounded-xl border-2 transition-all ${
                        isUnlocked
                          ? 'bg-[#f0fdf4] border-emerald-600 shadow-md'
                          : canUnlock
                          ? 'parchment-slot-active shadow-sm'
                          : 'bg-[#ebdcb9]/40 border-dashed border-[#b89d75] opacity-55'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2 mb-1.5">
                        <div className="flex items-center gap-1.5">
                          <span className="font-cinzel font-black text-[10px] px-1.5 py-0.5 rounded bg-[#ebd7b2] text-[#69390f] border border-[#a47e4e]">
                            Tier {skill.tier === 1 ? 'I' : skill.tier === 2 ? 'II' : 'III'}
                          </span>
                          <h4 className="font-cinzel font-bold text-xs sm:text-sm text-[#2b1706]">
                            {skill.name}
                          </h4>
                        </div>
                        {isUnlocked ? (
                          <span className="flex items-center gap-1 text-[9px] font-bold text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded border border-emerald-400">
                            <Check className="w-3 h-3" /> ATTIVO
                          </span>
                        ) : canUnlock ? (
                          <button
                            onClick={() => handleUnlock(skill.id)}
                            className="px-2.5 py-1 rounded parchment-button-gold text-[10px] font-bold shadow"
                          >
                            SBLOCCA (-1 Pt)
                          </button>
                        ) : (
                          <span className="text-[10px] font-semibold text-[#8c6b4d] italic">
                            Bloccato
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-[#523820] leading-relaxed font-serif italic">
                        "{skill.desc}"
                      </p>
                    </div>
                  </React.Fragment>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
