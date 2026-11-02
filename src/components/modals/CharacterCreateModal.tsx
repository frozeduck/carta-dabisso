import React, { useState } from 'react';
import { GameEngine } from '../../game/GameEngine';
import { PlayerClassType, CLASSES_DATA, SUIT_COLORS } from '../../types/game';
import { RANDOM_NAMES } from '../../game/skillsData';
import { Dices, ArrowLeft, Check, Sparkles } from 'lucide-react';

interface CharacterCreateModalProps {
  engine: GameEngine;
  slotIndex: number;
  onCreated: () => void;
  onCancel: () => void;
}

export const CharacterCreateModal: React.FC<CharacterCreateModalProps> = ({
  engine,
  slotIndex,
  onCreated,
  onCancel,
}) => {
  const [name, setName] = useState<string>(
    RANDOM_NAMES[Math.floor(Math.random() * RANDOM_NAMES.length)]
  );
  const [selectedClass, setSelectedClass] = useState<PlayerClassType>('hunter');
  const [suitColorIndex, setSuitColorIndex] = useState<number>(0);

  const rollRandomName = () => {
    const r = RANDOM_NAMES[Math.floor(Math.random() * RANDOM_NAMES.length)];
    setName(r);
  };

  const handleCreate = () => {
    engine.createCharacterInSlot(slotIndex, name, selectedClass, suitColorIndex);
    onCreated();
  };

  const activeClz = CLASSES_DATA[selectedClass];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-4 select-none">
      <div className="relative w-full max-w-4xl max-h-[95vh] overflow-y-auto bg-slate-900 border-2 border-sky-400 rounded-2xl shadow-2xl p-6">
        {/* Header */}
        <div className="flex items-center justify-between mb-4 border-b border-slate-800 pb-3">
          <button
            onClick={onCancel}
            className="flex items-center gap-1.5 text-xs font-semibold text-slate-400 hover:text-sky-300 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Torna all'Archivio
          </button>
          <h2 className="font-cinzel text-xl sm:text-2xl font-black text-sky-200">
            CREAZIONE PALOMBARO · SLOT {slotIndex + 1}
          </h2>
          <div className="w-16" />
        </div>

        {/* Name Input & Nautical Dice */}
        <div className="mb-6 bg-slate-950/60 p-4 rounded-xl border border-slate-800 flex flex-col sm:flex-row items-center gap-3">
          <span className="text-xs font-cinzel font-bold text-sky-300 whitespace-nowrap">
            Nome Eroe:
          </span>
          <div className="relative w-full flex items-center">
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              maxLength={24}
              placeholder="Inserisci il nome del palombaro..."
              className="w-full px-3 py-2 rounded-lg bg-slate-900 border border-slate-700 text-sm text-sky-100 focus:outline-none focus:border-sky-400 font-cinzel font-semibold"
            />
            <button
              onClick={rollRandomName}
              title="Genera Nome Casuale Marinaresco"
              className="absolute right-1 p-1.5 rounded-md bg-slate-800 hover:bg-sky-600 text-sky-300 hover:text-white transition-all"
            >
              <Dices className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* 3 Classes Selection */}
        <div className="mb-6">
          <div className="text-xs font-cinzel font-bold text-slate-400 mb-2">
            1. SELEZIONA LA TUA CLASSE PERMANENTE:
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            {(Object.keys(CLASSES_DATA) as PlayerClassType[]).map((cKey) => {
              const clz = CLASSES_DATA[cKey];
              const isSelected = selectedClass === cKey;

              return (
                <div
                  key={cKey}
                  onClick={() => setSelectedClass(cKey)}
                  className={`relative p-3.5 rounded-xl border-2 cursor-pointer transition-all ${
                    isSelected
                      ? 'border-sky-400 bg-sky-950/40 shadow-[0_0_15px_rgba(56,189,248,0.3)] scale-[1.01]'
                      : 'border-slate-800 bg-slate-950/40 hover:border-slate-700'
                  }`}
                >
                  {isSelected && (
                    <div className="absolute top-2 right-2 bg-sky-500 text-slate-950 rounded-full p-0.5">
                      <Check className="w-3.5 h-3.5 stroke-[3]" />
                    </div>
                  )}
                  <div className="flex items-center gap-3 mb-2.5">
                    <img
                      src={clz.portrait}
                      alt={clz.name}
                      className="w-12 h-12 rounded-lg border border-sky-400/50 object-cover bg-slate-900"
                    />
                    <div>
                      <h4 className="font-cinzel font-bold text-sm text-sky-200">
                        {clz.name}
                      </h4>
                      <p className="text-[10px] text-amber-400 font-medium">
                        {clz.subtitle}
                      </p>
                    </div>
                  </div>
                  <p className="text-xs text-slate-300 mb-3 leading-relaxed">
                    {clz.description}
                  </p>
                  <div className="text-[10px] text-sky-300/80 bg-slate-900/80 p-2 rounded border border-slate-800">
                    <span className="font-bold text-slate-400">Attacco: </span>
                    {clz.attackStyle}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Suit Color Customization */}
        <div className="mb-6">
          <div className="text-xs font-cinzel font-bold text-slate-400 mb-2">
            2. PERSONALIZZAZIONE MUTA SUBACQUEA:
          </div>
          <div className="flex flex-wrap items-center gap-3">
            {SUIT_COLORS.map((col, idx) => {
              const isSelected = suitColorIndex === idx;
              return (
                <button
                  key={idx}
                  onClick={() => setSuitColorIndex(idx)}
                  className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border-2 transition-all ${
                    isSelected
                      ? 'border-white bg-slate-800 shadow-md scale-105'
                      : 'border-slate-800 hover:border-slate-600 bg-slate-950/40'
                  }`}
                >
                  <div
                    className="w-4 h-4 rounded-full border border-white/50"
                    style={{ backgroundColor: col.hex }}
                  />
                  <span className="text-xs font-semibold text-slate-200">{col.name}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Selected Hero Preview Summary & Confirm */}
        <div className="bg-gradient-to-r from-sky-950/60 to-slate-950 p-4 rounded-xl border border-sky-500/40 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div
              className="w-12 h-12 rounded-xl border-2 p-0.5 flex items-center justify-center bg-slate-900"
              style={{ borderColor: SUIT_COLORS[suitColorIndex].hex }}
            >
              <img
                src={activeClz.portrait}
                alt="Selected"
                className="w-full h-full rounded object-cover"
              />
            </div>
            <div>
              <div className="font-cinzel font-bold text-sm text-sky-200">
                {name || 'Palombaro'}
              </div>
              <div className="text-xs text-slate-400">
                {activeClz.name} · Muta {SUIT_COLORS[suitColorIndex].name}
              </div>
            </div>
          </div>

          <button
            onClick={handleCreate}
            className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-gradient-to-r from-sky-500 to-blue-600 hover:from-sky-400 hover:to-blue-500 font-cinzel font-bold text-sm text-slate-950 shadow-[0_0_20px_rgba(56,189,248,0.5)] transition-all flex items-center justify-center gap-2 active:scale-95"
          >
            <Sparkles className="w-4 h-4 text-slate-950 fill-current" />
            CREA E INIZIA AVVENTURA
          </button>
        </div>
      </div>
    </div>
  );
};
