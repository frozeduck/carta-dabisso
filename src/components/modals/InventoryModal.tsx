import React, { useState } from 'react';
import { GameEngine } from '../../game/GameEngine';
import { Item, ItemSlot } from '../../types/game';
import { X, Shield, Sword, Sparkles, Footprints, Trash2, CheckCircle2 } from 'lucide-react';

interface InventoryModalProps {
  engine: GameEngine;
  onClose: () => void;
  onRefresh: () => void;
}

export const InventoryModal: React.FC<InventoryModalProps> = ({
  engine,
  onClose,
  onRefresh,
}) => {
  const [selectedItem, setSelectedItem] = useState<Item | null>(
    engine.equippedWeapon || engine.backpack[0] || null
  );
  const [selectedSlot, setSelectedSlot] = useState<ItemSlot | 'backpack' | null>(
    engine.equippedWeapon ? 'weapon' : 'backpack'
  );
  const [selectedBackpackIndex, setSelectedBackpackIndex] = useState<number>(0);

  const handleSelectEquipped = (slot: ItemSlot) => {
    setSelectedSlot(slot);
    if (slot === 'weapon') setSelectedItem(engine.equippedWeapon);
    else if (slot === 'armor') setSelectedItem(engine.equippedArmor);
    else if (slot === 'relic') setSelectedItem(engine.equippedRelic);
  };

  const handleSelectBackpack = (item: Item, index: number) => {
    setSelectedSlot('backpack');
    setSelectedBackpackIndex(index);
    setSelectedItem(item);
  };

  const handleEquip = () => {
    if (selectedSlot === 'backpack') {
      engine.equipBackpackItem(selectedBackpackIndex);
      onRefresh();
      setSelectedItem(null);
    }
  };

  const handleUnequip = () => {
    if (selectedSlot && selectedSlot !== 'backpack') {
      engine.unequipSlot(selectedSlot);
      onRefresh();
      setSelectedItem(null);
    }
  };

  const handleDrop = () => {
    if (selectedSlot === 'backpack') {
      engine.dropBackpackItem(selectedBackpackIndex);
      onRefresh();
      setSelectedItem(null);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-3 sm:p-4 select-none">
      <div className="relative w-full max-w-3xl max-h-[92vh] overflow-y-auto bg-slate-900 border-2 border-amber-500/80 rounded-2xl shadow-2xl p-5 text-slate-100">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3 mb-4">
          <div className="flex items-center gap-2">
            <h2 className="font-cinzel text-xl font-black text-amber-300">
              EQUIPAGGIAMENTO & ZAINO
            </h2>
            <span className="text-xs bg-slate-800 text-amber-400 font-bold px-2 py-0.5 rounded border border-slate-700">
              {engine.backpack.length} / 8 Slot
            </span>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* LEFT: Paperdoll & Stats */}
          <div className="flex flex-col gap-4">
            {/* Diablo Paperdoll 3 Slots */}
            <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-4 flex flex-col items-center">
              <span className="text-xs font-cinzel font-bold text-slate-400 mb-3 uppercase tracking-wider">
                Slot Equipaggiati
              </span>

              <div className="flex items-center justify-center gap-3">
                {/* Weapon Slot */}
                <div
                  onClick={() => handleSelectEquipped('weapon')}
                  className={`w-20 h-24 rounded-xl border-2 flex flex-col items-center justify-center p-2 cursor-pointer transition-all ${
                    selectedSlot === 'weapon'
                      ? 'border-sky-400 bg-sky-950/40 shadow-[0_0_12px_rgba(56,189,248,0.4)]'
                      : 'border-slate-700 bg-slate-900 hover:border-slate-500'
                  }`}
                >
                  <Sword className="w-6 h-6 text-sky-400 mb-1" />
                  <span className="text-[10px] font-bold text-slate-400 uppercase">Arma</span>
                  {engine.equippedWeapon ? (
                    <span
                      className="text-[10px] font-bold truncate max-w-full text-center mt-1"
                      style={{ color: engine.getRarityColor(engine.equippedWeapon.rarity) }}
                    >
                      {engine.equippedWeapon.upgradeLevel > 0
                        ? `+${engine.equippedWeapon.upgradeLevel}`
                        : 'Equip.'}
                    </span>
                  ) : (
                    <span className="text-[9px] text-slate-600">Vuoto</span>
                  )}
                </div>

                {/* Armor Slot */}
                <div
                  onClick={() => handleSelectEquipped('armor')}
                  className={`w-20 h-24 rounded-xl border-2 flex flex-col items-center justify-center p-2 cursor-pointer transition-all ${
                    selectedSlot === 'armor'
                      ? 'border-emerald-400 bg-emerald-950/40 shadow-[0_0_12px_rgba(52,211,153,0.4)]'
                      : 'border-slate-700 bg-slate-900 hover:border-slate-500'
                  }`}
                >
                  <Shield className="w-6 h-6 text-emerald-400 mb-1" />
                  <span className="text-[10px] font-bold text-slate-400 uppercase">Corazza</span>
                  {engine.equippedArmor ? (
                    <span
                      className="text-[10px] font-bold truncate max-w-full text-center mt-1"
                      style={{ color: engine.getRarityColor(engine.equippedArmor.rarity) }}
                    >
                      {engine.equippedArmor.upgradeLevel > 0
                        ? `+${engine.equippedArmor.upgradeLevel}`
                        : 'Equip.'}
                    </span>
                  ) : (
                    <span className="text-[9px] text-slate-600">Vuoto</span>
                  )}
                </div>

                {/* Relic Slot */}
                <div
                  onClick={() => handleSelectEquipped('relic')}
                  className={`w-20 h-24 rounded-xl border-2 flex flex-col items-center justify-center p-2 cursor-pointer transition-all ${
                    selectedSlot === 'relic'
                      ? 'border-purple-400 bg-purple-950/40 shadow-[0_0_12px_rgba(192,132,252,0.4)]'
                      : 'border-slate-700 bg-slate-900 hover:border-slate-500'
                  }`}
                >
                  <Sparkles className="w-6 h-6 text-purple-400 mb-1" />
                  <span className="text-[10px] font-bold text-slate-400 uppercase">Reliquia</span>
                  {engine.equippedRelic ? (
                    <span
                      className="text-[10px] font-bold truncate max-w-full text-center mt-1"
                      style={{ color: engine.getRarityColor(engine.equippedRelic.rarity) }}
                    >
                      {engine.equippedRelic.upgradeLevel > 0
                        ? `+${engine.equippedRelic.upgradeLevel}`
                        : 'Equip.'}
                    </span>
                  ) : (
                    <span className="text-[9px] text-slate-600">Vuoto</span>
                  )}
                </div>
              </div>
            </div>

            {/* Character Stats Matrix */}
            <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-3.5 space-y-1.5 text-xs">
              <div className="font-cinzel text-xs font-bold text-slate-400 uppercase pb-1 border-b border-slate-800 flex justify-between">
                <span>Statistiche Palombaro</span>
                <span className="text-amber-400">Lv.{engine.level}</span>
              </div>
              <div className="flex justify-between py-0.5">
                <span className="text-slate-400 flex items-center gap-1">
                  <Sword className="w-3.5 h-3.5 text-rose-400" /> Danno Attacco:
                </span>
                <span className="font-bold text-rose-300">{engine.getAttackDamage()}</span>
              </div>
              <div className="flex justify-between py-0.5">
                <span className="text-slate-400 flex items-center gap-1">
                  <Shield className="w-3.5 h-3.5 text-emerald-400" /> Difesa Corazza:
                </span>
                <span className="font-bold text-emerald-300">{engine.getDefense()}</span>
              </div>
              <div className="flex justify-between py-0.5">
                <span className="text-slate-400 flex items-center gap-1">
                  <Sparkles className="w-3.5 h-3.5 text-red-400" /> Salute Massima:
                </span>
                <span className="font-bold text-red-300">{engine.getMaxHp()} PV</span>
              </div>
              <div className="flex justify-between py-0.5">
                <span className="text-slate-400 flex items-center gap-1">
                  <Footprints className="w-3.5 h-3.5 text-sky-400" /> Velocità Movimento:
                </span>
                <span className="font-bold text-sky-300">{Math.round(engine.getMoveSpeed())}</span>
              </div>
              <div className="flex justify-between py-0.5">
                <span className="text-slate-400">Probabilità Critico:</span>
                <span className="font-bold text-amber-300">
                  {Math.round(engine.getCritChance() * 100)}%
                </span>
              </div>
            </div>
          </div>

          {/* RIGHT: Backpack Grid & Selected Item Details */}
          <div className="flex flex-col gap-4">
            {/* Backpack 8 Slots */}
            <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-3.5">
              <span className="text-xs font-cinzel font-bold text-slate-400 mb-2.5 block uppercase tracking-wider">
                Zaino Spedizione
              </span>
              <div className="grid grid-cols-4 gap-2">
                {Array.from({ length: 8 }).map((_, idx) => {
                  const it = engine.backpack[idx];
                  const isSelected = selectedSlot === 'backpack' && selectedBackpackIndex === idx;

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
                      onClick={() => handleSelectBackpack(it, idx)}
                      className={`h-16 rounded-lg border-2 p-1.5 flex flex-col justify-between cursor-pointer transition-all ${
                        isSelected
                          ? 'border-white bg-slate-800 shadow-md scale-105'
                          : 'border-slate-700 bg-slate-900 hover:border-slate-500'
                      }`}
                      style={{ borderColor: isSelected ? '#ffffff' : rColor }}
                    >
                      <div className="text-[10px] font-bold truncate" style={{ color: rColor }}>
                        {it.name}
                      </div>
                      <div className="flex items-center justify-between text-[9px] text-slate-400">
                        <span className="uppercase">{it.slot === 'weapon' ? 'Arma' : it.slot === 'armor' ? 'Corazza' : 'Reliquia'}</span>
                        {it.upgradeLevel > 0 && (
                          <span className="text-amber-400 font-bold">+{it.upgradeLevel}</span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Selected Item Inspector Panel */}
            <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-3.5 flex-1 flex flex-col justify-between">
              {selectedItem ? (
                <div>
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <h4
                        className="font-cinzel font-bold text-sm"
                        style={{ color: engine.getRarityColor(selectedItem.rarity) }}
                      >
                        {selectedItem.name}
                        {selectedItem.upgradeLevel > 0 && ` +${selectedItem.upgradeLevel}`}
                      </h4>
                      <div className="text-[10px] text-slate-400 uppercase font-semibold">
                        {selectedItem.slot === 'weapon' ? 'Arma Marina' : selectedItem.slot === 'armor' ? 'Corazza Subacquea' : 'Reliquia Abissale'} · Grado {selectedItem.rarity === 0 ? 'Comune' : selectedItem.rarity === 1 ? 'Magico' : selectedItem.rarity === 2 ? 'Raro' : 'Abissale'}
                      </div>
                    </div>
                    {selectedItem.setName && (
                      <span className="text-[9px] font-bold px-2 py-0.5 rounded bg-amber-950/60 text-amber-300 border border-amber-600/50">
                        Set {selectedItem.setName}
                      </span>
                    )}
                  </div>

                  <p className="text-xs text-slate-300 mb-3 italic">
                    "{selectedItem.desc}"
                  </p>

                  <div className="space-y-1 text-xs mb-4">
                    {selectedItem.damageBonus > 0 && (
                      <div className="text-rose-400 font-semibold">
                        +{selectedItem.damageBonus} Danno
                      </div>
                    )}
                    {selectedItem.defenseBonus > 0 && (
                      <div className="text-emerald-400 font-semibold">
                        +{selectedItem.defenseBonus} Difesa
                      </div>
                    )}
                    {selectedItem.healthBonus > 0 && (
                      <div className="text-red-400 font-semibold">
                        +{selectedItem.healthBonus} Salute Massima
                      </div>
                    )}
                    {selectedItem.speedBonus > 0 && (
                      <div className="text-sky-400 font-semibold">
                        +{selectedItem.speedBonus} Velocità
                      </div>
                    )}
                    {selectedItem.critBonus > 0 && (
                      <div className="text-amber-400 font-semibold">
                        +{Math.round(selectedItem.critBonus * 100)}% Probabilità Critico
                      </div>
                    )}
                  </div>

                  {/* Action Buttons */}
                  <div className="flex items-center gap-2 pt-2 border-t border-slate-800">
                    {selectedSlot === 'backpack' ? (
                      <>
                        <button
                          onClick={handleEquip}
                          className="flex-1 py-1.5 rounded-lg bg-sky-600 hover:bg-sky-500 font-cinzel font-bold text-xs text-white shadow transition-colors flex items-center justify-center gap-1.5"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          EQUIPAGGIA
                        </button>
                        <button
                          onClick={handleDrop}
                          className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-red-950 text-slate-400 hover:text-red-400 text-xs font-semibold transition-colors flex items-center gap-1"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          GETTA
                        </button>
                      </>
                    ) : (
                      <button
                        onClick={handleUnequip}
                        disabled={engine.backpack.length >= 8}
                        className="w-full py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 font-cinzel font-bold text-xs text-slate-300 disabled:opacity-50 transition-colors"
                      >
                        DISINSERISCI NELLO ZAINO
                      </button>
                    )}
                  </div>
                </div>
              ) : (
                <div className="flex items-center justify-center h-full text-xs text-slate-500">
                  Seleziona un oggetto per visualizzarne le caratteristiche.
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
