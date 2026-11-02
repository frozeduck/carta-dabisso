import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { ALL_SKILLS } from '../../game/skillsData';
import { CLASSES_DATA } from '../../types/game';
import { X, Sparkles, RotateCcw, ArrowDown, Check } from 'lucide-react';

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
    if (confirm('Vuoi ripristinare tutti i talenti per 25 conchiglie? Recupererai tutti i punti abilità spesi.')) {
      engine.respecSkills();
      onRefresh();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-3 sm:p-4 select-none">
      <div className="relative w-full max-w-4xl max-h-[92vh] overflow-y-auto bg-slate-900 border-2 border-purple-500/80 rounded-2xl shadow-2xl p-5 text-slate-100">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3 mb-4">
          <div className="flex items-center gap-3">
            <img
              src={clz.portrait}
              alt={clz.name}
              className="w-10 h-10 rounded-lg border border-purple-400 object-cover bg-slate-950"
            />
            <div>
              <h2 className="font-cinzel text-xl font-black text-purple-300">
                ALBERO TALENTI · {clz.name.toUpperCase()}
              </h2>
              <div className="text-xs text-slate-400">
                Identità di classe permanente · Livello {engine.level}
              </div>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Skill Points Banner & Respec */}
        <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-3.5 mb-5 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-purple-400" />
            <span className="font-cinzel font-bold text-sm text-slate-300">
              Punti Abilità Disponibili:
            </span>
            <span className="font-black text-lg text-purple-400 px-2 py-0.5 rounded bg-purple-950/80 border border-purple-600/40">
              {engine.skillPoints}
            </span>
          </div>

          <button
            onClick={handleRespec}
            disabled={engine.unlockedSkills.size === 0 || engine.shells < 25}
            className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-slate-800 hover:bg-amber-950 text-xs font-bold text-amber-400 hover:text-amber-300 border border-slate-700 hover:border-amber-600 disabled:opacity-40 disabled:pointer-events-none transition-colors"
          >
            <RotateCcw className="w-4 h-4" />
            Azzera Talenti (25 🐚)
          </button>
        </div>

        {/* 2 Branches Grid with Flow Conduits */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Branch 1 */}
          <div className="bg-slate-950/50 border border-slate-800 rounded-xl p-4 flex flex-col items-center">
            <h3 className="font-cinzel font-bold text-sm text-sky-300 uppercase tracking-wider mb-4 pb-1 border-b border-sky-500/30 w-full text-center">
              Ramo I: {engine.playerClass === 'hunter' ? 'Balistica Pesante' : engine.playerClass === 'juggernaut' ? 'Bastione di Corallo' : 'Signore delle Tempeste'}
            </h3>

            <div className="w-full flex flex-col items-center gap-3">
              {branch1.map((skill, idx) => {
                const isUnlocked = engine.hasSkill(skill.id);
                const canUnlock = engine.canUnlockSkill(skill.id);

                return (
                  <React.Fragment key={skill.id}>
                    {idx > 0 && (
                      <div className="flex flex-col items-center my-0.5 text-sky-400/60">
                        <ArrowDown className="w-4 h-4 animate-bounce" />
                      </div>
                    )}
                    <div
                      className={`w-full p-3.5 rounded-xl border-2 transition-all ${
                        isUnlocked
                          ? 'border-purple-400 bg-purple-950/30 shadow-[0_0_12px_rgba(168,85,247,0.3)]'
                          : canUnlock
                          ? 'border-sky-500/80 bg-slate-900 hover:border-sky-400'
                          : 'border-slate-800 bg-slate-950/60 opacity-60'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2 mb-1.5">
                        <div className="flex items-center gap-2">
                          <span className="font-cinzel font-black text-xs px-1.5 py-0.5 rounded bg-slate-800 text-amber-400 border border-slate-700">
                            Tier {skill.tier === 1 ? 'I' : skill.tier === 2 ? 'II' : 'III'}
                          </span>
                          <h4 className="font-cinzel font-bold text-xs sm:text-sm text-sky-200">
                            {skill.name}
                          </h4>
                        </div>
                        {isUnlocked ? (
                          <span className="flex items-center gap-1 text-[10px] font-bold text-emerald-400 bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-500/40">
                            <Check className="w-3 h-3" /> ATTIVO
                          </span>
                        ) : canUnlock ? (
                          <button
                            onClick={() => handleUnlock(skill.id)}
                            className="px-2.5 py-1 rounded-lg bg-sky-600 hover:bg-sky-500 text-[10px] font-bold text-white shadow transition-colors"
                          >
                            SBLOCCA (-1 Pt)
                          </button>
                        ) : (
                          <span className="text-[10px] font-semibold text-slate-500">
                            Bloccato
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-slate-300 leading-relaxed">
                        {skill.desc}
                      </p>
                    </div>
                  </React.Fragment>
                );
              })}
            </div>
          </div>

          {/* Branch 2 */}
          <div className="bg-slate-950/50 border border-slate-800 rounded-xl p-4 flex flex-col items-center">
            <h3 className="font-cinzel font-bold text-sm text-amber-300 uppercase tracking-wider mb-4 pb-1 border-b border-amber-500/30 w-full text-center">
              Ramo II: {engine.playerClass === 'hunter' ? 'Agilità Idrodinamica' : engine.playerClass === 'juggernaut' ? 'Maglio Sismico' : 'Drenaggio Oscuro'}
            </h3>

            <div className="w-full flex flex-col items-center gap-3">
              {branch2.map((skill, idx) => {
                const isUnlocked = engine.hasSkill(skill.id);
                const canUnlock = engine.canUnlockSkill(skill.id);

                return (
                  <React.Fragment key={skill.id}>
                    {idx > 0 && (
                      <div className="flex flex-col items-center my-0.5 text-amber-400/60">
                        <ArrowDown className="w-4 h-4 animate-bounce" />
                      </div>
                    )}
                    <div
                      className={`w-full p-3.5 rounded-xl border-2 transition-all ${
                        isUnlocked
                          ? 'border-purple-400 bg-purple-950/30 shadow-[0_0_12px_rgba(168,85,247,0.3)]'
                          : canUnlock
                          ? 'border-amber-500/80 bg-slate-900 hover:border-amber-400'
                          : 'border-slate-800 bg-slate-950/60 opacity-60'
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2 mb-1.5">
                        <div className="flex items-center gap-2">
                          <span className="font-cinzel font-black text-xs px-1.5 py-0.5 rounded bg-slate-800 text-amber-400 border border-slate-700">
                            Tier {skill.tier === 1 ? 'I' : skill.tier === 2 ? 'II' : 'III'}
                          </span>
                          <h4 className="font-cinzel font-bold text-xs sm:text-sm text-amber-200">
                            {skill.name}
                          </h4>
                        </div>
                        {isUnlocked ? (
                          <span className="flex items-center gap-1 text-[10px] font-bold text-emerald-400 bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-500/40">
                            <Check className="w-3 h-3" /> ATTIVO
                          </span>
                        ) : canUnlock ? (
                          <button
                            onClick={() => handleUnlock(skill.id)}
                            className="px-2.5 py-1 rounded-lg bg-amber-600 hover:bg-amber-500 text-[10px] font-bold text-slate-950 shadow transition-colors"
                          >
                            SBLOCCA (-1 Pt)
                          </button>
                        ) : (
                          <span className="text-[10px] font-semibold text-slate-500">
                            Bloccato
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-slate-300 leading-relaxed">
                        {skill.desc}
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
