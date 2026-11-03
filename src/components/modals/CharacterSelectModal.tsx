import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { CLASSES_DATA, SUIT_COLORS } from '../../types/game';
import { Plus, Trash2, Play, Compass } from 'lucide-react';

interface CharacterSelectModalProps {
  engine: GameEngine;
  onSelectSlot: (index: number) => void;
  onCreateNewSlot: (index: number) => void;
  onRefresh: () => void;
}

export const CharacterSelectModal: React.FC<CharacterSelectModalProps> = ({
  engine,
  onSelectSlot,
  onCreateNewSlot,
  onRefresh,
}) => {
  const handleDelete = (index: number, e: React.MouseEvent) => {
    e.stopPropagation();
    if (confirm('Sei sicuro di voler eliminare questo palombaro? I dati andranno perduti.')) {
      engine.deleteCharacter(index);
      onRefresh();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-sm p-4 select-none animate-in fade-in duration-200">
      <div className="parchment-window w-full max-w-3xl rounded-2xl p-6 overflow-hidden shadow-2xl">
        {/* Header */}
        <div className="text-center mb-6 border-b-2 border-[#8c6239]/40 pb-4">
          <div className="flex items-center justify-center gap-2 mb-1">
            <Compass className="w-6 h-6 text-[#854d0e]" />
            <h1 className="font-cinzel text-2xl sm:text-3xl font-black text-[#381e09] tracking-wider">
              CARTA D'ABISSO
            </h1>
          </div>
          <p className="text-xs text-[#694625] font-serif italic">
            Registro di Bordo dei Palombari · Seleziona o crea il tuo eroe per calarti negli abissi
          </p>
        </div>

        {/* 3 Character Slots */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          {engine.slots.map((slot, index) => {
            const suitCol = SUIT_COLORS[slot.suitColorIndex] || SUIT_COLORS[0];
            const clz = CLASSES_DATA[slot.playerClass];

            if (!slot.exists) {
              return (
                <div
                  key={index}
                  onClick={() => onCreateNewSlot(index)}
                  className="flex flex-col items-center justify-center p-6 min-h-[220px] rounded-xl border-2 border-dashed border-[#a48154] bg-[#ebdcb7]/50 hover:bg-[#fff9ea] hover:border-[#7a4e21] cursor-pointer transition-all group shadow-inner"
                >
                  <div className="w-14 h-14 rounded-full border-2 border-[#8c6032] group-hover:border-[#58310c] flex items-center justify-center bg-[#fdf6e2] text-[#854d0e] transition-colors mb-3 shadow">
                    <Plus className="w-7 h-7" />
                  </div>
                  <span className="font-cinzel text-xs font-bold text-[#5c3a1e] group-hover:text-[#2b1706]">
                    ＋ NUOVO PALOMBARO
                  </span>
                  <span className="text-[10px] text-[#7d562c] mt-1 font-serif">Vano {index + 1} Disponibile</span>
                </div>
              );
            }

            return (
              <div
                key={index}
                onClick={() => onSelectSlot(index)}
                className="relative flex flex-col justify-between p-4 min-h-[220px] rounded-xl border-2 border-[#9c7546] hover:border-[#69390f] parchment-card shadow-md cursor-pointer transition-all hover:scale-[1.02] group"
              >
                {/* Delete button */}
                <button
                  onClick={(e) => handleDelete(index, e)}
                  title="Elimina Personaggio"
                  className="absolute top-2.5 right-2.5 p-1 rounded-md text-[#8c6b4d] hover:text-[#991b1b] hover:bg-[#fee2e2] transition-colors"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>

                {/* Top info */}
                <div>
                  <div className="flex items-center gap-3 mb-3">
                    <div
                      className="w-12 h-12 rounded-lg border-2 p-0.5 object-cover bg-[#ebd9b4] flex items-center justify-center shadow-inner"
                      style={{ borderColor: suitCol.hex }}
                    >
                      <img
                        src={clz.portrait}
                        alt={clz.name}
                        className="w-full h-full rounded object-cover"
                      />
                    </div>
                    <div>
                      <h3 className="font-cinzel font-bold text-sm text-[#2b1706] group-hover:text-[#854d0e]">
                        {slot.name}
                      </h3>
                      <div className="text-xs font-semibold text-amber-900">
                        Grado Lv.{slot.level}
                      </div>
                    </div>
                  </div>

                  <div className="space-y-1.5 text-xs text-[#4a2e12]">
                    <div className="flex items-center justify-between py-0.5 border-b border-[#dfcaa7]/60">
                      <span className="text-[#694625]">Classe</span>
                      <strong className="text-[#0369a1]">{clz.name}</strong>
                    </div>
                    <div className="flex items-center justify-between py-0.5 border-b border-[#dfcaa7]/60">
                      <span className="text-[#694625]">Muta</span>
                      <strong style={{ color: suitCol.border }}>
                        {suitCol.name}
                      </strong>
                    </div>
                    <div className="flex items-center justify-between py-0.5 border-b border-[#dfcaa7]/60">
                      <span className="text-[#694625]">Progressione</span>
                      <strong className="text-[#854d0e]">Atto {slot.maxActUnlocked || 1}</strong>
                    </div>
                    <div className="flex items-center justify-between py-0.5">
                      <span className="text-[#694625]">Conchiglie</span>
                      <strong className="text-[#b45309]">🐚 {slot.shells}</strong>
                    </div>
                  </div>
                </div>

                {/* Play button */}
                <button
                  onClick={() => onSelectSlot(index)}
                  className="w-full mt-4 py-2 rounded-lg parchment-button-green font-cinzel font-bold text-xs flex items-center justify-center gap-1.5 shadow"
                >
                  <Play className="w-3.5 h-3.5 fill-current" />
                  CALATI NEGLI ABISSI
                </button>
              </div>
            );
          })}
        </div>

        {/* Footer info */}
        <div className="text-center text-[11px] text-[#785226] font-serif italic border-t border-[#cbb28b] pt-3">
          Carta d'Abisso · 3 Slot Salvataggio Isolati · ARPG Nautico in Stile Carta Antica
        </div>
      </div>
    </div>
  );
};
