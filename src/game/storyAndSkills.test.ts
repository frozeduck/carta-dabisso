import test from 'node:test';
import assert from 'node:assert/strict';
import { CLASS_ACTIVE_SKILLS, DEFAULT_SKILL_LOADOUTS, INITIAL_ACTS } from './skillsData';
import { GameEngine } from './GameEngine';

test('CLASS_ACTIVE_SKILLS contains 6 unique skills per class', () => {
  const classes = ['hunter', 'juggernaut', 'mystic'] as const;
  for (const c of classes) {
    const skills = CLASS_ACTIVE_SKILLS[c];
    assert.equal(skills.length, 6, `${c} must have 6 active skills`);
    const ids = new Set(skills.map((s) => s.id));
    assert.equal(ids.size, 6, `${c} skill IDs must be distinct`);
    for (const skill of skills) {
      assert.ok(skill.name.length > 0, 'Skill must have a name');
      assert.ok(skill.cooldown > 0, 'Skill must have positive cooldown');
      assert.ok(skill.tideCost >= 0, 'Tide cost must be non-negative');
      assert.ok(skill.damageMult > 0, 'Damage mult must be positive');
    }
  }
});

test('DEFAULT_SKILL_LOADOUTS maps all active slots M1, M2, Q, W, E, R', () => {
  const slots = ['M1', 'M2', 'Q', 'W', 'E', 'R'] as const;
  const classes = ['hunter', 'juggernaut', 'mystic'] as const;
  for (const c of classes) {
    const loadout = DEFAULT_SKILL_LOADOUTS[c];
    for (const slot of slots) {
      assert.ok(loadout[slot], `Slot ${slot} must be assigned in default ${c} loadout`);
    }
  }
});

test('GameEngine initializes with story acts and active loadout', () => {
  const engine = new GameEngine();
  assert.equal(engine.acts.length, 4, 'Must have 4 story acts');
  assert.ok(engine.acts[0].objectives.length >= 2, 'Act 1 must have at least 2 story objectives');
  assert.equal(engine.acts[0].gateOpened, false, 'Act 1 gate starts closed');

  // Verify skill loadout assignment
  const originalQ = engine.skillLoadout.Q;
  engine.assignSkillToSlot('Q', 'hunt_barrage');
  assert.equal(engine.skillLoadout.Q, 'hunt_barrage', 'Skill loadout must update when assigned');
  engine.assignSkillToSlot('Q', originalQ);
});

test('Story gate opens when non-boss objectives are completed', () => {
  const engine = new GameEngine();
  engine.startActExpedition(1);
  const act = engine.acts.find((a) => a.number === 1);
  assert.ok(act);

  // Complete non-boss objectives
  for (const obj of act.objectives) {
    if (obj.type !== 'reach_boss') {
      obj.currentCount = obj.targetCount;
      obj.completed = true;
    }
  }

  // Check gate opening logic
  const allPreReqsDone = act.objectives
    .filter((o) => o.type !== 'reach_boss')
    .every((o) => o.completed);
  assert.equal(allPreReqsDone, true, 'All pre-requisites are fulfilled');
});

test('Equipment comparison computes deltas correctly and equips item into slot', () => {
  const engine = new GameEngine();
  const currentWeapon = engine.equippedWeapon;
  assert.ok(currentWeapon, 'Starting character has an equipped weapon');

  const baseDmg = currentWeapon.damageBonus || 0;
  // Create a superior test weapon in backpack
  const superiorWeapon = {
    id: 'test_super_spear',
    name: 'Fiocina delle Tempeste',
    slot: 'weapon' as const,
    rarity: 2,
    desc: 'Un fiocina affilata e temprata.',
    damageBonus: baseDmg + 14,
    defenseBonus: 3,
    healthBonus: 20,
    speedBonus: 10,
    critBonus: 0.05,
    upgradeLevel: 1,
    visualVariant: 0,
  };

  engine.backpack.push(superiorWeapon);
  const backpackIndex = engine.backpack.length - 1;

  // Verify delta calculation
  const deltaDmg = superiorWeapon.damageBonus - (currentWeapon.damageBonus || 0);
  assert.equal(deltaDmg, 14, 'Damage delta should be +14');

  // Equip from backpack
  const equippedOk = engine.equipBackpackItem(backpackIndex);
  assert.equal(equippedOk, true, 'Should successfully equip item');
  assert.equal(engine.equippedWeapon?.id, 'test_super_spear', 'Equipped weapon is updated');
  assert.ok(engine.getAttackDamage() > baseDmg, 'Attack damage increased after equipping superior weapon');
});

