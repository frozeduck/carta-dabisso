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
  ActiveSlotId,
  ClassActiveSkill,
  InteractiveWorldEntity,
  DialogueMessage,
} from '../types/game';
import {
  ALL_SKILLS,
  INITIAL_ACTS,
  RANDOM_NAMES,
  CLASS_ACTIVE_SKILLS,
  DEFAULT_SKILL_LOADOUTS,
} from './skillsData';
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

  // Customizable Active Skill Loadout (M1, M2, Q, W, E, R)
  public skillLoadout: Record<ActiveSlotId, string> = {
    ...DEFAULT_SKILL_LOADOUTS.hunter,
  };
  public slotCooldowns: Record<ActiveSlotId, number> = {
    M1: 0,
    M2: 0,
    Q: 0,
    W: 0,
    E: 0,
    R: 0,
  };
  public slotMaxCooldowns: Record<ActiveSlotId, number> = {
    M1: 0.35,
    M2: 2.5,
    Q: 4.5,
    W: 6.0,
    E: 5.0,
    R: 12.0,
  };

  // Player Physics
  public playerX: number = 640;
  public playerY: number = 440;
  public playerTargetX: number = 640;
  public playerTargetY: number = 440;
  public playerMoving: boolean = false;
  public playerFacingAngle: number = 0;
  public playerRadius: number = 22;
  public playerSquash: number = 0;

  // Active Action Timers
  public attackAnim: number = 0;
  public dashAnim: number = 0;
  public dashCooldown: number = 0;
  public shieldTimer: number = 0;
  public whirlwindTimer: number = 0;
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

  // Story & World Entities (Story progression instead of waves!)
  public worldEntities: InteractiveWorldEntity[] = [];
  public currentDialogue: DialogueMessage | null = null;
  public storyLog: string[] = [];

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
  private getStorageItem(key: string): string | null {
    try {
      const bridge = typeof window !== 'undefined' ? (window as unknown as { AndroidBridge?: { getString?: (k: string) => string } }).AndroidBridge : undefined;
      if (bridge && typeof bridge.getString === 'function') {
        const val = bridge.getString(key);
        if (val && val.length > 0) return val;
      }
    } catch {
      // ignore bridge error
    }
    return typeof localStorage !== 'undefined' ? localStorage.getItem(key) : null;
  }

  private setStorageItem(key: string, value: string) {
    try {
      const bridge = typeof window !== 'undefined' ? (window as unknown as { AndroidBridge?: { setString?: (k: string, v: string) => void } }).AndroidBridge : undefined;
      if (bridge && typeof bridge.setString === 'function') {
        bridge.setString(key, value);
      }
    } catch {
      // ignore bridge error
    }
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(key, value);
    }
  }

  private removeStorageItem(key: string) {
    try {
      const bridge = typeof window !== 'undefined' ? (window as unknown as { AndroidBridge?: { setString?: (k: string, v: string) => void } }).AndroidBridge : undefined;
      if (bridge && typeof bridge.setString === 'function') {
        bridge.setString(key, '');
      }
    } catch {
      // ignore bridge error
    }
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(key);
    }
  }

  public loadSlotsFromStorage() {
    this.slots = [];
    for (let i = 0; i < 3; i++) {
      const raw = this.getStorageItem(`cda_slot_${i}`);
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

    const hasAutostart = typeof window !== 'undefined' && window.location.search.includes('autostart=1');
    const firstExisting = this.slots.find((s) => s.exists);
    if (firstExisting) {
      this.currentSlot = firstExisting.index;
      this.loadSlotData(firstExisting);
      if (hasAutostart) {
        this.enterHub();
      }
    } else {
      // Auto-initialize a default explorer slot for immediate gameplay and seamless testing
      const starterSlot: CharacterSlot = {
        index: 0,
        exists: true,
        name: 'Palombaro Nautilus',
        playerClass: 'hunter',
        suitColorIndex: 0,
        level: 1,
        xp: 0,
        xpNext: 100,
        skillPoints: 0,
        shells: 150,
        abyssalShards: 5,
        maxActUnlocked: 1,
        equippedWeapon: null,
        equippedArmor: null,
        equippedRelic: null,
        backpack: [],
        stash: [],
        unlockedSkills: ['hunt_spear', 'hunt_scatter', 'hunt_electro_net', 'hunt_torpedo', 'hunt_trap', 'hunt_barrage'],
        skillLoadout: { ...DEFAULT_SKILL_LOADOUTS.hunter },
      };
      this.slots[0] = starterSlot;
      this.currentSlot = 0;
      this.loadSlotData(starterSlot);
      this.initStarterGear('hunter');
      this.saveActiveSlot();
      this.enterHub();
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
      skillLoadout: { ...DEFAULT_SKILL_LOADOUTS.hunter },
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
      skillLoadout: this.skillLoadout,
    };
    this.slots[this.currentSlot] = slotData;
    this.setStorageItem(`cda_slot_${this.currentSlot}`, JSON.stringify(slotData));
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
    this.removeStorageItem(`cda_slot_${slotIndex}`);
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
    this.skillLoadout = { ...DEFAULT_SKILL_LOADOUTS[clz] };

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
    this.skillLoadout = slot.skillLoadout || { ...DEFAULT_SKILL_LOADOUTS[this.playerClass] };

    for (let i = 0; i < this.acts.length; i++) {
      this.acts[i].completed = i + 1 < this.maxActUnlocked;
    }
  }

  // --- Skill Assignment & Execution ---
  public assignSkillToSlot(slot: ActiveSlotId, skillId: string) {
    this.skillLoadout[slot] = skillId;
    AudioHelper.playClick();
    this.saveActiveSlot();
  }

  public getSkillDefinition(skillId: string): ClassActiveSkill | undefined {
    return CLASS_ACTIVE_SKILLS[this.playerClass]?.find((s) => s.id === skillId);
  }

  public triggerSlot(slot: ActiveSlotId, targetWorldX: number = -1, targetWorldY: number = -1): boolean {
    if (this.gameState === 'HUB' || this.gameOver || this.victory) return false;
    const skillId = this.skillLoadout[slot];
    if (!skillId) return false;

    const skillDef = this.getSkillDefinition(skillId);
    if (!skillDef) return false;

    // Check cooldown & tide cost
    if (this.slotCooldowns[slot] > 0) return false;
    if (this.tide < skillDef.tideCost) {
      this.addFloater(this.playerX, this.playerY - 30, 'MAREA INSUFFICIENTE!', '#38bdf8', false);
      return false;
    }

    // Set cooldown
    this.slotCooldowns[slot] = skillDef.cooldown;
    this.slotMaxCooldowns[slot] = skillDef.cooldown;
    this.tide -= skillDef.tideCost;

    // Auto-aim target direction
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
        targetWorldX = this.playerX + Math.cos(this.playerFacingAngle) * 160;
        targetWorldY = this.playerY + Math.sin(this.playerFacingAngle) * 160;
      }
    }

    const dx = targetWorldX - this.playerX;
    const dy = targetWorldY - this.playerY;
    if (dx !== 0 || dy !== 0) {
      this.playerFacingAngle = Math.atan2(dy, dx);
    }
    const angle = this.playerFacingAngle;
    const baseDamage = this.getAttackDamage();
    const isCrit = Math.random() < this.getCritChance();
    const critMult = this.hasSkill('hunt_crit') ? 2.4 : 1.8;
    const dmg = Math.round(baseDamage * skillDef.damageMult * (isCrit ? critMult : 1.0));

    this.attackAnim = 1.0;
    this.playerSquash = 0.4;

    // Execute skill logic by ID
    this.executeSkillLogic(skillId, angle, dmg, isCrit, targetWorldX, targetWorldY);
    return true;
  }

  private executeSkillLogic(
    skillId: string,
    angle: number,
    dmg: number,
    _isCrit: boolean,
    targetX: number,
    targetY: number
  ) {
    // --- HUNTER SKILLS ---
    if (skillId === 'hunt_spear') {
      const speed = this.hasSkill('hunt_rapid') ? 640 : 520;
      const pierce = this.hasSkill('hunt_pierce') ? 3 : 1;
      this.projectiles.push({
        id: `p_spear_${Date.now()}_${Math.random()}`,
        x: this.playerX,
        y: this.playerY,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        radius: 8,
        damage: dmg,
        playerOwned: true,
        life: 0.5,
        maxLife: 0.5,
        color: '#38bdf8',
        pierceCount: pierce,
        hitEnemyIds: [],
      });
      AudioHelper.playHit();
    } else if (skillId === 'hunt_scatter') {
      const speed = 560;
      for (const off of [-0.25, 0, 0.25]) {
        const a = angle + off;
        this.projectiles.push({
          id: `p_scat_${Date.now()}_${Math.random()}`,
          x: this.playerX,
          y: this.playerY,
          vx: Math.cos(a) * speed,
          vy: Math.sin(a) * speed,
          radius: 8,
          damage: Math.round(dmg * 0.8),
          playerOwned: true,
          life: 0.5,
          maxLife: 0.5,
          color: '#60a5fa',
          pierceCount: 1,
          hitEnemyIds: [],
        });
      }
      AudioHelper.playHit();
    } else if (skillId === 'hunt_electro_net') {
      // Net projectile that detonates at target
      for (const e of this.enemies) {
        const dist = Math.hypot(e.x - targetX, e.y - targetY);
        if (dist < 130) {
          this.damageEnemy(e, dmg, e.x, e.y);
          e.speed = Math.max(20, e.speed * 0.4); // bind/slow
        }
      }
      for (let i = 0; i < 16; i++) {
        this.particles.push({
          id: `p_net_${Math.random()}`,
          x: targetX,
          y: targetY,
          vx: Math.random() * 120 - 60,
          vy: Math.random() * 120 - 60,
          radius: 5,
          color: '#fcd34d',
          life: 0.6,
          maxLife: 0.6,
        });
      }
      AudioHelper.playWave();
    } else if (skillId === 'hunt_torpedo') {
      const speed = 400;
      this.projectiles.push({
        id: `p_torp_${Date.now()}`,
        x: this.playerX,
        y: this.playerY,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        radius: 14,
        damage: dmg,
        playerOwned: true,
        life: 0.9,
        maxLife: 0.9,
        color: '#fb923c',
        hitEnemyIds: [],
      });
      AudioHelper.playHeavyHit();
    } else if (skillId === 'hunt_trap') {
      // Deploy coral mines near player
      for (let i = -1; i <= 1; i++) {
        const offA = angle + i * 0.6;
        const tx = this.playerX + Math.cos(offA) * 70;
        const ty = this.playerY + Math.sin(offA) * 70;
        this.particles.push({
          id: `p_mine_${Math.random()}`,
          x: tx,
          y: ty,
          vx: 0,
          vy: 0,
          radius: 12,
          color: '#f87171',
          life: 4.0,
          maxLife: 4.0,
        });
        // Check immediate proximity
        for (const e of this.enemies) {
          if (Math.hypot(e.x - tx, e.y - ty) < 80) {
            this.damageEnemy(e, dmg, tx, ty);
          }
        }
      }
      AudioHelper.playWave();
    } else if (skillId === 'hunt_barrage') {
      // Barrage Ultimate: 8 rapid spear wave
      for (let i = 0; i < 8; i++) {
        const a = angle + (Math.random() * 0.4 - 0.2);
        const speed = 680 + Math.random() * 120;
        this.projectiles.push({
          id: `p_bar_${Date.now()}_${i}`,
          x: this.playerX + Math.random() * 20 - 10,
          y: this.playerY + Math.random() * 20 - 10,
          vx: Math.cos(a) * speed,
          vy: Math.sin(a) * speed,
          radius: 9,
          damage: Math.round(dmg * 0.9),
          playerOwned: true,
          life: 0.7,
          maxLife: 0.7,
          color: '#38bdf8',
          pierceCount: 2,
          hitEnemyIds: [],
        });
      }
      this.trauma = Math.min(1.0, this.trauma + 0.4);
      AudioHelper.playHeavyHit();

      // --- JUGGERNAUT SKILLS ---
    } else if (skillId === 'jug_cleave') {
      let hitCount = 0;
      for (const e of this.enemies) {
        if (e.hp <= 0) continue;
        const edx = e.x - this.playerX;
        const edy = e.y - this.playerY;
        const dist = Math.hypot(edx, edy);
        if (dist <= 145 + e.radius) {
          const eAngle = Math.atan2(edy, edx);
          const diff = Math.abs(this.normalizeAngle(eAngle - angle));
          if (diff <= 1.4 || dist < 45) {
            this.damageEnemy(e, dmg, e.x, e.y);
            if (dist > 1) {
              e.x += (edx / dist) * 70;
              e.y += (edy / dist) * 70;
            }
            hitCount++;
          }
        }
      }
      for (let i = -4; i <= 4; i++) {
        const a = angle + i * 0.25;
        this.particles.push({
          id: `p_cleave_${Math.random()}`,
          x: this.playerX + Math.cos(a) * 60,
          y: this.playerY + Math.sin(a) * 60,
          vx: Math.cos(a) * 170,
          vy: Math.sin(a) * 170,
          radius: 6,
          color: '#f59e0b',
          life: 0.25,
          maxLife: 0.25,
        });
      }
      if (hitCount > 0) AudioHelper.playHeavyHit();
      else AudioHelper.playHit();
    } else if (skillId === 'jug_slam') {
      // Ground seismic shockwave
      this.trauma = Math.min(1.0, this.trauma + 0.5);
      for (const e of this.enemies) {
        const dist = Math.hypot(e.x - this.playerX, e.y - this.playerY);
        if (dist < 180) {
          this.damageEnemy(e, dmg, e.x, e.y);
          e.speed = Math.max(10, e.speed * 0.3); // Stun
        }
      }
      for (let i = 0; i < 20; i++) {
        const a = (Math.PI * 2 / 20) * i;
        this.particles.push({
          id: `p_slam_${Math.random()}`,
          x: this.playerX,
          y: this.playerY,
          vx: Math.cos(a) * 220,
          vy: Math.sin(a) * 220,
          radius: 7,
          color: '#d97706',
          life: 0.45,
          maxLife: 0.45,
        });
      }
      AudioHelper.playHeavyHit();
    } else if (skillId === 'jug_shield') {
      this.shieldTimer = 4.0;
      this.addFloater(this.playerX, this.playerY - 45, 'CARAPACE CORAZZATO ATTIVO!', '#34d399', true);
      AudioHelper.playWave();
    } else if (skillId === 'jug_chain') {
      // Chain pull distant enemies
      let pulled = 0;
      for (const e of this.enemies) {
        const dist = Math.hypot(e.x - this.playerX, e.y - this.playerY);
        if (dist > 50 && dist < 360) {
          const dx = this.playerX - e.x;
          const dy = this.playerY - e.y;
          e.x += (dx / dist) * 160;
          e.y += (dy / dist) * 160;
          this.damageEnemy(e, Math.round(dmg * 0.7), e.x, e.y);
          pulled++;
        }
      }
      AudioHelper.playHeavyHit();
    } else if (skillId === 'jug_whirlwind') {
      this.whirlwindTimer = 2.0;
      for (const e of this.enemies) {
        if (Math.hypot(e.x - this.playerX, e.y - this.playerY) < 160) {
          this.damageEnemy(e, dmg, e.x, e.y);
        }
      }
      AudioHelper.playHeavyHit();
    } else if (skillId === 'jug_earthquake') {
      // Line seismic fissure
      this.trauma = Math.min(1.0, this.trauma + 0.65);
      for (let step = 40; step <= 320; step += 40) {
        const fx = this.playerX + Math.cos(angle) * step;
        const fy = this.playerY + Math.sin(angle) * step;
        for (const e of this.enemies) {
          if (Math.hypot(e.x - fx, e.y - fy) < 70) {
            this.damageEnemy(e, dmg, fx, fy);
          }
        }
        this.particles.push({
          id: `p_eq_${Math.random()}`,
          x: fx,
          y: fy,
          vx: 0,
          vy: -40,
          radius: 12,
          color: '#ef4444',
          life: 0.6,
          maxLife: 0.6,
        });
      }
      AudioHelper.playHeavyHit();

      // --- MYSTIC SKILLS ---
    } else if (skillId === 'mys_homing') {
      const speed = 460;
      this.projectiles.push({
        id: `p_hom_${Date.now()}_${Math.random()}`,
        x: this.playerX,
        y: this.playerY,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        radius: 10,
        damage: dmg,
        playerOwned: true,
        life: 0.9,
        maxLife: 0.9,
        color: '#c084fc',
        homing: true,
        hitEnemyIds: [],
      });
      this.tide = Math.min(this.getMaxTide(), this.tide + 4);
      AudioHelper.playWave();
    } else if (skillId === 'mys_vortex') {
      for (const e of this.enemies) {
        const dist = Math.hypot(e.x - targetX, e.y - targetY);
        if (dist < 260) {
          const dx = targetX - e.x;
          const dy = targetY - e.y;
          e.x += (dx / dist) * 75;
          e.y += (dy / dist) * 75;
          this.damageEnemy(e, dmg, targetX, targetY);
        }
      }
      for (let i = 0; i < 24; i++) {
        const a = Math.random() * Math.PI * 2;
        this.particles.push({
          id: `p_vort_${Math.random()}`,
          x: targetX,
          y: targetY,
          vx: Math.cos(a) * 140,
          vy: Math.sin(a) * 140,
          radius: 6,
          color: '#818cf8',
          life: 0.7,
          maxLife: 0.7,
        });
      }
      AudioHelper.playWave();
    } else if (skillId === 'mys_chain_lightning') {
      let struck = 0;
      for (const e of this.enemies) {
        if (struck >= 5) break;
        if (Math.hypot(e.x - this.playerX, e.y - this.playerY) < 320) {
          this.damageEnemy(e, dmg, e.x, e.y);
          struck++;
        }
      }
      AudioHelper.playHit();
    } else if (skillId === 'mys_pearl_burst') {
      const count = 6;
      for (let i = 0; i < count; i++) {
        const a = (Math.PI * 2 / count) * i;
        this.projectiles.push({
          id: `p_pearl_${Date.now()}_${i}`,
          x: this.playerX,
          y: this.playerY,
          vx: Math.cos(a) * 340,
          vy: Math.sin(a) * 340,
          radius: 11,
          damage: dmg,
          playerOwned: true,
          life: 0.6,
          maxLife: 0.6,
          color: '#38bdf8',
          hitEnemyIds: [],
        });
      }
      AudioHelper.playWave();
    } else if (skillId === 'mys_bubble_shield') {
      this.hp = Math.min(this.getMaxHp(), this.hp + 25);
      this.addFloater(this.playerX, this.playerY - 45, '+25 PV BOLLE DI CURA', '#67e8f9', true);
      AudioHelper.playPotion();
    } else if (skillId === 'mys_kraken_wrath') {
      // Ultimate: spectral tentacles
      this.trauma = Math.min(1.0, this.trauma + 0.65);
      for (const e of this.enemies) {
        this.damageEnemy(e, dmg, e.x, e.y);
      }
      for (let i = 0; i < 30; i++) {
        this.particles.push({
          id: `p_kraken_${Math.random()}`,
          x: this.playerX + (Math.random() * 300 - 150),
          y: this.playerY + (Math.random() * 300 - 150),
          vx: 0,
          vy: -60,
          radius: 8 + Math.random() * 8,
          color: '#a855f7',
          life: 0.8,
          maxLife: 0.8,
        });
      }
      AudioHelper.playHeavyHit();
    }
  }

  // --- Controls Shortcuts ---
  public performAttack(targetWorldX: number = -1, targetWorldY: number = -1): boolean {
    return this.triggerSlot('M1', targetWorldX, targetWorldY);
  }

  public performM2(targetWorldX: number = -1, targetWorldY: number = -1): boolean {
    return this.triggerSlot('M2', targetWorldX, targetWorldY);
  }

  public performQ(targetWorldX: number = -1, targetWorldY: number = -1): boolean {
    return this.triggerSlot('Q', targetWorldX, targetWorldY);
  }

  public performW(targetWorldX: number = -1, targetWorldY: number = -1): boolean {
    return this.triggerSlot('W', targetWorldX, targetWorldY);
  }

  public performE(targetWorldX: number = -1, targetWorldY: number = -1): boolean {
    return this.triggerSlot('E', targetWorldX, targetWorldY);
  }

  public performR(targetWorldX: number = -1, targetWorldY: number = -1): boolean {
    return this.triggerSlot('R', targetWorldX, targetWorldY);
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
        if (Math.hypot(e.x - this.playerX, e.y - this.playerY) < 135) {
          this.damageEnemy(e, Math.round(this.getAttackDamage() * 0.85), e.x, e.y);
        }
      }
    }

    if (this.hasSkill('hunt_dash_cut')) {
      for (const e of this.enemies) {
        if (Math.hypot(e.x - this.playerX, e.y - this.playerY) < 110) {
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

  // --- Story Progression, Acts & World Entities ---
  public getActiveAct(): ActInfo {
    return this.acts[this.currentAct - 1] || this.acts[0];
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
    this.worldEntities = [];
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

    const act = this.getActiveAct();
    act.gateOpened = false;
    act.objectives.forEach((obj) => {
      obj.currentCount = 0;
      obj.completed = false;
    });

    if (this.zone === 1) {
      this.zoneName = 'Barriera Corallina';
      this.depthMeters = 220;

      // Story Entities for Act 1: 2 Ancient Coral Crystals + 1 Ancient Stone Gate
      this.worldEntities.push({
        id: 'crys_1',
        type: 'quest_item',
        name: 'Cristallo Solare delle Barriere (1/2)',
        x: mapW / 2 - 420,
        y: 860,
        radius: 26,
        color: '#f59e0b',
        active: true,
      });
      this.worldEntities.push({
        id: 'crys_2',
        type: 'quest_item',
        name: 'Cristallo Solare delle Barriere (2/2)',
        x: mapW / 2 + 420,
        y: 860,
        radius: 26,
        color: '#f59e0b',
        active: true,
      });
      this.worldEntities.push({
        id: 'gate_1',
        type: 'gate',
        name: 'Portale delle Antiche Rovine',
        x: mapW / 2,
        y: 540,
        radius: 48,
        color: '#38bdf8',
        active: false,
      });

      // Story Chest
      this.actChests.push({ id: 'chest_1', x: mapW / 2, y: 750, opened: false, animTimer: 0 });

      // Story Enemies stationed in sectors
      // Sector 1: Coral Entrance
      this.spawnEnemy('crab', 'Granchio di Scoglio', mapW / 2 - 350, 1100, 80, 12, 22, 75);
      this.spawnEnemy('crab', 'Granchio di Scoglio', mapW / 2 + 350, 1100, 80, 12, 22, 75);
      this.spawnEnemy('jelly', 'Medusa Elettrica', mapW / 2, 980, 70, 14, 20, 55);

      // Sector 2: Coral Relic Chamber
      this.spawnEnemy('crab', 'Granchio Tagliatore', mapW / 2 - 380, 800, 95, 15, 23, 80);
      this.spawnEnemy('crab', 'Granchio Tagliatore', mapW / 2 + 380, 800, 95, 15, 23, 80);
      this.spawnEnemy('jelly', 'Medusa Fluttuante', mapW / 2, 720, 75, 15, 20, 60);

      // Sector 3: Guardian's Lair (Behind the Gate!)
      this.spawnEnemy('crab', 'Granchio Corazzato', mapW / 2 - 200, 360, 130, 16, 24, 70);
      this.spawnEnemy('crab', 'Granchio Corazzato', mapW / 2 + 200, 360, 130, 16, 24, 70);
      this.spawnBoss('Granchio Gigante Corazzato', mapW / 2, 320, 550, 24, 45, 65);

      this.currentDialogue = {
        speaker: 'Anziana Tartaruga',
        portrait: '/sprites/anziano_tartaruga.png',
        text: 'Benvenuto alle Antiche Rovine Coralline! Il portale per la tana del Guardiano è sigillato. Esplora le ali est e ovest per recuperare i due Cristalli Solari!',
      };
    } else if (this.zone === 2) {
      this.zoneName = 'Fossa delle Tenebre';
      this.depthMeters = 680;

      // Story Entities for Act 2: 2 Transmitters to disarm + Submersible Core
      this.worldEntities.push({
        id: 'trans_1',
        type: 'valve',
        name: 'Trasmettitore a Scarica (1/2)',
        x: mapW / 2 - 450,
        y: 850,
        radius: 26,
        color: '#fcd34d',
        active: true,
      });
      this.worldEntities.push({
        id: 'trans_2',
        type: 'valve',
        name: 'Trasmettitore a Scarica (2/2)',
        x: mapW / 2 + 450,
        y: 850,
        radius: 26,
        color: '#fcd34d',
        active: true,
      });
      this.worldEntities.push({
        id: 'core_1',
        type: 'quest_item',
        name: 'Scatola Nera del Baticafo',
        x: mapW / 2,
        y: 720,
        radius: 26,
        color: '#38bdf8',
        active: true,
      });
      this.worldEntities.push({
        id: 'gate_2',
        type: 'gate',
        name: 'Grata d\'Acciaio della Fossa',
        x: mapW / 2,
        y: 520,
        radius: 48,
        color: '#f59e0b',
        active: false,
      });

      this.actChests.push({ id: 'chest_2', x: mapW / 2, y: 800, opened: false, animTimer: 0 });

      // Enemies
      this.spawnEnemy('crab', 'Granchio Abissale', mapW / 2 - 400, 1150, 160, 22, 24, 80);
      this.spawnEnemy('crab', 'Granchio Abissale', mapW / 2 + 400, 1150, 160, 22, 24, 80);
      this.spawnEnemy('jelly', 'Medusa dei Fulmini', mapW / 2, 980, 120, 24, 22, 65);
      this.spawnEnemy('eel', 'Murena dei Fondali', mapW / 2 - 350, 780, 150, 28, 22, 115);
      this.spawnEnemy('eel', 'Murena dei Fondali', mapW / 2 + 350, 780, 150, 28, 22, 115);
      this.spawnBoss('Murena Zannuta Primordiale', mapW / 2, 320, 750, 32, 50, 125);

      this.currentDialogue = {
        speaker: 'Anziana Tartaruga',
        portrait: '/sprites/anziano_tartaruga.png',
        text: 'La fossa elettrica è pervasa da correnti d\'alta tensione! Disarma entrambi i trasmettitori e recupera il nucleo dati per sbloccare la discesa.',
      };
    } else if (this.zone === 3) {
      this.zoneName = 'Dorsale Vulcanica';
      this.depthMeters = 1450;

      // Story Entities for Act 3: 2 Geothermal Coolant Valves
      this.worldEntities.push({
        id: 'valve_1',
        type: 'valve',
        name: 'Valvola di Raffreddamento Idraulico (1/2)',
        x: mapW / 2 - 450,
        y: 850,
        radius: 26,
        color: '#06b6d4',
        active: true,
      });
      this.worldEntities.push({
        id: 'valve_2',
        type: 'valve',
        name: 'Valvola di Raffreddamento Idraulico (2/2)',
        x: mapW / 2 + 450,
        y: 850,
        radius: 26,
        color: '#06b6d4',
        active: true,
      });
      this.worldEntities.push({
        id: 'gate_3',
        type: 'gate',
        name: 'Barriera di Magma Raffreddata',
        x: mapW / 2,
        y: 520,
        radius: 48,
        color: '#ef4444',
        active: false,
      });

      this.actChests.push({ id: 'chest_3', x: mapW / 2, y: 850, opened: false, animTimer: 0 });

      this.spawnEnemy('crab', 'Granchio Vulcanico', mapW / 2 - 450, 1200, 220, 32, 26, 85);
      this.spawnEnemy('crab', 'Granchio Vulcanico', mapW / 2 + 450, 1200, 220, 32, 26, 85);
      this.spawnEnemy('eel', 'Murena di Fuoco', mapW / 2, 1100, 180, 32, 24, 125);
      this.spawnEnemy('angler', 'Pesce Lanterna Cieco', mapW / 2 - 380, 820, 200, 34, 25, 95);
      this.spawnEnemy('angler', 'Predatore Fosforescente', mapW / 2 + 380, 820, 210, 35, 25, 95);
      this.spawnBoss('Rana Pescatrice Abissale', mapW / 2, 340, 1100, 38, 55, 90);

      this.currentDialogue = {
        speaker: 'Mastro Astice',
        portrait: '/sprites/fabbro_astice.png',
        text: 'Attento alla pressione e al calore geotermico! Chiudi le valvole per raffreddare i flussi di lava ed entrare nel cratere della Rana Pescatrice!',
      };
    } else if (this.zone === 4) {
      this.zoneName = 'Santuario del Kraken';
      this.depthMeters = 2400;

      // Story Entities for Act 4: 3 Void Monoliths
      this.worldEntities.push({
        id: 'mono_1',
        type: 'quest_item',
        name: 'Monolito del Vuoto Primordiale (1/3)',
        x: mapW / 2 - 400,
        y: 800,
        radius: 28,
        color: '#c084fc',
        active: true,
      });
      this.worldEntities.push({
        id: 'mono_2',
        type: 'quest_item',
        name: 'Monolito del Vuoto Primordiale (2/3)',
        x: mapW / 2 + 400,
        y: 800,
        radius: 28,
        color: '#c084fc',
        active: true,
      });
      this.worldEntities.push({
        id: 'mono_3',
        type: 'quest_item',
        name: 'Monolito del Vuoto Primordiale (3/3)',
        x: mapW / 2,
        y: 600,
        radius: 28,
        color: '#c084fc',
        active: true,
      });
      this.worldEntities.push({
        id: 'gate_4',
        type: 'gate',
        name: 'Sigillo dell\'Abisso Infinito',
        x: mapW / 2,
        y: 500,
        radius: 52,
        color: '#a855f7',
        active: false,
      });

      this.actChests.push({ id: 'chest_4', x: mapW / 2, y: 700, opened: false, animTimer: 0 });
      this.spawnBoss('Leviatano Primordiale', mapW / 2, 360, 1800, 44, 65, 50);

      this.currentDialogue = {
        speaker: 'Anziana Tartaruga',
        portrait: '/sprites/anziano_tartaruga.png',
        text: 'Siamo giunti alla faglia primordiale. Infrangi i 3 Monoliti del Vuoto per dissolvere il sigillo millenario e affrontare il Leviatano!',
      };
    } else {
      // Endless Dive
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

  public interactWithEntity(entity: InteractiveWorldEntity): boolean {
    if (!entity.active) return false;
    const act = this.getActiveAct();

    if (entity.type === 'quest_item' || entity.type === 'valve') {
      entity.active = false;
      AudioHelper.playLevelUp();
      this.addFloater(entity.x, entity.y - 35, `${entity.name.toUpperCase()} ATTIVATO!`, entity.color, true);

      // Check objective progress
      act.objectives.forEach((obj) => {
        if (!obj.completed) {
          obj.currentCount = Math.min(obj.targetCount, obj.currentCount + 1);
          if (obj.currentCount >= obj.targetCount) {
            obj.completed = true;
            this.addFloater(this.playerX, this.playerY - 60, `OBIETTIVO COMPLETATO: ${obj.title}!`, '#4ade80', true);
          }
        }
      });

      // Check if gate can be opened
      const uncompletedNonGate = act.objectives.filter(
        (o) => o.type !== 'reach_boss' && o.id.indexOf('gate') === -1 && !o.completed
      );
      if (uncompletedNonGate.length === 0) {
        const gate = this.worldEntities.find((e) => e.type === 'gate');
        if (gate) {
          gate.active = true;
          this.addFloater(gate.x, gate.y - 45, 'IL PORTONE PUÒ ESSERE APERTO!', '#38bdf8', true);
        }
      }
      return true;
    } else if (entity.type === 'gate') {
      // Open gate to boss lair
      entity.active = false;
      act.gateOpened = true;
      const gateObj = act.objectives.find((o) => o.id.indexOf('gate') !== -1);
      if (gateObj) gateObj.completed = true;

      this.trauma = Math.min(1.0, this.trauma + 0.5);
      this.addFloater(entity.x, entity.y - 45, 'PORTALE DELLE ROVINE SPALANCATO!', '#38bdf8', true);
      AudioHelper.playHeavyHit();
      return true;
    }
    return false;
  }

  // --- Stats, Items & Economy ---
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
    this.addFloater(this.playerX, this.playerY - 45, 'TALENTO SBLOCCATO!', '#38bdf8', true);
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

  // --- Hub Navigation & Town Portal ---
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
    this.worldEntities = [];
    this.portalOpen = false;
    this.gameOver = false;
    this.victory = false;
    this.currentDialogue = null;

    this.playerX = 640;
    this.playerY = 440;
    this.playerTargetX = this.playerX;
    this.playerTargetY = this.playerY;
    this.playerMoving = false;
    this.beaconTimer = 0;

    this.hp = this.getMaxHp();
    this.tide = this.getMaxTide();
    this.dashCooldown = 0;
    Object.keys(this.slotCooldowns).forEach((k) => {
      this.slotCooldowns[k as ActiveSlotId] = 0;
    });

    AudioHelper.startAmbient();
  }

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

  public damagePlayer(rawDamage: number, _fromX: number, _fromY: number) {
    if (this.gameOver || this.victory || this.dashAnim > 0.4) return;
    const def = this.getDefense();
    let taken = Math.max(1, rawDamage - Math.floor(def / 2));

    if (this.shieldTimer > 0) {
      taken = Math.max(1, Math.round(taken * 0.4));
      // Reflect damage
      for (const e of this.enemies) {
        if (Math.hypot(e.x - this.playerX, e.y - this.playerY) < 220) {
          e.hp -= Math.max(1, Math.round(taken * 0.5));
          this.addFloater(e.x, e.y - 25, `SPINE ${Math.round(taken * 0.5)}`, '#fb923c', false);
        }
      }
    }

    if (this.hasSkill('jug_unbreakable') && this.hp < this.getMaxHp() * 0.35) {
      taken = Math.max(1, Math.round(taken * 0.5));
    }

    this.hp -= taken;
    this.trauma = Math.min(1.0, this.trauma + 0.45);
    this.playerSquash = 0.8;

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

    if (e.isBoss) {
      const act = this.getActiveAct();
      const bossObj = act.objectives.find((o) => o.type === 'reach_boss');
      if (bossObj) bossObj.completed = true;
      this.portalOpen = true;
      this.addFloater(this.portalX, this.portalY - 45, 'PORTALE DI RITORNO APERTO!', '#38bdf8', true);
      AudioHelper.playLevelUp();
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

    const shellAmount = e.type === 'boss' ? 35 + Math.floor(Math.random() * 20) : 4 + Math.floor(Math.random() * 6);
    this.drops.push({
      id: `d_sh_${Date.now()}_${Math.random()}`,
      x: e.x + (Math.random() * 24 - 12),
      y: e.y + (Math.random() * 24 - 12),
      shellAmount,
      timer: 0,
    });

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
    let rarity = 0;
    if (isBoss) {
      if (roll < 35) rarity = 3;
      else if (roll < 80) rarity = 2;
      else rarity = 1;
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

  // --- Main Update Loop ---
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

    if (this.dashCooldown > 0) this.dashCooldown -= dt;
    if (this.dashAnim > 0) this.dashAnim = Math.max(0, this.dashAnim - dt * 3);
    if (this.attackAnim > 0) this.attackAnim = Math.max(0, this.attackAnim - dt * 4);
    if (this.playerSquash > 0) this.playerSquash = Math.max(0, this.playerSquash - dt * 3);
    if (this.shieldTimer > 0) this.shieldTimer = Math.max(0, this.shieldTimer - dt);
    if (this.whirlwindTimer > 0) this.whirlwindTimer = Math.max(0, this.whirlwindTimer - dt);

    // Slot cooldown ticks
    (Object.keys(this.slotCooldowns) as ActiveSlotId[]).forEach((slot) => {
      if (this.slotCooldowns[slot] > 0) {
        this.slotCooldowns[slot] = Math.max(0, this.slotCooldowns[slot] - dt);
      }
    });

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

    // Interactive World Entities Proximity Check
    if (this.gameState === 'EXPEDITION') {
      for (const ent of this.worldEntities) {
        if (ent.active && ent.type !== 'gate') {
          if (Math.hypot(ent.x - this.playerX, ent.y - this.playerY) < ent.radius + this.playerRadius + 15) {
            this.interactWithEntity(ent);
          }
        }
      }

      // Check Gate Barrier Collision
      const act = this.getActiveAct();
      if (!act.gateOpened) {
        const gate = this.worldEntities.find((e) => e.type === 'gate');
        if (gate && Math.hypot(gate.x - this.playerX, gate.y - this.playerY) < gate.radius + this.playerRadius) {
          // Push player back if gate is closed
          const gAngle = Math.atan2(this.playerY - gate.y, this.playerX - gate.x);
          this.playerX = gate.x + Math.cos(gAngle) * (gate.radius + this.playerRadius + 5);
          this.playerY = gate.y + Math.sin(gAngle) * (gate.radius + this.playerRadius + 5);
          this.playerMoving = false;
        }
      }

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

      if (dx !== 0 || dy !== 0) {
        e.facing = Math.atan2(dy, dx);
      }

      if (e.isBoss) {
        e.attackTimer -= dt;
        if (e.windupTimer && e.windupTimer > 0) {
          e.windupTimer -= dt;
          if (e.windupTimer <= 0) {
            const slamDist = Math.hypot(this.playerX - (e.windupX || e.x), this.playerY - (e.windupY || e.y));
            if (slamDist <= (e.windupRadius || 110)) {
              this.damagePlayer(e.dmg * 1.5, e.windupX || e.x, e.windupY || e.y);
            }
            this.trauma = Math.min(1.0, this.trauma + 0.6);
            AudioHelper.playHeavyHit();
          }
        } else if (e.attackTimer <= 0) {
          e.attackTimer = 3.2;
          e.windupTimer = 1.2;
          e.windupMaxTimer = 1.2;
          e.windupX = this.playerX;
          e.windupY = this.playerY;
          e.windupRadius = 110;

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

        if (dist > 120) {
          e.x += (dx / dist) * e.speed * dt;
          e.y += (dy / dist) * e.speed * dt;
        }
      } else {
        if (dist < 450 && dist > e.radius + this.playerRadius) {
          e.x += (dx / dist) * e.speed * dt;
          e.y += (dy / dist) * e.speed * dt;
        }

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
    if (this.portalOpen) {
      const dx = this.playerX - this.portalX;
      const dy = this.playerY - this.portalY;
      if (dx * dx + dy * dy < this.portalRadius * this.portalRadius) {
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
        return '#38bdf8';
      case 2:
        return '#fbbf24';
      case 3:
        return '#c084fc';
      default:
        return '#f8fafc';
    }
  }

  public getSuitColorHex(): string {
    return SUIT_COLORS[this.suitColorIndex]?.hex || '#0284c7';
  }
}
