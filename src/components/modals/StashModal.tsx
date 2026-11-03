import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { X, Package, ArrowDownUp } from 'lucide-react';

interface StashModalProps {
  engine: GameEngine;
  onClose: () => void;
  onRefresh: () => void;
}

export const StashModal: React.FC<StashModalProps> = ({
  engine,
  onClose,
  onRefresh,
}) => {
  const handleDeposit = (idx: number) => {
    if (engine.depositToStash(idx)) {
      onRefresh();
    }
  };

  const handleWithdraw = (idx: number) => {
    if (engine.withdrawFromStash(idx)) {
      onRefresh();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-2 sm:p-4 select-none animate-in fade-in duration-200">
      <div className="parchment-window w-full max-w-3xl max-h-[92vh] overflow-y-auto rounded-2xl p-4 sm:p-6 text-[#2b1c0c] shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b-2 border-[#8c6239]/40 pb-3 mb-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-lg bg-[#e8d7b7] border-2 border-[#b45309] p-1 flex items-center justify-center shadow-inner">
              <img
                src="/sprites/cassa_tesoro.png"
                alt="Cassa di Spedizione"
                className="w-full h-full object-contain filter drop-shadow"
              />
            </div>
            <div>
              <h2 className="font-cinzel text-lg sm:text-xl font-black text-[#381e09] tracking-wide">
                CASSA DI SPEDIZIONE · DEPOSITO SICURO
              </h2>
              <div className="text-[11px] text-[#694625] italic font-serif">
                16 Vani di stoccaggio permanente protetto tra un'immersione e l'altra
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

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Stash (16 slots) */}
          <div className="parchment-card rounded-xl p-3.5 border-2 border-[#b59868]">
            <div className="flex items-center justify-between mb-3 pb-1 border-b border-[#cbb28b]">
              <span className="font-cinzel font-bold text-xs text-[#854d0e] uppercase flex items-center gap-1.5">
                <Package className="w-3.5 h-3.5" />
                Deposito Cassa ({engine.stash.length} / 16)
              </span>
              <span className="text-[10px] text-[#714d2a] font-serif italic">Clicca per ritirare</span>
            </div>

            <div className="grid grid-cols-4 gap-2">
              {Array.from({ length: 16 }).map((_, idx) => {
                const it = engine.stash[idx];
                if (!it) {
                  return (
                    <div
                      key={idx}
                      className="h-16 rounded-lg border-2 border-dashed border-[#bda682] bg-[#f5ecd5]/50 flex items-center justify-center text-[10px] text-[#8a7253] font-serif"
                    >
                      #{idx + 1}
                    </div>
                  );
                }

                return (
                  <div
                    key={idx}
                    onClick={() => handleWithdraw(idx)}
                    title="Clicca per ritirare nello zaino"
                    className="h-16 rounded-lg p-1.5 flex flex-col justify-between cursor-pointer hover:scale-105 parchment-slot transition-all shadow-sm"
                  >
                    <div className="text-[10px] font-bold truncate text-[#2f1b0a]">
                      {it.name}
                    </div>
                    <div className="flex items-center justify-between text-[9px] text-[#694625]">
                      <span className="uppercase">{it.slot === 'weapon' ? 'Arma' : it.slot === 'armor' ? 'Corazza' : 'Reliquia'}</span>
                      {it.upgradeLevel > 0 && <span className="text-amber-900 font-extrabold">+{it.upgradeLevel}</span>}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Backpack (8 slots) */}
          <div className="parchment-card rounded-xl p-3.5 border-2 border-[#b59868]">
            <div className="flex items-center justify-between mb-3 pb-1 border-b border-[#cbb28b]">
              <span className="font-cinzel font-bold text-xs text-[#0369a1] uppercase flex items-center gap-1.5">
                <ArrowDownUp className="w-3.5 h-3.5" />
                Tuo Zaino ({engine.backpack.length} / 8)
              </span>
              <span className="text-[10px] text-[#714d2a] font-serif italic">Clicca per depositare</span>
            </div>

            <div className="grid grid-cols-4 gap-2">
              {Array.from({ length: 8 }).map((_, idx) => {
                const it = engine.backpack[idx];
                if (!it) {
                  return (
                    <div
                      key={idx}
                      className="h-16 rounded-lg border-2 border-dashed border-[#bda682] bg-[#f5ecd5]/50 flex items-center justify-center text-[10px] text-[#8a7253] font-serif"
                    >
                      #{idx + 1}
                    </div>
                  );
                }

                return (
                  <div
                    key={idx}
                    onClick={() => handleDeposit(idx)}
                    title="Clicca per depositare nella cassa"
                    className="h-16 rounded-lg p-1.5 flex flex-col justify-between cursor-pointer hover:scale-105 parchment-slot transition-all shadow-sm"
                  >
                    <div className="text-[10px] font-bold truncate text-[#2f1b0a]">
                      {it.name}
                    </div>
                    <div className="flex items-center justify-between text-[9px] text-[#694625]">
                      <span className="uppercase">{it.slot === 'weapon' ? 'Arma' : it.slot === 'armor' ? 'Corazza' : 'Reliquia'}</span>
                      {it.upgradeLevel > 0 && <span className="text-amber-900 font-extrabold">+{it.upgradeLevel}</span>}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
