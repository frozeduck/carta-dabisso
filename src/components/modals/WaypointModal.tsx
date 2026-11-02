import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { X, Navigation, Compass, Waves, Flame, Sparkles } from 'lucide-react';

interface WaypointModalProps {
  engine: GameEngine;
  onClose: () => void;
  onStartExpedition: (actNum: number) => void;
}

export const WaypointModal: React.FC<WaypointModalProps> = ({
  engine,
  onClose,
  onStartExpedition,
}) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-3 sm:p-4 select-none">
      <div className="relative w-full max-w-3xl max-h-[92vh] overflow-y-auto bg-slate-900 border-2 border-indigo-500/80 rounded-2xl shadow-2xl p-5 text-slate-100">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3 mb-4">
          <div className="flex items-center gap-3">
            <img
              src="/sprites/portale_citta.png"
              alt="Portale di Spedizione"
              className="w-12 h-12 rounded-lg border-2 border-indigo-500 object-cover bg-slate-950"
            />
            <div>
              <h2 className="font-cinzel text-xl font-black text-indigo-300">
                PORTALE DI SPEDIZIONE · WAYPOINT
              </h2>
              <div className="text-xs text-slate-400">
                Seleziona l'Atto sbloccato per immergerti negli abissi
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

        {/* Acts Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
          {engine.acts.map((act) => {
            const isUnlocked = act.number <= engine.maxActUnlocked;

            return (
              <div
                key={act.number}
                className={`p-4 rounded-xl border-2 flex flex-col justify-between transition-all ${
                  isUnlocked
                    ? 'border-indigo-400/80 bg-slate-950 hover:border-indigo-300 shadow-lg'
                    : 'border-slate-800 bg-slate-950/40 opacity-50'
                }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-[10px] font-bold text-amber-400 uppercase">
                      Profondità -{act.startDepth}m
                    </span>
                    {act.completed && (
                      <span className="text-[10px] font-bold text-emerald-400 bg-emerald-950/70 px-2 py-0.5 rounded border border-emerald-500/40">
                        COMPLETATO
                      </span>
                    )}
                  </div>
                  <h3 className="font-cinzel font-bold text-sm text-indigo-200 mb-1">
                    {act.title}
                  </h3>
                  <div className="text-xs text-sky-400 font-semibold mb-2">
                    {act.subtitle}
                  </div>
                  <p className="text-xs text-slate-300 leading-relaxed mb-3">
                    {act.questDesc}
                  </p>
                  <div className="text-[11px] text-slate-400">
                    Boss: <strong className="text-red-400">{act.bossName}</strong>
                  </div>
                </div>

                <button
                  onClick={() => onStartExpedition(act.number)}
                  disabled={!isUnlocked}
                  className="w-full mt-4 py-2 rounded-lg bg-gradient-to-r from-indigo-600 to-sky-600 hover:from-indigo-500 hover:to-sky-500 font-cinzel font-bold text-xs text-white disabled:opacity-40 transition-all flex items-center justify-center gap-1.5 shadow"
                >
                  <Navigation className="w-3.5 h-3.5" />
                  {isUnlocked ? 'IMMERGITI IN QUESTO ATTO' : 'BLOCCATO'}
                </button>
              </div>
            );
          })}
        </div>

        {/* Endless Dive (Available once Act 4 reached/beaten) */}
        {engine.maxActUnlocked >= 4 && (
          <div className="p-4 rounded-xl border-2 border-purple-500 bg-gradient-to-r from-purple-950/40 via-slate-950 to-slate-900 flex flex-col sm:flex-row items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 text-purple-300 font-cinzel font-black text-sm">
                <Sparkles className="w-4 h-4 text-purple-400" />
                ENDLESS DIVE · ABISSO SENZA FINE
              </div>
              <p className="text-xs text-slate-300 mt-1">
                Sfida le profondità estreme con mostri scalati all'infinito e forzieri colmi di bottino Abissale.
              </p>
            </div>
            <button
              onClick={() => onStartExpedition(5)}
              className="px-5 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 font-cinzel font-bold text-xs text-white shadow-[0_0_15px_rgba(168,85,247,0.5)] transition-all whitespace-nowrap"
            >
              IMMERSIONE INFINITA
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
