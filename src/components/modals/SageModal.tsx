import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { X, BookOpen, CheckCircle, Lock, Compass } from 'lucide-react';

interface SageModalProps {
  engine: GameEngine;
  onClose: () => void;
}

export const SageModal: React.FC<SageModalProps> = ({ engine, onClose }) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-3 sm:p-4 select-none">
      <div className="relative w-full max-w-3xl max-h-[92vh] overflow-y-auto bg-slate-900 border-2 border-emerald-500/80 rounded-2xl shadow-2xl p-5 text-slate-100">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3 mb-4">
          <div className="flex items-center gap-3">
            <img
              src="/sprites/anziano_tartaruga.png"
              alt="Anziana Tartaruga"
              className="w-12 h-12 rounded-lg border-2 border-emerald-500 object-cover bg-slate-950"
            />
            <div>
              <h2 className="font-cinzel text-xl font-black text-emerald-300">
                ARCHIVIO STORICO · ANZIANA TARTARUGA
              </h2>
              <div className="text-xs text-slate-400">
                Cronache delle Profondità, Atti di Spedizione e Taglie sui Boss
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
                    ? 'border-emerald-500/60 bg-emerald-950/20'
                    : isUnlocked
                    ? 'border-sky-400/80 bg-slate-950 shadow-md'
                    : 'border-slate-800 bg-slate-950/40 opacity-50'
                }`}
              >
                <div className="flex items-start justify-between gap-2 mb-2">
                  <div>
                    <span className="text-[10px] font-bold uppercase tracking-wider text-amber-400">
                      Profondità -{act.startDepth}m
                    </span>
                    <h3 className="font-cinzel font-bold text-base text-sky-200">
                      {act.title}
                    </h3>
                    <div className="text-xs font-semibold text-sky-400/80">
                      {act.subtitle}
                    </div>
                  </div>

                  {isCompleted ? (
                    <span className="flex items-center gap-1 text-xs font-bold text-emerald-400 bg-emerald-950/80 px-2.5 py-1 rounded-lg border border-emerald-500/40">
                      <CheckCircle className="w-4 h-4" /> COMPLETATO
                    </span>
                  ) : isUnlocked ? (
                    <span className="flex items-center gap-1 text-xs font-bold text-sky-300 bg-sky-950/80 px-2.5 py-1 rounded-lg border border-sky-500/40">
                      <Compass className="w-4 h-4 animate-spin-slow" /> IN CORSO
                    </span>
                  ) : (
                    <span className="flex items-center gap-1 text-xs font-bold text-slate-500 bg-slate-900 px-2.5 py-1 rounded-lg border border-slate-800">
                      <Lock className="w-4 h-4" /> BLOCCATO
                    </span>
                  )}
                </div>

                <div className="bg-slate-900/60 p-3 rounded-lg border border-slate-800/80 mb-2">
                  <div className="text-xs font-bold text-amber-300 mb-1">
                    Missione: {act.questTitle}
                  </div>
                  <p className="text-xs text-slate-300 leading-relaxed">
                    {act.questDesc}
                  </p>
                </div>

                <div className="flex items-center justify-between text-xs text-slate-400 pt-1">
                  <span>
                    Guardiano dell'Atto: <strong className="text-red-400">{act.bossName}</strong>
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
