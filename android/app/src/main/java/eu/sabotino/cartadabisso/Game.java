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

        public Item(String id, String name, String slot, int rarity,
                    int damageBonus, int defenseBonus, int healthBonus,
                    float speedBonus, float critBonus, String desc, int visualVariant) {
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
        }

        public String getRarityName() {
            switch (rarity) {
                case RARITY_MAGIC: return "Magico";
                case RARITY_RARE: return "Raro";
                case RARITY_ABYSSAL: return "Abissale";
                default: return "Comune";
            }
        }
    }

    public static class Drop {
        public float x, y;
        public Item item;
        public float bobPhase;
        public float life = 90f;

        public Drop(float x, float y, Item item) {
            this.x = x;
            this.y = y;
            this.item = item;
            this.bobPhase = (float) (Math.random() * Math.PI * 2);
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
    public float playerRadius = 24f;

    // Movement Beacon (Diablo golden click ring)
    public float beaconX = -100f;
    public float beaconY = -100f;
    public float beaconTimer = 0f;

    // Stats & Leveling
    public int level = 1;
    public int xp = 0;
    public int xpNext = 60;
    public int hp = 120;
    public float tide = 100f;
    public float maxTide = 100f;
    public int potions = 3;
    public int maxPotions = 5;

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
        tide = maxTide;

        // Starting Equipment (Copper set)
        equippedWeapon = new Item("w_harpoon_copper", "Arpione di Rame", "weapon", RARITY_COMMON,
                16, 0, 0, 0f, 0.05f, "Asta di ottone bilanciata con arpione scanalato.", 0);
        equippedArmor = new Item("a_diver_suit", "Scafandro Esploratore", "armor", RARITY_COMMON,
                0, 6, 40, 0f, 0f, "Tuta impermeabile con rinforzi alle giunture.", 0);
        equippedRelic = new Item("r_coral_pebble", "Frammento di Madreperla", "relic", RARITY_COMMON,
                0, 0, 0, 15f, 0.05f, "Un frammento levigato che risuona con la marea.", 0);

        hp = getMaxHp();
        backpack.clear();
    }

    public int getMaxHp() {
        int base = 80 + (level * 15);
        if (equippedArmor != null) base += equippedArmor.healthBonus;
        return base;
    }

    public int getAttackDamage() {
        int base = 10 + (level * 4);
        if (equippedWeapon != null) base += equippedWeapon.damageBonus;
        return base;
    }

    public int getDefense() {
        int base = 2 + (level * 2);
        if (equippedArmor != null) base += equippedArmor.defenseBonus;
        return base;
    }

    public float getMoveSpeed() {
        float speed = 190f;
        if (equippedRelic != null) speed += equippedRelic.speedBonus;
        return speed;
    }

    public float getCritChance() {
        float crit = 0.08f;
        if (equippedWeapon != null) crit += equippedWeapon.critBonus;
        if (equippedRelic != null) crit += equippedRelic.critBonus;
        return crit;
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
            // Spawn 8 enemies (Crabs and light Jellies)
            spawnEnemy("crab", "Granchio di Scoglio", 500f, 400f, 75, 12, 28f, 75f);
            spawnEnemy("crab", "Granchio Corazzato", 1100f, 420f, 90, 14, 30f, 70f);
            spawnEnemy("crab", "Granchio Tagliatore", 800f, 480f, 70, 13, 26f, 80f);
            spawnEnemy("jelly", "Medusa Elettrica", 650f, 320f, 60, 15, 24f, 55f);
            spawnEnemy("jelly", "Medusa Fluttuante", 950f, 300f, 65, 15, 24f, 55f);
            spawnEnemy("crab", "Granchio Gigante", 800f, 260f, 130, 18, 34f, 65f);
        } else if (zone == 2) {
            zoneName = "Fossa delle Tenebre";
            depthMeters = 680;
            // Spawn 9 tougher enemies (Crabs, electric Jellies, Abyssal Eels)
            spawnEnemy("crab", "Granchio Abissale", 450f, 450f, 160, 22, 32f, 80f);
            spawnEnemy("crab", "Granchio di Zolfo", 1150f, 450f, 170, 24, 32f, 80f);
            spawnEnemy("jelly", "Medusa dei Fulmini", 600f, 350f, 120, 25, 26f, 65f);
            spawnEnemy("jelly", "Medusa dei Fulmini", 1000f, 350f, 120, 25, 26f, 65f);
            spawnEnemy("eel", "Murena dei Fondali", 800f, 380f, 140, 28, 26f, 120f);
            spawnEnemy("eel", "Murena Zannuta", 700f, 260f, 145, 28, 26f, 120f);
            spawnEnemy("crab", "Corazza Nera", 900f, 250f, 220, 28, 36f, 75f);
        } else {
            zoneName = "Santuario del Kraken";
            depthMeters = 1350;
            // Boss Arena!
            spawnBoss("Leviatano Primordiale", MAP_W / 2f, 360f, 750, 36, 68f, 50f);
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
        if (tide < maxTide) {
            tide = Math.min(maxTide, tide + dt * 10f);
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
        if (attackCooldown > 0f || gameOver || victory) return false;

        attackCooldown = attackRate;
        attackAnim = 1.0f;
        playerSquash = 0.5f;

        float dx = targetWorldX - playerX;
        float dy = targetWorldY - playerY;
        if (dx != 0 || dy != 0) {
            playerFacingAngle = (float) Math.atan2(dy, dx);
        }

        float angle = playerFacingAngle;
        float speed = 520f;
        float vx = (float) Math.cos(angle) * speed;
        float vy = (float) Math.sin(angle) * speed;

        int dmg = getAttackDamage();
        boolean isCrit = rng.nextFloat() < getCritChance();
        if (isCrit) dmg = (int) (dmg * 1.8f);

        int pColor = 0xFF38BDF8; // Aqua spear
        if (equippedWeapon != null) {
            if (equippedWeapon.rarity == RARITY_RARE) pColor = 0xFFFBBF24; // Gold
            else if (equippedWeapon.rarity == RARITY_ABYSSAL) pColor = 0xFFC084FC; // Purple
        }

        // Shoot harpoon spear projectile
        projectiles.add(new Projectile(playerX, playerY, vx, vy, 14f, dmg, true, 0.45f, pColor));

        if (soundListener != null) soundListener.onHit();
        return true;
    }

    public boolean performWave() {
        if (waveCooldown > 0f || tide < 30f || gameOver || victory) return false;

        tide -= 30f;
        waveCooldown = waveMaxCooldown;
        trauma = Math.min(1.0f, trauma + 0.45f);

        // Spawn outward wave projectiles
        int waveCount = 14;
        int dmg = (int) (getAttackDamage() * 1.6f);
        for (int i = 0; i < waveCount; i++) {
            double angle = (2.0 * Math.PI / waveCount) * i;
            float vx = (float) (Math.cos(angle) * 380.0);
            float vy = (float) (Math.sin(angle) * 380.0);
            projectiles.add(new Projectile(playerX, playerY, vx, vy, 18f, dmg, true, 0.65f, 0xFF0284C7));
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
        if (dashCooldown > 0f || gameOver || victory) return false;

        dashCooldown = dashMaxCooldown;
        dashAnim = 1.0f;

        // Thrust toward facing direction or target
        float dx = (float) Math.cos(playerFacingAngle);
        float dy = (float) Math.sin(playerFacingAngle);
        playerTargetX = playerX + dx * 180f;
        playerTargetY = playerY + dy * 180f;
        playerMoving = true;

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
        if (potions <= 0 || hp >= getMaxHp() || potionCooldown > 0f || gameOver || victory) return false;

        potions--;
        potionCooldown = 1.2f;
        int heal = (int) (getMaxHp() * 0.45f);
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
                // Check collision against enemies
                boolean hit = false;
                for (Enemy e : enemies) {
                    float dx = e.x - p.x;
                    float dy = e.y - p.y;
                    float combinedR = e.radius + p.radius;
                    if (dx * dx + dy * dy < combinedR * combinedR) {
                        hit = true;
                        damageEnemy(e, p.damage, p.x, p.y);
                        break;
                    }
                }
                if (hit) {
                    it.remove();
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
        hp -= taken;
        trauma = Math.min(1.0f, trauma + 0.45f);
        playerSquash = 0.8f;

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

        // Knockback
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

        // Death burst particles
        int particleCount = "boss".equals(e.type) ? 40 : 16;
        for (int i = 0; i < particleCount; i++) {
            particles.add(new Particle(e.x, e.y,
                    (float)(Math.random()*160-80), (float)(Math.random()*160-80),
                    5f + (float)(Math.random()*5), 0xEE38BDF8, 0.8f));
        }

        // Floor Loot Drop!
        rollLootDrop(e);
    }

    private void addXp(int amount) {
        xp += amount;
        floaters.add(new Floater(playerX, playerY - 55f, "+" + amount + " ESP", 0xFF60A5FA, false, 0.9f));

        while (xp >= xpNext) {
            xp -= xpNext;
            level++;
            xpNext = (int) (xpNext * 1.45f);
            hp = getMaxHp(); // Full heal on level up
            floaters.add(new Floater(playerX, playerY - 75f, "LIVELLO " + level + "!", 0xFFFCD34D, true, 1.4f));
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
                    "Un'arma marina pesante che penetra le corazze nemiche.", variant);
        } else if (slotType == 1) {
            String[] names = {
                    "Scafandro Rinforzato", "Corazza di Chitina", "Piastra del Nautilus",
                    "Carapace Abissale", "Manto dei Fondali", "Guscio d'Acciaio"
            };
            String name = names[rng.nextInt(names.length)];
            int def = 6 + (zoneLevel * 4) + (rarity * 5) + rng.nextInt(3);
            int hpBonus = 30 + (zoneLevel * 25) + (rarity * 35) + rng.nextInt(20);
            return new Item(id, name, "armor", rarity, 0, def, hpBonus, 0f, 0f,
                    "Resiste alla tremenda pressione idrostatica dell'abisso.", variant);
        } else {
            String[] names = {
                    "Cuore di Corallo", "Occhio del Kraken", "Perla Nera",
                    "Bussola Sommergibile", "Talismano delle Maree", "Amuleto del Fondo"
            };
            String name = names[rng.nextInt(names.length)];
            float spd = 10f + (rarity * 10f);
            float crit = 0.04f + (rarity * 0.03f);
            return new Item(id, name, "relic", rarity, 0, 0, 0, spd, crit,
                    "Un manufatto misterioso che incrementa marea e rapidita.", variant);
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
                if (backpack.size() < 8) {
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
            projectiles.add(new Projectile(e.x, e.y, vx, vy, 11f, e.attackDamage, false, 2.5f, 0xFF38BDF8));
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

