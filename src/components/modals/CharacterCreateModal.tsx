import React, { useState } from 'react';
import { GameEngine } from '../../game/GameEngine';
import { PlayerClassType, CLASSES_DATA, SUIT_COLORS } from '../../types/game';
import { RANDOM_NAMES } from '../../game/skillsData';
import { Dices, ArrowLeft, Check, Sparkles, Compass } from 'lucide-react';

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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-sm p-4 select-none animate-in fade-in duration-200">
      <div className="parchment-window w-full max-w-4xl max-h-[95vh] overflow-y-auto rounded-2xl p-6 text-[#2b1c0c] shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between mb-4 border-b-2 border-[#8c6239]/40 pb-3">
          <button
            onClick={onCancel}
            className="flex items-center gap-1.5 text-xs font-semibold text-[#694625] hover:text-[#2b1706] transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Torna all'Archivio
          </button>
          <h2 className="font-cinzel text-lg sm:text-xl font-black text-[#381e09] tracking-wide flex items-center gap-2">
            <span>REGISTRAZIONE PALOMBARO · VANO {slotIndex + 1}</span>
            <Compass className="w-4 h-4 text-[#854d0e]" />
          </h2>
          <div className="w-16" />
        </div>

        {/* Name Input & Nautical Dice */}
        <div className="mb-5 parchment-card p-3.5 rounded-xl border border-[#bfa57b] flex flex-col sm:flex-row items-center gap-3">
          <span className="text-xs font-cinzel font-bold text-[#5c3a1e] whitespace-nowrap">
            Nome Palombaro:
          </span>
          <div className="relative w-full flex items-center">
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              maxLength={24}
              placeholder="Inserisci il nome del palombaro..."
              className="w-full px-3 py-2 rounded-lg bg-[#fffdf5] border-2 border-[#a47e4e] text-sm text-[#2b1706] focus:outline-none focus:border-[#7a4e21] font-cinzel font-semibold shadow-inner"
            />
            <button
              onClick={rollRandomName}
              title="Genera Nome Casuale Marinaresco"
              className="absolute right-1 p-1.5 rounded-md parchment-button-neutral text-[#4a2e12] hover:text-[#2b1706]"
            >
              <Dices className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* 3 Classes Selection */}
        <div className="mb-5">
          <div className="text-xs font-cinzel font-bold text-[#5c3a1e] mb-2 tracking-wide uppercase">
            1. SELEZIONA LA TUA CLASSE SUBACQUEA:
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
                      ? 'parchment-slot-active scale-[1.01] shadow-md'
                      : 'parchment-card hover:border-[#854d0e]'
                  }`}
                >
                  {isSelected && (
                    <div className="absolute top-2 right-2 bg-emerald-700 text-white rounded-full p-0.5 shadow">
                      <Check className="w-3.5 h-3.5 stroke-[3]" />
                    </div>
                  )}
                  <div className="flex items-center gap-3 mb-2.5">
                    <img
                      src={clz.portrait}
                      alt={clz.name}
                      className="w-12 h-12 rounded-lg border-2 border-[#854d0e] object-cover bg-[#ebd9b4] shadow-sm"
                    />
                    <div>
                      <h4 className="font-cinzel font-bold text-sm text-[#2b1706]">
                        {clz.name}
                      </h4>
                      <p className="text-[10px] text-[#854d0e] font-semibold">
                        {clz.subtitle}
                      </p>
                    </div>
                  </div>
                  <p className="text-xs text-[#523820] mb-3 leading-relaxed font-serif italic">
                    "{clz.description}"
                  </p>
                  <div className="text-[10px] text-[#4a2e12] bg-[#eeddbb]/70 p-2 rounded border border-[#cbb28b]">
                    <span className="font-bold text-[#854d0e]">Stile: </span>
                    {clz.attackStyle}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Suit Color Customization */}
        <div className="mb-5">
          <div className="text-xs font-cinzel font-bold text-[#5c3a1e] mb-2 tracking-wide uppercase">
            2. PERSONALIZZAZIONE MUTA SUBACQUEA:
          </div>
          <div className="flex flex-wrap items-center gap-2.5">
            {SUIT_COLORS.map((col, idx) => {
              const isSelected = suitColorIndex === idx;
              return (
                <button
                  key={idx}
                  onClick={() => setSuitColorIndex(idx)}
                  className={`flex items-center gap-2 px-3 py-1.5 rounded-lg border-2 transition-all ${
                    isSelected
                      ? 'parchment-slot-active scale-105 shadow-sm'
                      : 'parchment-card hover:border-[#854d0e]'
                  }`}
                >
                  <div
                    className="w-4 h-4 rounded-full border border-black/30 shadow-inner"
                    style={{ backgroundColor: col.hex }}
                  />
                  <span className="text-xs font-semibold text-[#3b240f]">{col.name}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Selected Hero Preview Summary & Confirm */}
        <div className="parchment-card p-4 rounded-xl border-2 border-[#b59868] flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div
              className="w-12 h-12 rounded-xl border-2 p-0.5 flex items-center justify-center bg-[#ebd9b4] shadow-inner"
              style={{ borderColor: SUIT_COLORS[suitColorIndex].hex }}
            >
              <img
                src={activeClz.portrait}
                alt="Selected"
                className="w-full h-full rounded object-cover"
              />
            </div>
            <div>
              <div className="font-cinzel font-bold text-sm text-[#2b1706]">
                {name || 'Palombaro'}
              </div>
              <div className="text-xs text-[#694625]">
                {activeClz.name} · Muta {SUIT_COLORS[suitColorIndex].name}
              </div>
            </div>
          </div>

          <button
            onClick={handleCreate}
            className="w-full sm:w-auto px-6 py-2.5 rounded-xl parchment-button-green font-cinzel font-bold text-xs flex items-center justify-center gap-2 shadow-md"
          >
            <Sparkles className="w-4 h-4 text-white" />
            ISCRIVI A RUOLO & IMMERGITI
          </button>
        </div>
      </div>
    </div>
  );
};
