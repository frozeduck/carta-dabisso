import React, { useState } from 'react';
import { GameEngine } from '../../game/GameEngine';
import { Item, ItemSlot } from '../../types/game';
import {
  X,
  Shield,
  Sword,
  Sparkles,
  Footprints,
  Trash2,
  CheckCircle2,
  ArrowUpRight,
  ArrowDownRight,
  Minus,
  Scale,
  Compass,
  Scroll,
} from 'lucide-react';

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
  // If player has backpack items, select first backpack item by default to showcase comparison
  const [selectedSlot, setSelectedSlot] = useState<ItemSlot | 'backpack'>(
    engine.backpack.length > 0 ? 'backpack' : 'weapon'
  );
  const [selectedBackpackIndex, setSelectedBackpackIndex] = useState<number>(0);
  const [selectedEquippedSlot, setSelectedEquippedSlot] = useState<ItemSlot>('weapon');

  // Currently selected item
  const selectedItem: Item | null =
    selectedSlot === 'backpack'
      ? engine.backpack[selectedBackpackIndex] || null
      : selectedSlot === 'weapon'
      ? engine.equippedWeapon
      : selectedSlot === 'armor'
      ? engine.equippedArmor
      : engine.equippedRelic;

  // The equipped item corresponding to the inspected item's slot
  const comparedEquippedItem: Item | null =
    selectedItem?.slot === 'weapon'
      ? engine.equippedWeapon
      : selectedItem?.slot === 'armor'
      ? engine.equippedArmor
      : selectedItem?.slot === 'relic'
      ? engine.equippedRelic
      : null;

  const isBackpackSelected = selectedSlot === 'backpack' && !!selectedItem;
  // Are we comparing two different items (a backpack item against an equipped item)?
  const isComparing = isBackpackSelected && selectedItem !== null;

  const handleSelectEquipped = (slot: ItemSlot) => {
    setSelectedSlot(slot);
    setSelectedEquippedSlot(slot);
  };

  const handleSelectBackpack = (index: number) => {
    setSelectedSlot('backpack');
    setSelectedBackpackIndex(index);
  };

  const handleEquip = () => {
    if (selectedSlot === 'backpack' && selectedItem) {
      engine.equipBackpackItem(selectedBackpackIndex);
      onRefresh();
      // Keep selection valid
      if (selectedBackpackIndex >= engine.backpack.length) {
        setSelectedBackpackIndex(Math.max(0, engine.backpack.length - 1));
      }
    }
  };

  const handleUnequip = (slot: ItemSlot) => {
    engine.unequipSlot(slot);
    onRefresh();
  };

  const handleDrop = () => {
    if (selectedSlot === 'backpack') {
      engine.dropBackpackItem(selectedBackpackIndex);
      onRefresh();
      if (selectedBackpackIndex >= engine.backpack.length) {
        setSelectedBackpackIndex(Math.max(0, engine.backpack.length - 1));
      }
    }
  };

  // Helper to calculate delta and score between two items
  const getItemScore = (it: Item | null) => {
    if (!it) return 0;
    return (
      (it.damageBonus || 0) * 2.0 +
      (it.defenseBonus || 0) * 1.8 +
      (it.healthBonus || 0) * 0.6 +
      (it.speedBonus || 0) * 0.3 +
      (it.critBonus || 0) * 150 +
      (it.upgradeLevel || 0) * 8
    );
  };

  // Helper to check if a backpack item is overall better than the equipped one
  const getComparisonBadge = (bpItem: Item) => {
    const eq =
      bpItem.slot === 'weapon'
        ? engine.equippedWeapon
        : bpItem.slot === 'armor'
        ? engine.equippedArmor
        : engine.equippedRelic;

    const diff = getItemScore(bpItem) - getItemScore(eq);
    if (!eq) return 'NEW';
    if (diff > 2) return 'UP';
    if (diff < -2) return 'DOWN';
    return 'EQUAL';
  };

  const renderDeltaStat = (
    label: string,
    currentVal: number,
    newVal: number,
    icon: React.ReactNode,
    isPercentage: boolean = false
  ) => {
    const delta = newVal - currentVal;
    const formattedDelta = isPercentage
      ? `${delta > 0 ? '+' : ''}${Math.round(delta * 100)}%`
      : `${delta > 0 ? '+' : ''}${Math.round(delta * 10) / 10}`;

    return (
      <div className="flex items-center justify-between py-1 px-2 rounded parchment-panel-inset text-xs">
        <span className="flex items-center gap-1.5 font-medium text-[#4a3219]">
          {icon}
          {label}
        </span>
        <div className="flex items-center gap-2">
          <span className="text-[#6c4e2e] text-[11px]">
            {isPercentage ? `${Math.round(currentVal * 100)}%` : currentVal}
            {' → '}
            <strong className="text-[#2b1c0c]">
              {isPercentage ? `${Math.round(newVal * 100)}%` : newVal}
            </strong>
          </span>
          {delta > 0 ? (
            <span className="flex items-center text-xs font-bold text-emerald-700 bg-emerald-100/90 px-1.5 py-0.5 rounded border border-emerald-300">
              <ArrowUpRight className="w-3.5 h-3.5 inline mr-0.5" />
              {formattedDelta}
            </span>
          ) : delta < 0 ? (
            <span className="flex items-center text-xs font-bold text-rose-700 bg-rose-100/90 px-1.5 py-0.5 rounded border border-rose-300">
              <ArrowDownRight className="w-3.5 h-3.5 inline mr-0.5" />
              {formattedDelta}
            </span>
          ) : (
            <span className="flex items-center text-[10px] font-semibold text-stone-600 bg-stone-200/80 px-1 py-0.5 rounded">
              <Minus className="w-3 h-3 inline mr-0.5" /> 0
            </span>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-2 sm:p-4 select-none animate-in fade-in duration-200">
      <div className="parchment-window w-full max-w-4xl max-h-[94vh] overflow-y-auto rounded-2xl p-4 sm:p-6 text-[#2b1c0c] shadow-2xl">
        {/* Parchment Nautical Cartouche Header */}
        <div className="flex items-center justify-between border-b-2 border-[#8c6239]/40 pb-3 mb-4">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-full bg-[#dec89b] border-2 border-[#7a4e21] flex items-center justify-center shadow-inner">
              <Compass className="w-5 h-5 text-[#69390f]" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="font-cinzel text-lg sm:text-xl font-black text-[#381e09] tracking-wide">
                  CARTA D'EQUIPAGGIAMENTO & ZAINO
                </h2>
                <span className="parchment-tag text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider">
                  {engine.backpack.length} / 8 Comparti
                </span>
              </div>
              <p className="text-[11px] text-[#694625] italic font-serif">
                Ispeziona, confronta statistiche e assegna gli strumenti marini del palombaro.
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-[#ebd9b4] hover:bg-[#d8be8d] border border-[#8c6032] flex items-center justify-center text-[#4a2e12] transition-colors shadow"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Main Grid: Left (Paperdoll & Stats) | Right (Backpack & Side-by-Side Comparison) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
          {/* LEFT COLUMN: Paperdoll & Current Stats (5 cols on lg) */}
          <div className="lg:col-span-5 flex flex-col gap-3.5">
            {/* Paperdoll: 3 Active Gear Slots */}
            <div className="parchment-card rounded-xl p-3.5 border-2 border-[#b59868]">
              <div className="flex items-center justify-between mb-2 pb-1 border-b border-[#cbb28b]">
                <span className="font-cinzel text-xs font-bold text-[#4e3116] uppercase tracking-wider flex items-center gap-1.5">
                  <Scroll className="w-3.5 h-3.5 text-[#854d0e]" />
                  Slot Attualmente Indossati
                </span>
                <span className="text-[10px] text-[#714d2a] font-serif italic">Clicca per rimuovere</span>
              </div>

              <div className="grid grid-cols-3 gap-2.5">
                {/* Weapon Slot */}
                <div
                  onClick={() => handleSelectEquipped('weapon')}
                  className={`h-24 rounded-lg flex flex-col items-center justify-between p-2 cursor-pointer transition-all ${
                    selectedSlot === 'weapon'
                      ? 'parchment-slot-active'
                      : 'parchment-slot'
                  }`}
                >
                  <div className="w-7 h-7 rounded-full bg-[#f3e7ce] border border-[#a47e4e] flex items-center justify-center">
                    <Sword className="w-4 h-4 text-[#8a3800]" />
                  </div>
                  <span className="text-[10px] font-bold uppercase text-[#5a3617]">Arma</span>
                  {engine.equippedWeapon ? (
                    <div className="text-center w-full">
                      <span
                        className="text-[10px] font-bold block truncate max-w-full"
                        style={{ color: engine.getRarityColor(engine.equippedWeapon.rarity) === '#f8fafc' ? '#452b14' : engine.getRarityColor(engine.equippedWeapon.rarity) }}
                      >
                        {engine.equippedWeapon.name}
                      </span>
                      {engine.equippedWeapon.upgradeLevel > 0 && (
                        <span className="text-[9px] font-extrabold text-amber-800">
                          +{engine.equippedWeapon.upgradeLevel}
                        </span>
                      )}
                    </div>
                  ) : (
                    <span className="text-[9px] text-[#8c6b4d] italic">Vuoto</span>
                  )}
                </div>

                {/* Armor Slot */}
                <div
                  onClick={() => handleSelectEquipped('armor')}
                  className={`h-24 rounded-lg flex flex-col items-center justify-between p-2 cursor-pointer transition-all ${
                    selectedSlot === 'armor'
                      ? 'parchment-slot-active'
                      : 'parchment-slot'
                  }`}
                >
                  <div className="w-7 h-7 rounded-full bg-[#f3e7ce] border border-[#a47e4e] flex items-center justify-center">
                    <Shield className="w-4 h-4 text-[#166534]" />
                  </div>
                  <span className="text-[10px] font-bold uppercase text-[#5a3617]">Corazza</span>
                  {engine.equippedArmor ? (
                    <div className="text-center w-full">
                      <span
                        className="text-[10px] font-bold block truncate max-w-full"
                        style={{ color: engine.getRarityColor(engine.equippedArmor.rarity) === '#f8fafc' ? '#452b14' : engine.getRarityColor(engine.equippedArmor.rarity) }}
                      >
                        {engine.equippedArmor.name}
                      </span>
                      {engine.equippedArmor.upgradeLevel > 0 && (
                        <span className="text-[9px] font-extrabold text-amber-800">
                          +{engine.equippedArmor.upgradeLevel}
                        </span>
                      )}
                    </div>
                  ) : (
                    <span className="text-[9px] text-[#8c6b4d] italic">Vuoto</span>
                  )}
                </div>

                {/* Relic Slot */}
                <div
                  onClick={() => handleSelectEquipped('relic')}
                  className={`h-24 rounded-lg flex flex-col items-center justify-between p-2 cursor-pointer transition-all ${
                    selectedSlot === 'relic'
                      ? 'parchment-slot-active'
                      : 'parchment-slot'
                  }`}
                >
                  <div className="w-7 h-7 rounded-full bg-[#f3e7ce] border border-[#a47e4e] flex items-center justify-center">
                    <Sparkles className="w-4 h-4 text-[#6b21a8]" />
                  </div>
                  <span className="text-[10px] font-bold uppercase text-[#5a3617]">Reliquia</span>
                  {engine.equippedRelic ? (
                    <div className="text-center w-full">
                      <span
                        className="text-[10px] font-bold block truncate max-w-full"
                        style={{ color: engine.getRarityColor(engine.equippedRelic.rarity) === '#f8fafc' ? '#452b14' : engine.getRarityColor(engine.equippedRelic.rarity) }}
                      >
                        {engine.equippedRelic.name}
                      </span>
                      {engine.equippedRelic.upgradeLevel > 0 && (
                        <span className="text-[9px] font-extrabold text-amber-800">
                          +{engine.equippedRelic.upgradeLevel}
                        </span>
                      )}
                    </div>
                  ) : (
                    <span className="text-[9px] text-[#8c6b4d] italic">Vuoto</span>
                  )}
                </div>
              </div>
            </div>

            {/* Character Stats Ledger */}
            <div className="parchment-card rounded-xl p-3.5 border-2 border-[#b59868] text-xs space-y-1.5">
              <div className="flex items-center justify-between pb-1.5 border-b border-[#cbb28b]">
                <span className="font-cinzel text-xs font-bold text-[#4e3116] uppercase tracking-wider">
                  Registro del Palombaro
                </span>
                <span className="font-cinzel font-bold text-[#854d0e] bg-[#eeddbb] px-2 py-0.5 rounded border border-[#bfa378]">
                  Grado Lv.{engine.level}
                </span>
              </div>

              <div className="flex justify-between py-1 border-b border-[#dfcaa7]/60">
                <span className="flex items-center gap-1.5 text-[#54391e]">
                  <Sword className="w-3.5 h-3.5 text-orange-700" /> Danno Attacco:
                </span>
                <strong className="text-[#321e0e]">{engine.getAttackDamage()}</strong>
              </div>

              <div className="flex justify-between py-1 border-b border-[#dfcaa7]/60">
                <span className="flex items-center gap-1.5 text-[#54391e]">
                  <Shield className="w-3.5 h-3.5 text-emerald-800" /> Difesa Corazza:
                </span>
                <strong className="text-[#321e0e]">{engine.getDefense()}</strong>
              </div>

              <div className="flex justify-between py-1 border-b border-[#dfcaa7]/60">
                <span className="flex items-center gap-1.5 text-[#54391e]">
                  <Sparkles className="w-3.5 h-3.5 text-red-700" /> Salute Massima:
                </span>
                <strong className="text-[#321e0e]">{engine.getMaxHp()} PV</strong>
              </div>

              <div className="flex justify-between py-1 border-b border-[#dfcaa7]/60">
                <span className="flex items-center gap-1.5 text-[#54391e]">
                  <Footprints className="w-3.5 h-3.5 text-cyan-800" /> Velocità Movimento:
                </span>
                <strong className="text-[#321e0e]">{Math.round(engine.getMoveSpeed())}</strong>
              </div>

              <div className="flex justify-between py-1">
                <span className="text-[#54391e]">Probabilità Critico:</span>
                <strong className="text-[#321e0e]">
                  {Math.round(engine.getCritChance() * 100)}%
                </strong>
              </div>
            </div>

            {/* Currency Pouch */}
            <div className="parchment-panel-inset rounded-xl p-2.5 flex items-center justify-around text-xs font-bold text-[#3d2712]">
              <span className="flex items-center gap-1">
                💎 <span className="text-[#0369a1]">{engine.abyssalShards}</span> Frammenti
              </span>
              <span className="text-[#ab8f63]">|</span>
              <span className="flex items-center gap-1">
                🐚 <span className="text-[#b45309]">{engine.shells}</span> Conchiglie
              </span>
            </div>
          </div>

          {/* RIGHT COLUMN: Backpack Compartments & Comparison Engine (7 cols on lg) */}
          <div className="lg:col-span-7 flex flex-col gap-3.5">
            {/* Backpack 8 Slots */}
            <div className="parchment-card rounded-xl p-3.5 border-2 border-[#b59868]">
              <div className="flex items-center justify-between mb-2 pb-1 border-b border-[#cbb28b]">
                <span className="font-cinzel text-xs font-bold text-[#4e3116] uppercase tracking-wider flex items-center gap-1.5">
                  <Scale className="w-3.5 h-3.5 text-[#854d0e]" />
                  Zaino di Spedizione (Clicca per Confrontare)
                </span>
                <span className="text-[10px] text-[#714d2a] font-serif italic">
                  8 Vani Ermetici
                </span>
              </div>

              <div className="grid grid-cols-4 gap-2">
                {Array.from({ length: 8 }).map((_, idx) => {
                  const it = engine.backpack[idx];
                  const isSelected = selectedSlot === 'backpack' && selectedBackpackIndex === idx;

                  if (!it) {
                    return (
                      <div
                        key={idx}
                        className="h-16 rounded-lg border-2 border-dashed border-[#bda682] bg-[#f5ecd5]/50 flex items-center justify-center text-[11px] text-[#8a7253] font-serif"
                      >
                        #{idx + 1}
                      </div>
                    );
                  }

                  const badge = getComparisonBadge(it);
                  const isEquipType = it.slot === 'weapon' ? 'Arma' : it.slot === 'armor' ? 'Corazza' : 'Reliquia';

                  return (
                    <div
                      key={idx}
                      onClick={() => handleSelectBackpack(idx)}
                      className={`relative h-16 rounded-lg p-1.5 flex flex-col justify-between cursor-pointer transition-all ${
                        isSelected
                          ? 'parchment-slot-active scale-105'
                          : 'parchment-slot'
                      }`}
                    >
                      {/* Comparison Delta Indicator Flag */}
                      {badge === 'UP' && (
                        <div
                          title="Statistiche complessive superiori!"
                          className="absolute -top-1.5 -right-1.5 w-4 h-4 rounded-full bg-emerald-600 text-white text-[9px] font-black flex items-center justify-center shadow"
                        >
                          ▲
                        </div>
                      )}
                      {badge === 'DOWN' && (
                        <div
                          title="Statistiche complessive inferiori"
                          className="absolute -top-1.5 -right-1.5 w-4 h-4 rounded-full bg-rose-600 text-white text-[9px] font-black flex items-center justify-center shadow"
                        >
                          ▼
                        </div>
                      )}
                      {badge === 'NEW' && (
                        <div
                          title="Slot attualmente vuoto"
                          className="absolute -top-1.5 -right-1.5 px-1 rounded bg-sky-600 text-white text-[8px] font-black flex items-center justify-center shadow"
                        >
                          NUOVO
                        </div>
                      )}

                      <div className="text-[10px] font-bold truncate text-[#2f1b0a]">
                        {it.name}
                      </div>
                      <div className="flex items-center justify-between text-[9px] text-[#694625]">
                        <span className="uppercase font-semibold">{isEquipType}</span>
                        {it.upgradeLevel > 0 && (
                          <span className="text-amber-800 font-extrabold">+{it.upgradeLevel}</span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* COMPARISON & INSPECTOR MODULE */}
            <div className="parchment-card rounded-xl p-4 border-2 border-[#b59868] flex-1 flex flex-col justify-between">
              {selectedItem ? (
                <div>
                  {/* Top Bar with Verdict & Actions */}
                  <div className="flex items-center justify-between mb-3 pb-2 border-b-2 border-[#bfa57b]">
                    <div className="flex items-center gap-2">
                      <Scale className="w-4 h-4 text-[#854d0e]" />
                      <h3 className="font-cinzel text-xs font-black uppercase tracking-wider text-[#3d240e]">
                        {isComparing ? 'Tavola di Comparazione Attrezzatura' : 'Dettaglio Oggetto'}
                      </h3>
                    </div>

                    {isComparing && (
                      <div>
                        {getComparisonBadge(selectedItem) === 'UP' ? (
                          <span className="text-[11px] font-bold text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded border border-emerald-400 flex items-center gap-1 shadow-sm">
                            <ArrowUpRight className="w-3.5 h-3.5" />
                            CONSIGLIATO (+POTENZA)
                          </span>
                        ) : getComparisonBadge(selectedItem) === 'DOWN' ? (
                          <span className="text-[11px] font-bold text-rose-800 bg-rose-100 px-2 py-0.5 rounded border border-rose-400 flex items-center gap-1 shadow-sm">
                            <ArrowDownRight className="w-3.5 h-3.5" />
                            INFERIORE (-POTENZA)
                          </span>
                        ) : (
                          <span className="text-[11px] font-bold text-stone-700 bg-stone-200 px-2 py-0.5 rounded border border-stone-400">
                            EQUIVALENTE
                          </span>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Side-by-Side Cards (Equipped VS New) */}
                  {isComparing ? (
                    <div className="grid grid-cols-2 gap-3 mb-3">
                      {/* Left: Currently Equipped */}
                      <div className="parchment-panel-inset rounded-lg p-2.5 flex flex-col justify-between">
                        <div>
                          <div className="text-[9px] font-cinzel font-bold text-[#714d2a] uppercase tracking-wider mb-1 flex items-center gap-1">
                            <span className="w-2 h-2 rounded-full bg-[#8c6b4d]" />
                            Attualmente Equipaggiato:
                          </div>
                          {comparedEquippedItem ? (
                            <>
                              <h4 className="font-cinzel font-bold text-xs text-[#321c0b] truncate">
                                {comparedEquippedItem.name}
                                {comparedEquippedItem.upgradeLevel > 0 && ` +${comparedEquippedItem.upgradeLevel}`}
                              </h4>
                              <div className="text-[9px] text-[#694625] italic mb-1.5">
                                Grado {comparedEquippedItem.rarity === 0 ? 'Comune' : comparedEquippedItem.rarity === 1 ? 'Magico' : comparedEquippedItem.rarity === 2 ? 'Raro' : 'Abissale'}
                              </div>
                            </>
                          ) : (
                            <div className="text-xs text-[#8c6b4d] italic py-2">
                              (Nessun oggetto equipaggiato in questo slot)
                            </div>
                          )}
                        </div>
                      </div>

                      {/* Right: Selected New Item to Equip */}
                      <div className="bg-[#fff9ea] border-2 border-[#b45309] rounded-lg p-2.5 shadow-sm flex flex-col justify-between">
                        <div>
                          <div className="text-[9px] font-cinzel font-bold text-[#b45309] uppercase tracking-wider mb-1 flex items-center gap-1">
                            <span className="w-2 h-2 rounded-full bg-[#b45309]" />
                            Selezionato nello Zaino:
                          </div>
                          <h4 className="font-cinzel font-bold text-xs text-[#2b1706] truncate">
                            {selectedItem.name}
                            {selectedItem.upgradeLevel > 0 && ` +${selectedItem.upgradeLevel}`}
                          </h4>
                          <div className="text-[9px] text-[#694625] italic mb-1.5">
                            Grado {selectedItem.rarity === 0 ? 'Comune' : selectedItem.rarity === 1 ? 'Magico' : selectedItem.rarity === 2 ? 'Raro' : 'Abissale'}
                          </div>
                        </div>
                      </div>
                    </div>
                  ) : (
                    /* Single Item View when inspecting an equipped slot directly */
                    <div className="bg-[#fff9ea] border border-[#ab8f63] rounded-lg p-3 mb-3">
                      <div className="flex items-start justify-between">
                        <div>
                          <h4 className="font-cinzel font-bold text-sm text-[#2b1706]">
                            {selectedItem.name}
                            {selectedItem.upgradeLevel > 0 && ` +${selectedItem.upgradeLevel}`}
                          </h4>
                          <div className="text-[10px] text-[#694625] font-semibold uppercase">
                            {selectedItem.slot === 'weapon' ? 'Arma Marina' : selectedItem.slot === 'armor' ? 'Corazza Subacquea' : 'Reliquia Abissale'} · Grado {selectedItem.rarity === 0 ? 'Comune' : selectedItem.rarity === 1 ? 'Magico' : selectedItem.rarity === 2 ? 'Raro' : 'Abissale'}
                          </div>
                        </div>
                        {selectedItem.setName && (
                          <span className="parchment-tag text-[9px] font-bold px-2 py-0.5 rounded border border-[#a48154]">
                            Set {selectedItem.setName}
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-[#523820] mt-1.5 italic font-serif">
                        "{selectedItem.desc}"
                      </p>
                    </div>
                  )}

                  {/* Comparative Stat Delta Matrix */}
                  <div className="space-y-1 mb-3">
                    {renderDeltaStat(
                      'Attacco & Danno',
                      comparedEquippedItem?.damageBonus || 0,
                      selectedItem.damageBonus || 0,
                      <Sword className="w-3.5 h-3.5 text-orange-700" />
                    )}
                    {renderDeltaStat(
                      'Difesa & Armatura',
                      comparedEquippedItem?.defenseBonus || 0,
                      selectedItem.defenseBonus || 0,
                      <Shield className="w-3.5 h-3.5 text-emerald-800" />
                    )}
                    {renderDeltaStat(
                      'Salute Massima (PV)',
                      comparedEquippedItem?.healthBonus || 0,
                      selectedItem.healthBonus || 0,
                      <Sparkles className="w-3.5 h-3.5 text-red-700" />
                    )}
                    {renderDeltaStat(
                      'Velocità Movimento',
                      comparedEquippedItem?.speedBonus || 0,
                      selectedItem.speedBonus || 0,
                      <Footprints className="w-3.5 h-3.5 text-cyan-800" />
                    )}
                    {renderDeltaStat(
                      'Probabilità Critico',
                      comparedEquippedItem?.critBonus || 0,
                      selectedItem.critBonus || 0,
                      <Sparkles className="w-3.5 h-3.5 text-amber-700" />,
                      true
                    )}
                  </div>

                  {/* Action Buttons */}
                  <div className="flex items-center gap-2 pt-2 border-t border-[#cbb28b]">
                    {selectedSlot === 'backpack' ? (
                      <>
                        <button
                          onClick={handleEquip}
                          className="flex-1 py-2 rounded-lg parchment-button-green font-cinzel font-bold text-xs flex items-center justify-center gap-1.5"
                        >
                          <CheckCircle2 className="w-4 h-4" />
                          EQUIPAGGIA QUESTO PEZZO
                        </button>
                        <button
                          onClick={handleDrop}
                          className="px-3 py-2 rounded-lg parchment-button-danger text-xs font-semibold flex items-center gap-1"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          GETTA
                        </button>
                      </>
                    ) : (
                      <button
                        onClick={() => handleUnequip(selectedSlot as ItemSlot)}
                        disabled={engine.backpack.length >= 8}
                        className="w-full py-2 rounded-lg parchment-button-neutral font-cinzel font-bold text-xs disabled:opacity-50"
                      >
                        RIPONI NELLO ZAINO
                      </button>
                    )}
                  </div>
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center py-10 text-xs text-[#8c6b4d] italic">
                  <Scroll className="w-8 h-8 mb-2 opacity-40" />
                  Seleziona un oggetto dallo zaino o dagli slot per aprirne la comparazione dettagliata.
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
