import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { Skull, RotateCcw } from 'lucide-react';

interface GameOverModalProps {
  engine: GameEngine;
  onReturnToHub: () => void;
}

export const GameOverModal: React.FC<GameOverModalProps> = ({
  engine,
  onReturnToHub,
}) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-md p-4 select-none">
      <div className="relative w-full max-w-md bg-slate-900 border-2 border-red-600 rounded-2xl shadow-[0_0_30px_rgba(220,38,38,0.5)] p-6 text-center text-slate-100">
        <div className="w-16 h-16 rounded-full bg-red-950/80 border-2 border-red-500 mx-auto mb-4 flex items-center justify-center text-red-500">
          <Skull className="w-9 h-9" />
        </div>

        <h2 className="font-cinzel text-2xl font-black text-red-400 mb-2">
          LA PRESSIONE TI HA SOPRAFFATTO
        </h2>
        <p className="text-xs text-slate-400 mb-5 leading-relaxed">
          Lo scafandro è collassato sotto l'attacco dei predatori dell'abisso. Le correnti di risalita d'emergenza ti hanno riportato al Santuario.
        </p>

        <div className="bg-slate-950 p-3 rounded-xl border border-slate-800 text-xs text-slate-300 space-y-1 mb-6">
          <div>Zona: <span className="font-bold text-sky-300">{engine.zoneName}</span></div>
          <div>Profondità raggiunta: <span className="font-bold text-cyan-400">-{engine.depthMeters}m</span></div>
          <div>Conchiglie salvaguardate: <span className="font-bold text-amber-400">🐚 {engine.shells}</span></div>
        </div>

        <button
          onClick={onReturnToHub}
          className="w-full py-2.5 rounded-xl bg-gradient-to-r from-red-600 to-rose-700 hover:from-red-500 hover:to-rose-600 font-cinzel font-bold text-sm text-white shadow-lg transition-all flex items-center justify-center gap-2"
        >
          <RotateCcw className="w-4 h-4" />
          RIENTRA AL SANTUARIO
        </button>
      </div>
    </div>
  );
};
