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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-2 sm:p-4 select-none animate-in fade-in duration-200">
      <div className="parchment-window w-full max-w-2xl max-h-[92vh] overflow-y-auto rounded-2xl p-4 sm:p-6 text-[#2b1c0c] shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b-2 border-[#8c6239]/40 pb-3 mb-4">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-lg bg-[#e8d7b7] border-2 border-[#0284c7] p-1 flex items-center justify-center shadow-inner">
              <img
                src="/sprites/paguro_bottegaio.png"
                alt="Mastro Paguro"
                className="w-full h-full object-contain filter drop-shadow"
              />
            </div>
            <div>
              <h2 className="font-cinzel text-lg sm:text-xl font-black text-[#381e09] tracking-wide">
                BOTTEGA DI MASTRO PAGURO
              </h2>
              <div className="text-[11px] text-[#694625] italic font-serif">
                Alchimia Marina, Pozioni e Manufatti Rari dell'Eremita
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

        {/* Currency status banner */}
        <div className="parchment-panel-inset rounded-xl px-4 py-2 mb-4 flex items-center justify-between text-xs">
          <span className="text-[#54391e] font-serif font-semibold">Tuo Bottino di Conchiglie:</span>
          <span className="font-bold text-[#b45309] text-sm">🐚 {engine.shells} Conchiglie</span>
        </div>

        {/* Goods Grid */}
        <div className="space-y-3">
          {/* Item 1: Potion */}
          <div className="p-3.5 rounded-xl parchment-card flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-lg bg-[#dcfce7] border border-[#86efac] flex items-center justify-center text-xl shadow-inner">
                🧪
              </div>
              <div>
                <h4 className="font-cinzel font-bold text-sm text-[#14532d]">
                  Fiala di Linfa Marina
                </h4>
                <p className="text-xs text-[#523820] font-serif italic">
                  Ripristina il 50% dei tuoi Punti Vita massimi durante l'immersione.
                </p>
                <div className="text-[10px] text-[#694625] mt-0.5">
                  In possesso: <strong>{engine.potions} / {engine.maxPotions}</strong>
                </div>
              </div>
            </div>
            <button
              onClick={handleBuyPotion}
              disabled={engine.shells < 15 || engine.potions >= engine.maxPotions}
              className="px-4 py-2 rounded-lg parchment-button-green font-cinzel font-bold text-xs disabled:opacity-40 transition-colors whitespace-nowrap"
            >
              15 🐚
            </button>
          </div>

          {/* Item 2: Tide Blessing */}
          <div className="p-3.5 rounded-xl parchment-card flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-lg bg-[#e0f2fe] border border-[#7dd3fc] flex items-center justify-center text-xl shadow-inner">
                🌊
              </div>
              <div>
                <h4 className="font-cinzel font-bold text-sm text-[#0369a1]">
                  Benedizione della Marea
                </h4>
                <p className="text-xs text-[#523820] font-serif italic">
                  Ricarica istantaneamente l'energia della Marea al 100% e cura +30 PV.
                </p>
              </div>
            </div>
            <button
              onClick={handleBuyBlessing}
              disabled={engine.shells < 25}
              className="px-4 py-2 rounded-lg parchment-button-gold font-cinzel font-bold text-xs disabled:opacity-40 transition-colors whitespace-nowrap"
            >
              25 🐚
            </button>
          </div>

          {/* Item 3: Mystery Coffer */}
          <div className="p-3.5 rounded-xl parchment-card flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-lg bg-[#fef3c7] border border-[#fde047] flex items-center justify-center text-xl shadow-inner">
                🎁
              </div>
              <div>
                <h4 className="font-cinzel font-bold text-sm text-[#854d0e]">
                  Forziere Misterioso Sigillato
                </h4>
                <p className="text-xs text-[#523820] font-serif italic">
                  Contiene un equipaggiamento casuale garantito di grado Raro o Abissale!
                </p>
                <div className="text-[10px] text-[#694625] mt-0.5">
                  Spazio zaino: <strong>{engine.backpack.length} / 8</strong>
                </div>
              </div>
            </div>
            <button
              onClick={handleBuyCoffer}
              disabled={engine.shells < 55 || engine.backpack.length >= 8}
              className="px-4 py-2 rounded-lg parchment-button-gold font-cinzel font-bold text-xs disabled:opacity-40 transition-colors whitespace-nowrap"
            >
              55 🐚
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
