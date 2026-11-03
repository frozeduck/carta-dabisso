import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { Trophy, Navigation, Compass } from 'lucide-react';

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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-sm p-4 select-none animate-in fade-in duration-200">
      <div className="parchment-window w-full max-w-lg rounded-2xl p-6 text-center text-[#2b1c0c] shadow-2xl border-4 border-[#b45309]">
        <div className="w-16 h-16 rounded-full bg-[#fef3c7] border-2 border-[#d97706] mx-auto mb-4 flex items-center justify-center text-[#b45309] shadow-inner">
          <Trophy className="w-9 h-9" />
        </div>

        <h2 className="font-cinzel text-xl sm:text-2xl font-black text-[#854d0e] mb-2 tracking-wide flex items-center justify-center gap-2">
          <span>TRIONFO DEGLI ABISSI!</span>
        </h2>
        <p className="text-xs text-[#523820] mb-5 leading-relaxed font-serif italic">
          Hai domato il Leviatano Primordiale e liberato le rotte sottomarine! I tuoi traguardi sono stati scolpiti per sempre nelle cronache di Carta d'Abisso.
        </p>

        <div className="parchment-panel-inset p-3.5 rounded-xl text-xs text-[#4a2e12] space-y-1.5 mb-6">
          <div className="text-emerald-800 font-bold">+25 FRAMMENTI ABISSALI & +250 CONCHIGLIE</div>
          <div>Eroe Palombaro: <strong className="text-[#0369a1]">{engine.playerName}</strong> (Lv.{engine.level})</div>
          <div>Modalità Endless Dive sbloccata permanentemente!</div>
        </div>

        <div className="flex flex-col sm:flex-row gap-3">
          <button
            onClick={onReturnToHub}
            className="flex-1 py-2.5 rounded-xl parchment-button-neutral font-cinzel font-bold text-xs"
          >
            TORNA AL SANTUARIO
          </button>
          <button
            onClick={onContinueEndless}
            className="flex-1 py-2.5 rounded-xl parchment-button-gold font-cinzel font-bold text-xs shadow-md flex items-center justify-center gap-1.5"
          >
            <Navigation className="w-3.5 h-3.5" />
            ENDLESS DIVE
          </button>
        </div>
      </div>
    </div>
  );
};
