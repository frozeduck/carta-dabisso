import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { Trophy, Sparkles, Navigation } from 'lucide-react';

interface VictoryModalProps {
  engine: GameEngine;
  onReturnToHub: () => void;
  onContinueEndless: () => void;
}

export const VictoryModal: React.FC<VictoryModalProps> = ({
  engine,
  onReturnToHub,
  onContinueEndless,
}) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-md p-4 select-none">
      <div className="relative w-full max-w-lg bg-slate-900 border-2 border-amber-400 rounded-2xl shadow-[0_0_35px_rgba(251,191,36,0.5)] p-6 text-center text-slate-100">
        <div className="w-16 h-16 rounded-full bg-amber-950/80 border-2 border-amber-400 mx-auto mb-4 flex items-center justify-center text-amber-400">
          <Trophy className="w-9 h-9" />
        </div>

        <h2 className="font-cinzel text-2xl font-black text-amber-300 mb-2">
          VITTORIA DELL'ABISSO!
        </h2>
        <p className="text-xs text-slate-300 mb-5 leading-relaxed">
          Hai domato il Leviatano Primordiale e liberato le rotte sottomarine! I tuoi traguardi sono stati scolpiti nell'Archivio del Santuario.
        </p>

        <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 text-xs text-slate-300 space-y-1.5 mb-6">
          <div className="text-emerald-400 font-bold">+25 FRAMMENTI ABISSALI & +250 CONCHIGLIE</div>
          <div>Eroe: <strong className="text-sky-300">{engine.playerName}</strong> (Lv.{engine.level})</div>
          <div>Modalità Endless Dive sbloccata permanentemente!</div>
        </div>

        <div className="flex flex-col sm:flex-row gap-3">
          <button
            onClick={onReturnToHub}
            className="flex-1 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 font-cinzel font-bold text-xs text-slate-200 transition-colors"
          >
            TORNA AL SANTUARIO
          </button>
          <button
            onClick={onContinueEndless}
            className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-amber-500 to-yellow-600 hover:from-amber-400 hover:to-yellow-500 font-cinzel font-bold text-xs text-slate-950 shadow-md transition-all flex items-center justify-center gap-1.5"
          >
            <Navigation className="w-3.5 h-3.5" />
            ENDLESS DIVE
          </button>
        </div>
      </div>
    </div>
  );
};
