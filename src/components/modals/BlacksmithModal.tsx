import React, { useState } from 'react';
import { GameEngine } from '../../game/GameEngine';
import { Item, ItemSlot } from '../../types/game';
import { X, Anvil, Hammer, Sparkles, Flame, Trash2 } from 'lucide-react';

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
    if (confirm('Vuoi smantellare questo oggetto? Verrà distrutto in cambio di frammenti e conchiglie.')) {
      engine.salvageBackpackItem(index);
      onRefresh();
    }
  };

  const handleForge = (rarity: number) => {
    if (engine.forgeRandomItem(rarity)) {
      onRefresh();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-3 sm:p-4 select-none">
      <div className="relative w-full max-w-3xl max-h-[92vh] overflow-y-auto bg-slate-900 border-2 border-orange-500/80 rounded-2xl shadow-2xl p-5 text-slate-100">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3 mb-4">
          <div className="flex items-center gap-3">
            <img
              src="/sprites/fabbro_astice.png"
              alt="Mastro Astice"
              className="w-12 h-12 rounded-lg border-2 border-orange-500 object-cover bg-slate-950"
            />
            <div>
              <h2 className="font-cinzel text-xl font-black text-orange-300">
                MASTRO ASTICE · FORGIA DEGLI ABISSI
              </h2>
              <div className="text-xs text-slate-400">
                Potenziamento (+1..+5) · Smantellamento · Forgiatura Antica
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
          <span className="text-slate-400">Materiali Disponibili:</span>
          <div className="flex items-center gap-4">
            <span className="font-bold text-sky-300">💎 {engine.abyssalShards} Frammenti</span>
            <span className="font-bold text-amber-400">🐚 {engine.shells} Conchiglie</span>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="flex gap-2 border-b border-slate-800 mb-4 pb-2">
          <button
            onClick={() => setActiveTab('UPGRADE')}
            className={`px-3 py-1.5 rounded-lg font-cinzel text-xs font-bold transition-all flex items-center gap-1.5 ${
              activeTab === 'UPGRADE'
                ? 'bg-orange-600 text-white shadow'
                : 'bg-slate-800 text-slate-400 hover:text-white'
            }`}
          >
            <Hammer className="w-3.5 h-3.5" />
            POTENZIA (+1..+5)
          </button>
          <button
            onClick={() => setActiveTab('SALVAGE')}
            className={`px-3 py-1.5 rounded-lg font-cinzel text-xs font-bold transition-all flex items-center gap-1.5 ${
              activeTab === 'SALVAGE'
                ? 'bg-orange-600 text-white shadow'
                : 'bg-slate-800 text-slate-400 hover:text-white'
            }`}
          >
            <Trash2 className="w-3.5 h-3.5" />
            SMANTELLA
          </button>
          <button
            onClick={() => setActiveTab('FORGE')}
            className={`px-3 py-1.5 rounded-lg font-cinzel text-xs font-bold transition-all flex items-center gap-1.5 ${
              activeTab === 'FORGE'
                ? 'bg-orange-600 text-white shadow'
                : 'bg-slate-800 text-slate-400 hover:text-white'
            }`}
          >
            <Flame className="w-3.5 h-3.5" />
            FORGIA ANTICA
          </button>
        </div>

        {/* TAB 1: UPGRADE */}
        {activeTab === 'UPGRADE' && (
          <div className="space-y-4">
            <div className="text-xs text-slate-400">
              Seleziona uno slot equipaggiato da potenziare con le chele di Mastro Astice:
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
                        ? 'border-orange-500 bg-orange-950/30 shadow'
                        : 'border-slate-800 bg-slate-950 hover:border-slate-700'
                    }`}
                  >
                    <div className="text-[10px] font-bold text-slate-400 uppercase mb-1">
                      {slotKey === 'weapon' ? 'Arma' : slotKey === 'armor' ? 'Corazza' : 'Reliquia'}
                    </div>
                    {it ? (
                      <div>
                        <div
                          className="font-cinzel font-bold text-xs truncate"
                          style={{ color: engine.getRarityColor(it.rarity) }}
                        >
                          {it.name}
                        </div>
                        <div className="text-[10px] text-amber-400 font-bold mt-1">
                          Grado: +{it.upgradeLevel} / 5
                        </div>
                      </div>
                    ) : (
                      <span className="text-xs text-slate-600">Nessun oggetto</span>
                    )}
                  </div>
                );
              })}
            </div>

            {selectedItem ? (
              <div className="bg-slate-950/80 border border-slate-800 rounded-xl p-4">
                <div className="flex items-center justify-between mb-2">
                  <h4
                    className="font-cinzel font-bold text-sm"
                    style={{ color: engine.getRarityColor(selectedItem.rarity) }}
                  >
                    {selectedItem.name} +{selectedItem.upgradeLevel}
                  </h4>
                  <span className="text-xs text-amber-400 font-bold">
                    Livello Attuale: +{selectedItem.upgradeLevel} / 5
                  </span>
                </div>

                <div className="space-y-1.5 text-xs text-slate-300 mb-4">
                  {selectedItem.damageBonus > 0 && (
                    <div>Danno Attuale: <span className="font-bold text-rose-400">{selectedItem.damageBonus}</span> → Prossimo: <span className="font-bold text-emerald-400">+{Math.round(selectedItem.damageBonus * 1.15)}</span></div>
                  )}
                  {selectedItem.defenseBonus > 0 && (
                    <div>Difesa Attuale: <span className="font-bold text-emerald-400">{selectedItem.defenseBonus}</span> → Prossimo: <span className="font-bold text-emerald-300">+{Math.round(selectedItem.defenseBonus * 1.15)}</span></div>
                  )}
                  {selectedItem.healthBonus > 0 && (
                    <div>Salute Attuale: <span className="font-bold text-red-400">{selectedItem.healthBonus}</span> → Prossimo: <span className="font-bold text-emerald-300">+{Math.round(selectedItem.healthBonus * 1.15)}</span></div>
                  )}
                </div>

                {selectedItem.upgradeLevel < 5 ? (
                  <div className="flex items-center justify-between pt-3 border-t border-slate-800">
                    <div className="text-xs text-slate-400">
                      Costo: <span className="font-bold text-sky-300">💎 {engine.getUpgradeShardCost(selectedItem)} Frammenti</span> + <span className="font-bold text-amber-400">🐚 {engine.getUpgradeShellCost(selectedItem)} Conchiglie</span>
                    </div>
                    <button
                      onClick={handleUpgrade}
                      disabled={!engine.canUpgradeItem(selectedItem)}
                      className="px-4 py-2 rounded-lg bg-orange-600 hover:bg-orange-500 font-cinzel font-bold text-xs text-white disabled:opacity-40 transition-colors"
                    >
                      POTENZIA OGGETTO
                    </button>
                  </div>
                ) : (
                  <div className="text-center text-xs font-bold text-amber-400 py-2 border-t border-slate-800">
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
            <div className="text-xs text-slate-400">
              Smantella gli oggetti presenti nel tuo zaino per ricavare frammenti e conchiglie:
            </div>

            {engine.backpack.length === 0 ? (
              <div className="text-center py-8 text-xs text-slate-500">
                Lo zaino è vuoto. Trova bottino durante le immersioni per smantellarlo.
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {engine.backpack.map((it, idx) => (
                  <div
                    key={idx}
                    className="p-2.5 rounded-lg bg-slate-950 border border-slate-800 flex items-center justify-between"
                  >
                    <div>
                      <div
                        className="font-cinzel font-bold text-xs"
                        style={{ color: engine.getRarityColor(it.rarity) }}
                      >
                        {it.name} {it.upgradeLevel > 0 && `+${it.upgradeLevel}`}
                      </div>
                      <div className="text-[10px] text-slate-500">
                        {it.slot === 'weapon' ? 'Arma' : it.slot === 'armor' ? 'Corazza' : 'Reliquia'}
                      </div>
                    </div>
                    <button
                      onClick={() => handleSalvage(idx)}
                      className="px-2.5 py-1 rounded bg-red-950/80 hover:bg-red-800 text-red-300 text-xs font-bold border border-red-700/50 transition-colors"
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
            <div className="text-xs text-slate-400">
              Forgia manufatti leggendari dell'abisso incanalando materiali rari:
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Rare Forge */}
              <div className="p-4 rounded-xl border border-amber-500/60 bg-slate-950/80 flex flex-col justify-between">
                <div>
                  <h4 className="font-cinzel font-bold text-sm text-amber-300 mb-1">
                    Forgia Oggetto Raro
                  </h4>
                  <p className="text-xs text-slate-400 mb-3">
                    Crea un'arma, corazza o reliquia di grado Raro con statistiche superiori.
                  </p>
                  <div className="text-xs text-slate-300 space-y-1 mb-4">
                    <div>Costo: <span className="font-bold text-sky-300">💎 8 Frammenti</span></div>
                    <div>Conchiglie: <span className="font-bold text-amber-400">🐚 40 Conchiglie</span></div>
                  </div>
                </div>
                <button
                  onClick={() => handleForge(2)}
                  disabled={engine.abyssalShards < 8 || engine.shells < 40 || engine.backpack.length >= 8}
                  className="w-full py-2 rounded-lg bg-amber-600 hover:bg-amber-500 font-cinzel font-bold text-xs text-slate-950 disabled:opacity-40 transition-colors"
                >
                  FORGIA RARO
                </button>
              </div>

              {/* Abyssal Forge */}
              <div className="p-4 rounded-xl border border-purple-500/60 bg-slate-950/80 flex flex-col justify-between">
                <div>
                  <h4 className="font-cinzel font-bold text-sm text-purple-300 mb-1">
                    Forgia Oggetto Abissale
                  </h4>
                  <p className="text-xs text-slate-400 mb-3">
                    Forgiatura maestosa delle faglie profonde. Grado Abissale con affiliazioni di Set.
                  </p>
                  <div className="text-xs text-slate-300 space-y-1 mb-4">
                    <div>Costo: <span className="font-bold text-sky-300">💎 20 Frammenti</span></div>
                    <div>Conchiglie: <span className="font-bold text-amber-400">🐚 100 Conchiglie</span></div>
                  </div>
                </div>
                <button
                  onClick={() => handleForge(3)}
                  disabled={engine.abyssalShards < 20 || engine.shells < 100 || engine.backpack.length >= 8}
                  className="w-full py-2 rounded-lg bg-purple-600 hover:bg-purple-500 font-cinzel font-bold text-xs text-white disabled:opacity-40 transition-colors"
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
