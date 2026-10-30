package eu.sabotino.cartadabisso;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Game {
    public static final float SCREEN_W = 960f;
    public static final float SCREEN_H = 540f;
    public static final float MAP_W = 1800f;
    public static final float MAP_H = 1100f;

    public static final int RARITY_COMMON = 0;
    public static final int RARITY_MAGIC = 1;
    public static final int RARITY_RARE = 2;
    public static final int RARITY_ABYSSAL = 3;

    public interface SoundListener {
        void onHit();
        void onHeavyHit();
        void onWave();
        void onDash();
        void onPotion();
        void onLoot(int rarity);
        void onClick();
        void onLevelUp();
    }

    public static class Item {
        public String id;
        public String name;
        public String slot; // "weapon", "armor", "relic"
        public int rarity; // 0..3
        public int damageBonus;
        public int defenseBonus;
        public int healthBonus;
        public float speedBonus;
        public float critBonus;
        public String desc;
        public int visualVariant; // 0..3
        public String setName; // null or "Corallo", "Marea", "Abisso"

        public Item(String id, String name, String slot, int rarity,
                    int damageBonus, int defenseBonus, int healthBonus,
                    float speedBonus, float critBonus, String desc, int visualVariant) {
            this(id, name, slot, rarity, damageBonus, defenseBonus, healthBonus, speedBonus, critBonus, desc, visualVariant, null);
        }

        public Item(String id, String name, String slot, int rarity,
                    int damageBonus, int defenseBonus, int healthBonus,
                    float speedBonus, float critBonus, String desc, int visualVariant, String setName) {
            this.id = id;
            this.name = name;
            this.slot = slot;
            this.rarity = rarity;
            this.damageBonus = damageBonus;
            this.defenseBonus = defenseBonus;
            this.healthBonus = healthBonus;
            this.speedBonus = speedBonus;
            this.critBonus = critBonus;
            this.desc = desc;
            this.visualVariant = visualVariant;
            this.setName = setName;
        }

        public String getRarityName() {
            switch (rarity) {
                case RARITY_MAGIC: return "Magico";
                case RARITY_RARE: return "Raro";
                case RARITY_ABYSSAL: return "Abissale";
                default: return "Comune";
            }
        }

        public String getSlotTitle() {
            if ("weapon".equals(slot)) return "ARMA";
            if ("armor".equals(slot)) return "CORAZZA";
            if ("relic".equals(slot)) return "RELIQUIA";
            return slot != null ? slot.toUpperCase() : "OGGETTO";
        }

        public String getCategoryName() {
            if ("weapon".equals(slot)) return "ARMA (Mano Principale)";
            if ("armor".equals(slot)) return "CORAZZA (Torace)";
            if ("relic".equals(slot)) return "RELIQUIA (Talismano)";
            return slot != null ? slot.toUpperCase() : "OGGETTO";
        }
    }

    public static class Skill {
        public String id;
        public String name;
        public String desc;
        public String classType; // "hunter", "juggernaut", "mystic"
        public int branch; // 1 or 2
        public int tier; // 1, 2, or 3
        public int maxRank; // 1 or 3
        public String requiredSkillId;
        public String badge;
        public int color;

        public Skill(String id, String name, String desc, String classType, int branch, int tier, int maxRank, String requiredSkillId, String badge, int color) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.classType = classType;
            this.branch = branch;
            this.tier = tier;
            this.maxRank = maxRank;
            this.requiredSkillId = requiredSkillId;
            this.badge = badge;
            this.color = color;
        }
    }

    public static final String CLASS_HUNTER = "hunter";
    public static final String CLASS_JUGGERNAUT = "juggernaut";
    public static final String CLASS_MYSTIC = "mystic";

    public static final List<Skill> ALL_SKILLS = new ArrayList<>();
    static {
        // --- CACCIATORE DEI MARI (HUNTER) ---
        // Ramo 1: Balistica Pesante
        ALL_SKILLS.add(new Skill("hunt_pierce", "Arpione Perforante", "I proiettili trapassano fino a 3 nemici in linea retta.", CLASS_HUNTER, 1, 1, 1, null, "PERFORA", 0xFF38BDF8));
        ALL_SKILLS.add(new Skill("hunt_multishot", "Tiro a Ventaglio", "L'attacco primario scaglia 3 arpioni contemporaneamente.", CLASS_HUNTER, 1, 2, 1, "hunt_pierce", "MULTITIRO", 0xFF60A5FA));
        ALL_SKILLS.add(new Skill("hunt_crit", "Occhio di Falco", "+15% probabilità di Critico e danno critico 2.4x.", CLASS_HUNTER, 1, 3, 1, "hunt_multishot", "CRITICO", 0xFFF43F5E));

        // Ramo 2: Agilità Idrodinamica
        ALL_SKILLS.add(new Skill("hunt_rapid", "Tiro Rapido", "Cadenza di fuoco +35% e velocità proiettile +25%.", CLASS_HUNTER, 2, 1, 1, null, "CADENZA", 0xFFFBBF24));
        ALL_SKILLS.add(new Skill("hunt_dash_cut", "Scia Tagliente", "Lo scatto rilascia una scia d'acqua affilata che recide i nemici.", CLASS_HUNTER, 2, 2, 1, "hunt_rapid", "SCIA", 0xFF34D399));
        ALL_SKILLS.add(new Skill("hunt_surge", "Impeto Oceanico", "+50 Velocità permanente e -25% tempo di ricarica dello scatto.", CLASS_HUNTER, 2, 3, 1, "hunt_dash_cut", "IMPETO", 0xFF06B6D4));

        // --- CUSTODE DEI FONDALI (JUGGERNAUT) ---
        // Ramo 1: Bastione di Corallo
        ALL_SKILLS.add(new Skill("jug_armor", "Guscio Corazzato", "+8 Difesa permanente e +35 Salute massima.", CLASS_JUGGERNAUT, 1, 1, 1, null, "CORAZZA", 0xFFF87171));
        ALL_SKILLS.add(new Skill("jug_reflect", "Spine di Corallo", "Riflette il 40% del danno subito direttamente sugli attaccanti.", CLASS_JUGGERNAUT, 1, 2, 1, "jug_armor", "SPINE", 0xFFFB923C));
        ALL_SKILLS.add(new Skill("jug_unbreakable", "Fortezza Marina", "Sotto il 35% di PV subisci il 50% di danni in meno.", CLASS_JUGGERNAUT, 1, 3, 1, "jug_reflect", "FORTEZZA", 0xFFEF4444));

        // Ramo 2: Maglio Sismico
        ALL_SKILLS.add(new Skill("jug_slam", "Ancora Pesante", "L'attacco primario infligge +30% danno in mischia con onde d'urto.", CLASS_JUGGERNAUT, 2, 1, 1, null, "MAGLIO", 0xFFD97706));
        ALL_SKILLS.add(new Skill("jug_shockwave", "Onda Tellurica", "L'onda di Marea ha raggio +60% e scaglia i mostri marini.", CLASS_JUGGERNAUT, 2, 2, 1, "jug_slam", "SISMICO", 0xFFF59E0B));
        ALL_SKILLS.add(new Skill("jug_titan", "Colosso Abissale", "+35% Danno totale a tutte le armi e abilità.", CLASS_JUGGERNAUT, 2, 3, 1, "jug_shockwave", "COLOSSO", 0xFFB45309));

        // --- MISTICO DELL'ABISSO (MYSTIC) ---
        // Ramo 1: Signore delle Tempeste
        ALL_SKILLS.add(new Skill("mys_vortex", "Marea Vorticosa", "L'onda di Marea risucchia con forza tutti i nemici al centro.", CLASS_MYSTIC, 1, 1, 1, null, "VORTICE", 0xFF38BDF8));
        ALL_SKILLS.add(new Skill("mys_lightning", "Scarica Voltaica", "Lo scatto rilascia archi elettrici a catena tra tutti i mostri vicini.", CLASS_MYSTIC, 1, 2, 1, "mys_vortex", "FOLGORE", 0xFFFCD34D));
        ALL_SKILLS.add(new Skill("mys_tideflow", "Flusso Eterno", "+40 Marea massima e rigenerazione passiva costante (+8/s).", CLASS_MYSTIC, 1, 3, 1, "mys_lightning", "FLUSSO", 0xFF0284C7));

        // Ramo 2: Drenaggio Oscuro
        ALL_SKILLS.add(new Skill("mys_siphon", "Sifone Vitale", "Uccidere nemici o colpirli con la Marea rigenera +12 PV al palombaro.", CLASS_MYSTIC, 2, 1, 1, null, "SIFONE", 0xFFA78BFA));
        ALL_SKILLS.add(new Skill("mys_alchemy", "Elisir Abissale", "Pozioni curano +50% PV e conferiscono super-velocità per 5s.", CLASS_MYSTIC, 2, 2, 1, "mys_siphon", "ELISIR", 0xFF818CF8));
        ALL_SKILLS.add(new Skill("mys_cataclysm", "Cataclisma del Kraken", "L'onda di Marea rilascia 12 sfere d'acqua oscure aggiuntive.", CLASS_MYSTIC, 2, 3, 1, "mys_alchemy", "CATACLISMA", 0xFFC084FC));
    }

    public static class Perk {
        public String id;
        public String title;
        public String desc;
        public String badge;
        public int color;

        public Perk(String id, String title, String desc, String badge, int color) {
            this.id = id;
            this.title = title;
            this.desc = desc;
            this.badge = badge;
            this.color = color;
        }
    }

    public static final List<Perk> ALL_PERKS = new ArrayList<>();
    static {
        ALL_PERKS.add(new Perk("perk_pierce", "Arpione Perforante", "L'arpione trapassa fino a 3 nemici in linea retta.", "ARPIONE", 0xFF38BDF8));
        ALL_PERKS.add(new Perk("perk_rapid", "Tiro Rapido", "Cadenza di fuoco +35% e velocità proiettile aumentata.", "CADENZA", 0xFFFBBF24));
        ALL_PERKS.add(new Perk("perk_vortex", "Marea Vorticosa", "L'onda d'urto attira i mostri e infligge +25% di danno.", "MAREA", 0xFF60A5FA));
        ALL_PERKS.add(new Perk("perk_electric_dash", "Scatto Elettrico", "Lo scatto rilascia una scia elettrica che fulmina i nemici.", "SCATTO", 0xFFFCD34D));
        ALL_PERKS.add(new Perk("perk_armor", "Guscio Corazzato", "Difesa aumentata di +6 e immunità al contraccolpo.", "DIFESA", 0xFF34D399));
        ALL_PERKS.add(new Perk("perk_tide_surge", "Flusso Marino", "Rigenerazione passiva della Marea aumentata del +60%.", "RIGENERO", 0xFF06B6D4));
        ALL_PERKS.add(new Perk("perk_alchemy", "Elisir Abissale", "Pozioni curano +40% PV e danno velocità bonus per 5s.", "POZIONI", 0xFFA78BFA));
        ALL_PERKS.add(new Perk("perk_crit", "Occhio di Falco", "+15% probabilità di colpo critico e danno critico 2.2x.", "CRITICO", 0xFFF43F5E));
    }

    public static class Drop {
        public float x, y;
        public Item item;
        public int shellAmount = 0;
        public float bobPhase;
        public float life = 90f;

        public Drop(float x, float y, Item item) {
            this.x = x;
            this.y = y;
            this.item = item;
            this.shellAmount = 0;
            this.bobPhase = (float) (Math.random() * Math.PI * 2);
        }

        public Drop(float x, float y, int shells) {
            this.x = x;
            this.y = y;
            this.item = null;
            this.shellAmount = shells;
            this.bobPhase = (float) (Math.random() * Math.PI * 2);
        }

        public boolean isShell() {
            return shellAmount > 0;
        }
    }

    public static class Enemy {
        public String id;
        public String type; // "crab", "jelly", "eel", "boss"
        public String name;
        public float x, y;
        public float vx, vy;
        public float radius;
        public int hp, maxHp;
        public int attackDamage;
        public float attackCooldown = 0f;
        public float attackInterval = 1.8f;
        public float speed = 70f;

        // Telegraphed attack
        public boolean isWindingUp = false;
        public float windupTimer = 0f;
        public float windupDuration = 1.0f;
        public float windupX, windupY, windupRadius;

        public float hitSquash = 0f;
        public float animTimer = 0f;
        public int bossPhase = 1;

        public Enemy(String id, String type, String name, float x, float y, int hp, int dmg, float radius, float speed) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.maxHp = hp;
            this.attackDamage = dmg;
            this.radius = radius;
            this.speed = speed;
        }
    }

    public static class Projectile {
        public float x, y, vx, vy;
        public float radius;
        public int damage;
        public boolean playerOwned;
        public float life;
        public int color;
        public int pierceCount = 1;
        public final java.util.Set<String> hitEnemyIds = new java.util.HashSet<>();

        public Projectile(float x, float y, float vx, float vy, float radius, int damage, boolean playerOwned, float life, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
            this.damage = damage;
            this.playerOwned = playerOwned;
            this.life = life;
            this.color = color;
        }
    }

    public static class Floater {
        public float x, y;
        public String text;
        public int color;
        public boolean isCrit;
        public float life;
        public float maxLife;

        public Floater(float x, float y, String text, int color, boolean isCrit, float duration) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
            this.isCrit = isCrit;
            this.life = duration;
            this.maxLife = duration;
        }
    }

    public static class Particle {
        public float x, y, vx, vy;
        public float size;
        public int color;
        public float life;
        public float maxLife;

        public Particle(float x, float y, float vx, float vy, float size, int color, float life) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.life = life;
            this.maxLife = life;
        }
    }

    // Player State
    public float playerX = 800f;
    public float playerY = 600f;
    public float playerTargetX = 800f;
    public float playerTargetY = 600f;
    public boolean playerMoving = false;
    public float playerFacingAngle = 0f;
    public float playerRadius = 18f;

    // Movement Beacon (Diablo golden click ring)
    public float beaconX = -100f;
    public float beaconY = -100f;
    public float beaconTimer = 0f;

    // Mobile Virtual Joystick Input
    public float joystickX = 0f;
    public float joystickY = 0f;
    public boolean joystickActive = false;

    // Stats & Leveling
    public int level = 1;
    public int xp = 0;
    public int xpNext = 60;
    public int hp = 120;
    public float tide = 100f;
    public float maxTide = 100f;
    public int potions = 3;
    public int maxPotions = 5;
    public int shells = 15; // Conchiglie d'oro (valuta abissale)
    public float speedBoostTimer = 0f;

    // Classi e Albero delle Abilità Fisso
    public String playerClass = CLASS_HUNTER;
    public int skillPoints = 0;
    public final java.util.Map<String, Integer> skillRanks = new java.util.HashMap<>();
    public boolean skillTreeOpen = false;

    // Talenti e Perk al Level Up (retro-compatibilità)
    public final java.util.Set<String> activePerks = new java.util.HashSet<>();
    public final List<Perk> pendingPerkChoices = new ArrayList<>();
    public boolean perkChoicePending = false;

    // Bottegaio Mastro Paguro
    public float merchantX = 260f;
    public float merchantY = 260f;
    public boolean merchantPresent = true;
    public boolean merchantShopOpen = false;

    // Cooldowns
    public float attackCooldown = 0f;
    public float attackRate = 0.42f;
    public float waveCooldown = 0f;
    public float waveMaxCooldown = 6.0f;
    public float dashCooldown = 0f;
    public float dashMaxCooldown = 2.5f;
    public float potionCooldown = 0f;

    // Active Action Timers
    public float attackAnim = 0f;
    public float dashAnim = 0f;
    public float playerSquash = 0f;

    // Equipment & Inventory
    public Item equippedWeapon;
    public Item equippedArmor;
    public Item equippedRelic;
    public final List<Item> backpack = new ArrayList<>();

    // World Entities
    public final List<Enemy> enemies = new ArrayList<>();
    public final List<Drop> drops = new ArrayList<>();
    public final List<Projectile> projectiles = new ArrayList<>();
    public final List<Floater> floaters = new ArrayList<>();
    public final List<Particle> particles = new ArrayList<>();

    // Dungeon & Zone Progression
    public int zone = 1;
    public String zoneName = "Barriera Corallina";
    public int depthMeters = 200;
    public boolean portalOpen = false;
    public float portalX = 800f;
    public float portalY = 220f;
    public float portalRadius = 46f;
    public boolean gameOver = false;
    public boolean victory = false;

    // Juice & Feel knobs
    public float trauma = 0f;
    public float hitStop = 0f;
    public SoundListener soundListener;

    private final Random rng = new Random();

    public Game() {
        initPlayer();
        loadZone(1);
    }

    public void initPlayer() {
        level = 1;
        xp = 0;
        xpNext = 60;
        potions = 3;
        shells = 15;
        speedBoostTimer = 0f;
        skillPoints = 0;
        skillRanks.clear();
        skillTreeOpen = false;
        activePerks.clear();
        pendingPerkChoices.clear();
        perkChoicePending = false;
        merchantShopOpen = false;
        tide = getMaxTide();

        // Starting Equipment (con Set Corallino iniziale per apprendere i bonus)
        equippedWeapon = new Item("w_harpoon_copper", "Arpione di Rame", "weapon", RARITY_COMMON,
                16, 0, 0, 0f, 0.05f, "Asta di ottone bilanciata con arpione scanalato.", 0, null);
        equippedArmor = new Item("a_diver_suit", "Scafandro Esploratore", "armor", RARITY_COMMON,
                0, 6, 40, 0f, 0f, "Tuta impermeabile con rinforzi alle giunture.", 0, "Corallo");
        equippedRelic = new Item("r_coral_pebble", "Frammento di Madreperla", "relic", RARITY_COMMON,
                0, 0, 0, 15f, 0.05f, "Un frammento levigato che risuona con la marea.", 0, "Corallo");

        hp = getMaxHp();
        backpack.clear();
    }

    // Calcolo Set di Equipaggiamento
    public int countSetPieces(String setName) {
        if (setName == null) return 0;
        int count = 0;
        if (equippedWeapon != null && setName.equals(equippedWeapon.setName)) count++;
        if (equippedArmor != null && setName.equals(equippedArmor.setName)) count++;
        if (equippedRelic != null && setName.equals(equippedRelic.setName)) count++;
        return count;
    }

    public boolean isSetBonusActive(String setName) {
        return countSetPieces(setName) >= 2;
    }

    public Item getEquippedForSlot(String slot) {
        if ("weapon".equals(slot)) return equippedWeapon;
        if ("armor".equals(slot)) return equippedArmor;
        if ("relic".equals(slot)) return equippedRelic;
        return null;
    }

    public int getMaxHp() {
        int base = 80 + (level * 15);
        if (equippedArmor != null) base += equippedArmor.healthBonus;
        if (isSetBonusActive("Corallo")) base += 35; // Set Corallo (2pz): +35 PV
        if (hasSkill("jug_armor")) base += 35 * getSkillRank("jug_armor");
        return base;
    }

    public int getAttackDamage() {
        int base = 10 + (level * 4);
        if (equippedWeapon != null) base += equippedWeapon.damageBonus;
        if (isSetBonusActive("Abisso")) base += (int) (base * 0.22f); // Set Abisso (2pz): +22% Danno
        if (hasSkill("jug_titan")) base += (int) (base * 0.35f);
        if (hasSkill("jug_slam")) base += (int) (base * 0.30f);
        return base;
    }

    public int getDefense() {
        int base = 2 + (level * 2);
        if (equippedArmor != null) base += equippedArmor.defenseBonus;
        if (isSetBonusActive("Corallo")) base += 5; // Set Corallo (2pz): +5 Difesa
        if (hasSkill("jug_armor")) base += 8 * getSkillRank("jug_armor");
        if (activePerks.contains("perk_armor")) base += 6; // Talento retro-compatibile Guscio Corazzato
        return base;
    }

    public float getMoveSpeed() {
        float speed = 190f;
        if (equippedRelic != null) speed += equippedRelic.speedBonus;
        if (hasSkill("hunt_surge")) speed += 50f;
        if (speedBoostTimer > 0f) speed += 60f; // Buff velocità da pozione alchemica
        return speed;
    }

    public float getCritChance() {
        float crit = 0.08f;
        if (equippedWeapon != null) crit += equippedWeapon.critBonus;
        if (equippedRelic != null) crit += equippedRelic.critBonus;
        if (isSetBonusActive("Abisso")) crit += 0.12f; // Set Abisso (2pz): +12% Critico
        if (hasSkill("hunt_crit")) crit += 0.15f;
        if (activePerks.contains("perk_crit")) crit += 0.15f;
        return crit;
    }

    public float getMaxTide() {
        float base = 100f;
        if (isSetBonusActive("Marea")) base += 30f; // Set Marea (2pz): +30 Marea
        if (hasSkill("mys_tideflow")) base += 40f;
        return base;
    }

    // Gestione Classi e Albero Abilità Fisso
    public List<Skill> getSkillsForClass(String clz) {
        List<Skill> list = new ArrayList<>();
        for (Skill s : ALL_SKILLS) {
            if (s.classType.equals(clz)) list.add(s);
        }
        return list;
    }

    public int getSkillRank(String skillId) {
        return skillRanks.getOrDefault(skillId, 0);
    }

    public boolean hasSkill(String skillId) {
        return getSkillRank(skillId) > 0;
    }

    public Skill getSkillById(String id) {
        for (Skill s : ALL_SKILLS) {
            if (s.id.equals(id)) return s;
        }
        return null;
    }

    public boolean canUpgradeSkill(String skillId) {
        if (skillPoints <= 0) return false;
        Skill s = getSkillById(skillId);
        if (s == null) return false;
        if (getSkillRank(skillId) >= s.maxRank) return false;
        if (s.requiredSkillId != null && getSkillRank(s.requiredSkillId) <= 0) return false;
        return true;
    }

    public boolean upgradeSkill(String skillId) {
        if (!canUpgradeSkill(skillId)) return false;
        skillPoints--;
        int next = getSkillRank(skillId) + 1;
        skillRanks.put(skillId, next);
        Skill s = getSkillById(skillId);
        if (s != null) {
            floaters.add(new Floater(playerX, playerY - 60f, "ABILITÀ: " + s.name, s.color, true, 1.5f));
        }
        if (soundListener != null) soundListener.onLevelUp();
        return true;
    }

    public void respecSkills() {
        int spent = 0;
        for (int r : skillRanks.values()) spent += r;
        skillPoints += spent;
        skillRanks.clear();
        if (soundListener != null) soundListener.onClick();
        floaters.add(new Floater(playerX, playerY - 50f, "PUNTI ABILITÀ RESETTATI", 0xFFFCD34D, true, 1.3f));
    }

    public void selectClass(String newClass) {
        if (newClass.equals(playerClass)) return;
        respecSkills();
        playerClass = newClass;
        if (soundListener != null) soundListener.onClick();
        floaters.add(new Floater(playerX, playerY - 60f, "CLASSE: " + getClassName(newClass), 0xFF38BDF8, true, 1.5f));
    }

    public String getClassName(String clz) {
        if (CLASS_HUNTER.equals(clz)) return "Cacciatore dei Mari";
        if (CLASS_JUGGERNAUT.equals(clz)) return "Custode dei Fondali";
        if (CLASS_MYSTIC.equals(clz)) return "Mistico dell'Abisso";
        return clz;
    }

    // Gestione Talenti / Perk (Retro-compatibilità)
    public void generatePerkChoices() {
        pendingPerkChoices.clear();
        List<Perk> available = new ArrayList<>();
        for (Perk p : ALL_PERKS) {
            if (!activePerks.contains(p.id)) {
                available.add(p);
            }
        }
        if (available.isEmpty()) return;
        java.util.Collections.shuffle(available, rng);
        int count = Math.min(3, available.size());
        for (int i = 0; i < count; i++) {
            pendingPerkChoices.add(available.get(i));
        }
        perkChoicePending = true;
    }

    public boolean selectPerk(String perkId) {
        if (!perkChoicePending) return false;
        for (Perk p : pendingPerkChoices) {
            if (p.id.equals(perkId)) {
                activePerks.add(perkId);
                perkChoicePending = false;
                pendingPerkChoices.clear();
                floaters.add(new Floater(playerX, playerY - 60f, "TALENTO: " + p.title, p.color, true, 1.5f));
                if (soundListener != null) soundListener.onLevelUp();
                return true;
            }
        }
        return false;
    }

    public boolean hasPerk(String perkId) {
        if (activePerks.contains(perkId)) return true;
        if ("perk_pierce".equals(perkId)) return hasSkill("hunt_pierce");
        if ("perk_rapid".equals(perkId)) return hasSkill("hunt_rapid");
        if ("perk_vortex".equals(perkId)) return hasSkill("mys_vortex");
        if ("perk_electric_dash".equals(perkId)) return hasSkill("mys_lightning") || hasSkill("hunt_dash_cut");
        if ("perk_armor".equals(perkId)) return hasSkill("jug_armor");
        if ("perk_crit".equals(perkId)) return hasSkill("hunt_crit");
        if ("perk_alchemy".equals(perkId)) return hasSkill("mys_alchemy");
        return false;
    }

    // Bottega di Mastro Paguro
    public boolean buyPotionFromMerchant() {
        if (shells < 15 || potions >= maxPotions) return false;
        shells -= 15;
        potions++;
        floaters.add(new Floater(playerX, playerY - 45f, "+1 POZIONE", 0xFF4ADE80, true, 1.2f));
        if (soundListener != null) soundListener.onPotion();
        return true;
    }

    public boolean buyTideBlessing() {
        if (shells < 25) return false;
        shells -= 25;
        tide = getMaxTide();
        hp = Math.min(getMaxHp(), hp + 30);
        floaters.add(new Floater(playerX, playerY - 45f, "BENEDIZIONE DELLA MAREA!", 0xFF38BDF8, true, 1.4f));
        if (soundListener != null) soundListener.onWave();
        return true;
    }

    public boolean buyMysteryCoffer() {
        if (shells < 55 || backpack.size() >= 8) return false;
        shells -= 55;
        int targetRarity = rng.nextBoolean() ? RARITY_RARE : RARITY_ABYSSAL;
        Item cofferItem = generateRandomItem(zone + 1, true);
        cofferItem.rarity = targetRarity;
        backpack.add(cofferItem);
        floaters.add(new Floater(playerX, playerY - 45f, "+ " + cofferItem.name + "!", getRarityColor(targetRarity), true, 1.5f));
        if (soundListener != null) soundListener.onLoot(targetRarity);
        return true;
    }

    public void setSoundListener(SoundListener listener) {
        this.soundListener = listener;
    }

    public void loadZone(int z) {
        zone = z;
        enemies.clear();
        drops.clear();
        projectiles.clear();
        floaters.clear();
        particles.clear();
        portalOpen = false;

        playerX = MAP_W / 2f;
        playerY = MAP_H - 180f;
        playerTargetX = playerX;
        playerTargetY = playerY;
        playerMoving = false;
        beaconTimer = 0f;

        if (zone == 1) {
            zoneName = "Barriera Corallina";
            depthMeters = 220;
            // Spawn 6 enemies (Crabs and light Jellies) with balanced, compact radii
            spawnEnemy("crab", "Granchio di Scoglio", 500f, 400f, 75, 12, 16f, 75f);
            spawnEnemy("crab", "Granchio Corazzato", 1100f, 420f, 90, 14, 18f, 70f);
            spawnEnemy("crab", "Granchio Tagliatore", 800f, 480f, 70, 13, 16f, 80f);
            spawnEnemy("jelly", "Medusa Elettrica", 650f, 320f, 60, 15, 15f, 55f);
            spawnEnemy("jelly", "Medusa Fluttuante", 950f, 300f, 65, 15, 15f, 55f);
            spawnEnemy("crab", "Granchio Gigante", 800f, 260f, 130, 18, 20f, 65f);
        } else if (zone == 2) {
            zoneName = "Fossa delle Tenebre";
            depthMeters = 680;
            // Spawn 7 tougher enemies (Crabs, electric Jellies, Abyssal Eels)
            spawnEnemy("crab", "Granchio Abissale", 450f, 450f, 160, 22, 17f, 80f);
            spawnEnemy("crab", "Granchio di Zolfo", 1150f, 450f, 170, 24, 17f, 80f);
            spawnEnemy("jelly", "Medusa dei Fulmini", 600f, 350f, 120, 25, 15f, 65f);
            spawnEnemy("jelly", "Medusa dei Fulmini", 1000f, 350f, 120, 25, 15f, 65f);
            spawnEnemy("eel", "Murena dei Fondali", 800f, 380f, 140, 28, 16f, 120f);
            spawnEnemy("eel", "Murena Zannuta", 700f, 260f, 145, 28, 16f, 120f);
            spawnEnemy("crab", "Corazza Nera", 900f, 250f, 220, 28, 20f, 75f);
        } else {
            zoneName = "Santuario del Kraken";
            depthMeters = 1350;
            // Boss Arena!
            spawnBoss("Leviatano Primordiale", MAP_W / 2f, 360f, 750, 36, 52f, 50f);
        }

        // Refill half tide
        tide = maxTide;
    }

    private void spawnEnemy(String type, String name, float x, float y, int hp, int dmg, float radius, float speed) {
        String id = "e_" + System.nanoTime() + "_" + rng.nextInt(1000);
        enemies.add(new Enemy(id, type, name, x, y, hp, dmg, radius, speed));
    }

    private void spawnBoss(String name, float x, float y, int hp, int dmg, float radius, float speed) {
        String id = "boss_" + System.nanoTime();
        Enemy boss = new Enemy(id, "boss", name, x, y, hp, dmg, radius, speed);
        boss.attackInterval = 2.4f;
        boss.windupDuration = 1.3f;
        enemies.add(boss);
    }

    public void update(float dt) {
        if (gameOver || victory) return;

        // Hit-stop processing (freezes logic briefly on huge impacts)
        if (hitStop > 0f) {
            hitStop -= dt;
            return;
        }

        // Trauma decay
        if (trauma > 0f) {
            trauma = Math.max(0f, trauma - dt * 1.5f);
        }

        // Cooldowns
        if (attackCooldown > 0f) attackCooldown = Math.max(0f, attackCooldown - dt);
        if (waveCooldown > 0f) waveCooldown = Math.max(0f, waveCooldown - dt);
        if (dashCooldown > 0f) dashCooldown = Math.max(0f, dashCooldown - dt);
        if (potionCooldown > 0f) potionCooldown = Math.max(0f, potionCooldown - dt);

        if (attackAnim > 0f) attackAnim = Math.max(0f, attackAnim - dt * 4f);
        if (dashAnim > 0f) dashAnim = Math.max(0f, dashAnim - dt * 3.5f);
        if (playerSquash > 0f) playerSquash = Math.max(0f, playerSquash - dt * 5f);

        // Passive Tide regeneration
        float tideRegen = 10f;
        if (hasSkill("mys_tideflow")) tideRegen += 12f;
        if (tide < maxTide) {
            tide = Math.min(maxTide, tide + dt * tideRegen);
        }

        // Beacon animation
        if (beaconTimer > 0f) {
            beaconTimer = Math.max(0f, beaconTimer - dt * 2.2f);
        }

        // Player Movement
        updatePlayerMovement(dt);

        // Update Projectiles
        updateProjectiles(dt);

        // Update Enemies & AI
        updateEnemies(dt);

        // Drops pickup check
        updateDrops(dt);

        // Portal check
        updatePortal();

        // Update visual floaters & particles
        updateEffects(dt);
    }

    public void setPlayerTarget(float targetX, float targetY) {
        playerTargetX = Math.max(60f, Math.min(MAP_W - 60f, targetX));
        playerTargetY = Math.max(60f, Math.min(MAP_H - 60f, targetY));
        playerMoving = true;
        beaconX = playerTargetX;
        beaconY = playerTargetY;
        beaconTimer = 1.0f;
    }

    private void updatePlayerMovement(float dt) {
        if (joystickActive) {
            float lenSq = joystickX * joystickX + joystickY * joystickY;
            if (lenSq > 0.04f) {
                float len = (float) Math.sqrt(lenSq);
                float normX = joystickX / len;
                float normY = joystickY / len;
                playerFacingAngle = (float) Math.atan2(normY, normX);
                float currentSpeed = getMoveSpeed();
                if (dashAnim > 0f) currentSpeed *= 2.2f;

                playerX += normX * currentSpeed * dt;
                playerY += normY * currentSpeed * dt;
                playerMoving = true;

                // Spawn movement bubbles
                if (Math.random() < (dashAnim > 0f ? 0.8 : 0.25)) {
                    particles.add(new Particle(playerX + (float)(Math.random()*12-6),
                            playerY + (float)(Math.random()*12-6),
                            (float)(Math.random()*20-10), -30f - (float)(Math.random()*20),
                            3f + (float)(Math.random()*4), 0x9944C8FF, 0.6f));
                }
            } else {
                playerMoving = false;
            }

            // Boundary clamping
            playerX = Math.max(60f, Math.min(MAP_W - 60f, playerX));
            playerY = Math.max(60f, Math.min(MAP_H - 60f, playerY));
            return;
        }

        if (!playerMoving) return;

        float dx = playerTargetX - playerX;
        float dy = playerTargetY - playerY;
        float distSq = dx * dx + dy * dy;

        if (distSq > 16f) {
            float dist = (float) Math.sqrt(distSq);
            playerFacingAngle = (float) Math.atan2(dy, dx);
            float currentSpeed = getMoveSpeed();
            if (dashAnim > 0f) currentSpeed *= 2.2f;

            float step = currentSpeed * dt;
            if (step >= dist) {
                playerX = playerTargetX;
                playerY = playerTargetY;
                playerMoving = false;
            } else {
                playerX += (dx / dist) * step;
                playerY += (dy / dist) * step;
            }

            // Spawn movement bubbles
            if (Math.random() < (dashAnim > 0f ? 0.8 : 0.2)) {
                particles.add(new Particle(playerX + (float)(Math.random()*12-6),
                        playerY + (float)(Math.random()*12-6),
                        (float)(Math.random()*20-10), -30f - (float)(Math.random()*20),
                        3f + (float)(Math.random()*4), 0x9944C8FF, 0.6f));
            }
        } else {
            playerMoving = false;
        }

        // Boundary clamping
        playerX = Math.max(60f, Math.min(MAP_W - 60f, playerX));
        playerY = Math.max(60f, Math.min(MAP_H - 60f, playerY));
    }

    public boolean performAttack(float targetWorldX, float targetWorldY) {
        if (attackCooldown > 0f || gameOver || victory || perkChoicePending) return false;

        float effectiveRate = attackRate;
        if (hasPerk("perk_rapid")) effectiveRate = 0.28f;
        attackCooldown = effectiveRate;
        attackAnim = 1.0f;
        playerSquash = 0.5f;

        // Auto-aim to closest enemy if no specific coordinate provided
        if (targetWorldX < 0f && targetWorldY < 0f) {
            Enemy closest = null;
            float minDistSq = 600f * 600f;
            for (Enemy e : enemies) {
                if (e.hp <= 0) continue;
                float dX = e.x - playerX;
                float dY = e.y - playerY;
                float d2 = dX * dX + dY * dY;
                if (d2 < minDistSq) {
                    minDistSq = d2;
                    closest = e;
                }
            }
            if (closest != null) {
                targetWorldX = closest.x;
                targetWorldY = closest.y;
            } else {
                targetWorldX = playerX + (float) Math.cos(playerFacingAngle) * 150f;
                targetWorldY = playerY + (float) Math.sin(playerFacingAngle) * 150f;
            }
        }

        float dx = targetWorldX - playerX;
        float dy = targetWorldY - playerY;
        if (dx != 0 || dy != 0) {
            playerFacingAngle = (float) Math.atan2(dy, dx);
        }

        float angle = playerFacingAngle;
        float speed = hasPerk("perk_rapid") ? 640f : 520f;
        float vx = (float) Math.cos(angle) * speed;
        float vy = (float) Math.sin(angle) * speed;

        int dmg = getAttackDamage();
        boolean isCrit = rng.nextFloat() < getCritChance();
        float critMult = hasPerk("perk_crit") ? 2.2f : 1.8f;
        if (isCrit) dmg = (int) (dmg * critMult);

        int pColor = 0xFF38BDF8; // Aqua spear
        if (equippedWeapon != null) {
            if (equippedWeapon.rarity == RARITY_RARE) pColor = 0xFFFBBF24; // Gold
            else if (equippedWeapon.rarity == RARITY_ABYSSAL) pColor = 0xFFC084FC; // Purple
        }

        // Shoot harpoon spear projectile (with piercing if perk active)
        int pCount = hasPerk("perk_pierce") ? (1 + Math.max(1, getSkillRank("hunt_pierce")) * 2) : 1;
        Projectile proj = new Projectile(playerX, playerY, vx, vy, 8f, dmg, true, 0.45f, pColor);
        proj.pierceCount = pCount;
        projectiles.add(proj);

        // Tiro a Ventaglio (Cacciatore dei Mari)
        if (hasSkill("hunt_multishot")) {
            for (float offset : new float[]{-0.24f, 0.24f}) {
                float a = angle + offset;
                float msVx = (float) Math.cos(a) * speed;
                float msVy = (float) Math.sin(a) * speed;
                Projectile msProj = new Projectile(playerX, playerY, msVx, msVy, 8f, (int)(dmg * 0.80f), true, 0.45f, pColor);
                msProj.pierceCount = pCount;
                projectiles.add(msProj);
            }
        }

        if (soundListener != null) soundListener.onHit();
        return true;
    }

    public boolean performWave() {
        if (waveCooldown > 0f || tide < 30f || gameOver || victory || perkChoicePending) return false;

        tide -= 30f;
        float cd = isSetBonusActive("Marea") ? (waveMaxCooldown * 0.80f) : waveMaxCooldown;
        waveCooldown = cd;
        trauma = Math.min(1.0f, trauma + 0.45f);

        // Vortex perk: attrae i nemici verso il palombaro
        if (hasPerk("perk_vortex")) {
            for (Enemy e : enemies) {
                float dx = playerX - e.x;
                float dy = playerY - e.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > 10f && dist < 380f) {
                    e.x += (dx / dist) * 45f;
                    e.y += (dy / dist) * 45f;
                }
            }
        }

        // Spawn outward wave projectiles
        int waveCount = hasSkill("jug_shockwave") ? 20 : 14;
        float waveSpeed = hasSkill("jug_shockwave") ? 460f : 380f;
        int dmg = (int) (getAttackDamage() * (hasPerk("perk_vortex") ? 1.95f : 1.6f));
        for (int i = 0; i < waveCount; i++) {
            double angle = (2.0 * Math.PI / waveCount) * i;
            float vx = (float) (Math.cos(angle) * waveSpeed);
            float vy = (float) (Math.sin(angle) * waveSpeed);
            projectiles.add(new Projectile(playerX, playerY, vx, vy, 11f, dmg, true, 0.65f, 0xFF0284C7));
        }

        // Cataclisma del Kraken (Mistico dell'Abisso)
        if (hasSkill("mys_cataclysm")) {
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 / 10) * i;
                float vx = (float) (Math.cos(angle) * 310f);
                float vy = (float) (Math.sin(angle) * 310f);
                projectiles.add(new Projectile(playerX, playerY, vx, vy, 14f, (int)(dmg * 1.3f), true, 0.8f, 0xFFC084FC));
            }
        }

        // Wave particles
        for (int i = 0; i < 24; i++) {
            double angle = Math.random() * Math.PI * 2;
            float spd = 60f + (float)(Math.random() * 220f);
            particles.add(new Particle(playerX, playerY,
                    (float)Math.cos(angle) * spd, (float)Math.sin(angle) * spd,
                    6f, 0xBB38BDF8, 0.6f));
        }

        if (soundListener != null) soundListener.onWave();
        return true;
    }

    public boolean performDash() {
        if (dashCooldown > 0f || gameOver || victory || perkChoicePending) return false;

        float cd = isSetBonusActive("Marea") ? (dashMaxCooldown * 0.80f) : dashMaxCooldown;
        dashCooldown = cd;
        dashAnim = 1.0f;

        // Thrust toward facing direction or target
        float dx = (float) Math.cos(playerFacingAngle);
        float dy = (float) Math.sin(playerFacingAngle);
        playerTargetX = playerX + dx * 180f;
        playerTargetY = playerY + dy * 180f;
        playerMoving = true;

        // Talento Scatto Elettrico: scarica d'urto che folgora i nemici vicini
        if (hasPerk("perk_electric_dash")) {
            for (Enemy e : enemies) {
                float edx = e.x - playerX;
                float edy = e.y - playerY;
                if (edx * edx + edy * edy < 125f * 125f) {
                    damageEnemy(e, (int) (getAttackDamage() * 0.8f), e.x, e.y);
                }
            }
            for (int i = 0; i < 20; i++) {
                particles.add(new Particle(playerX, playerY,
                        (float) (Math.random() * 160 - 80), (float) (Math.random() * 160 - 80),
                        5f, 0xFFFCD34D, 0.5f));
            }
        }

        for (int i = 0; i < 15; i++) {
            particles.add(new Particle(playerX, playerY,
                    -dx * 60f + (float)(Math.random()*40-20),
                    -dy * 60f + (float)(Math.random()*40-20),
                    5f + (float)(Math.random()*4), 0xAA67E8F9, 0.45f));
        }

        if (soundListener != null) soundListener.onDash();
        return true;
    }

    public boolean usePotion() {
        if (potions <= 0 || hp >= getMaxHp() || potionCooldown > 0f || gameOver || victory || perkChoicePending) return false;

        potions--;
        potionCooldown = 1.2f;

        int heal = (int) (getMaxHp() * 0.45f);
        if (hasPerk("perk_alchemy")) {
            heal = (int) (getMaxHp() * 0.65f); // +40% cura aumentata
            speedBoostTimer = 5.0f; // Buff di velocita
            floaters.add(new Floater(playerX, playerY - 60f, "VELOCITÀ ALCHEMICA!", 0xFFA78BFA, true, 1.2f));
        }
        hp = Math.min(getMaxHp(), hp + heal);

        floaters.add(new Floater(playerX, playerY - 30f, "+" + heal + " PV", 0xFF22C55E, false, 0.9f));

        // Green healing sparkles
        for (int i = 0; i < 18; i++) {
            particles.add(new Particle(playerX + (float)(Math.random()*30-15),
                    playerY + (float)(Math.random()*30-15),
                    (float)(Math.random()*40-20), -50f - (float)(Math.random()*30),
                    5f, 0xCC4ADE80, 0.7f));
        }

        if (soundListener != null) soundListener.onPotion();
        return true;
    }

    private void updateProjectiles(float dt) {
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.life -= dt;

            if (p.life <= 0f) {
                it.remove();
                continue;
            }

            if (p.playerOwned) {
                // Check collision against enemies (supporta perforazione con perk_pierce)
                for (Enemy e : enemies) {
                    if (p.hitEnemyIds.contains(e.id)) continue;
                    float dx = e.x - p.x;
                    float dy = e.y - p.y;
                    float combinedR = e.radius + p.radius;
                    if (dx * dx + dy * dy < combinedR * combinedR) {
                        p.hitEnemyIds.add(e.id);
                        damageEnemy(e, p.damage, p.x, p.y);
                        p.pierceCount--;
                        if (p.pierceCount <= 0) {
                            it.remove();
                            break;
                        }
                    }
                }
            } else {
                // Check collision against player
                float dx = playerX - p.x;
                float dy = playerY - p.y;
                float combinedR = playerRadius + p.radius;
                if (dx * dx + dy * dy < combinedR * combinedR) {
                    damagePlayer(p.damage, p.x, p.y);
                    it.remove();
                }
            }
        }
    }

    public void damagePlayer(int rawDamage, float fromX, float fromY) {
        if (gameOver || victory || dashAnim > 0.4f) return; // Invincible during peak dash

        int def = getDefense();
        int taken = Math.max(1, rawDamage - (def / 2));

        // Custode: Fortezza Marina (riduzione danno 50% se HP bassi)
        if (hasSkill("jug_unbreakable") && hp < (int)(getMaxHp() * 0.35f)) {
            taken = Math.max(1, (int)(taken * 0.5f));
        }

        hp -= taken;
        trauma = Math.min(1.0f, trauma + 0.45f);
        playerSquash = 0.8f;

        // Custode: Spine di Corallo (riflette 40% del danno subito)
        if (hasSkill("jug_reflect")) {
            int reflected = Math.max(1, (int)(taken * 0.40f));
            for (Enemy ne : enemies) {
                float dx = ne.x - playerX;
                float dy = ne.y - playerY;
                if (dx * dx + dy * dy < 260f * 260f) {
                    ne.hp -= reflected;
                    floaters.add(new Floater(ne.x, ne.y - 30f, "SPINE " + reflected, 0xFFFB923C, false, 0.6f));
                }
            }
        }

        floaters.add(new Floater(playerX, playerY - 35f, "-" + taken, 0xFFEF4444, false, 0.8f));

        if (soundListener != null) soundListener.onHeavyHit();

        if (hp <= 0) {
            hp = 0;
            gameOver = true;
        }
    }

    public void damageEnemy(Enemy e, int damage, float hitX, float hitY) {
        e.hp -= damage;
        e.hitSquash = 0.6f;

        // Sifone Vitale (Mistico dell'Abisso)
        if (hasSkill("mys_siphon")) {
            int heal = 4;
            hp = Math.min(getMaxHp(), hp + heal);
            floaters.add(new Floater(playerX, playerY - 30f, "+" + heal + " PV", 0xFFA78BFA, false, 0.6f));
        }

        // Knockback (attenuato dal talento perk_armor se applicato a player, qui e' su nemici)
        float kx = e.x - hitX;
        float ky = e.y - hitY;
        float len = (float) Math.sqrt(kx * kx + ky * ky);
        if (len > 0.001f && !"boss".equals(e.type)) {
            e.x += (kx / len) * 20f;
            e.y += (ky / len) * 20f;
        }

        boolean isCrit = damage > (getAttackDamage() * 1.5f);
        floaters.add(new Floater(e.x, e.y - 25f, String.valueOf(damage),
                isCrit ? 0xFFFBBF24 : 0xFFFFFFFF, isCrit, 0.75f));

        if (isCrit) {
            trauma = Math.min(1.0f, trauma + 0.25f);
            hitStop = 0.045f;
            if (soundListener != null) soundListener.onHeavyHit();
        } else {
            if (soundListener != null) soundListener.onHit();
        }

        // Blood/fluid particles
        for (int i = 0; i < 8; i++) {
            particles.add(new Particle(hitX, hitY,
                    (float)(Math.random()*80-40), (float)(Math.random()*80-40),
                    4f, 0xCCF43F5E, 0.4f));
        }

        if (e.hp <= 0) {
            killEnemy(e);
        }
    }

    private void killEnemy(Enemy e) {
        e.hp = 0;
        int gainedXp = 20;
        if ("eel".equals(e.type)) gainedXp = 35;
        else if ("boss".equals(e.type)) gainedXp = 150;

        addXp(gainedXp);

        // Sifone Vitale per uccisione (Mistico dell'Abisso)
        if (hasSkill("mys_siphon")) {
            int heal = 12;
            hp = Math.min(getMaxHp(), hp + heal);
            floaters.add(new Floater(playerX, playerY - 30f, "+" + heal + " PV", 0xFFA78BFA, true, 0.8f));
        }

        // Death burst particles
        int particleCount = "boss".equals(e.type) ? 40 : 16;
        for (int i = 0; i < particleCount; i++) {
            particles.add(new Particle(e.x, e.y,
                    (float)(Math.random()*160-80), (float)(Math.random()*160-80),
                    5f + (float)(Math.random()*5), 0xEE38BDF8, 0.8f));
        }

        // Drop di Conchiglie d'oro sempre garantito!
        int shellReward = "boss".equals(e.type) ? (25 + rng.nextInt(15)) : (3 + rng.nextInt(5));
        drops.add(new Drop(e.x + rng.nextInt(24) - 12, e.y + rng.nextInt(24) - 12, shellReward));

        // Floor Loot Drop!
        rollLootDrop(e);
    }

    public void addXp(int amount) {
        xp += amount;
        floaters.add(new Floater(playerX, playerY - 55f, "+" + amount + " ESP", 0xFF60A5FA, false, 0.9f));

        while (xp >= xpNext) {
            xp -= xpNext;
            level++;
            skillPoints++;
            xpNext = (int) (xpNext * 1.45f);
            hp = getMaxHp(); // Full heal on level up
            floaters.add(new Floater(playerX, playerY - 75f, "LIVELLO " + level + "! +1 PUNTO ABILITÀ", 0xFFFCD34D, true, 1.4f));
            if (soundListener != null) soundListener.onLevelUp();
        }
    }

    private void rollLootDrop(Enemy e) {
        float roll = rng.nextFloat();
        // High chance for boss, normal for mobs
        float dropThreshold = "boss".equals(e.type) ? 1.0f : 0.45f;
        if (roll > dropThreshold) return;

        Item dropped = generateRandomItem(zone, "boss".equals(e.type));
        Drop drop = new Drop(e.x, e.y, dropped);
        drops.add(drop);

        if (soundListener != null) soundListener.onLoot(dropped.rarity);
    }

    public Item generateRandomItem(int zoneLevel, boolean isBoss) {
        int rarityRoll = rng.nextInt(100);
        int rarity = RARITY_COMMON;

        if (isBoss) {
            if (rarityRoll < 35) rarity = RARITY_ABYSSAL;
            else if (rarityRoll < 80) rarity = RARITY_RARE;
            else rarity = RARITY_MAGIC;
        } else {
            if (zoneLevel == 1) {
                if (rarityRoll < 5) rarity = RARITY_RARE;
                else if (rarityRoll < 35) rarity = RARITY_MAGIC;
                else rarity = RARITY_COMMON;
            } else if (zoneLevel == 2) {
                if (rarityRoll < 12) rarity = RARITY_ABYSSAL;
                else if (rarityRoll < 40) rarity = RARITY_RARE;
                else if (rarityRoll < 75) rarity = RARITY_MAGIC;
                else rarity = RARITY_COMMON;
            } else {
                if (rarityRoll < 25) rarity = RARITY_ABYSSAL;
                else if (rarityRoll < 65) rarity = RARITY_RARE;
                else rarity = RARITY_MAGIC;
            }
        }

        // Determinazione Set di Appartenenza (solo per oggetti non comuni)
        String setName = null;
        if (rarity >= RARITY_MAGIC) {
            int setRoll = rng.nextInt(100);
            if (setRoll < 38) setName = "Corallo";
            else if (setRoll < 72) setName = "Marea";
            else setName = "Abisso";
        }

        int slotType = rng.nextInt(3); // 0=weapon, 1=armor, 2=relic
        String id = "loot_" + System.nanoTime();
        int variant = rng.nextInt(3);

        if (slotType == 0) {
            String[] names = {
                    "Arpione di Cobalto", "Tridente della Barriera", "Lama degli Abissi",
                    "Falcione Idraulico", "Spiedo Corallino", "Squartatore Oceanico"
            };
            String name = names[rng.nextInt(names.length)];
            int dmg = 18 + (zoneLevel * 8) + (rarity * 12) + rng.nextInt(6);
            float crit = 0.05f + (rarity * 0.04f);
            return new Item(id, name, "weapon", rarity, dmg, 0, 0, 0f, crit,
                    "Un'arma marina pesante che penetra le corazze nemiche.", variant, setName);
        } else if (slotType == 1) {
            String[] names = {
                    "Scafandro Rinforzato", "Corazza di Chitina", "Piastra del Nautilus",
                    "Carapace Abissale", "Manto dei Fondali", "Guscio d'Acciaio"
            };
            String name = names[rng.nextInt(names.length)];
            int def = 6 + (zoneLevel * 4) + (rarity * 5) + rng.nextInt(3);
            int hpBonus = 30 + (zoneLevel * 25) + (rarity * 35) + rng.nextInt(20);
            return new Item(id, name, "armor", rarity, 0, def, hpBonus, 0f, 0f,
                    "Resiste alla tremenda pressione idrostatica dell'abisso.", variant, setName);
        } else {
            String[] names = {
                    "Cuore di Corallo", "Occhio del Kraken", "Perla Nera",
                    "Bussola Sommergibile", "Talismano delle Maree", "Amuleto del Fondo"
            };
            String name = names[rng.nextInt(names.length)];
            float spd = 10f + (rarity * 10f);
            float crit = 0.04f + (rarity * 0.03f);
            return new Item(id, name, "relic", rarity, 0, 0, 0, spd, crit,
                    "Un manufatto misterioso che incrementa marea e rapidita.", variant, setName);
        }
    }

    private void updateDrops(float dt) {
        Iterator<Drop> it = drops.iterator();
        while (it.hasNext()) {
            Drop d = it.next();
            d.bobPhase += dt * 3f;
            d.life -= dt;
            if (d.life <= 0f) {
                it.remove();
                continue;
            }

            // Auto-pickup if player is close
            float dx = playerX - d.x;
            float dy = playerY - d.y;
            if (dx * dx + dy * dy < 45f * 45f) {
                if (d.isShell()) {
                    shells += d.shellAmount;
                    floaters.add(new Floater(playerX, playerY - 40f, "+" + d.shellAmount + " CONCHIGLIE", 0xFFFBBF24, true, 1.1f));
                    if (soundListener != null) soundListener.onClick();
                    it.remove();
                } else if (d.item != null && backpack.size() < 8) {
                    backpack.add(d.item);
                    floaters.add(new Floater(playerX, playerY - 40f, "+ " + d.item.name,
                            getRarityColor(d.item.rarity), true, 1.2f));
                    if (soundListener != null) soundListener.onLoot(d.item.rarity);
                    it.remove();
                }
            }
        }
    }

    public int getRarityColor(int rarity) {
        switch (rarity) {
            case RARITY_MAGIC: return 0xFF38BDF8;
            case RARITY_RARE: return 0xFFFBBF24;
            case RARITY_ABYSSAL: return 0xFFC084FC;
            default: return 0xFFE2E8F0;
        }
    }

    private void updateEnemies(float dt) {
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (e.hp <= 0) {
                it.remove();
                continue;
            }

            if (e.hitSquash > 0f) e.hitSquash = Math.max(0f, e.hitSquash - dt * 4f);
            e.animTimer += dt;
            if (e.attackCooldown > 0f) e.attackCooldown = Math.max(0f, e.attackCooldown - dt);

            // Enemy AI
            float dx = playerX - e.x;
            float dy = playerY - e.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if ("boss".equals(e.type)) {
                updateBossAI(e, dist, dx, dy, dt);
            } else if ("jelly".equals(e.type)) {
                updateJellyAI(e, dist, dx, dy, dt);
            } else if ("eel".equals(e.type)) {
                updateEelAI(e, dist, dx, dy, dt);
            } else {
                // Crab melee
                updateCrabAI(e, dist, dx, dy, dt);
            }
        }
    }

    private void updateCrabAI(Enemy e, float dist, float dx, float dy, float dt) {
        if (dist > 30f && dist < 550f) {
            e.x += (dx / dist) * e.speed * dt;
            e.y += (dy / dist) * e.speed * dt;
        }

        if (dist < e.radius + playerRadius + 10f && e.attackCooldown <= 0f) {
            e.attackCooldown = e.attackInterval;
            damagePlayer(e.attackDamage, e.x, e.y);
        }
    }

    private void updateJellyAI(Enemy e, float dist, float dx, float dy, float dt) {
        // Floating rhythmically
        e.y += (float) Math.sin(e.animTimer * 2.5) * 15f * dt;

        // Keep distance from player, shoot electric bubbles
        if (dist < 220f) {
            e.x -= (dx / dist) * e.speed * 0.8f * dt;
            e.y -= (dy / dist) * e.speed * 0.8f * dt;
        } else if (dist > 380f && dist < 650f) {
            e.x += (dx / dist) * e.speed * dt;
            e.y += (dy / dist) * e.speed * dt;
        }

        if (dist < 500f && e.attackCooldown <= 0f) {
            e.attackCooldown = e.attackInterval;
            float spd = 200f;
            float vx = (dx / dist) * spd;
            float vy = (dy / dist) * spd;
            projectiles.add(new Projectile(e.x, e.y, vx, vy, 8f, e.attackDamage, false, 2.5f, 0xFF38BDF8));
        }
    }

    private void updateEelAI(Enemy e, float dist, float dx, float dy, float dt) {
        // High speed slither toward player
        if (dist > 35f && dist < 600f) {
            float wave = (float) Math.cos(e.animTimer * 6.0) * 40f;
            float perpX = -dy / dist;
            float perpY = dx / dist;

            e.x += ((dx / dist) * e.speed + perpX * wave) * dt;
            e.y += ((dy / dist) * e.speed + perpY * wave) * dt;
        }

        if (dist < e.radius + playerRadius + 12f && e.attackCooldown <= 0f) {
            e.attackCooldown = e.attackInterval;
            damagePlayer(e.attackDamage, e.x, e.y);
        }
    }

    private void updateBossAI(Enemy e, float dist, float dx, float dy, float dt) {
        // Boss telegraphs attacks with red ground circles!
        if (e.isWindingUp) {
            e.windupTimer -= dt;
            if (e.windupTimer <= 0f) {
                e.isWindingUp = false;
                // Unleash devastating slam at windup location!
                float sDx = playerX - e.windupX;
                float sDy = playerY - e.windupY;
                if (sDx * sDx + sDy * sDy < e.windupRadius * e.windupRadius) {
                    damagePlayer(e.attackDamage, e.windupX, e.windupY);
                }

                // Shockwave projectiles
                int ringCount = 10;
                for (int i = 0; i < ringCount; i++) {
                    double angle = (Math.PI * 2 / ringCount) * i;
                    float vx = (float) Math.cos(angle) * 240f;
                    float vy = (float) Math.sin(angle) * 240f;
                    projectiles.add(new Projectile(e.windupX, e.windupY, vx, vy, 14f, (int)(e.attackDamage * 0.7f), false, 1.8f, 0xFFEF4444));
                }

                trauma = 0.8f;
                hitStop = 0.06f;
                if (soundListener != null) soundListener.onHeavyHit();
            }
            return;
        }

        // Slowly advance toward center/player
        if (dist > 180f) {
            e.x += (dx / dist) * e.speed * dt;
            e.y += (dy / dist) * e.speed * dt;
        }

        if (e.attackCooldown <= 0f) {
            e.attackCooldown = e.attackInterval;
            // Initiate telegraphed slam on player's position
            e.isWindingUp = true;
            e.windupTimer = e.windupDuration;
            e.windupX = playerX;
            e.windupY = playerY;
            e.windupRadius = 110f;
        }
    }

    private void updatePortal() {
        if (enemies.isEmpty() && !portalOpen) {
            portalOpen = true;
            floaters.add(new Floater(portalX, portalY - 40f, "PORTALE DEGLI ABISSI APERTO", 0xFF38BDF8, true, 2.0f));
            if (soundListener != null) soundListener.onLevelUp();
        }

        if (portalOpen) {
            float dx = playerX - portalX;
            float dy = playerY - portalY;
            if (dx * dx + dy * dy < portalRadius * portalRadius) {
                // Enter portal to next zone or victory!
                if (zone < 3) {
                    loadZone(zone + 1);
                } else {
                    victory = true;
                }
            }
        }
    }

    private void updateEffects(float dt) {
        Iterator<Floater> fit = floaters.iterator();
        while (fit.hasNext()) {
            Floater f = fit.next();
            f.life -= dt;
            f.y -= dt * 38f;
            if (f.life <= 0f) fit.remove();
        }

        Iterator<Particle> pit = particles.iterator();
        while (pit.hasNext()) {
            Particle p = pit.next();
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.life -= dt;
            if (p.life <= 0f) pit.remove();
        }
    }

    // Equipment actions
    public boolean equipBackpackItem(int index) {
        if (index < 0 || index >= backpack.size()) return false;
        Item item = backpack.get(index);
        backpack.remove(index);

        if ("weapon".equals(item.slot)) {
            if (equippedWeapon != null) backpack.add(equippedWeapon);
            equippedWeapon = item;
        } else if ("armor".equals(item.slot)) {
            if (equippedArmor != null) backpack.add(equippedArmor);
            equippedArmor = item;
            hp = Math.min(hp, getMaxHp());
        } else if ("relic".equals(item.slot)) {
            if (equippedRelic != null) backpack.add(equippedRelic);
            equippedRelic = item;
        }

        if (soundListener != null) soundListener.onClick();
        return true;
    }

    public boolean dropBackpackItem(int index) {
        if (index < 0 || index >= backpack.size()) return false;
        Item item = backpack.remove(index);
        drops.add(new Drop(playerX + (float)(Math.random()*40-20), playerY + (float)(Math.random()*40-20), item));
        if (soundListener != null) soundListener.onClick();
        return true;
    }
}

