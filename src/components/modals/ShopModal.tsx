import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { X, Sparkles, FlaskConical, Waves, Gift } from 'lucide-react';

interface ShopModalProps {
  engine: GameEngine;
  onClose: () => void;
  onRefresh: () => void;
}

export const ShopModal: React.FC<ShopModalProps> = ({
  engine,
  onClose,
  onRefresh,
}) => {
  const handleBuyPotion = () => {
    if (engine.buyPotionFromMerchant()) onRefresh();
  };

  const handleBuyBlessing = () => {
    if (engine.buyTideBlessing()) onRefresh();
  };

  const handleBuyCoffer = () => {
    if (engine.buyMysteryCoffer()) onRefresh();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-3 sm:p-4 select-none">
      <div className="relative w-full max-w-2xl bg-slate-900 border-2 border-sky-400/80 rounded-2xl shadow-2xl p-5 text-slate-100">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3 mb-4">
          <div className="flex items-center gap-3">
            <img
              src="/sprites/paguro_bottegaio.png"
              alt="Mastro Paguro"
              className="w-12 h-12 rounded-lg border-2 border-sky-400 object-cover bg-slate-950"
            />
            <div>
              <h2 className="font-cinzel text-xl font-black text-sky-300">
                BOTTEGA DI MASTRO PAGURO
              </h2>
              <div className="text-xs text-slate-400">
                Alchimia Marina, Pozioni e Manufatti Rari
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

        {/* Currency status banner */}
        <div className="bg-slate-950/70 border border-slate-800 rounded-xl px-4 py-2 mb-4 flex items-center justify-between text-xs">
          <span className="text-slate-400">Tuo Bottino:</span>
          <span className="font-bold text-amber-400 text-sm">🐚 {engine.shells} Conchiglie</span>
        </div>

        {/* Goods Grid */}
        <div className="space-y-3">
          {/* Item 1: Potion */}
          <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-emerald-950/60 border border-emerald-500/40 flex items-center justify-center text-xl">
                🧪
              </div>
              <div>
                <h4 className="font-cinzel font-bold text-sm text-emerald-300">
                  Fiala di Linfa Marina
                </h4>
                <p className="text-xs text-slate-400">
                  Ripristina il 50% dei tuoi Punti Vita massimi durante l'immersione.
                </p>
                <div className="text-[10px] text-slate-500 mt-0.5">
                  In possesso: {engine.potions} / {engine.maxPotions}
                </div>
              </div>
            </div>
            <button
              onClick={handleBuyPotion}
              disabled={engine.shells < 15 || engine.potions >= engine.maxPotions}
              className="px-3.5 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 font-cinzel font-bold text-xs text-white disabled:opacity-40 transition-colors whitespace-nowrap"
            >
              15 🐚
            </button>
          </div>

          {/* Item 2: Tide Blessing */}
          <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-sky-950/60 border border-sky-500/40 flex items-center justify-center text-xl">
                🌊
              </div>
              <div>
                <h4 className="font-cinzel font-bold text-sm text-sky-300">
                  Benedizione della Marea
                </h4>
                <p className="text-xs text-slate-400">
                  Ricarica istantaneamente l'energia della Marea al 100% e cura +30 PV.
                </p>
              </div>
            </div>
            <button
              onClick={handleBuyBlessing}
              disabled={engine.shells < 25}
              className="px-3.5 py-1.5 rounded-lg bg-sky-600 hover:bg-sky-500 font-cinzel font-bold text-xs text-white disabled:opacity-40 transition-colors whitespace-nowrap"
            >
              25 🐚
            </button>
          </div>

          {/* Item 3: Mystery Coffer */}
          <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-amber-950/60 border border-amber-500/40 flex items-center justify-center text-xl">
                🎁
              </div>
              <div>
                <h4 className="font-cinzel font-bold text-sm text-amber-300">
                  Forziere Misterioso Sigillato
                </h4>
                <p className="text-xs text-slate-400">
                  Contiene un equipaggiamento casuale garantito di grado Raro o Abissale!
                </p>
                <div className="text-[10px] text-slate-500 mt-0.5">
                  Spazio zaino: {engine.backpack.length} / 8
                </div>
              </div>
            </div>
            <button
              onClick={handleBuyCoffer}
              disabled={engine.shells < 55 || engine.backpack.length >= 8}
              className="px-3.5 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-500 font-cinzel font-bold text-xs text-slate-950 disabled:opacity-40 transition-colors whitespace-nowrap"
            >
              55 🐚
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
