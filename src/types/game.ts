export type PlayerClassType = 'hunter' | 'juggernaut' | 'mystic';

export interface ClassDefinition {
  id: PlayerClassType;
  name: string;
  subtitle: string;
  description: string;
  weaponType: string;
  attackStyle: string;
  portrait: string;
  primaryColor: string;
  baseHp: number;
  baseDefense: number;
  baseDamage: number;
  baseSpeed: number;
}

export const CLASSES_DATA: Record<PlayerClassType, ClassDefinition> = {
  hunter: {
    id: 'hunter',
    name: 'Cacciatore dei Mari',
    subtitle: 'Tiratore Balistico Subacqueo',
    description: 'Specialista nel combattimento a lungo raggio. Spara arpioni perforanti ad altissima velocità e precisione letale.',
    weaponType: 'Fucile ad Arpione Balistico',
    attackStyle: 'Tiro balistico a perforazione multipla & multitiro',
    portrait: '/sprites/classe_cacciatore.png',
    primaryColor: '#38bdf8',
    baseHp: 100,
    baseDefense: 6,
    baseDamage: 22,
    baseSpeed: 175,
  },
  juggernaut: {
    id: 'juggernaut',
    name: 'Custode dei Fondali',
    subtitle: 'Baluardo Corazzato da Mischia',
    description: 'Campione in campana di bronzo imbullonata. Brandisce una colossale ancora nautica per fendenti spazzanti con forte contraccolpo.',
    weaponType: 'Pesante Ancora da Combattimento',
    attackStyle: 'Fendente circolare da mischia con knockback & scaling difesa',
    portrait: '/sprites/classe_custode.png',
    primaryColor: '#f59e0b',
    baseHp: 140,
    baseDefense: 14,
    baseDamage: 26,
    baseSpeed: 155,
  },
  mystic: {
    id: 'mystic',
    name: 'Mistico dell\'Abisso',
    subtitle: 'Stregone delle Correnti',
    description: 'Ermetico con tonaca cerimoniale e scettro di perla di marea. Lancia sfere a guida fluida che inseguono i bersagli e rigenerano Marea.',
    weaponType: 'Bastone di Corallo con Perla di Marea',
    attackStyle: 'Globi marini a traiettoria guidata fluida (homing orbs)',
    portrait: '/sprites/classe_mistico.png',
    primaryColor: '#c084fc',
    baseHp: 85,
    baseDefense: 4,
    baseDamage: 20,
    baseSpeed: 165,
  },
};

export const SUIT_COLORS = [
  { name: 'Oceano', hex: '#0284c7', border: '#38bdf8' },
  { name: 'Corallo', hex: '#e11d48', border: '#fb7185' },
  { name: 'Oro Abisso', hex: '#d97706', border: '#fcd34d' },
  { name: 'Smeraldo', hex: '#059669', border: '#34d399' },
  { name: 'Ametista', hex: '#7c3aed', border: '#c084fc' },
];

export type ItemSlot = 'weapon' | 'armor' | 'relic';

export interface Item {
  id: string;
  name: string;
  slot: ItemSlot;
  rarity: number; // 0=Common, 1=Magic, 2=Rare, 3=Abyssal
  damageBonus: number;
  defenseBonus: number;
  healthBonus: number;
  speedBonus: number;
  critBonus: number;
  desc: string;
  visualVariant: number;
  setName?: string; // "Corallo", "Marea", "Abisso"
  upgradeLevel: number; // 0..5
}

export interface Skill {
  id: string;
  name: string;
  desc: string;
  classType: PlayerClassType;
  branch: 1 | 2;
  tier: 1 | 2 | 3;
  maxRank: number;
  requiredSkillId: string | null;
  badge: string;
  color: string;
}

export interface Perk {
  id: string;
  title: string;
  desc: string;
  badge: string;
  color: string;
}

export type ActiveSlotId = 'M1' | 'M2' | 'Q' | 'W' | 'E' | 'R';

export interface ClassActiveSkill {
  id: string;
  name: string;
  desc: string;
  classType: PlayerClassType;
  icon: string;
  cooldown: number; // in seconds
  tideCost: number;
  damageMult: number;
  color: string;
  isUltimate?: boolean;
}

export interface StoryObjective {
  id: string;
  title: string;
  desc: string;
  type: 'collect' | 'activate' | 'kill_miniboss' | 'reach_boss';
  targetCount: number;
  currentCount: number;
  completed: boolean;
}

export interface InteractiveWorldEntity {
  id: string;
  type: 'quest_item' | 'gate' | 'lore_tablet' | 'valve';
  name: string;
  x: number;
  y: number;
  radius: number;
  color: string;
  active: boolean;
  requiresObjectiveId?: string;
  loreText?: string;
}

export interface DialogueMessage {
  speaker: string;
  portrait: string;
  text: string;
}

export interface ActInfo {
  number: number;
  title: string;
  subtitle: string;
  questTitle: string;
  questDesc: string;
  bossName: string;
  startDepth: number;
  completed: boolean;
  objectives: StoryObjective[];
  gateX?: number;
  gateY?: number;
  gateOpened?: boolean;
}

export interface Enemy {
  id: string;
  type: 'crab' | 'jelly' | 'eel' | 'angler' | 'boss';
  name: string;
  x: number;
  y: number;
  hp: number;
  maxHp: number;
  dmg: number;
  radius: number;
  speed: number;
  vx: number;
  vy: number;
  hitSquash: number;
  attackTimer: number;
  isBoss: boolean;
  bossPhase?: number;
  windupX?: number;
  windupY?: number;
  windupRadius?: number;
  windupTimer?: number;
  windupMaxTimer?: number;
  facing?: number;
}

export interface Projectile {
  id: string;
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
  damage: number;
  playerOwned: boolean;
  life: number;
  maxLife: number;
  color: string;
  homing?: boolean;
  pierceCount?: number;
  hitEnemyIds?: string[];
}

export interface Drop {
  id: string;
  x: number;
  y: number;
  item?: Item;
  shellAmount?: number;
  shardAmount?: number;
  timer: number;
}

export interface Particle {
  id: string;
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
  color: string;
  life: number;
  maxLife: number;
}

export interface Floater {
  id: string;
  x: number;
  y: number;
  text: string;
  color: string;
  isCrit: boolean;
  life: number;
  maxLife: number;
}

export interface ActChest {
  id: string;
  x: number;
  y: number;
  opened: boolean;
  animTimer: number;
}

export interface CharacterSlot {
  index: number;
  exists: boolean;
  name: string;
  playerClass: PlayerClassType;
  suitColorIndex: number;
  level: number;
  xp: number;
  xpNext: number;
  skillPoints: number;
  shells: number;
  abyssalShards: number;
  maxActUnlocked: number;
  equippedWeapon: Item | null;
  equippedArmor: Item | null;
  equippedRelic: Item | null;
  backpack: Item[];
  stash: Item[];
  unlockedSkills: string[];
  skillLoadout?: Record<ActiveSlotId, string>;
}

export type GameState = 'CHAR_SELECT' | 'CHAR_CREATE' | 'HUB' | 'EXPEDITION';

export type ActiveModal =
  | null
  | 'INVENTORY'
  | 'TALENTS'
  | 'SKILL_ASSIGN'
  | 'BLACKSMITH'
  | 'SHOP'
  | 'STASH'
  | 'SAGE'
  | 'WAYPOINT'
  | 'STORY_DIALOGUE'
  | 'GAMEOVER'
  | 'VICTORY';
