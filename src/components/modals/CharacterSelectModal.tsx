import React from 'react';
import { GameEngine } from '../../game/GameEngine';
import { CLASSES_DATA, SUIT_COLORS } from '../../types/game';
import { Plus, Trash2, Play, Shield, Crosshair, Sparkles } from 'lucide-react';
import { AudioHelper } from '../../audio/AudioHelper';

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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-4 select-none">
      <div className="relative w-full max-w-3xl bg-slate-900 border-2 border-sky-400 rounded-2xl shadow-2xl p-6 overflow-hidden">
        {/* Header */}
        <div className="text-center mb-6">
          <h1 className="font-cinzel text-3xl font-black text-sky-300 tracking-wider">
            CARTA D'ABISSO
          </h1>
          <p className="text-sm text-slate-400 font-medium">
            Archivio Palombari · Seleziona o crea il tuo eroe per immergerti
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
                  className="flex flex-col items-center justify-center p-6 min-h-[220px] rounded-xl border-2 border-dashed border-slate-700 hover:border-sky-400 bg-slate-950/40 hover:bg-slate-800/40 cursor-pointer transition-all group"
                >
                  <div className="w-14 h-14 rounded-full border border-slate-600 group-hover:border-sky-400 flex items-center justify-center bg-slate-900 text-slate-500 group-hover:text-sky-300 transition-colors mb-3">
                    <Plus className="w-8 h-8" />
                  </div>
                  <span className="font-cinzel text-sm font-bold text-slate-400 group-hover:text-sky-200">
                    ＋ NUOVO EROE
                  </span>
                  <span className="text-xs text-slate-600 mt-1">Slot {index + 1} Libero</span>
                </div>
              );
            }

            return (
              <div
                key={index}
                onClick={() => onSelectSlot(index)}
                className="relative flex flex-col justify-between p-4 min-h-[220px] rounded-xl border-2 border-sky-500/70 hover:border-sky-300 bg-gradient-to-b from-slate-800/80 to-slate-950 shadow-lg cursor-pointer transition-all hover:scale-[1.02] group"
              >
                {/* Delete button */}
                <button
                  onClick={(e) => handleDelete(index, e)}
                  title="Elimina Personaggio"
                  className="absolute top-3 right-3 p-1.5 rounded-lg text-slate-500 hover:text-red-400 hover:bg-red-950/50 transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>

                {/* Top info */}
                <div>
                  <div className="flex items-center gap-3 mb-3">
                    <div
                      className="w-12 h-12 rounded-lg border-2 p-0.5 object-cover bg-slate-900 flex items-center justify-center"
                      style={{ borderColor: suitCol.hex }}
                    >
                      <img
                        src={clz.portrait}
                        alt={clz.name}
                        className="w-full h-full rounded object-cover"
                      />
                    </div>
                    <div>
                      <h3 className="font-cinzel font-bold text-base text-sky-100 group-hover:text-sky-300">
                        {slot.name}
                      </h3>
                      <div className="text-xs font-semibold text-amber-400">
                        Livello {slot.level}
                      </div>
                    </div>
                  </div>

                  <div className="space-y-1.5 text-xs text-slate-300">
                    <div className="flex items-center justify-between py-0.5 border-b border-slate-800">
                      <span className="text-slate-400">Classe</span>
                      <span className="font-bold text-sky-300">{clz.name}</span>
                    </div>
                    <div className="flex items-center justify-between py-0.5 border-b border-slate-800">
                      <span className="text-slate-400">Muta</span>
                      <span className="font-bold" style={{ color: suitCol.border }}>
                        {suitCol.name}
                      </span>
                    </div>
                    <div className="flex items-center justify-between py-0.5 border-b border-slate-800">
                      <span className="text-slate-400">Progressione</span>
                      <span className="font-bold text-amber-300">Atto {slot.maxActUnlocked || 1}</span>
                    </div>
                    <div className="flex items-center justify-between py-0.5">
                      <span className="text-slate-400">Conchiglie</span>
                      <span className="font-bold text-amber-400">🐚 {slot.shells}</span>
                    </div>
                  </div>
                </div>

                {/* Play button */}
                <button
                  onClick={() => onSelectSlot(index)}
                  className="w-full mt-4 py-2 rounded-lg bg-gradient-to-r from-sky-600 to-blue-600 hover:from-sky-500 hover:to-blue-500 font-cinzel font-bold text-xs text-white shadow-md flex items-center justify-center gap-1.5 transition-all"
                >
                  <Play className="w-3.5 h-3.5 fill-current" />
                  GIOCA
                </button>
              </div>
            );
          })}
        </div>

        {/* Footer info */}
        <div className="text-center text-xs text-slate-500">
          Carta d’Abisso v0.6.0 · 3 Slot Salvataggio Isolati · ARPG Subacqueo
        </div>
      </div>
    </div>
  );
};
