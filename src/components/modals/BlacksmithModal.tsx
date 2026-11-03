import React, { useState } from 'react';
import { GameEngine } from '../../game/GameEngine';
import { Item, ItemSlot } from '../../types/game';
import { X, Hammer, Flame, Trash2 } from 'lucide-react';

interface BlacksmithModalProps {
  engine: GameEngine;
  onClose: () => void;
  onRefresh: () => void;
}

export const BlacksmithModal: React.FC<BlacksmithModalProps> = ({
  engine,
  onClose,
  onRefresh,
}) => {
  const [activeTab, setActiveTab] = useState<'UPGRADE' | 'SALVAGE' | 'FORGE'>('UPGRADE');
  const [selectedEquippedSlot, setSelectedEquippedSlot] = useState<ItemSlot>('weapon');

  const selectedItem: Item | null =
    selectedEquippedSlot === 'weapon'
      ? engine.equippedWeapon
      : selectedEquippedSlot === 'armor'
      ? engine.equippedArmor
      : engine.equippedRelic;

  const handleUpgrade = () => {
    if (selectedItem && engine.canUpgradeItem(selectedItem)) {
      engine.upgradeItem(selectedItem);
      onRefresh();
    }
  };

  const handleSalvage = (index: number) => {
    engine.salvageBackpackItem(index);
    onRefresh();
  };

  const handleForge = (rarity: number) => {
    if (engine.forgeRandomItem(rarity)) {
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
                src="/sprites/fabbro_astice.png"
                alt="Mastro Astice"
                className="w-full h-full object-contain filter drop-shadow"
              />
            </div>
            <div>
              <h2 className="font-cinzel text-lg sm:text-xl font-black text-[#381e09] tracking-wide">
                MASTRO ASTICE · FORGIA DEGLI ABISSI
              </h2>
              <div className="text-[11px] text-[#694625] italic font-serif">
                Potenziamento (+1..+5) · Smantellamento · Forgiatura Antica
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
        <div className="parchment-panel-inset rounded-xl px-4 py-2.5 mb-4 flex items-center justify-between text-xs">
          <span className="text-[#54391e] font-serif font-semibold">Materiali Forgiatura Disponibili:</span>
          <div className="flex items-center gap-4">
            <span className="font-bold text-[#0369a1]">💎 {engine.abyssalShards} Frammenti</span>
            <span className="font-bold text-[#b45309]">🐚 {engine.shells} Conchiglie</span>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="flex gap-2 border-b border-[#cbb28b] mb-4 pb-2">
          <button
            onClick={() => setActiveTab('UPGRADE')}
            className={`px-3.5 py-1.5 rounded-lg font-cinzel text-xs font-bold transition-all flex items-center gap-1.5 ${
              activeTab === 'UPGRADE'
                ? 'parchment-button-gold shadow'
                : 'parchment-button-neutral'
            }`}
          >
            <Hammer className="w-3.5 h-3.5" />
            POTENZIA (+1..+5)
          </button>
          <button
            onClick={() => setActiveTab('SALVAGE')}
            className={`px-3.5 py-1.5 rounded-lg font-cinzel text-xs font-bold transition-all flex items-center gap-1.5 ${
              activeTab === 'SALVAGE'
                ? 'parchment-button-gold shadow'
                : 'parchment-button-neutral'
            }`}
          >
            <Trash2 className="w-3.5 h-3.5" />
            SMANTELLA
          </button>
          <button
            onClick={() => setActiveTab('FORGE')}
            className={`px-3.5 py-1.5 rounded-lg font-cinzel text-xs font-bold transition-all flex items-center gap-1.5 ${
              activeTab === 'FORGE'
                ? 'parchment-button-gold shadow'
                : 'parchment-button-neutral'
            }`}
          >
            <Flame className="w-3.5 h-3.5" />
            FORGIA ANTICA
          </button>
        </div>

        {/* TAB 1: UPGRADE */}
        {activeTab === 'UPGRADE' && (
          <div className="space-y-4">
            <div className="text-xs text-[#694625] font-serif italic">
              Seleziona uno slot equipaggiato da potenziare con le possenti chele di Mastro Astice:
            </div>

            <div className="grid grid-cols-3 gap-3">
              {(['weapon', 'armor', 'relic'] as ItemSlot[]).map((slotKey) => {
                const it =
                  slotKey === 'weapon'
                    ? engine.equippedWeapon
                    : slotKey === 'armor'
                    ? engine.equippedArmor
                    : engine.equippedRelic;
                const isSelected = selectedEquippedSlot === slotKey;

                return (
                  <div
                    key={slotKey}
                    onClick={() => setSelectedEquippedSlot(slotKey)}
                    className={`p-3 rounded-xl border-2 cursor-pointer transition-all ${
                      isSelected
                        ? 'parchment-slot-active shadow-md'
                        : 'parchment-slot'
                    }`}
                  >
                    <div className="text-[10px] font-bold text-[#5c3a1e] uppercase mb-1">
                      {slotKey === 'weapon' ? 'Arma' : slotKey === 'armor' ? 'Corazza' : 'Reliquia'}
                    </div>
                    {it ? (
                      <div>
                        <div className="font-cinzel font-bold text-xs truncate text-[#2f1b0a]">
                          {it.name}
                        </div>
                        <div className="text-[10px] text-amber-900 font-extrabold mt-1">
                          Grado: +{it.upgradeLevel} / 5
                        </div>
                      </div>
                    ) : (
                      <span className="text-xs text-[#8c6b4d] italic">Nessun pezzo</span>
                    )}
                  </div>
                );
              })}
            </div>

            {selectedItem ? (
              <div className="parchment-card rounded-xl p-4 border-2 border-[#b59868]">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="font-cinzel font-bold text-sm text-[#2b1706]">
                    {selectedItem.name} +{selectedItem.upgradeLevel}
                  </h4>
                  <span className="text-xs text-amber-900 font-bold bg-[#eeddbb] px-2 py-0.5 rounded border border-[#bfa57b]">
                    Livello Attuale: +{selectedItem.upgradeLevel} / 5
                  </span>
                </div>

                <div className="space-y-1.5 text-xs text-[#4a2e12] mb-4">
                  {selectedItem.damageBonus > 0 && (
                    <div>Danno Attuale: <strong className="text-rose-800">{selectedItem.damageBonus}</strong> → Prossimo: <strong className="text-emerald-800">+{Math.round(selectedItem.damageBonus * 1.15)}</strong></div>
                  )}
                  {selectedItem.defenseBonus > 0 && (
                    <div>Difesa Attuale: <strong className="text-emerald-800">{selectedItem.defenseBonus}</strong> → Prossimo: <strong className="text-emerald-700">+{Math.round(selectedItem.defenseBonus * 1.15)}</strong></div>
                  )}
                  {selectedItem.healthBonus > 0 && (
                    <div>Salute Attuale: <strong className="text-red-800">{selectedItem.healthBonus}</strong> → Prossimo: <strong className="text-emerald-700">+{Math.round(selectedItem.healthBonus * 1.15)}</strong></div>
                  )}
                </div>

                {selectedItem.upgradeLevel < 5 ? (
                  <div className="flex items-center justify-between pt-3 border-t border-[#cbb28b]">
                    <div className="text-xs text-[#5c3a1e]">
                      Costo: <strong className="text-[#0369a1]">💎 {engine.getUpgradeShardCost(selectedItem)} Frammenti</strong> + <strong className="text-[#b45309]">🐚 {engine.getUpgradeShellCost(selectedItem)} Conchiglie</strong>
                    </div>
                    <button
                      onClick={handleUpgrade}
                      disabled={!engine.canUpgradeItem(selectedItem)}
                      className="px-4 py-2 rounded-lg parchment-button-gold font-cinzel font-bold text-xs disabled:opacity-40"
                    >
                      POTENZIA OGGETTO
                    </button>
                  </div>
                ) : (
                  <div className="text-center text-xs font-bold text-amber-900 py-2 border-t border-[#cbb28b]">
                    Livello Massimo Raggiunto (+5)!
                  </div>
                )}
              </div>
            ) : null}
          </div>
        )}

        {/* TAB 2: SALVAGE */}
        {activeTab === 'SALVAGE' && (
          <div className="space-y-3">
            <div className="text-xs text-[#694625] font-serif italic">
              Smantella gli oggetti presenti nel tuo zaino per ricavare frammenti e conchiglie preziose:
            </div>

            {engine.backpack.length === 0 ? (
              <div className="text-center py-8 text-xs text-[#8c6b4d] italic">
                Lo zaino è vuoto. Trova bottino durante le immersioni per smantellarlo.
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {engine.backpack.map((it, idx) => (
                  <div
                    key={idx}
                    className="p-2.5 rounded-lg parchment-card flex items-center justify-between"
                  >
                    <div>
                      <div className="font-cinzel font-bold text-xs text-[#2b1706]">
                        {it.name} {it.upgradeLevel > 0 && `+${it.upgradeLevel}`}
                      </div>
                      <div className="text-[10px] text-[#694625]">
                        {it.slot === 'weapon' ? 'Arma' : it.slot === 'armor' ? 'Corazza' : 'Reliquia'}
                      </div>
                    </div>
                    <button
                      onClick={() => handleSalvage(idx)}
                      className="px-2.5 py-1 rounded parchment-button-danger text-xs font-bold"
                    >
                      Smantella
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* TAB 3: FORGE */}
        {activeTab === 'FORGE' && (
          <div className="space-y-4">
            <div className="text-xs text-[#694625] font-serif italic">
              Forgia manufatti leggendari dell'abisso incanalando materiali rari:
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Rare Forge */}
              <div className="p-4 rounded-xl parchment-card border-2 border-amber-600/60 flex flex-col justify-between">
                <div>
                  <h4 className="font-cinzel font-bold text-sm text-amber-900 mb-1">
                    Forgia Oggetto Raro
                  </h4>
                  <p className="text-xs text-[#5c3a1e] mb-3 font-serif italic">
                    Crea un'arma, corazza o reliquia di grado Raro con statistiche superiori.
                  </p>
                  <div className="text-xs text-[#381e09] space-y-1 mb-4">
                    <div>Costo: <strong className="text-[#0369a1]">💎 8 Frammenti</strong></div>
                    <div>Conchiglie: <strong className="text-[#b45309]">🐚 40 Conchiglie</strong></div>
                  </div>
                </div>
                <button
                  onClick={() => handleForge(2)}
                  disabled={engine.abyssalShards < 8 || engine.shells < 40 || engine.backpack.length >= 8}
                  className="w-full py-2 rounded-lg parchment-button-gold font-cinzel font-bold text-xs disabled:opacity-40"
                >
                  FORGIA RARO
                </button>
              </div>

              {/* Abyssal Forge */}
              <div className="p-4 rounded-xl parchment-card border-2 border-purple-600/60 flex flex-col justify-between">
                <div>
                  <h4 className="font-cinzel font-bold text-sm text-purple-900 mb-1">
                    Forgia Oggetto Abissale
                  </h4>
                  <p className="text-xs text-[#5c3a1e] mb-3 font-serif italic">
                    Forgiatura maestosa delle faglie profonde. Grado Abissale con affiliazioni di Set.
                  </p>
                  <div className="text-xs text-[#381e09] space-y-1 mb-4">
                    <div>Costo: <strong className="text-[#0369a1]">💎 20 Frammenti</strong></div>
                    <div>Conchiglie: <strong className="text-[#b45309]">🐚 100 Conchiglie</strong></div>
                  </div>
                </div>
                <button
                  onClick={() => handleForge(3)}
                  disabled={engine.abyssalShards < 20 || engine.shells < 100 || engine.backpack.length >= 8}
                  className="w-full py-2 rounded-lg bg-purple-700 hover:bg-purple-800 text-amber-100 font-cinzel font-bold text-xs disabled:opacity-40 shadow transition-colors"
                >
                  FORGIA ABISSALE
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
