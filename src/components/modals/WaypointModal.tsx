import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { X, Navigation, Compass, Sparkles, Anchor, MapPin } from 'lucide-react';

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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-2 sm:p-4 select-none animate-in fade-in duration-200">
      <div className="parchment-window w-full max-w-3xl max-h-[92vh] overflow-y-auto rounded-2xl p-4 sm:p-6 text-[#2b1c0c] shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b-2 border-[#8c6239]/40 pb-3 mb-4">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-lg bg-[#e8d7b7] border-2 border-[#7a4e21] p-1 flex items-center justify-center shadow-inner">
              <img
                src="/sprites/portale_citta.png"
                alt="Portale di Spedizione"
                className="w-full h-full object-contain filter drop-shadow"
              />
            </div>
            <div>
              <h2 className="font-cinzel text-lg sm:text-xl font-black text-[#381e09] tracking-wide flex items-center gap-2">
                <span>CARTA NAUTICA DI SPEDIZIONE</span>
                <Compass className="w-4 h-4 text-[#854d0e]" />
              </h2>
              <div className="text-[11px] text-[#694625] italic font-serif">
                Seleziona la rotta cartografica per calarti negli abissi profondi.
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

        {/* Acts Grid styled as antique maritime chart sections */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
          {engine.acts.map((act) => {
            const isUnlocked = act.number <= engine.maxActUnlocked;

            return (
              <div
                key={act.number}
                className={`p-4 rounded-xl border-2 flex flex-col justify-between transition-all ${
                  isUnlocked
                    ? 'parchment-card border-[#9c7546] hover:border-[#69390f] shadow-md'
                    : 'bg-[#ebdcb9]/40 border-dashed border-[#b89d75] opacity-55'
                }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-1.5 pb-1 border-b border-[#cbb28b]">
                    <span className="text-[10px] font-bold text-[#854d0e] uppercase tracking-wider flex items-center gap-1">
                      <Anchor className="w-3 h-3 text-[#69390f]" />
                      Profondità -{act.startDepth}m
                    </span>
                    {act.completed ? (
                      <span className="text-[9px] font-black text-emerald-800 bg-emerald-100/90 px-2 py-0.5 rounded border border-emerald-400 uppercase">
                        ✓ ATTO COMPLETATO
                      </span>
                    ) : isUnlocked ? (
                      <span className="text-[9px] font-bold text-amber-900 bg-amber-100/90 px-2 py-0.5 rounded border border-amber-300">
                        DISPONIBILE
                      </span>
                    ) : (
                      <span className="text-[9px] font-bold text-stone-600 bg-stone-200/90 px-2 py-0.5 rounded border border-stone-300">
                        SIGILLATO
                      </span>
                    )}
                  </div>

                  <h3 className="font-cinzel font-bold text-sm text-[#2e1705] mb-0.5">
                    {act.title}
                  </h3>
                  <div className="text-xs text-[#0369a1] font-semibold mb-2 flex items-center gap-1">
                    <MapPin className="w-3 h-3" />
                    {act.subtitle}
                  </div>
                  <p className="text-xs text-[#523820] leading-relaxed mb-3 font-serif italic">
                    "{act.questDesc}"
                  </p>
                  <div className="text-[11px] text-[#633f1b] bg-[#f2e5cb] px-2 py-1 rounded border border-[#cbaf83]">
                    Leviatano: <strong className="text-rose-900 font-cinzel">{act.bossName}</strong>
                  </div>
                </div>

                <button
                  onClick={() => onStartExpedition(act.number)}
                  disabled={!isUnlocked}
                  className={`w-full mt-4 py-2 rounded-lg font-cinzel font-bold text-xs flex items-center justify-center gap-1.5 transition-all shadow ${
                    isUnlocked
                      ? 'parchment-button-green'
                      : 'parchment-button-neutral opacity-50 cursor-not-allowed'
                  }`}
                >
                  <Navigation className="w-3.5 h-3.5" />
                  {isUnlocked ? 'SALPA VERSO QUESTO ATTO' : 'ROTTA BLOCCATA'}
                </button>
              </div>
            );
          })}
        </div>

        {/* Endless Dive (Parchment Seal) */}
        {engine.maxActUnlocked >= 4 && (
          <div className="parchment-panel-inset p-4 rounded-xl border-2 border-[#7e22ce] flex flex-col sm:flex-row items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 text-[#581c87] font-cinzel font-black text-sm">
                <Sparkles className="w-4 h-4 text-[#7e22ce]" />
                ABISSO SENZA FINE · ENDLESS DIVE
              </div>
              <p className="text-xs text-[#4a2e12] mt-1 font-serif italic">
                La mappa si estende oltre i limiti conosciuti. Mostri scalati e tesori abissali infiniti ti attendono.
              </p>
            </div>
            <button
              onClick={() => onStartExpedition(5)}
              className="px-5 py-2.5 rounded-xl bg-purple-700 hover:bg-purple-800 text-amber-100 font-cinzel font-bold text-xs shadow transition-all whitespace-nowrap"
            >
              IMMERSIONE INFINITA
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
