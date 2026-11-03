import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { X, BookOpen, CheckCircle, Lock, Compass, Scroll } from 'lucide-react';

interface SageModalProps {
  engine: GameEngine;
  onClose: () => void;
}

export const SageModal: React.FC<SageModalProps> = ({ engine, onClose }) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-2 sm:p-4 select-none animate-in fade-in duration-200">
      <div className="parchment-window w-full max-w-3xl max-h-[92vh] overflow-y-auto rounded-2xl p-4 sm:p-6 text-[#2b1c0c] shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b-2 border-[#8c6239]/40 pb-3 mb-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-lg bg-[#e8d7b7] border-2 border-[#16a34a] p-1 flex items-center justify-center shadow-inner">
              <img
                src="/sprites/anziano_tartaruga.png"
                alt="Anziana Tartaruga"
                className="w-full h-full object-contain filter drop-shadow"
              />
            </div>
            <div>
              <h2 className="font-cinzel text-lg sm:text-xl font-black text-[#381e09] tracking-wide flex items-center gap-2">
                <span>ARCHIVIO STORICO · ANZIANA TARTARUGA</span>
                <Scroll className="w-4 h-4 text-[#854d0e]" />
              </h2>
              <div className="text-[11px] text-[#694625] italic font-serif">
                Cronache delle Profondità, Atti di Spedizione e Taglie sui Leviatani
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

        {/* Acts List */}
        <div className="space-y-4">
          {engine.acts.map((act) => {
            const isUnlocked = act.number <= engine.maxActUnlocked;
            const isCompleted = act.completed;

            return (
              <div
                key={act.number}
                className={`p-4 rounded-xl border-2 transition-all ${
                  isCompleted
                    ? 'parchment-card border-emerald-600/70 bg-[#f0fdf4]'
                    : isUnlocked
                    ? 'parchment-card border-[#9c7546] shadow-md'
                    : 'bg-[#ebdcb9]/40 border-dashed border-[#b89d75] opacity-55'
                }`}
              >
                <div className="flex items-start justify-between gap-2 mb-2 pb-1.5 border-b border-[#cbb28b]">
                  <div>
                    <span className="text-[10px] font-bold uppercase tracking-wider text-[#854d0e]">
                      Profondità -{act.startDepth}m
                    </span>
                    <h3 className="font-cinzel font-bold text-base text-[#2e1705]">
                      {act.title}
                    </h3>
                    <div className="text-xs font-semibold text-[#0369a1]">
                      {act.subtitle}
                    </div>
                  </div>

                  {isCompleted ? (
                    <span className="flex items-center gap-1 text-xs font-bold text-emerald-800 bg-emerald-100/90 px-2.5 py-1 rounded-lg border border-emerald-400">
                      <CheckCircle className="w-4 h-4 text-emerald-700" /> COMPLETATO
                    </span>
                  ) : isUnlocked ? (
                    <span className="flex items-center gap-1 text-xs font-bold text-[#854d0e] bg-amber-100/90 px-2.5 py-1 rounded-lg border border-amber-300">
                      <Compass className="w-4 h-4 text-[#854d0e]" /> IN CORSO
                    </span>
                  ) : (
                    <span className="flex items-center gap-1 text-xs font-bold text-stone-600 bg-stone-200/90 px-2.5 py-1 rounded-lg border border-stone-300">
                      <Lock className="w-4 h-4" /> BLOCCATO
                    </span>
                  )}
                </div>

                <div className="parchment-panel-inset p-3 rounded-lg mb-2">
                  <div className="text-xs font-bold text-[#854d0e] mb-1 font-cinzel">
                    Missione: {act.questTitle}
                  </div>
                  <p className="text-xs text-[#523820] leading-relaxed font-serif italic">
                    "{act.questDesc}"
                  </p>
                </div>

                <div className="flex items-center justify-between text-xs text-[#694625] pt-1">
                  <span>
                    Guardiano dell'Atto: <strong className="text-rose-900 font-cinzel">{act.bossName}</strong>
                  </span>
                  <span>Ricompensa: 💎 Frammenti & 🐚 Conchiglie</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
