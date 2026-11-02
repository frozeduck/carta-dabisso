import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { X, ArrowDownUp, Package, Shield, Sword, Sparkles } from 'lucide-react';

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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-3 sm:p-4 select-none">
      <div className="relative w-full max-w-3xl max-h-[92vh] overflow-y-auto bg-slate-900 border-2 border-amber-500/80 rounded-2xl shadow-2xl p-5 text-slate-100">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3 mb-4">
          <div className="flex items-center gap-3">
            <img
              src="/sprites/cassa_tesoro.png"
              alt="Cassa di Spedizione"
              className="w-12 h-12 rounded-lg border-2 border-amber-500 object-cover bg-slate-950"
            />
            <div>
              <h2 className="font-cinzel text-xl font-black text-amber-300">
                CASSA DI SPEDIZIONE · DEPOSITO SICURO
              </h2>
              <div className="text-xs text-slate-400">
                16 Slot di stoccaggio permanente protetto tra un'immersione e l'altra
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

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Stash (16 slots) */}
          <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-4">
            <div className="flex items-center justify-between mb-3 pb-1 border-b border-slate-800">
              <span className="font-cinzel font-bold text-xs text-amber-300 uppercase">
                Deposito Cassa ({engine.stash.length} / 16)
              </span>
              <span className="text-[10px] text-slate-500">Clicca per ritirare</span>
            </div>

            <div className="grid grid-cols-4 gap-2">
              {Array.from({ length: 16 }).map((_, idx) => {
                const it = engine.stash[idx];
                if (!it) {
                  return (
                    <div
                      key={idx}
                      className="h-16 rounded-lg border border-dashed border-slate-800 bg-slate-900/30 flex items-center justify-center text-[10px] text-slate-700"
                    >
                      {idx + 1}
                    </div>
                  );
                }

                const rColor = engine.getRarityColor(it.rarity);
                return (
                  <div
                    key={idx}
                    onClick={() => handleWithdraw(idx)}
                    title="Clicca per ritirare nello zaino"
                    className="h-16 rounded-lg border-2 p-1.5 flex flex-col justify-between cursor-pointer hover:scale-105 bg-slate-900 transition-all"
                    style={{ borderColor: rColor }}
                  >
                    <div className="text-[10px] font-bold truncate" style={{ color: rColor }}>
                      {it.name}
                    </div>
                    <div className="flex items-center justify-between text-[9px] text-slate-400">
                      <span className="uppercase">{it.slot === 'weapon' ? 'Arma' : it.slot === 'armor' ? 'Corazza' : 'Reliquia'}</span>
                      {it.upgradeLevel > 0 && <span className="text-amber-400 font-bold">+{it.upgradeLevel}</span>}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Backpack (8 slots) */}
          <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-4">
            <div className="flex items-center justify-between mb-3 pb-1 border-b border-slate-800">
              <span className="font-cinzel font-bold text-xs text-sky-300 uppercase">
                Tuo Zaino ({engine.backpack.length} / 8)
              </span>
              <span className="text-[10px] text-slate-500">Clicca per depositare</span>
            </div>

            <div className="grid grid-cols-4 gap-2">
              {Array.from({ length: 8 }).map((_, idx) => {
                const it = engine.backpack[idx];
                if (!it) {
                  return (
                    <div
                      key={idx}
                      className="h-16 rounded-lg border border-dashed border-slate-800 bg-slate-900/30 flex items-center justify-center text-[10px] text-slate-700"
                    >
                      {idx + 1}
                    </div>
                  );
                }

                const rColor = engine.getRarityColor(it.rarity);
                return (
                  <div
                    key={idx}
                    onClick={() => handleDeposit(idx)}
                    title="Clicca per depositare nella cassa"
                    className="h-16 rounded-lg border-2 p-1.5 flex flex-col justify-between cursor-pointer hover:scale-105 bg-slate-900 transition-all"
                    style={{ borderColor: rColor }}
                  >
                    <div className="text-[10px] font-bold truncate" style={{ color: rColor }}>
                      {it.name}
                    </div>
                    <div className="flex items-center justify-between text-[9px] text-slate-400">
                      <span className="uppercase">{it.slot === 'weapon' ? 'Arma' : it.slot === 'armor' ? 'Corazza' : 'Reliquia'}</span>
                      {it.upgradeLevel > 0 && <span className="text-amber-400 font-bold">+{it.upgradeLevel}</span>}
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
