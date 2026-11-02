import {
  CharacterSlot,
  GameState,
  Item,
  Enemy,
  Projectile,
  Drop,
  Particle,
  Floater,
  ActChest,
  PlayerClassType,
  CLASSES_DATA,
  SUIT_COLORS,
  ActInfo,
} from '../types/game';
import { ALL_SKILLS, INITIAL_ACTS, RANDOM_NAMES } from './skillsData';
import { AudioHelper } from '../audio/AudioHelper';

export class GameEngine {
  // Screen & Coordinate settings
  public static readonly SCREEN_W = 960;
  public static readonly SCREEN_H = 540;
  public static readonly HUB_W = 1400;
  public static readonly HUB_H = 800;

  // Hub Station Coordinates
  public static readonly HUB_BLACKSMITH_X = 350;
  public static readonly HUB_BLACKSMITH_Y = 320;
  public static readonly HUB_MERCHANT_X = 640;
  public static readonly HUB_MERCHANT_Y = 260;
  public static readonly HUB_STASH_X = 950;
  public static readonly HUB_STASH_Y = 320;
  public static readonly HUB_SAGE_X = 480;
  public static readonly HUB_SAGE_Y = 560;
  public static readonly HUB_WAYPOINT_X = 780;
  public static readonly HUB_WAYPOINT_Y = 560;
  public static readonly HUB_TOWN_PORTAL_X = 640;
  public static readonly HUB_TOWN_PORTAL_Y = 440;

  // State
  public gameState: GameState = 'CHAR_SELECT';
  public currentSlot: number = 0;
  public slots: CharacterSlot[] = [];

  // Active Player Data
  public playerName: string = 'Palombaro';
  public playerClass: PlayerClassType = 'hunter';
  public suitColorIndex: number = 0;
  public level: number = 1;
  public xp: number = 0;
  public xpNext: number = 100;
  public skillPoints: number = 0;
  public shells: number = 100;
  public abyssalShards: number = 5;
  public maxActUnlocked: number = 1;
  public currentAct: number = 1;
  public zone: number = 1;
  public zoneName: string = 'Barriera Corallina';
  public depthMeters: number = 220;

  // Health, Tide & Potions
  public hp: number = 100;
  public tide: number = 100;
  public potions: number = 3;
  public maxPotions: number = 5;

  // Equipment & Inventories
  public equippedWeapon: Item | null = null;
  public equippedArmor: Item | null = null;
  public equippedRelic: Item | null = null;
  public backpack: Item[] = [];
  public stash: Item[] = [];
  public unlockedSkills: Set<string> = new Set();
  public acts: ActInfo[] = JSON.parse(JSON.stringify(INITIAL_ACTS));

  // Player Physics
  public playerX: number = 640;
  public playerY: number = 440;
  public playerTargetX: number = 640;
  public playerTargetY: number = 440;
  public playerMoving: boolean = false;
  public playerFacingAngle: number = 0;
  public playerRadius: number = 22;
  public playerSquash: number = 0;

  // Action Timers & Cooldowns
  public attackCooldown: number = 0;
  public waveCooldown: number = 0;
  public dashCooldown: number = 0;
  public potionCooldown: number = 0;
  public attackAnim: number = 0;
  public dashAnim: number = 0;
  public beaconTimer: number = 0;
  public beaconX: number = 0;
  public beaconY: number = 0;

  // Town Portal
  public townPortalActive: boolean = false;
  public townPortalAct: number = 1;
  public townPortalZone: number = 1;
  public townPortalPlayerX: number = 0;
  public townPortalPlayerY: number = 0;

  // Game Status
  public gameOver: boolean = false;
  public victory: boolean = false;
  public portalOpen: boolean = false;
  public portalX: number = 800;
  public portalY: number = 220;
  public portalRadius: number = 48;

  // Juice
  public trauma: number = 0;
  public hitStop: number = 0;

  // Entities
  public enemies: Enemy[] = [];
  public projectiles: Projectile[] = [];
  public drops: Drop[] = [];
  public particles: Particle[] = [];
  public floaters: Floater[] = [];
  public actChests: ActChest[] = [];

  constructor() {
    this.loadSlotsFromStorage();
  }

  // --- Persistence & Slots ---
  public loadSlotsFromStorage() {
    this.slots = [];
    for (let i = 0; i < 3; i++) {
      const raw = localStorage.getItem(`cda_slot_${i}`);
      if (raw) {
        try {
          const parsed = JSON.parse(raw);
          this.slots.push({ ...parsed, index: i, exists: true });
        } catch {
          this.slots.push(this.createEmptySlot(i));
        }
      } else {
        this.slots.push(this.createEmptySlot(i));
      }
    }

    // Default to first existing slot if present
    const firstExisting = this.slots.find((s) => s.exists);
    if (firstExisting) {
      this.currentSlot = firstExisting.index;
      this.loadSlotData(firstExisting);
    }
  }

  private createEmptySlot(index: number): CharacterSlot {
    return {
      index,
      exists: false,
      name: '',
      playerClass: 'hunter',
      suitColorIndex: 0,
      level: 1,
      xp: 0,
      xpNext: 100,
      skillPoints: 0,
      shells: 50,
      abyssalShards: 3,
      maxActUnlocked: 1,
      equippedWeapon: null,
      equippedArmor: null,
      equippedRelic: null,
      backpack: [],
      stash: [],
      unlockedSkills: [],
    };
  }

  public saveActiveSlot() {
    if (this.currentSlot < 0 || this.currentSlot >= 3) return;
    const slotData: CharacterSlot = {
      index: this.currentSlot,
      exists: true,
      name: this.playerName,
      playerClass: this.playerClass,
      suitColorIndex: this.suitColorIndex,
      level: this.level,
      xp: this.xp,
      xpNext: this.xpNext,
      skillPoints: this.skillPoints,
      shells: this.shells,
      abyssalShards: this.abyssalShards,
      maxActUnlocked: this.maxActUnlocked,
      equippedWeapon: this.equippedWeapon,
      equippedArmor: this.equippedArmor,
      equippedRelic: this.equippedRelic,
      backpack: this.backpack,
      stash: this.stash,
      unlockedSkills: Array.from(this.unlockedSkills),
    };
    this.slots[this.currentSlot] = slotData;
    localStorage.setItem(`cda_slot_${this.currentSlot}`, JSON.stringify(slotData));
  }

  public selectCharacter(slotIndex: number): boolean {
    const s = this.slots[slotIndex];
    if (!s || !s.exists) return false;
    this.currentSlot = slotIndex;
    this.loadSlotData(s);
    this.enterHub();
    return true;
  }

  public deleteCharacter(slotIndex: number): boolean {
    if (slotIndex < 0 || slotIndex >= 3) return false;
    localStorage.removeItem(`cda_slot_${slotIndex}`);
    this.slots[slotIndex] = this.createEmptySlot(slotIndex);
    AudioHelper.playClick();
    return true;
  }

  public createCharacterInSlot(
    slotIndex: number,
    name: string,
    clz: PlayerClassType,
    suitColorIndex: number
  ) {
    this.currentSlot = slotIndex;
    this.playerName = name.trim() || RANDOM_NAMES[Math.floor(Math.random() * RANDOM_NAMES.length)];
    this.playerClass = clz;
    this.suitColorIndex = suitColorIndex;
    this.level = 1;
    this.xp = 0;
    this.xpNext = 100;
    this.skillPoints = 1;
    this.shells = 50;
    this.abyssalShards = 4;
    this.maxActUnlocked = 1;
    this.unlockedSkills.clear();
    this.stash = [];
    this.backpack = [];

    // Starter equipment based on class
    this.initStarterGear(clz);

    this.saveActiveSlot();
    this.enterHub();
    AudioHelper.playLevelUp();
  }

  private initStarterGear(clz: PlayerClassType) {
    if (clz === 'hunter') {
      this.equippedWeapon = {
        id: 'start_wep_hunter',
        name: 'Arpione da Esplorazione',
        slot: 'weapon',
        rarity: 0,
        damageBonus: 15,
        defenseBonus: 0,
        healthBonus: 0,
        speedBonus: 5,
        critBonus: 0.05,
        desc: 'Un affidabile arpione a propulsione pneumatica per immersioni costiere.',
        visualVariant: 0,
        upgradeLevel: 0,
      };
      this.equippedArmor = {
        id: 'start_arm_hunter',
        name: 'Scafandro di Cuoio Marino',
        slot: 'armor',
        rarity: 0,
        damageBonus: 0,
        defenseBonus: 6,
        healthBonus: 25,
        speedBonus: 10,
        critBonus: 0,
        desc: 'Tessuto leggero e flessibile trattato con grasso di balena.',
        visualVariant: 0,
        upgradeLevel: 0,
      };
    } else if (clz === 'juggernaut') {
      this.equippedWeapon = {
        id: 'start_wep_jug',
        name: 'Ancora Zavorrata',
        slot: 'weapon',
        rarity: 0,
        damageBonus: 22,
        defenseBonus: 3,
        healthBonus: 0,
        speedBonus: -5,
        critBonus: 0.02,
        desc: 'Una pesante ancora da chiatta riadattata per colpi devastanti in mischia.',
        visualVariant: 1,
        upgradeLevel: 0,
      };
      this.equippedArmor = {
        id: 'start_arm_jug',
        name: 'Campana Imbullonata',
        slot: 'armor',
        rarity: 0,
        damageBonus: 0,
        defenseBonus: 12,
        healthBonus: 50,
        speedBonus: -8,
        critBonus: 0,
        desc: 'Spesse lastre di ferro rivettate per sopportare la schiacciante pressione abissale.',
        visualVariant: 1,
        upgradeLevel: 0,
      };
    } else {
      this.equippedWeapon = {
        id: 'start_wep_mystic',
        name: 'Bacchetta di Corallo Vivo',
        slot: 'weapon',
        rarity: 0,
        damageBonus: 18,
        defenseBonus: 0,
        healthBonus: 0,
        speedBonus: 0,
        critBonus: 0.06,
        desc: 'Canalizza l\'energia delle maree in sfere pulsanti a ricerca automatica.',
        visualVariant: 2,
        upgradeLevel: 0,
      };
      this.equippedArmor = {
        id: 'start_arm_mystic',
        name: 'Tonaca della Marea',
        slot: 'armor',
        rarity: 0,
        damageBonus: 0,
        defenseBonus: 5,
        healthBonus: 20,
        speedBonus: 8,
        critBonus: 0.03,
        desc: 'Intessuta di alghe luminescenti e perle runiche protettive.',
        visualVariant: 2,
        upgradeLevel: 0,
      };
    }

    this.equippedRelic = {
      id: 'start_relic',
      name: 'Amuleto del Nautilus',
      slot: 'relic',
      rarity: 0,
      damageBonus: 2,
      defenseBonus: 2,
      healthBonus: 10,
      speedBonus: 6,
      critBonus: 0.03,
      desc: 'Una conchiglia fossile che vibra in sintonia con le correnti marine.',
      visualVariant: 0,
      upgradeLevel: 0,
    };
  }

  private loadSlotData(slot: CharacterSlot) {
    this.playerName = slot.name;
    this.playerClass = slot.playerClass;
    this.suitColorIndex = slot.suitColorIndex;
    this.level = slot.level;
    this.xp = slot.xp;
    this.xpNext = slot.xpNext;
    this.skillPoints = slot.skillPoints;
    this.shells = slot.shells;
    this.abyssalShards = slot.abyssalShards;
    this.maxActUnlocked = slot.maxActUnlocked || 1;
    this.equippedWeapon = slot.equippedWeapon;
    this.equippedArmor = slot.equippedArmor;
    this.equippedRelic = slot.equippedRelic;
    this.backpack = slot.backpack || [];
    this.stash = slot.stash || [];
    this.unlockedSkills = new Set(slot.unlockedSkills || []);

    // Sync acts completion
    for (let i = 0; i < this.acts.length; i++) {
      this.acts[i].completed = i + 1 < this.maxActUnlocked;
    }
  }

  // --- Stats Calculation ---
  public getMaxHp(): number {
    const base = CLASSES_DATA[this.playerClass].baseHp;
    const armorBonus = this.equippedArmor ? this.equippedArmor.healthBonus : 0;
    const relicBonus = this.equippedRelic ? this.equippedRelic.healthBonus : 0;
    const levelBonus = (this.level - 1) * 12;
    const jugPerk = this.hasSkill('jug_armor') ? 35 : 0;
    return base + armorBonus + relicBonus + levelBonus + jugPerk;
  }

  public getMaxTide(): number {
    let max = 100;
    if (this.hasSkill('mys_tideflow')) max += 40;
    return max;
  }

  public getAttackDamage(): number {
    const base = CLASSES_DATA[this.playerClass].baseDamage;
    const wepBonus = this.equippedWeapon ? this.equippedWeapon.damageBonus : 0;
    const relicBonus = this.equippedRelic ? this.equippedRelic.damageBonus : 0;
    const levelBonus = (this.level - 1) * 3;
    let total = base + wepBonus + relicBonus + levelBonus;
    if (this.hasSkill('jug_titan')) total = Math.round(total * 1.35);
    return total;
  }

  public getDefense(): number {
    const base = CLASSES_DATA[this.playerClass].baseDefense;
    const armorBonus = this.equippedArmor ? this.equippedArmor.defenseBonus : 0;
    const relicBonus = this.equippedRelic ? this.equippedRelic.defenseBonus : 0;
    const jugPerk = this.hasSkill('jug_armor') ? 8 : 0;
    return base + armorBonus + relicBonus + jugPerk;
  }

  public getMoveSpeed(): number {
    const base = CLASSES_DATA[this.playerClass].baseSpeed;
    const armorSpd = this.equippedArmor ? this.equippedArmor.speedBonus : 0;
    const wepSpd = this.equippedWeapon ? this.equippedWeapon.speedBonus : 0;
    const relicSpd = this.equippedRelic ? this.equippedRelic.speedBonus : 0;
    let speed = base + armorSpd + wepSpd + relicSpd;
    if (this.hasSkill('hunt_surge')) speed += 50;
    return speed;
  }

  public getCritChance(): number {
    let crit = 0.05;
    if (this.equippedWeapon) crit += this.equippedWeapon.critBonus;
    if (this.equippedRelic) crit += this.equippedRelic.critBonus;
    if (this.hasSkill('hunt_crit')) crit += 0.15;
    return Math.min(0.75, crit);
  }

  public hasSkill(skillId: string): boolean {
    return this.unlockedSkills.has(skillId);
  }

  public canUnlockSkill(skillId: string): boolean {
    if (this.skillPoints <= 0) return false;
    if (this.hasSkill(skillId)) return false;
    const skill = ALL_SKILLS.find((s) => s.id === skillId);
    if (!skill || skill.classType !== this.playerClass) return false;
    if (skill.requiredSkillId && !this.hasSkill(skill.requiredSkillId)) return false;
    return true;
  }

  public unlockSkill(skillId: string): boolean {
    if (!this.canUnlockSkill(skillId)) return false;
    this.skillPoints--;
    this.unlockedSkills.add(skillId);
    AudioHelper.playLevelUp();
    this.saveActiveSlot();
    this.addFloater(this.playerX, this.playerY - 45, 'ABILITÀ SBLOCCATA!', '#38bdf8', true);
    return true;
  }

  public respecSkills(): boolean {
    if (this.unlockedSkills.size === 0) return false;
    if (this.shells < 25) return false;
    this.shells -= 25;
    this.skillPoints += this.unlockedSkills.size;
    this.unlockedSkills.clear();
    AudioHelper.playWave();
    this.saveActiveSlot();
    this.addFloater(this.playerX, this.playerY - 45, 'TALENTI AZZERATI (-25 🐚)', '#f59e0b', true);
    return true;
  }

  // --- Hub & Zone Navigation ---
  public getMapWidth(): number {
    if (this.gameState === 'HUB') return GameEngine.HUB_W;
    switch (this.currentAct) {
      case 1:
        return 2400;
      case 2:
        return 2600;
      case 3:
        return 2800;
      case 4:
      default:
        return 2400;
    }
  }

  public getMapHeight(): number {
    if (this.gameState === 'HUB') return GameEngine.HUB_H;
    switch (this.currentAct) {
      case 1:
        return 1400;
      case 2:
        return 1500;
      case 3:
        return 1600;
      case 4:
      default:
        return 1400;
    }
  }

  public enterHub() {
    this.gameState = 'HUB';
    this.enemies = [];
    this.drops = [];
    this.projectiles = [];
    this.floaters = [];
    this.particles = [];
    this.actChests = [];
    this.portalOpen = false;
    this.gameOver = false;
    this.victory = false;

    this.playerX = 640;
    this.playerY = 440;
    this.playerTargetX = this.playerX;
    this.playerTargetY = this.playerY;
    this.playerMoving = false;
    this.beaconTimer = 0;

    this.hp = this.getMaxHp();
    this.tide = this.getMaxTide();
    this.attackCooldown = 0;
    this.waveCooldown = 0;
    this.dashCooldown = 0;
    AudioHelper.startAmbient();
  }

  public startActExpedition(actNum: number) {
    if (actNum < 1) actNum = 1;
    if (actNum > this.maxActUnlocked) actNum = this.maxActUnlocked;
    this.currentAct = actNum;
    this.loadZone(actNum);
    AudioHelper.playWave();
  }

  public loadZone(z: number) {
    this.zone = z;
    this.gameState = 'EXPEDITION';
    this.currentAct = Math.min(4, Math.max(1, z));
    this.enemies = [];
    this.drops = [];
    this.projectiles = [];
    this.floaters = [];
    this.particles = [];
    this.actChests = [];
    this.portalOpen = false;
    this.gameOver = false;
    this.victory = false;

    const mapW = this.getMapWidth();
    const mapH = this.getMapHeight();

    this.playerX = mapW / 2;
    this.playerY = mapH - 180;
    this.playerTargetX = this.playerX;
    this.playerTargetY = this.playerY;
    this.playerMoving = false;
    this.beaconTimer = 0;

    this.portalX = mapW / 2;
    this.portalY = 220;

    if (this.zone === 1) {
      this.zoneName = 'Barriera Corallina';
      this.depthMeters = 220;
      this.actChests.push({ id: 'chest_1', x: mapW / 2, y: 750, opened: false, animTimer: 0 });

      // Sector 1
      this.spawnEnemy('crab', 'Granchio di Scoglio', mapW / 2 - 400, 1080, 80, 12, 22, 75);
      this.spawnEnemy('crab', 'Granchio di Scoglio', mapW / 2 + 400, 1080, 80, 12, 22, 75);
      this.spawnEnemy('jelly', 'Medusa Elettrica', mapW / 2, 980, 70, 14, 20, 55);

      // Sector 2
      this.spawnEnemy('crab', 'Granchio Tagliatore', mapW / 2 - 320, 720, 95, 15, 23, 80);
      this.spawnEnemy('crab', 'Granchio Tagliatore', mapW / 2 + 320, 720, 95, 15, 23, 80);
      this.spawnEnemy('jelly', 'Medusa Fluttuante', mapW / 2 - 520, 620, 75, 15, 20, 60);
      this.spawnEnemy('jelly', 'Medusa Fluttuante', mapW / 2 + 520, 620, 75, 15, 20, 60);

      // Sector 3 (Boss)
      this.spawnEnemy('crab', 'Granchio Corazzato', mapW / 2 - 240, 380, 130, 16, 24, 70);
      this.spawnEnemy('crab', 'Granchio Corazzato', mapW / 2 + 240, 380, 130, 16, 24, 70);
      this.spawnBoss('Granchio Gigante Corazzato', mapW / 2, 320, 550, 24, 45, 65);
    } else if (this.zone === 2) {
      this.zoneName = 'Fossa delle Tenebre';
      this.depthMeters = 680;
      this.actChests.push({ id: 'chest_2', x: mapW / 2, y: 800, opened: false, animTimer: 0 });

      // Sector 1
      this.spawnEnemy('crab', 'Granchio Abissale', mapW / 2 - 450, 1150, 160, 22, 24, 80);
      this.spawnEnemy('crab', 'Granchio Abissale', mapW / 2 + 450, 1150, 160, 22, 24, 80);
      this.spawnEnemy('jelly', 'Medusa dei Fulmini', mapW / 2, 1020, 120, 24, 22, 65);

      // Sector 2
      this.spawnEnemy('eel', 'Murena dei Fondali', mapW / 2 - 350, 780, 150, 28, 22, 115);
      this.spawnEnemy('eel', 'Murena dei Fondali', mapW / 2 + 350, 780, 150, 28, 22, 115);
      this.spawnEnemy('jelly', 'Medusa dei Fulmini', mapW / 2 - 620, 650, 130, 25, 22, 65);
      this.spawnEnemy('jelly', 'Medusa dei Fulmini', mapW / 2 + 620, 650, 130, 25, 22, 65);

      // Sector 3 (Boss)
      this.spawnEnemy('eel', 'Murena Zannuta', mapW / 2 - 260, 400, 170, 28, 22, 120);
      this.spawnEnemy('eel', 'Murena Zannuta', mapW / 2 + 260, 400, 170, 28, 22, 120);
      this.spawnBoss('Murena Zannuta Primordiale', mapW / 2, 320, 750, 32, 50, 125);
    } else if (this.zone === 3) {
      this.zoneName = 'Dorsale Vulcanica';
      this.depthMeters = 1450;
      this.actChests.push({ id: 'chest_3', x: mapW / 2, y: 850, opened: false, animTimer: 0 });

      // Sector 1
      this.spawnEnemy('crab', 'Granchio Vulcanico', mapW / 2 - 500, 1220, 220, 32, 26, 85);
      this.spawnEnemy('crab', 'Granchio Vulcanico', mapW / 2 + 500, 1220, 220, 32, 26, 85);
      this.spawnEnemy('eel', 'Murena di Fuoco', mapW / 2, 1120, 180, 32, 24, 125);

      // Sector 2
      this.spawnEnemy('angler', 'Pesce Lanterna Cieco', mapW / 2 - 400, 820, 200, 34, 25, 95);
      this.spawnEnemy('angler', 'Predatore Fosforescente', mapW / 2 + 400, 820, 210, 35, 25, 95);
      this.spawnEnemy('jelly', 'Medusa Radiante', mapW / 2, 720, 170, 30, 22, 70);

      // Sector 3 (Boss)
      this.spawnEnemy('crab', 'Granchio Vulcanico', mapW / 2 - 300, 420, 240, 32, 26, 85);
      this.spawnEnemy('crab', 'Granchio Vulcanico', mapW / 2 + 300, 420, 240, 32, 26, 85);
      this.spawnBoss('Rana Pescatrice Abissale', mapW / 2, 340, 1100, 38, 55, 90);
    } else if (this.zone === 4) {
      this.zoneName = 'Santuario del Kraken';
      this.depthMeters = 2400;
      this.actChests.push({ id: 'chest_4', x: mapW / 2, y: 700, opened: false, animTimer: 0 });
      this.spawnBoss('Leviatano Primordiale', mapW / 2, 360, 1800, 44, 65, 50);
    } else {
      // Endless Dive (Zone 5+)
      const cycle = this.zone - 4;
      this.zoneName = `Abisso Senza Fine (${cycle})`;
      this.depthMeters = 2400 + cycle * 650;
      const scaleHp = 1 + (cycle * 2) / 10;
      const scaleDmg = cycle * 3;
      this.actChests.push({ id: `chest_${this.zone}`, x: mapW / 2, y: 750, opened: false, animTimer: 0 });

      this.spawnEnemy('angler', 'Fosforo Ancestrale', mapW / 2 - 350, 950, Math.round(260 * scaleHp), 36 + scaleDmg, 28, 105);
      this.spawnEnemy('angler', 'Abominio delle Profondità', mapW / 2 + 350, 950, Math.round(300 * scaleHp), 40 + scaleDmg, 30, 110);
      this.spawnEnemy('crab', 'Colosso di Basalto', mapW / 2, 650, Math.round(340 * scaleHp), 38 + scaleDmg, 30, 90);
      this.spawnEnemy('eel', 'Leviatano Minore', mapW / 2, 380, Math.round(360 * scaleHp), 42 + scaleDmg, 28, 130);
    }

    this.tide = this.getMaxTide();
    this.hp = this.getMaxHp();
  }

  private spawnEnemy(
    type: 'crab' | 'jelly' | 'eel' | 'angler',
    name: string,
    x: number,
    y: number,
    hp: number,
    dmg: number,
    radius: number,
    speed: number
  ) {
    this.enemies.push({
      id: `e_${Date.now()}_${Math.random()}`,
      type,
      name,
      x,
      y,
      hp,
      maxHp: hp,
      dmg,
      radius,
      speed,
      vx: 0,
      vy: 0,
      hitSquash: 0,
      attackTimer: Math.random() * 2,
      isBoss: false,
    });
  }

  private spawnBoss(name: string, x: number, y: number, hp: number, dmg: number, radius: number, speed: number) {
    this.enemies.push({
      id: `boss_${Date.now()}`,
      type: 'boss',
      name,
      x,
      y,
      hp,
      maxHp: hp,
      dmg,
      radius,
      speed,
      vx: 0,
      vy: 0,
      hitSquash: 0,
      attackTimer: 2.0,
      isBoss: true,
      bossPhase: 1,
      windupX: x,
      windupY: y,
      windupRadius: 0,
      windupTimer: 0,
      windupMaxTimer: 1.2,
    });
  }

  // --- Town Portal ---
  public openTownPortal(): boolean {
    if (this.gameState !== 'EXPEDITION' || this.gameOver || this.victory) return false;
    this.townPortalActive = true;
    this.townPortalAct = this.currentAct;
    this.townPortalZone = this.zone;
    this.townPortalPlayerX = this.playerX;
    this.townPortalPlayerY = this.playerY;
    this.enterHub();
    this.addFloater(this.playerX, this.playerY - 45, 'PORTALE CITTADINO APERTO', '#38bdf8', true);
    AudioHelper.playWave();
    return true;
  }

  public enterTownPortalToDungeon(): boolean {
    if (this.gameState !== 'HUB' || !this.townPortalActive) return false;
    this.currentAct = this.townPortalAct;
    this.loadZone(this.townPortalZone);
    this.playerX = this.townPortalPlayerX;
    this.playerY = this.townPortalPlayerY;
    this.playerTargetX = this.playerX;
    this.playerTargetY = this.playerY;
    this.townPortalActive = false;
    this.addFloater(this.playerX, this.playerY - 45, 'RITORNO NELL\'ABISSO', '#38bdf8', true);
    AudioHelper.playWave();
    return true;
  }

  // --- Controls & Actions ---
  public setPlayerTarget(x: number, y: number) {
    const mapW = this.getMapWidth();
    const mapH = this.getMapHeight();
    this.playerTargetX = Math.max(30, Math.min(mapW - 30, x));
    this.playerTargetY = Math.max(30, Math.min(mapH - 30, y));
    this.playerMoving = true;
    this.beaconX = this.playerTargetX;
    this.beaconY = this.playerTargetY;
    this.beaconTimer = 0.6;
  }

  public performAttack(targetWorldX: number = -1, targetWorldY: number = -1): boolean {
    if (this.gameState === 'HUB' || this.attackCooldown > 0 || this.gameOver || this.victory) return false;

    let effectiveRate = 0.42;
    if (this.hasSkill('hunt_rapid')) effectiveRate = 0.28;
    this.attackCooldown = effectiveRate;
    this.attackAnim = 1.0;
    this.playerSquash = 0.4;

    // Auto aim if no target coordinate
    if (targetWorldX < 0 && targetWorldY < 0) {
      let closest: Enemy | null = null;
      let minDistSq = 600 * 600;
      for (const e of this.enemies) {
        if (e.hp <= 0) continue;
        const dx = e.x - this.playerX;
        const dy = e.y - this.playerY;
        const d2 = dx * dx + dy * dy;
        if (d2 < minDistSq) {
          minDistSq = d2;
          closest = e;
        }
      }
      if (closest) {
        targetWorldX = closest.x;
        targetWorldY = closest.y;
      } else {
        targetWorldX = this.playerX + Math.cos(this.playerFacingAngle) * 150;
        targetWorldY = this.playerY + Math.sin(this.playerFacingAngle) * 150;
      }
    }

    const dx = targetWorldX - this.playerX;
    const dy = targetWorldY - this.playerY;
    if (dx !== 0 || dy !== 0) {
      this.playerFacingAngle = Math.atan2(dy, dx);
    }
    const angle = this.playerFacingAngle;
    let dmg = this.getAttackDamage();
    const isCrit = Math.random() < this.getCritChance();
    const critMult = this.hasSkill('hunt_crit') ? 2.4 : 1.8;
    if (isCrit) dmg = Math.round(dmg * critMult);

    if (this.playerClass === 'juggernaut') {
      // Melee Anchor Cleave
      let cleaveDmg = Math.round(dmg * 1.35 + this.getDefense() * 0.5);
      if (this.hasSkill('jug_slam')) cleaveDmg = Math.round(cleaveDmg * 1.3);

      let hitCount = 0;
      for (const e of this.enemies) {
        if (e.hp <= 0) continue;
        const edx = e.x - this.playerX;
        const edy = e.y - this.playerY;
        const dist = Math.sqrt(edx * edx + edy * edy);
        if (dist <= 140 + e.radius) {
          const eAngle = Math.atan2(edy, edx);
          const diff = Math.abs(this.normalizeAngle(eAngle - this.playerFacingAngle));
          if (diff <= 1.35 || dist < 45) {
            this.damageEnemy(e, cleaveDmg, e.x, e.y);
            if (dist > 1) {
              e.x += (edx / dist) * 65;
              e.y += (edy / dist) * 65;
            }
            hitCount++;
          }
        }
      }

      for (let i = -4; i <= 4; i++) {
        const a = this.playerFacingAngle + i * 0.25;
        const px = this.playerX + Math.cos(a) * 60;
        const py = this.playerY + Math.sin(a) * 60;
        this.particles.push({
          id: `p_${Math.random()}`,
          x: px,
          y: py,
          vx: Math.cos(a) * 160,
          vy: Math.sin(a) * 160,
          radius: 6,
          color: '#f59e0b',
          life: 0.25,
          maxLife: 0.25,
        });
      }
      this.trauma = Math.min(1.0, this.trauma + 0.25);
      if (hitCount > 0) AudioHelper.playHeavyHit();
      else AudioHelper.playHit();
      return true;
    } else if (this.playerClass === 'mystic') {
      // Homing Tidal Bolt
      const speed = 460;
      const vx = Math.cos(angle) * speed;
      const vy = Math.sin(angle) * speed;
      let pColor = '#38bdf8';
      if (this.equippedWeapon && this.equippedWeapon.rarity === 3) pColor = '#c084fc';

      this.projectiles.push({
        id: `proj_${Date.now()}_${Math.random()}`,
        x: this.playerX,
        y: this.playerY,
        vx,
        vy,
        radius: 10,
        damage: dmg,
        playerOwned: true,
        life: 0.9,
        maxLife: 0.9,
        color: pColor,
        homing: true,
        hitEnemyIds: [],
      });

      this.tide = Math.min(this.getMaxTide(), this.tide + 3);
      AudioHelper.playWave();
      return true;
    }

    // Hunter: Piercing Harpoon
    const speed = this.hasSkill('hunt_rapid') ? 640 : 520;
    const vx = Math.cos(angle) * speed;
    const vy = Math.sin(angle) * speed;
    let pColor = '#38bdf8';
    if (this.equippedWeapon?.rarity === 2) pColor = '#fbbf24';
    if (this.equippedWeapon?.rarity === 3) pColor = '#c084fc';

    const pCount = this.hasSkill('hunt_pierce') ? 3 : 1;

    this.projectiles.push({
      id: `proj_${Date.now()}_${Math.random()}`,
      x: this.playerX,
      y: this.playerY,
      vx,
      vy,
      radius: 8,
      damage: dmg,
      playerOwned: true,
      life: 0.5,
      maxLife: 0.5,
      color: pColor,
      pierceCount: pCount,
      hitEnemyIds: [],
    });

    if (this.hasSkill('hunt_multishot')) {
      for (const offset of [-0.24, 0.24]) {
        const a = angle + offset;
        this.projectiles.push({
          id: `proj_${Date.now()}_${Math.random()}`,
          x: this.playerX,
          y: this.playerY,
          vx: Math.cos(a) * speed,
          vy: Math.sin(a) * speed,
          radius: 8,
          damage: Math.round(dmg * 0.8),
          playerOwned: true,
          life: 0.5,
          maxLife: 0.5,
          color: pColor,
          pierceCount: pCount,
          hitEnemyIds: [],
        });
      }
    }

    AudioHelper.playHit();
    return true;
  }

  public performWave(): boolean {
    if (this.waveCooldown > 0 || this.tide < 30 || this.gameOver || this.victory) return false;
    this.tide -= 30;
    this.waveCooldown = 6.0;
    this.trauma = Math.min(1.0, this.trauma + 0.45);

    if (this.hasSkill('mys_vortex')) {
      // Pull enemies toward player
      for (const e of this.enemies) {
        const dx = this.playerX - e.x;
        const dy = this.playerY - e.y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > 10 && dist < 380) {
          e.x += (dx / dist) * 55;
          e.y += (dy / dist) * 55;
        }
      }
    }

    const waveCount = this.hasSkill('jug_shockwave') ? 20 : 14;
    const waveSpeed = this.hasSkill('jug_shockwave') ? 460 : 380;
    const dmg = Math.round(this.getAttackDamage() * (this.hasSkill('mys_vortex') ? 1.95 : 1.6));

    for (let i = 0; i < waveCount; i++) {
      const angle = (2.0 * Math.PI / waveCount) * i;
      this.projectiles.push({
        id: `p_wave_${Date.now()}_${i}`,
        x: this.playerX,
        y: this.playerY,
        vx: Math.cos(angle) * waveSpeed,
        vy: Math.sin(angle) * waveSpeed,
        radius: 11,
        damage: dmg,
        playerOwned: true,
        life: 0.65,
        maxLife: 0.65,
        color: '#0284c7',
        hitEnemyIds: [],
      });
    }

    if (this.hasSkill('mys_cataclysm')) {
      for (let i = 0; i < 12; i++) {
        const angle = (Math.PI * 2 / 12) * i;
        this.projectiles.push({
          id: `p_cata_${Date.now()}_${i}`,
          x: this.playerX,
          y: this.playerY,
          vx: Math.cos(angle) * 310,
          vy: Math.sin(angle) * 310,
          radius: 14,
          damage: Math.round(dmg * 1.3),
          playerOwned: true,
          life: 0.8,
          maxLife: 0.8,
          color: '#c084fc',
          hitEnemyIds: [],
        });
      }
    }

    for (let i = 0; i < 24; i++) {
      const angle = Math.random() * Math.PI * 2;
      const spd = 60 + Math.random() * 220;
      this.particles.push({
        id: `pw_${Math.random()}`,
        x: this.playerX,
        y: this.playerY,
        vx: Math.cos(angle) * spd,
        vy: Math.sin(angle) * spd,
        radius: 6,
        color: '#38bdf8',
        life: 0.6,
        maxLife: 0.6,
      });
    }

    AudioHelper.playWave();
    return true;
  }

  public performDash(): boolean {
    if (this.dashCooldown > 0 || this.gameOver || this.victory) return false;
    let cd = 2.5;
    if (this.hasSkill('hunt_surge')) cd = 1.85;
    this.dashCooldown = cd;
    this.dashAnim = 1.0;

    const dx = Math.cos(this.playerFacingAngle);
    const dy = Math.sin(this.playerFacingAngle);
    this.playerTargetX = this.playerX + dx * 180;
    this.playerTargetY = this.playerY + dy * 180;
    this.playerMoving = true;

    if (this.hasSkill('mys_lightning')) {
      for (const e of this.enemies) {
        const edx = e.x - this.playerX;
        const edy = e.y - this.playerY;
        if (edx * edx + edy * edy < 135 * 135) {
          this.damageEnemy(e, Math.round(this.getAttackDamage() * 0.85), e.x, e.y);
        }
      }
      for (let i = 0; i < 20; i++) {
        this.particles.push({
          id: `pe_${Math.random()}`,
          x: this.playerX,
          y: this.playerY,
          vx: Math.random() * 160 - 80,
          vy: Math.random() * 160 - 80,
          radius: 5,
          color: '#fcd34d',
          life: 0.5,
          maxLife: 0.5,
        });
      }
    }

    if (this.hasSkill('hunt_dash_cut')) {
      for (const e of this.enemies) {
        const edx = e.x - this.playerX;
        const edy = e.y - this.playerY;
        if (edx * edx + edy * edy < 110 * 110) {
          this.damageEnemy(e, Math.round(this.getAttackDamage() * 0.75), e.x, e.y);
        }
      }
    }

    AudioHelper.playDash();
    return true;
  }

  public usePotion(): boolean {
    if (this.potions <= 0 || this.hp >= this.getMaxHp() || this.gameOver || this.victory) return false;
    this.potions--;
    let heal = Math.round(this.getMaxHp() * 0.5);
    if (this.hasSkill('mys_alchemy')) heal = Math.round(this.getMaxHp() * 0.75);

    this.hp = Math.min(this.getMaxHp(), this.hp + heal);
    this.addFloater(this.playerX, this.playerY - 45, `+${heal} PV`, '#4ade80', true);
    AudioHelper.playPotion();
    return true;
  }

  public damagePlayer(rawDamage: number, _fromX: number, _fromY: number) {
    if (this.gameOver || this.victory || this.dashAnim > 0.4) return;
    const def = this.getDefense();
    let taken = Math.max(1, rawDamage - Math.floor(def / 2));

    if (this.hasSkill('jug_unbreakable') && this.hp < this.getMaxHp() * 0.35) {
      taken = Math.max(1, Math.round(taken * 0.5));
    }

    this.hp -= taken;
    this.trauma = Math.min(1.0, this.trauma + 0.45);
    this.playerSquash = 0.8;

    if (this.hasSkill('jug_reflect')) {
      const reflected = Math.max(1, Math.round(taken * 0.4));
      for (const e of this.enemies) {
        const dx = e.x - this.playerX;
        const dy = e.y - this.playerY;
        if (dx * dx + dy * dy < 260 * 260) {
          e.hp -= reflected;
          this.addFloater(e.x, e.y - 30, `SPINE ${reflected}`, '#fb923c', false);
        }
      }
    }

    this.addFloater(this.playerX, this.playerY - 35, `-${taken}`, '#ef4444', false);
    AudioHelper.playHeavyHit();

    if (this.hp <= 0) {
      this.hp = 0;
      this.gameOver = true;
      AudioHelper.playHeavyHit();
    }
  }

  public damageEnemy(e: Enemy, damage: number, hitX: number, hitY: number) {
    e.hp -= damage;
    e.hitSquash = 0.6;

    if (this.hasSkill('mys_siphon')) {
      const heal = 4;
      this.hp = Math.min(this.getMaxHp(), this.hp + heal);
      this.addFloater(this.playerX, this.playerY - 30, `+${heal} PV`, '#a78bfa', false);
    }

    const kx = e.x - hitX;
    const ky = e.y - hitY;
    const len = Math.sqrt(kx * kx + ky * ky);
    if (len > 0.001 && e.type !== 'boss') {
      e.x += (kx / len) * 20;
      e.y += (ky / len) * 20;
    }

    const isCrit = damage > this.getAttackDamage() * 1.5;
    this.addFloater(e.x, e.y - 25, `${damage}`, isCrit ? '#fbbf24' : '#ffffff', isCrit);

    if (isCrit) {
      this.trauma = Math.min(1.0, this.trauma + 0.25);
      this.hitStop = 0.045;
      AudioHelper.playHeavyHit();
    } else {
      AudioHelper.playHit();
    }

    for (let i = 0; i < 8; i++) {
      this.particles.push({
        id: `pb_${Math.random()}`,
        x: hitX,
        y: hitY,
        vx: Math.random() * 80 - 40,
        vy: Math.random() * 80 - 40,
        radius: 4,
        color: '#f43f5e',
        life: 0.4,
        maxLife: 0.4,
      });
    }

    if (e.hp <= 0) {
      this.killEnemy(e);
    }
  }

  private killEnemy(e: Enemy) {
    e.hp = 0;
    let gainedXp = 20;
    if (e.type === 'eel') gainedXp = 35;
    else if (e.type === 'angler') gainedXp = 48;
    else if (e.type === 'boss') gainedXp = 160;
    this.addXp(gainedXp);

    if (this.hasSkill('mys_siphon')) {
      const heal = 12;
      this.hp = Math.min(this.getMaxHp(), this.hp + heal);
      this.addFloater(this.playerX, this.playerY - 30, `+${heal} PV`, '#a78bfa', true);
    }

    const count = e.type === 'boss' ? 40 : 16;
    for (let i = 0; i < count; i++) {
      this.particles.push({
        id: `pk_${Math.random()}`,
        x: e.x,
        y: e.y,
        vx: Math.random() * 160 - 80,
        vy: Math.random() * 160 - 80,
        radius: 5 + Math.random() * 5,
        color: '#38bdf8',
        life: 0.8,
        maxLife: 0.8,
      });
    }

    // Always drop gold shells
    const shellAmount = e.type === 'boss' ? 30 + Math.floor(Math.random() * 20) : 4 + Math.floor(Math.random() * 6);
    this.drops.push({
      id: `d_sh_${Date.now()}_${Math.random()}`,
      x: e.x + (Math.random() * 24 - 12),
      y: e.y + (Math.random() * 24 - 12),
      shellAmount,
      timer: 0,
    });

    // Roll item loot drop
    const dropChance = e.type === 'boss' ? 1.0 : e.type === 'angler' ? 0.65 : 0.4;
    if (Math.random() <= dropChance) {
      const droppedItem = this.generateRandomItem(this.zone, e.type === 'boss');
      this.drops.push({
        id: `d_item_${Date.now()}_${Math.random()}`,
        x: e.x,
        y: e.y,
        item: droppedItem,
        timer: 0,
      });
      AudioHelper.playLoot(droppedItem.rarity);
    }
  }

  public addXp(amount: number) {
    this.xp += amount;
    this.addFloater(this.playerX, this.playerY - 55, `+${amount} ESP`, '#60a5fa', false);

    while (this.xp >= this.xpNext) {
      this.xp -= this.xpNext;
      this.level++;
      this.skillPoints++;
      this.xpNext = Math.round(this.xpNext * 1.45);
      this.hp = this.getMaxHp();
      this.addFloater(this.playerX, this.playerY - 75, `LIVELLO ${this.level}! +1 PUNTO ABILITÀ`, '#fcd34d', true);
      AudioHelper.playLevelUp();
      this.saveActiveSlot();
    }
  }

  public openActChest(chest: ActChest) {
    if (chest.opened) return;
    chest.opened = true;
    chest.animTimer = 1.0;

    const rewardedItem = this.generateRandomItem(this.zone, true);
    this.drops.push({
      id: `d_chest_item_${Date.now()}`,
      x: chest.x,
      y: chest.y - 20,
      item: rewardedItem,
      timer: 0,
    });

    this.drops.push({
      id: `d_chest_sh_${Date.now()}`,
      x: chest.x + 35,
      y: chest.y,
      shellAmount: 35 + Math.floor(Math.random() * 25),
      timer: 0,
    });

    this.drops.push({
      id: `d_chest_shd_${Date.now()}`,
      x: chest.x - 35,
      y: chest.y,
      shardAmount: 3 + Math.floor(Math.random() * 3),
      timer: 0,
    });

    this.addFloater(chest.x, chest.y - 45, 'FORZIERE SOMMERSO APERTO!', '#fbbf24', true);
    AudioHelper.playLoot(rewardedItem.rarity);
  }

  public generateRandomItem(zoneLevel: number, isBoss: boolean): Item {
    const roll = Math.floor(Math.random() * 100);
    let rarity = 0; // Common
    if (isBoss) {
      if (roll < 35) rarity = 3; // Abyssal
      else if (roll < 80) rarity = 2; // Rare
      else rarity = 1; // Magic
    } else {
      if (zoneLevel === 1) {
        if (roll < 6) rarity = 2;
        else if (roll < 35) rarity = 1;
        else rarity = 0;
      } else if (zoneLevel === 2) {
        if (roll < 12) rarity = 3;
        else if (roll < 40) rarity = 2;
        else if (roll < 75) rarity = 1;
        else rarity = 0;
      } else {
        if (roll < 25) rarity = 3;
        else if (roll < 65) rarity = 2;
        else rarity = 1;
      }
    }

    let setName: string | undefined = undefined;
    if (rarity >= 1) {
      const setRoll = Math.random();
      if (setRoll < 0.38) setName = 'Corallo';
      else if (setRoll < 0.72) setName = 'Marea';
      else setName = 'Abisso';
    }

    const slotTypes: ('weapon' | 'armor' | 'relic')[] = ['weapon', 'armor', 'relic'];
    const slot = slotTypes[Math.floor(Math.random() * 3)];
    const id = `item_${Date.now()}_${Math.floor(Math.random() * 1000)}`;
    const variant = Math.floor(Math.random() * 3);

    if (slot === 'weapon') {
      const names = [
        'Arpione di Cobalto', 'Tridente della Barriera', 'Lama degli Abissi',
        'Falcione Idraulico', 'Spiedo Corallino', 'Squartatore Oceanico'
      ];
      const name = names[Math.floor(Math.random() * names.length)];
      const dmg = 18 + zoneLevel * 8 + rarity * 12 + Math.floor(Math.random() * 6);
      const crit = 0.05 + rarity * 0.04;
      return {
        id,
        name,
        slot,
        rarity,
        damageBonus: dmg,
        defenseBonus: 0,
        healthBonus: 0,
        speedBonus: 0,
        critBonus: crit,
        desc: 'Un\'arma marina pesante che penetra le corazze nemiche.',
        visualVariant: variant,
        setName,
        upgradeLevel: 0,
      };
    } else if (slot === 'armor') {
      const names = [
        'Scafandro Rinforzato', 'Corazza di Chitina', 'Piastra del Nautilus',
        'Carapace Abissale', 'Manto dei Fondali', 'Guscio d\'Acciaio'
      ];
      const name = names[Math.floor(Math.random() * names.length)];
      const def = 6 + zoneLevel * 4 + rarity * 5 + Math.floor(Math.random() * 3);
      const hp = 30 + zoneLevel * 25 + rarity * 35 + Math.floor(Math.random() * 20);
      return {
        id,
        name,
        slot,
        rarity,
        damageBonus: 0,
        defenseBonus: def,
        healthBonus: hp,
        speedBonus: 0,
        critBonus: 0,
        desc: 'Resiste alla tremenda pressione idrostatica dell\'abisso.',
        visualVariant: variant,
        setName,
        upgradeLevel: 0,
      };
    } else {
      const names = [
        'Cuore di Corallo', 'Occhio del Kraken', 'Perla Nera',
        'Bussola Sommergibile', 'Talismano delle Maree', 'Amuleto del Fondo'
      ];
      const name = names[Math.floor(Math.random() * names.length)];
      const spd = 10 + rarity * 10;
      const crit = 0.04 + rarity * 0.03;
      return {
        id,
        name,
        slot,
        rarity,
        damageBonus: 0,
        defenseBonus: 0,
        healthBonus: 0,
        speedBonus: spd,
        critBonus: crit,
        desc: 'Un manufatto misterioso che incrementa marea e rapidità idrodinamica.',
        visualVariant: variant,
        setName,
        upgradeLevel: 0,
      };
    }
  }

  // --- Blacksmith & Economy ---
  public getUpgradeShardCost(item: Item): number {
    return (item.upgradeLevel + 1) * 3;
  }

  public getUpgradeShellCost(item: Item): number {
    return (item.upgradeLevel + 1) * 25;
  }

  public canUpgradeItem(item: Item | null): boolean {
    if (!item || item.upgradeLevel >= 5) return false;
    return (
      this.abyssalShards >= this.getUpgradeShardCost(item) &&
      this.shells >= this.getUpgradeShellCost(item)
    );
  }

  public upgradeItem(item: Item): boolean {
    if (!this.canUpgradeItem(item)) return false;
    this.abyssalShards -= this.getUpgradeShardCost(item);
    this.shells -= this.getUpgradeShellCost(item);
    item.upgradeLevel++;

    if (item.damageBonus > 0) item.damageBonus = Math.round(item.damageBonus * 1.15);
    if (item.defenseBonus > 0) item.defenseBonus = Math.round(item.defenseBonus * 1.15);
    if (item.healthBonus > 0) item.healthBonus = Math.round(item.healthBonus * 1.15);

    AudioHelper.playLevelUp();
    this.addFloater(this.playerX, this.playerY - 45, `POTENZIATO: ${item.name} +${item.upgradeLevel}!`, '#4ade80', true);
    this.saveActiveSlot();
    return true;
  }

  public salvageBackpackItem(index: number): boolean {
    if (index < 0 || index >= this.backpack.length) return false;
    const item = this.backpack.splice(index, 1)[0];

    let shards = 1;
    let shells = 5;
    if (item.rarity === 1) { shards = 3; shells = 12; }
    else if (item.rarity === 2) { shards = 7; shells = 25; }
    else if (item.rarity === 3) { shards = 15; shells = 50; }

    if (item.upgradeLevel > 0) {
      shards += item.upgradeLevel * 2;
      shells += item.upgradeLevel * 10;
    }

    this.abyssalShards += shards;
    this.shells += shells;
    AudioHelper.playHeavyHit();
    this.addFloater(this.playerX, this.playerY - 45, `+${shards} FRAMMENTI, +${shells} 🐚`, '#fbbf24', true);
    this.saveActiveSlot();
    return true;
  }

  public forgeRandomItem(targetRarity: number): boolean {
    if (this.backpack.length >= 8) return false;
    const shardCost = targetRarity === 3 ? 20 : 8;
    const shellCost = targetRarity === 3 ? 100 : 40;
    if (this.abyssalShards < shardCost || this.shells < shellCost) return false;

    this.abyssalShards -= shardCost;
    this.shells -= shellCost;
    const forged = this.generateRandomItem(this.currentAct + 1, targetRarity === 3);
    forged.rarity = targetRarity;
    this.backpack.push(forged);

    AudioHelper.playLoot(targetRarity);
    this.addFloater(this.playerX, this.playerY - 45, `FORGIATO: ${forged.name}!`, targetRarity === 3 ? '#c084fc' : '#fbbf24', true);
    this.saveActiveSlot();
    return true;
  }

  public buyPotionFromMerchant(): boolean {
    if (this.shells < 15 || this.potions >= this.maxPotions) return false;
    this.shells -= 15;
    this.potions++;
    AudioHelper.playPotion();
    this.addFloater(this.playerX, this.playerY - 45, '+1 POZIONE', '#4ade80', true);
    this.saveActiveSlot();
    return true;
  }

  public buyTideBlessing(): boolean {
    if (this.shells < 25) return false;
    this.shells -= 25;
    this.tide = this.getMaxTide();
    this.hp = Math.min(this.getMaxHp(), this.hp + 30);
    AudioHelper.playWave();
    this.addFloater(this.playerX, this.playerY - 45, 'BENEDIZIONE DELLA MAREA!', '#38bdf8', true);
    this.saveActiveSlot();
    return true;
  }

  public buyMysteryCoffer(): boolean {
    if (this.shells < 55 || this.backpack.length >= 8) return false;
    this.shells -= 55;
    const targetRarity = Math.random() < 0.5 ? 2 : 3;
    const item = this.generateRandomItem(this.currentAct + 1, true);
    item.rarity = targetRarity;
    this.backpack.push(item);
    AudioHelper.playLoot(targetRarity);
    this.addFloater(this.playerX, this.playerY - 45, `+ ${item.name}!`, targetRarity === 3 ? '#c084fc' : '#fbbf24', true);
    this.saveActiveSlot();
    return true;
  }

  public depositToStash(backpackIndex: number): boolean {
    if (backpackIndex < 0 || backpackIndex >= this.backpack.length) return false;
    if (this.stash.length >= 16) return false;
    const item = this.backpack.splice(backpackIndex, 1)[0];
    this.stash.push(item);
    AudioHelper.playClick();
    this.saveActiveSlot();
    return true;
  }

  public withdrawFromStash(stashIndex: number): boolean {
    if (stashIndex < 0 || stashIndex >= this.stash.length) return false;
    if (this.backpack.length >= 8) return false;
    const item = this.stash.splice(stashIndex, 1)[0];
    this.backpack.push(item);
    AudioHelper.playClick();
    this.saveActiveSlot();
    return true;
  }

  public equipBackpackItem(index: number): boolean {
    if (index < 0 || index >= this.backpack.length) return false;
    const item = this.backpack[index];
    if (item.slot === 'weapon') {
      const prev = this.equippedWeapon;
      this.equippedWeapon = item;
      this.backpack.splice(index, 1);
      if (prev) this.backpack.push(prev);
    } else if (item.slot === 'armor') {
      const prev = this.equippedArmor;
      this.equippedArmor = item;
      this.backpack.splice(index, 1);
      if (prev) this.backpack.push(prev);
    } else if (item.slot === 'relic') {
      const prev = this.equippedRelic;
      this.equippedRelic = item;
      this.backpack.splice(index, 1);
      if (prev) this.backpack.push(prev);
    }
    AudioHelper.playClick();
    this.saveActiveSlot();
    return true;
  }

  public dropBackpackItem(index: number): boolean {
    if (index < 0 || index >= this.backpack.length) return false;
    const item = this.backpack.splice(index, 1)[0];
    this.drops.push({
      id: `d_dropped_${Date.now()}`,
      x: this.playerX + (Math.random() * 30 - 15),
      y: this.playerY + (Math.random() * 30 - 15),
      item,
      timer: 0,
    });
    AudioHelper.playClick();
    this.saveActiveSlot();
    return true;
  }

  public unequipSlot(slot: 'weapon' | 'armor' | 'relic'): boolean {
    if (this.backpack.length >= 8) return false;
    if (slot === 'weapon' && this.equippedWeapon) {
      this.backpack.push(this.equippedWeapon);
      this.equippedWeapon = null;
    } else if (slot === 'armor' && this.equippedArmor) {
      this.backpack.push(this.equippedArmor);
      this.equippedArmor = null;
    } else if (slot === 'relic' && this.equippedRelic) {
      this.backpack.push(this.equippedRelic);
      this.equippedRelic = null;
    }
    AudioHelper.playClick();
    this.saveActiveSlot();
    return true;
  }

  // --- Main Game Update Loop (60 FPS) ---
  public update(dt: number) {
    if (this.hitStop > 0) {
      this.hitStop -= dt;
      return;
    }

    if (this.trauma > 0) {
      this.trauma = Math.max(0, this.trauma - dt * 1.2);
    }

    if (this.beaconTimer > 0) {
      this.beaconTimer -= dt;
    }

    // Cooldown decays
    if (this.attackCooldown > 0) this.attackCooldown -= dt;
    if (this.waveCooldown > 0) this.waveCooldown -= dt;
    if (this.dashCooldown > 0) this.dashCooldown -= dt;
    if (this.attackAnim > 0) this.attackAnim = Math.max(0, this.attackAnim - dt * 4);
    if (this.dashAnim > 0) this.dashAnim = Math.max(0, this.dashAnim - dt * 3);
    if (this.playerSquash > 0) this.playerSquash = Math.max(0, this.playerSquash - dt * 3);

    // Passive Tide Regeneration
    const tideRegen = this.hasSkill('mys_tideflow') ? 12 : 5;
    this.tide = Math.min(this.getMaxTide(), this.tide + tideRegen * dt);

    // Player movement
    if (this.playerMoving) {
      const dx = this.playerTargetX - this.playerX;
      const dy = this.playerTargetY - this.playerY;
      const dist = Math.sqrt(dx * dx + dy * dy);
      const speed = this.getMoveSpeed() * (this.dashAnim > 0 ? 2.5 : 1.0);

      if (dist <= speed * dt || dist < 4) {
        this.playerX = this.playerTargetX;
        this.playerY = this.playerTargetY;
        this.playerMoving = false;
      } else {
        this.playerFacingAngle = Math.atan2(dy, dx);
        this.playerX += (dx / dist) * speed * dt;
        this.playerY += (dy / dist) * speed * dt;
      }

      // Bubble trail
      if (Math.random() < 0.25) {
        this.particles.push({
          id: `p_trail_${Math.random()}`,
          x: this.playerX,
          y: this.playerY + 12,
          vx: (Math.random() - 0.5) * 20,
          vy: -30 - Math.random() * 20,
          radius: 2 + Math.random() * 3,
          color: '#67e8f9',
          life: 0.5,
          maxLife: 0.5,
        });
      }
    }

    // Update Entities
    if (this.gameState === 'EXPEDITION') {
      this.updateProjectiles(dt);
      this.updateEnemies(dt);
      this.updateDrops(dt);
      this.updateActChests(dt);
      this.updatePortal();
    } else if (this.gameState === 'HUB') {
      this.updateDrops(dt);
    }

    this.updateParticles(dt);
    this.updateFloaters(dt);
  }

  private updateProjectiles(dt: number) {
    for (let i = this.projectiles.length - 1; i >= 0; i--) {
      const p = this.projectiles[i];

      // Homing for Mystic projectiles
      if (p.homing && p.playerOwned) {
        let closest: Enemy | null = null;
        let minDistSq = 450 * 450;
        for (const e of this.enemies) {
          if (e.hp <= 0 || p.hitEnemyIds?.includes(e.id)) continue;
          const dx = e.x - p.x;
          const dy = e.y - p.y;
          const d2 = dx * dx + dy * dy;
          if (d2 < minDistSq) {
            minDistSq = d2;
            closest = e;
          }
        }
        if (closest) {
          const dx = closest.x - p.x;
          const dy = closest.y - p.y;
          const dist = Math.sqrt(dx * dx + dy * dy);
          if (dist > 5) {
            const curSpd = Math.sqrt(p.vx * p.vx + p.vy * p.vy) || 460;
            const targetVx = (dx / dist) * curSpd;
            const targetVy = (dy / dist) * curSpd;
            p.vx += (targetVx - p.vx) * Math.min(1.0, dt * 7.5);
            p.vy += (targetVy - p.vy) * Math.min(1.0, dt * 7.5);
          }
        }
      }

      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.life -= dt;

      if (p.life <= 0) {
        this.projectiles.splice(i, 1);
        continue;
      }

      if (p.playerOwned) {
        for (const e of this.enemies) {
          if (p.hitEnemyIds?.includes(e.id)) continue;
          const dx = e.x - p.x;
          const dy = e.y - p.y;
          const combR = e.radius + p.radius;
          if (dx * dx + dy * dy < combR * combR) {
            if (!p.hitEnemyIds) p.hitEnemyIds = [];
            p.hitEnemyIds.push(e.id);
            this.damageEnemy(e, p.damage, p.x, p.y);
            if (p.pierceCount !== undefined) {
              p.pierceCount--;
              if (p.pierceCount <= 0) {
                this.projectiles.splice(i, 1);
                break;
              }
            } else {
              this.projectiles.splice(i, 1);
              break;
            }
          }
        }
      } else {
        const dx = this.playerX - p.x;
        const dy = this.playerY - p.y;
        const combR = this.playerRadius + p.radius;
        if (dx * dx + dy * dy < combR * combR) {
          this.damagePlayer(p.damage, p.x, p.y);
          this.projectiles.splice(i, 1);
        }
      }
    }
  }

  private updateEnemies(dt: number) {
    for (let i = this.enemies.length - 1; i >= 0; i--) {
      const e = this.enemies[i];
      if (e.hp <= 0) {
        this.enemies.splice(i, 1);
        continue;
      }

      if (e.hitSquash > 0) e.hitSquash = Math.max(0, e.hitSquash - dt * 3);

      const dx = this.playerX - e.x;
      const dy = this.playerY - e.y;
      const dist = Math.sqrt(dx * dx + dy * dy);

      // Facing
      if (dx !== 0 || dy !== 0) {
        e.facing = Math.atan2(dy, dx);
      }

      // AI movement
      if (e.isBoss) {
        // Boss Behavior: Windup heavy slams & bullet rings
        e.attackTimer -= dt;
        if (e.windupTimer && e.windupTimer > 0) {
          e.windupTimer -= dt;
          if (e.windupTimer <= 0) {
            // Slam impact
            const slamDist = Math.sqrt(
              (this.playerX - (e.windupX || e.x)) ** 2 +
              (this.playerY - (e.windupY || e.y)) ** 2
            );
            if (slamDist <= (e.windupRadius || 110)) {
              this.damagePlayer(e.dmg * 1.5, e.windupX || e.x, e.windupY || e.y);
            }
            this.trauma = Math.min(1.0, this.trauma + 0.6);
            AudioHelper.playHeavyHit();
          }
        } else if (e.attackTimer <= 0) {
          e.attackTimer = 3.0;
          e.windupTimer = 1.2;
          e.windupMaxTimer = 1.2;
          e.windupX = this.playerX;
          e.windupY = this.playerY;
          e.windupRadius = 110;

          // Also launch projectiles
          const ringCount = 8;
          for (let r = 0; r < ringCount; r++) {
            const a = (Math.PI * 2 / ringCount) * r;
            this.projectiles.push({
              id: `boss_p_${Date.now()}_${r}`,
              x: e.x,
              y: e.y,
              vx: Math.cos(a) * 200,
              vy: Math.sin(a) * 200,
              radius: 9,
              damage: Math.round(e.dmg * 0.75),
              playerOwned: false,
              life: 2.5,
              maxLife: 2.5,
              color: '#ef4444',
            });
          }
        }

        // Slow drift toward player
        if (dist > 120) {
          e.x += (dx / dist) * e.speed * dt;
          e.y += (dy / dist) * e.speed * dt;
        }
      } else {
        // Regular enemy movement
        if (dist < 450 && dist > e.radius + this.playerRadius) {
          e.x += (dx / dist) * e.speed * dt;
          e.y += (dy / dist) * e.speed * dt;
        }

        // Attack when in range
        if (dist <= e.radius + this.playerRadius + 5) {
          e.attackTimer -= dt;
          if (e.attackTimer <= 0) {
            e.attackTimer = 1.0;
            this.damagePlayer(e.dmg, e.x, e.y);
          }
        }
      }
    }
  }

  private updateDrops(dt: number) {
    for (let i = this.drops.length - 1; i >= 0; i--) {
      const d = this.drops[i];
      d.timer += dt;
      const dx = this.playerX - d.x;
      const dy = this.playerY - d.y;
      const dist = Math.sqrt(dx * dx + dy * dy);

      // Vacuum magnetic pickup when close
      if (dist < 85) {
        d.x += (dx / dist) * 220 * dt;
        d.y += (dy / dist) * 220 * dt;
      }

      if (dist < 35) {
        if (d.shellAmount) {
          this.shells += d.shellAmount;
          this.addFloater(d.x, d.y - 25, `+${d.shellAmount} 🐚`, '#fbbf24', false);
          AudioHelper.playClick();
          this.drops.splice(i, 1);
          this.saveActiveSlot();
        } else if (d.shardAmount) {
          this.abyssalShards += d.shardAmount;
          this.addFloater(d.x, d.y - 25, `+${d.shardAmount} FRAMMENTI`, '#38bdf8', true);
          AudioHelper.playClick();
          this.drops.splice(i, 1);
          this.saveActiveSlot();
        } else if (d.item) {
          if (this.backpack.length < 8) {
            this.backpack.push(d.item);
            this.addFloater(d.x, d.y - 25, `+ ${d.item.name}!`, this.getRarityColor(d.item.rarity), true);
            AudioHelper.playLoot(d.item.rarity);
            this.drops.splice(i, 1);
            this.saveActiveSlot();
          }
        }
      }
    }
  }

  private updateActChests(dt: number) {
    for (const c of this.actChests) {
      if (!c.opened) {
        const dx = this.playerX - c.x;
        const dy = this.playerY - c.y;
        if (dx * dx + dy * dy < 75 * 75) {
          this.openActChest(c);
        }
      } else if (c.animTimer > 0) {
        c.animTimer = Math.max(0, c.animTimer - dt * 2);
      }
    }
  }

  private updatePortal() {
    if (this.enemies.length === 0 && !this.portalOpen) {
      this.portalOpen = true;
      this.addFloater(this.portalX, this.portalY - 40, 'PORTALE DEGLI ABISSI APERTO', '#38bdf8', true);
      AudioHelper.playLevelUp();
    }

    if (this.portalOpen) {
      const dx = this.playerX - this.portalX;
      const dy = this.playerY - this.portalY;
      if (dx * dx + dy * dy < this.portalRadius * this.portalRadius) {
        // Advanced act unlocked
        if (this.zone === 1 && this.maxActUnlocked < 2) {
          this.maxActUnlocked = 2;
          this.acts[0].completed = true;
          this.abyssalShards += 5;
          this.shells += 50;
        } else if (this.zone === 2 && this.maxActUnlocked < 3) {
          this.maxActUnlocked = 3;
          this.acts[1].completed = true;
          this.abyssalShards += 8;
          this.shells += 80;
        } else if (this.zone === 3 && this.maxActUnlocked < 4) {
          this.maxActUnlocked = 4;
          this.acts[2].completed = true;
          this.abyssalShards += 12;
          this.shells += 120;
        } else if (this.zone === 4) {
          this.acts[3].completed = true;
          this.victory = true;
          this.abyssalShards += 25;
          this.shells += 250;
        }

        this.saveActiveSlot();

        if (this.zone === 4) {
          this.victory = true;
        } else {
          // Progress to next zone or enter hub
          this.enterHub();
        }
      }
    }
  }

  private updateParticles(dt: number) {
    for (let i = this.particles.length - 1; i >= 0; i--) {
      const p = this.particles[i];
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.life -= dt;
      if (p.life <= 0) {
        this.particles.splice(i, 1);
      }
    }
  }

  private updateFloaters(dt: number) {
    for (let i = this.floaters.length - 1; i >= 0; i--) {
      const f = this.floaters[i];
      f.y -= 25 * dt;
      f.life -= dt;
      if (f.life <= 0) {
        this.floaters.splice(i, 1);
      }
    }
  }

  public addFloater(x: number, y: number, text: string, color: string, isCrit: boolean = false) {
    this.floaters.push({
      id: `fl_${Date.now()}_${Math.random()}`,
      x,
      y,
      text,
      color,
      isCrit,
      life: isCrit ? 1.4 : 0.85,
      maxLife: isCrit ? 1.4 : 0.85,
    });
  }

  public normalizeAngle(angle: number): number {
    while (angle > Math.PI) angle -= 2 * Math.PI;
    while (angle < -Math.PI) angle += 2 * Math.PI;
    return angle;
  }

  public getRarityColor(rarity: number): string {
    switch (rarity) {
      case 1:
        return '#38bdf8'; // Magic Cobalt
      case 2:
        return '#fbbf24'; // Rare Gold
      case 3:
        return '#c084fc'; // Abyssal Purple
      default:
        return '#f8fafc'; // Common Slate
    }
  }

  public getSuitColorHex(): string {
    return SUIT_COLORS[this.suitColorIndex]?.hex || '#0284c7';
  }
}
