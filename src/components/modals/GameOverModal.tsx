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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-sm p-4 select-none animate-in fade-in duration-200">
      <div className="parchment-window w-full max-w-md rounded-2xl p-6 text-center text-[#2b1c0c] shadow-2xl border-4 border-[#7f1d1d]">
        <div className="w-16 h-16 rounded-full bg-[#fee2e2] border-2 border-[#b91c1c] mx-auto mb-4 flex items-center justify-center text-[#991b1b] shadow-inner">
          <Skull className="w-9 h-9" />
        </div>

        <h2 className="font-cinzel text-xl sm:text-2xl font-black text-[#991b1b] mb-2 tracking-wide">
          LA MAREA TI HA SOPRAFFATTO
        </h2>
        <p className="text-xs text-[#523820] mb-5 leading-relaxed font-serif italic">
          Lo scafandro è collassato sotto l'impeto dei mostri abissali. Le correnti di risalita d'emergenza hanno recuperato il tuo equipaggio, riportandoti al Santuario.
        </p>

        <div className="parchment-panel-inset p-3.5 rounded-xl text-xs text-[#4a2e12] space-y-1 mb-6">
          <div>Regione sottomarina: <strong className="text-[#0369a1]">{engine.zoneName}</strong></div>
          <div>Profondità raggiunta: <strong className="text-[#0891b2]">-{engine.depthMeters}m</strong></div>
          <div>Conchiglie recuperate: <strong className="text-[#b45309]">🐚 {engine.shells}</strong></div>
        </div>

        <button
          onClick={onReturnToHub}
          className="w-full py-2.5 rounded-xl parchment-button-gold font-cinzel font-bold text-sm shadow-lg flex items-center justify-center gap-2"
        >
          <RotateCcw className="w-4 h-4" />
          RIENTRA AL SANTUARIO NAUTILUS
        </button>
      </div>
    </div>
  );
};
