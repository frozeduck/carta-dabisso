package eu.sabotino.cartadabisso;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private Thread gameThread;
    private volatile boolean running = false;
    private final SurfaceHolder holder;

    public final Game game;
    public boolean paused = false;
    public boolean inventoryOpen = false;
    private int selectedBackpackIndex = -1;

    // Viewport scaling
    private float viewScale = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private long lastTimeNs = 0;

    // Camera
    private float camX = 0f;
    private float camY = 0f;

    // Reusable Paints
    private final Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tempPath = new Path();
    private final RectF tempRect = new RectF();

    // HUD Hit Rectangles in Virtual Coordinates (960x540)
    private final RectF globeHpRect = new RectF(14f, 426f, 110f, 522f);
    private final RectF globeTideRect = new RectF(850f, 426f, 946f, 522f);

    // Skill Bar Buttons (Bottom-Center)
    private final RectF btnAttack = new RectF(330f, 464f, 386f, 520f);
    private final RectF btnWave = new RectF(396f, 464f, 452f, 520f);
    private final RectF btnDash = new RectF(462f, 464f, 518f, 520f);
    private final RectF btnPotion = new RectF(528f, 464f, 584f, 520f);
    private final RectF btnInventory = new RectF(594f, 464f, 650f, 520f);

    // Sound toggle (Top-Right)
    private final RectF btnSound = new RectF(905f, 12f, 945f, 48f);

    // Inventory Modal Rectangles
    private final RectF modalRect = new RectF(100f, 45f, 860f, 495f);
    private final RectF btnCloseModal = new RectF(815f, 55f, 850f, 85f);
    private final RectF slotEquipWeapon = new RectF(130f, 120f, 310f, 175f);
    private final RectF slotEquipArmor = new RectF(130f, 185f, 310f, 240f);
    private final RectF slotEquipRelic = new RectF(130f, 250f, 310f, 305f);

    // Backpack 8-slot grid (4x2)
    private final RectF[] backpackSlots = new RectF[8];
    private final RectF btnEquipSelected = new RectF(560f, 370f, 680f, 415f);
    private final RectF btnDropSelected = new RectF(700f, 370f, 810f, 415f);

    // Game Over / Victory Buttons
    private final RectF btnRestart = new RectF(380f, 340f, 580f, 395f);

    // Drag-to-move tracker
    private boolean isDraggingToMove = false;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        game = new Game();
        setupSoundBridge();
        initBackpackSlotRects();
    }

    private void setupSoundBridge() {
        game.setSoundListener(new Game.SoundListener() {
            @Override public void onHit() { AudioHelper.playHit(); }
            @Override public void onHeavyHit() { AudioHelper.playHeavyHit(); }
            @Override public void onWave() { AudioHelper.playWave(); }
            @Override public void onDash() { AudioHelper.playDash(); }
            @Override public void onPotion() { AudioHelper.playPotion(); }
            @Override public void onLoot(int rarity) { AudioHelper.playLoot(rarity); }
            @Override public void onClick() { AudioHelper.playClick(); }
            @Override public void onLevelUp() { AudioHelper.playLevelUp(); }
        });
    }

    private void initBackpackSlotRects() {
        float startX = 540f;
        float startY = 120f;
        float slotW = 65f;
        float slotH = 65f;
        float gap = 12f;

        for (int i = 0; i < 8; i++) {
            int col = i % 4;
            int row = i / 4;
            float left = startX + col * (slotW + gap);
            float top = startY + row * (slotH + gap);
            backpackSlots[i] = new RectF(left, top, left + slotW, top + slotH);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        resume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        updateViewport(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    private void updateViewport(int w, int h) {
        viewScale = Math.min((float) w / Game.SCREEN_W, (float) h / Game.SCREEN_H);
        offsetX = (w - Game.SCREEN_W * viewScale) / 2f;
        offsetY = (h - Game.SCREEN_H * viewScale) / 2f;
    }

    public void resume() {
        if (!running) {
            running = true;
            lastTimeNs = System.nanoTime();
            gameThread = new Thread(this, "GameLoopThread");
            gameThread.start();
        }
    }

    public void pause() {
        running = false;
        if (gameThread != null) {
            try {
                gameThread.join();
            } catch (InterruptedException ignored) {}
            gameThread = null;
        }
    }

    public void togglePause() {
        paused = !paused;
    }

    public void toggleInventory() {
        inventoryOpen = !inventoryOpen;
        selectedBackpackIndex = -1;
        AudioHelper.playClick();
    }

    public void restartGame() {
        game.initPlayer();
        game.loadZone(1);
        game.gameOver = false;
        game.victory = false;
        inventoryOpen = false;
        AudioHelper.playClick();
    }

    @Override
    public void run() {
        while (running) {
            long nowNs = System.nanoTime();
            float dt = (nowNs - lastTimeNs) / 1_000_000_000f;
            lastTimeNs = nowNs;
            if (dt > 0.08f) dt = 0.08f; // Clamp large delta drops

            if (!paused && !inventoryOpen) {
                game.update(dt);
            }

            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    synchronized (holder) {
                        drawGame(canvas);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (Exception ignored) {}
                }
            }

            // Target ~60 FPS
            long sleepMs = 16 - (System.nanoTime() - nowNs) / 1_000_000;
            if (sleepMs > 0) {
                SystemClock.sleep(sleepMs);
            }
        }
    }

    private void updateCamera() {
        // Center on diver with boundary clamping
        float targetCamX = game.playerX - Game.SCREEN_W / 2f;
        float targetCamY = game.playerY - Game.SCREEN_H / 2f;

        targetCamX = Math.max(0f, Math.min(Game.MAP_W - Game.SCREEN_W, targetCamX));
        targetCamY = Math.max(0f, Math.min(Game.MAP_H - Game.SCREEN_H, targetCamY));

        // Smooth camera lerp
        camX += (targetCamX - camX) * 0.15f;
        camY += (targetCamY - camY) * 0.15f;

        // Apply trauma shake
        if (game.trauma > 0.01f) {
            float shakeMag = game.trauma * game.trauma * 16f;
            float time = (float) (System.currentTimeMillis() / 1000.0);
            camX += (float) Math.sin(time * 38.0) * shakeMag;
            camY += (float) Math.cos(time * 44.0) * shakeMag;
        }
    }

    private void drawGame(Canvas canvas) {
        updateCamera();

        // Fill background letterbox bars
        canvas.drawColor(0xFF030712);

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(viewScale, viewScale);

        // Clip game viewport
        canvas.clipRect(0f, 0f, Game.SCREEN_W, Game.SCREEN_H);

        // 1. Draw World Layer (under camera transform)
        canvas.save();
        canvas.translate(-camX, -camY);
        drawWorld(canvas);
        canvas.restore();

        // 2. Draw Diablo HUD Layer (fixed screen coordinates)
        drawHud(canvas);

        // 3. Draw Overlays (Inventory, Game Over, Victory, Paused)
        if (inventoryOpen) {
            drawInventoryModal(canvas);
        } else if (game.gameOver) {
            drawGameOverOverlay(canvas);
        } else if (game.victory) {
            drawVictoryOverlay(canvas);
        } else if (paused) {
            drawPauseOverlay(canvas);
        }

        canvas.restore();
    }

    private void drawWorld(Canvas canvas) {
        // Deep seabed gradient
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(0, 0, 0, Game.MAP_H,
                0xFF082838, 0xFF02121D, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, Game.MAP_W, Game.MAP_H, paintFill);
        paintFill.setShader(null);

        // Isometric seabed stone grid
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0x1838BDF8);
        paintStroke.setStrokeWidth(1.2f);
        float tileSize = 80f;
        for (float x = 0; x < Game.MAP_W + tileSize; x += tileSize) {
            canvas.drawLine(x, 0, x - tileSize * 4, Game.MAP_H, paintStroke);
            canvas.drawLine(x, 0, x + tileSize * 4, Game.MAP_H, paintStroke);
        }

        // Seabed boundary line
        paintStroke.setColor(0x440284C7);
        paintStroke.setStrokeWidth(4f);
        canvas.drawRect(50f, 50f, Game.MAP_W - 50f, Game.MAP_H - 50f, paintStroke);

        // Oceanic corals & seabed rocks
        drawEnvironmentProps(canvas);

        // Destination Click Beacon (Diablo golden concentric rings)
        if (game.beaconTimer > 0f) {
            drawClickBeacon(canvas, game.beaconX, game.beaconY, game.beaconTimer);
        }

        // Portal to next depth
        if (game.portalOpen) {
            drawPortal(canvas, game.portalX, game.portalY);
        }

        // Drops on seabed with Rarity Beams
        for (Game.Drop drop : game.drops) {
            drawDrop(canvas, drop);
        }

        // Enemies & Boss
        for (Game.Enemy enemy : game.enemies) {
            drawEnemy(canvas, enemy);
        }

        // Diver (Player)
        drawDiver(canvas);

        // Projectiles
        for (Game.Projectile p : game.projectiles) {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(p.color);
            canvas.drawCircle(p.x, p.y, p.radius, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFFFFFFFF);
            paintStroke.setStrokeWidth(2f);
            canvas.drawCircle(p.x, p.y, p.radius, paintStroke);
        }

        // Floating combat numbers
        for (Game.Floater f : game.floaters) {
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setColor(f.color);
            paintText.setTextSize(f.isCrit ? 22f : 16f);
            paintText.setFakeBoldText(true);
            paintText.setAlpha((int) (Math.min(1.0f, f.life / 0.35f) * 255));
            canvas.drawText(f.text, f.x, f.y, paintText);
        }
        paintText.setAlpha(255);

        // Underwater particles (bubbles / debris)
        for (Game.Particle p : game.particles) {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(p.color);
            float alpha = p.life / p.maxLife;
            paintFill.setAlpha((int) (alpha * 220));
            canvas.drawCircle(p.x, p.y, p.size * alpha, paintFill);
        }
        paintFill.setAlpha(255);
    }

    private void drawEnvironmentProps(Canvas canvas) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x2A155E75);

        // Fixed coral mounds
        float[][] corals = {
                {220f, 260f, 45f}, {480f, 180f, 55f}, {1250f, 240f, 60f},
                {1420f, 680f, 50f}, {280f, 850f, 65f}, {1280f, 860f, 55f}
        };
        for (float[] c : corals) {
            canvas.drawCircle(c[0], c[1], c[2], paintFill);
            // Inner ring
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0x3522D3EE);
            paintStroke.setStrokeWidth(2f);
            canvas.drawCircle(c[0], c[1], c[2] * 0.7f, paintStroke);
        }
    }

    private void drawClickBeacon(Canvas canvas, float x, float y, float timer) {
        float radius = (1.0f - timer) * 48f;
        int alpha = (int) (timer * 255);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(3f);
        paintStroke.setColor(0xFFFBBF24); // Diablo golden beacon
        paintStroke.setAlpha(alpha);
        canvas.drawCircle(x, y, radius, paintStroke);

        paintStroke.setStrokeWidth(1.5f);
        paintStroke.setColor(0xFFF59E0B);
        paintStroke.setAlpha((int) (alpha * 0.6f));
        canvas.drawCircle(x, y, radius * 0.5f, paintStroke);
        paintStroke.setAlpha(255);
    }

    private void drawPortal(Canvas canvas, float x, float y) {
        float time = (float) (System.currentTimeMillis() / 400.0);
        // Swirling vortex rings
        paintStroke.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < 3; i++) {
            float r = 24f + i * 12f + (float) Math.sin(time + i) * 4f;
            paintStroke.setStrokeWidth(3.5f - i);
            paintStroke.setColor(i == 0 ? 0xFF67E8F9 : 0xFF0284C7);
            canvas.drawCircle(x, y, r, paintStroke);
        }

        // Center glow
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x8838BDF8);
        canvas.drawCircle(x, y, 20f, paintFill);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(0xFFE0F2FE);
        paintText.setTextSize(13f);
        paintText.setFakeBoldText(true);
        canvas.drawText("DISCESA", x, y - 48f, paintText);
    }

    private void drawDrop(Canvas canvas, Game.Drop drop) {
        int rColor = game.getRarityColor(drop.item.rarity);
        float bobY = (float) Math.sin(drop.bobPhase) * 6f;
        float cy = drop.y + bobY;

        // 1. Rarity Light Beam shooting skyward
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(drop.x, cy, drop.x, cy - 240f,
                rColor & 0x77FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        tempRect.set(drop.x - 12f, cy - 240f, drop.x + 12f, cy);
        canvas.drawRect(tempRect, paintFill);
        paintFill.setShader(null);

        // 2. Base glow ring on ground
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(rColor);
        paintStroke.setStrokeWidth(2f);
        canvas.drawOval(drop.x - 18f, drop.y - 6f, drop.x + 18f, drop.y + 6f, paintStroke);

        // 3. Floating Diamond Rune
        tempPath.reset();
        tempPath.moveTo(drop.x, cy - 16f);
        tempPath.lineTo(drop.x + 10f, cy);
        tempPath.lineTo(drop.x, cy + 16f);
        tempPath.lineTo(drop.x - 10f, cy);
        tempPath.close();

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(rColor);
        canvas.drawPath(tempPath, paintFill);
        paintStroke.setColor(0xFFFFFFFF);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawPath(tempPath, paintStroke);

        // 4. Compact Name Plaque
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(12f);
        paintText.setFakeBoldText(true);
        paintText.setColor(0xFF030712);

        float textW = paintText.measureText(drop.item.name) + 16f;
        paintFill.setColor(0xCC030712);
        canvas.drawRoundRect(drop.x - textW / 2f, cy - 38f, drop.x + textW / 2f, cy - 20f, 4f, 4f, paintFill);

        paintText.setColor(rColor);
        canvas.drawText(drop.item.name, drop.x, cy - 26f, paintText);
    }

    private void drawDiver(Canvas canvas) {
        float px = game.playerX;
        float py = game.playerY;
        float facing = game.playerFacingAngle;

        canvas.save();
        canvas.translate(px, py);

        // Squash & stretch on hit/attack
        float scaleX = 1f + game.playerSquash * 0.25f;
        float scaleY = 1f - game.playerSquash * 0.2f;
        canvas.scale(scaleX, scaleY);

        // Seabed shadow
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x55020617);
        canvas.drawOval(-22f, 16f, 22f, 26f, paintFill);

        // Relic aura glow (if equipped)
        if (game.equippedRelic != null) {
            float relicPhase = (float) (System.currentTimeMillis() / 300.0);
            float orbitX = (float) Math.cos(relicPhase) * 28f;
            float orbitY = (float) Math.sin(relicPhase) * 20f;

            paintFill.setColor(game.getRarityColor(game.equippedRelic.rarity));
            paintFill.setAlpha(160);
            canvas.drawCircle(orbitX, orbitY, 6f, paintFill);
            paintFill.setAlpha(255);
        }

        // Diving Suit Color based on equipped Armor
        int suitColor = 0xFF475569; // Common slate
        int plateColor = 0xFF334155;
        if (game.equippedArmor != null) {
            if (game.equippedArmor.rarity == Game.RARITY_MAGIC) {
                suitColor = 0xFF1E3A8A; plateColor = 0xFF172554; // Marine blue
            } else if (game.equippedArmor.rarity == Game.RARITY_RARE) {
                suitColor = 0xFFB45309; plateColor = 0xFF78350F; // Bronze/Gold
            } else if (game.equippedArmor.rarity == Game.RARITY_ABYSSAL) {
                suitColor = 0xFF581C87; plateColor = 0xFF3B0764; // Abyssal purple
            }
        }

        // Oxygen Tanks on diver's back
        paintFill.setColor(0xFFD97706); // Brass
        canvas.drawRoundRect(-16f, -14f, -8f, 16f, 4f, 4f, paintFill);
        canvas.drawRoundRect(8f, -14f, 16f, 16f, 4f, 4f, paintFill);

        // Main Diving Suit Body
        paintFill.setColor(suitColor);
        canvas.drawRoundRect(-15f, -18f, 15f, 18f, 8f, 8f, paintFill);

        // Armor Breastplate
        paintFill.setColor(plateColor);
        canvas.drawRoundRect(-10f, -10f, 10f, 12f, 4f, 4f, paintFill);

        // Brass Diving Helmet
        paintFill.setColor(0xFFD97706); // Brass rim
        canvas.drawCircle(0f, -22f, 14f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF78350F);
        paintStroke.setStrokeWidth(2f);
        canvas.drawCircle(0f, -22f, 14f, paintStroke);

        // Helmet Porthole / Glass Visor
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF67E8F9); // Glowing cyan porthole
        canvas.drawCircle((float) Math.cos(facing) * 5f, -22f + (float) Math.sin(facing) * 3f, 6.5f, paintFill);

        // Weapon (held in hand toward facing direction)
        drawEquippedWeapon(canvas, facing);

        canvas.restore();
    }

    private void drawEquippedWeapon(Canvas canvas, float facing) {
        canvas.save();
        canvas.rotate((float) Math.toDegrees(facing));

        float thrustOffset = game.attackAnim * 28f;
        int weaponColor = 0xFFCBD5E1;
        if (game.equippedWeapon != null) {
            weaponColor = game.getRarityColor(game.equippedWeapon.rarity);
        }

        // Weapon Shaft (Spear pole)
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF78350F);
        paintStroke.setStrokeWidth(3.5f);
        canvas.drawLine(10f + thrustOffset, 0f, 34f + thrustOffset, 0f, paintStroke);

        // Weapon Blade / Head
        tempPath.reset();
        tempPath.moveTo(34f + thrustOffset, -6f);
        tempPath.lineTo(48f + thrustOffset, 0f);
        tempPath.lineTo(34f + thrustOffset, 6f);
        tempPath.close();

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(weaponColor);
        canvas.drawPath(tempPath, paintFill);

        // Weapon Tip Glow
        paintStroke.setColor(0xFFFFFFFF);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawPath(tempPath, paintStroke);

        canvas.restore();
    }

    private void drawEnemy(Canvas canvas, Game.Enemy e) {
        // Red warning circle if enemy is winding up a telegraphed slam!
        if (e.isWindingUp) {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0x44EF4444);
            canvas.drawCircle(e.windupX, e.windupY, e.windupRadius, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFFEF4444);
            paintStroke.setStrokeWidth(3f);
            canvas.drawCircle(e.windupX, e.windupY, e.windupRadius, paintStroke);

            // Expanding telegraph shock ring
            float progress = 1.0f - (e.windupTimer / e.windupDuration);
            canvas.drawCircle(e.windupX, e.windupY, e.windupRadius * progress, paintStroke);
        }

        canvas.save();
        canvas.translate(e.x, e.y);

        float squashX = 1f + e.hitSquash * 0.3f;
        float squashY = 1f - e.hitSquash * 0.25f;
        canvas.scale(squashX, squashY);

        if ("boss".equals(e.type)) {
            drawBossLeviathan(canvas, e);
        } else if ("jelly".equals(e.type)) {
            drawJellyfish(canvas, e);
        } else if ("eel".equals(e.type)) {
            drawAbyssalEel(canvas, e);
        } else {
            drawCrab(canvas, e);
        }

        // Enemy Health Bar
        float barW = e.radius * 2f;
        float barH = 5f;
        float barY = -e.radius - 12f;

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF0F172A);
        canvas.drawRect(-barW / 2f, barY, barW / 2f, barY + barH, paintFill);

        float hpPct = (float) e.hp / e.maxHp;
        paintFill.setColor(hpPct > 0.4f ? 0xFFEF4444 : 0xFFDC2626);
        canvas.drawRect(-barW / 2f, barY, -barW / 2f + (barW * hpPct), barY + barH, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF334155);
        paintStroke.setStrokeWidth(1f);
        canvas.drawRect(-barW / 2f, barY, barW / 2f, barY + barH, paintStroke);

        canvas.restore();
    }

    private void drawCrab(Canvas canvas, Game.Enemy e) {
        // Crab Carapace
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFB91C1C); // Deep red chitin
        canvas.drawOval(-e.radius, -e.radius * 0.8f, e.radius, e.radius * 0.8f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF7F1D1D);
        paintStroke.setStrokeWidth(2f);
        canvas.drawOval(-e.radius, -e.radius * 0.8f, e.radius, e.radius * 0.8f, paintStroke);

        // Crab Claws
        paintFill.setColor(0xFFDC2626);
        canvas.drawOval(-e.radius * 1.3f, -e.radius * 0.9f, -e.radius * 0.7f, -e.radius * 0.3f, paintFill);
        canvas.drawOval(e.radius * 0.7f, -e.radius * 0.9f, e.radius * 1.3f, -e.radius * 0.3f, paintFill);

        // Eyes
        paintFill.setColor(0xFFFBBF24);
        canvas.drawCircle(-e.radius * 0.4f, -e.radius * 0.6f, 3.5f, paintFill);
        canvas.drawCircle(e.radius * 0.4f, -e.radius * 0.6f, 3.5f, paintFill);
    }

    private void drawJellyfish(Canvas canvas, Game.Enemy e) {
        // Translucent pulsating bell
        float pulse = (float) Math.sin(e.animTimer * 4.0) * 3f;
        tempPath.reset();
        tempPath.moveTo(-e.radius, 0f);
        tempPath.quadTo(0f, -e.radius * 1.4f + pulse, e.radius, 0f);
        tempPath.close();

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xCC06B6D4); // Cyan translucent
        canvas.drawPath(tempPath, paintFill);

        // Trailing tentacles
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xAA67E8F9);
        paintStroke.setStrokeWidth(2f);
        for (int i = -2; i <= 2; i++) {
            float ox = i * (e.radius / 3f);
            float wave = (float) Math.sin(e.animTimer * 5.0 + i) * 6f;
            canvas.drawLine(ox, 0f, ox + wave, e.radius * 1.1f, paintStroke);
        }
    }

    private void drawAbyssalEel(Canvas canvas, Game.Enemy e) {
        // Serpentine slither
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF1E293B);
        paintStroke.setStrokeWidth(16f);
        paintStroke.setStrokeCap(Paint.Cap.ROUND);

        tempPath.reset();
        tempPath.moveTo(-e.radius * 1.2f, 0f);
        float s1 = (float) Math.sin(e.animTimer * 6.0) * 14f;
        tempPath.quadTo(0f, s1, e.radius * 1.2f, 0f);
        canvas.drawPath(tempPath, paintStroke);

        paintStroke.setColor(0xFF38BDF8);
        paintStroke.setStrokeWidth(4f);
        canvas.drawPath(tempPath, paintStroke);
        paintStroke.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawBossLeviathan(Canvas canvas, Game.Enemy e) {
        // Massive Kraken / Leviathan Head
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF1E1B4B); // Deep abyssal obsidian
        canvas.drawOval(-e.radius, -e.radius * 0.9f, e.radius, e.radius * 0.9f, paintFill);

        // Bioluminescent Crest
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFFC084FC); // Purple glow
        paintStroke.setStrokeWidth(4f);
        canvas.drawOval(-e.radius * 0.8f, -e.radius * 0.7f, e.radius * 0.8f, e.radius * 0.7f, paintStroke);

        // Writhing Tentacles
        paintStroke.setColor(0xFF312E81);
        paintStroke.setStrokeWidth(12f);
        paintStroke.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 6; i++) {
            double angle = (Math.PI / 5) * i;
            float tLen = e.radius * 1.4f;
            float wave = (float) Math.sin(e.animTimer * 4.0 + i) * 18f;
            float tx = (float) Math.cos(angle) * tLen + wave;
            float ty = (float) Math.sin(angle) * tLen;
            canvas.drawLine(0f, 0f, tx, ty, paintStroke);
        }
        paintStroke.setStrokeCap(Paint.Cap.BUTT);

        // Central Giant Abyssal Eye
        paintFill.setColor(0xFFF59E0B); // Amber eye
        canvas.drawCircle(0f, 0f, 18f, paintFill);
        paintFill.setColor(0xFF030712); // Slit pupil
        canvas.drawOval(-4f, -14f, 4f, 14f, paintFill);
    }

    // =========================================================================
    // DIABLO HUD (Canvas Drawing, 100% Procedural, NO EMOJIS)
    // =========================================================================
    private void drawHud(Canvas canvas) {
        // 1. Top Depth Banner
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xCC0B1320);
        canvas.drawRoundRect(Game.SCREEN_W / 2f - 210f, 10f, Game.SCREEN_W / 2f + 210f, 44f, 8f, 8f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0x5538BDF8);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(Game.SCREEN_W / 2f - 210f, 10f, Game.SCREEN_W / 2f + 210f, 44f, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(0xFFE2E8F0);
        paintText.setTextSize(14f);
        paintText.setFakeBoldText(true);
        canvas.drawText(game.zoneName.toUpperCase() + "  -  " + game.depthMeters + "m",
                Game.SCREEN_W / 2f, 32f, paintText);

        // Diver Level Tag & XP Bar
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(0xFFFBBF24);
        paintText.setTextSize(13f);
        canvas.drawText("LIV. " + game.level, 20f, 30f, paintText);

        // Sleek XP bar under top banner
        float xpBarW = 180f;
        paintFill.setColor(0xFF0F172A);
        canvas.drawRect(80f, 20f, 80f + xpBarW, 30f, paintFill);
        float xpPct = (float) game.xp / game.xpNext;
        paintFill.setColor(0xFF38BDF8);
        canvas.drawRect(80f, 20f, 80f + (xpBarW * xpPct), 30f, paintFill);
        paintStroke.setColor(0xFF334155);
        paintStroke.setStrokeWidth(1f);
        canvas.drawRect(80f, 20f, 80f + xpBarW, 30f, paintStroke);

        // Sound Button (Top-Right)
        paintFill.setColor(0xCC0F172A);
        canvas.drawRoundRect(btnSound, 6f, 6f, paintFill);
        paintStroke.setColor(0x5538BDF8);
        canvas.drawRoundRect(btnSound, 6f, 6f, paintStroke);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(AudioHelper.isSoundEnabled() ? 0xFF22C55E : 0xFF94A3B8);
        paintText.setTextSize(11f);
        canvas.drawText(AudioHelper.isSoundEnabled() ? "AUDIO" : "MUTATO",
                btnSound.centerX(), btnSound.centerY() + 4f, paintText);

        // 2. DIABLO RED LIFE GLOBE (Sfera della Vita)
        drawRedLifeGlobe(canvas);

        // 3. DIABLO BLUE TIDE GLOBE (Sfera della Marea)
        drawBlueTideGlobe(canvas);

        // 4. ACTION SKILL BELT (Bottom-Center)
        drawSkillBar(canvas);
    }

    private void drawRedLifeGlobe(Canvas canvas) {
        float cx = globeHpRect.centerX();
        float cy = globeHpRect.centerY();
        float radius = globeHpRect.width() / 2f;

        // Outer Gothic Metallic Ring
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(5f);
        paintStroke.setColor(0xFF334155); // Dark wrought iron
        canvas.drawCircle(cx, cy, radius, paintStroke);
        paintStroke.setStrokeWidth(2f);
        paintStroke.setColor(0xFFB45309); // Bronze trim
        canvas.drawCircle(cx, cy, radius - 2f, paintStroke);

        // Dark background of the globe
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF0F172A);
        canvas.drawCircle(cx, cy, radius - 4f, paintFill);

        // Red Liquid fill level
        float hpPct = Math.max(0f, Math.min(1.0f, (float) game.hp / game.getMaxHp()));
        float liquidTop = (cy + radius - 4f) - (2f * (radius - 4f) * hpPct);

        canvas.save();
        tempPath.reset();
        tempPath.addCircle(cx, cy, radius - 4f, Path.Direction.CW);
        canvas.clipPath(tempPath);

        // Liquid body
        paintFill.setColor(0xFFDC2626); // Deep ruby blood
        canvas.drawRect(cx - radius, liquidTop, cx + radius, cy + radius, paintFill);

        // Swirling highlight on liquid surface
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFFFCA5A5);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawLine(cx - radius, liquidTop, cx + radius, liquidTop, paintStroke);

        // Glass reflection arc
        paintStroke.setColor(0x55FFFFFF);
        paintStroke.setStrokeWidth(3f);
        canvas.drawArc(cx - radius + 8f, cy - radius + 8f, cx + radius - 8f, cy + radius - 8f,
                -130f, 70f, false, paintStroke);

        canvas.restore();

        // Numeric HP readout
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(14f);
        paintText.setFakeBoldText(true);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText(game.hp + " / " + game.getMaxHp(), cx, cy + 5f, paintText);

        paintText.setTextSize(10f);
        paintText.setColor(0xFFFCA5A5);
        canvas.drawText("VITA", cx, cy - 14f, paintText);
    }

    private void drawBlueTideGlobe(Canvas canvas) {
        float cx = globeTideRect.centerX();
        float cy = globeTideRect.centerY();
        float radius = globeTideRect.width() / 2f;

        // Outer Gothic Metallic Ring
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(5f);
        paintStroke.setColor(0xFF334155);
        canvas.drawCircle(cx, cy, radius, paintStroke);
        paintStroke.setStrokeWidth(2f);
        paintStroke.setColor(0xFF0284C7); // Azure trim
        canvas.drawCircle(cx, cy, radius - 2f, paintStroke);

        // Globe dark backdrop
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF0F172A);
        canvas.drawCircle(cx, cy, radius - 4f, paintFill);

        // Blue Liquid fill level
        float tidePct = Math.max(0f, Math.min(1.0f, game.tide / game.maxTide));
        float liquidTop = (cy + radius - 4f) - (2f * (radius - 4f) * tidePct);

        canvas.save();
        tempPath.reset();
        tempPath.addCircle(cx, cy, radius - 4f, Path.Direction.CW);
        canvas.clipPath(tempPath);

        // Liquid body
        paintFill.setColor(0xFF0284C7); // Oceanic sapphire
        canvas.drawRect(cx - radius, liquidTop, cx + radius, cy + radius, paintFill);

        // Liquid surface foam
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF7DD3FC);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawLine(cx - radius, liquidTop, cx + radius, liquidTop, paintStroke);

        // Glass reflection arc
        paintStroke.setColor(0x55FFFFFF);
        paintStroke.setStrokeWidth(3f);
        canvas.drawArc(cx - radius + 8f, cy - radius + 8f, cx + radius - 8f, cy + radius - 8f,
                -130f, 70f, false, paintStroke);

        canvas.restore();

        // Numeric Tide readout
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(14f);
        paintText.setFakeBoldText(true);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText((int) game.tide + " / " + (int) game.maxTide, cx, cy + 5f, paintText);

        paintText.setTextSize(10f);
        paintText.setColor(0xFF7DD3FC);
        canvas.drawText("MAREA", cx, cy - 14f, paintText);
    }

    private void drawSkillBar(Canvas canvas) {
        // Skill Belt Base Plate
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xDD0B1320);
        canvas.drawRoundRect(318f, 452f, 662f, 532f, 8f, 8f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF334155);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(318f, 452f, 662f, 532f, 8f, 8f, paintStroke);

        // Button 1: Arpione (Primary Attack)
        drawSkillButton(canvas, btnAttack, "ARPIONE", 0f, 0f, () -> {
            // Draw procedural spear tip icon
            paintStroke.setColor(0xFFE2E8F0);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawLine(btnAttack.centerX() - 12f, btnAttack.centerY() + 10f,
                    btnAttack.centerX() + 10f, btnAttack.centerY() - 12f, paintStroke);
            tempPath.reset();
            tempPath.moveTo(btnAttack.centerX() + 5f, btnAttack.centerY() - 14f);
            tempPath.lineTo(btnAttack.centerX() + 14f, btnAttack.centerY() - 14f);
            tempPath.lineTo(btnAttack.centerX() + 14f, btnAttack.centerY() - 5f);
            canvas.drawPath(tempPath, paintStroke);
        });

        // Button 2: Marea (AoE Wave)
        float waveCd = game.waveCooldown / game.waveMaxCooldown;
        drawSkillButton(canvas, btnWave, "MAREA", waveCd, game.waveCooldown, () -> {
            // Draw procedural ocean wave icon
            paintStroke.setColor(0xFF38BDF8);
            paintStroke.setStrokeWidth(2.5f);
            tempPath.reset();
            tempPath.moveTo(btnWave.centerX() - 14f, btnWave.centerY() + 4f);
            tempPath.quadTo(btnWave.centerX() - 4f, btnWave.centerY() - 12f,
                    btnWave.centerX() + 2f, btnWave.centerY());
            tempPath.quadTo(btnWave.centerX() + 8f, btnWave.centerY() - 6f,
                    btnWave.centerX() + 14f, btnWave.centerY() + 4f);
            canvas.drawPath(tempPath, paintStroke);
        });

        // Button 3: Scatto (Dash)
        float dashCd = game.dashCooldown / game.dashMaxCooldown;
        drawSkillButton(canvas, btnDash, "SCATTO", dashCd, game.dashCooldown, () -> {
            // Draw procedural winged fin / sprint arrow icon
            paintStroke.setColor(0xFFFBBF24);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawLine(btnDash.centerX() - 12f, btnDash.centerY(),
                    btnDash.centerX() + 10f, btnDash.centerY(), paintStroke);
            canvas.drawLine(btnDash.centerX() + 2f, btnDash.centerY() - 8f,
                    btnDash.centerX() + 10f, btnDash.centerY(), paintStroke);
            canvas.drawLine(btnDash.centerX() + 2f, btnDash.centerY() + 8f,
                    btnDash.centerX() + 10f, btnDash.centerY(), paintStroke);
        });

        // Button 4: Pozione (Heal)
        drawSkillButton(canvas, btnPotion, "POZIONE", 0f, 0f, () -> {
            // Draw procedural potion bottle with cork
            paintStroke.setColor(0xFF22C55E);
            paintStroke.setStrokeWidth(2.2f);
            canvas.drawRect(btnPotion.centerX() - 4f, btnPotion.centerY() - 12f,
                    btnPotion.centerX() + 4f, btnPotion.centerY() - 7f, paintStroke);
            canvas.drawRoundRect(btnPotion.centerX() - 11f, btnPotion.centerY() - 7f,
                    btnPotion.centerX() + 11f, btnPotion.centerY() + 11f, 5f, 5f, paintStroke);

            paintText.setTextAlign(Paint.Align.RIGHT);
            paintText.setColor(0xFFE2E8F0);
            paintText.setTextSize(10f);
            canvas.drawText("x" + game.potions, btnPotion.right - 4f, btnPotion.bottom - 4f, paintText);
        });

        // Button 5: Inventario (Backpack)
        drawSkillButton(canvas, btnInventory, "ZAINO", 0f, 0f, () -> {
            // Draw procedural nautical chest / satchel
            paintStroke.setColor(0xFFF59E0B);
            paintStroke.setStrokeWidth(2.2f);
            canvas.drawRoundRect(btnInventory.centerX() - 12f, btnInventory.centerY() - 7f,
                    btnInventory.centerX() + 12f, btnInventory.centerY() + 10f, 4f, 4f, paintStroke);
            canvas.drawLine(btnInventory.centerX() - 12f, btnInventory.centerY() - 1f,
                    btnInventory.centerX() + 12f, btnInventory.centerY() - 1f, paintStroke);
            canvas.drawCircle(btnInventory.centerX(), btnInventory.centerY() + 2f, 2f, paintStroke);
        });
    }

    private void drawSkillButton(Canvas canvas, RectF r, String label, float cdPct, float cdSec, Runnable iconDraw) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF1E293B);
        canvas.drawRoundRect(r, 6f, 6f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF475569);
        paintStroke.setStrokeWidth(1.8f);
        canvas.drawRoundRect(r, 6f, 6f, paintStroke);

        // Icon
        iconDraw.run();

        // Cooldown sweep overlay
        if (cdPct > 0.001f) {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xAA020617);
            canvas.drawRect(r.left, r.bottom - (r.height() * cdPct), r.right, r.bottom, paintFill);

            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(13f);
            paintText.setFakeBoldText(true);
            paintText.setColor(0xFFFFFFFF);
            canvas.drawText(String.format("%.1f", cdSec), r.centerX(), r.centerY() + 4f, paintText);
        }

        // Subtitle tag
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(8f);
        paintText.setFakeBoldText(false);
        paintText.setColor(0xFF94A3B8);
        canvas.drawText(label, r.centerX(), r.bottom - 2f, paintText);
    }

    // =========================================================================
    // INVENTORY OVERLAY (Papercraft/Diablo Slate, NO EMOJIS)
    // =========================================================================
    private void drawInventoryModal(Canvas canvas) {
        // Darken backdrop
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xCC020617);
        canvas.drawRect(0f, 0f, Game.SCREEN_W, Game.SCREEN_H, paintFill);

        // Diablo Stone Slate Box
        paintFill.setColor(0xFF0F172A);
        canvas.drawRoundRect(modalRect, 10f, 10f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFFD97706); // Bronze trim
        paintStroke.setStrokeWidth(3f);
        canvas.drawRoundRect(modalRect, 10f, 10f, paintStroke);

        // Modal Title
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(0xFFFBBF24);
        paintText.setTextSize(18f);
        paintText.setFakeBoldText(true);
        canvas.drawText("EQUIPAGGIAMENTO E ZAINO ABISSALE", modalRect.left + 24f, modalRect.top + 34f, paintText);

        // Close Button [X]
        paintFill.setColor(0xFF334155);
        canvas.drawRoundRect(btnCloseModal, 4f, 4f, paintFill);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(0xFFE2E8F0);
        paintText.setTextSize(16f);
        canvas.drawText("X", btnCloseModal.centerX(), btnCloseModal.centerY() + 5f, paintText);

        // Divider
        paintStroke.setColor(0xFF334155);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawLine(modalRect.left + 20f, modalRect.top + 50f, modalRect.right - 20f, modalRect.top + 50f, paintStroke);

        // Left Section: Equipped Gear Slots
        drawEquippedSlot(canvas, slotEquipWeapon, "ARMA", game.equippedWeapon);
        drawEquippedSlot(canvas, slotEquipArmor, "CORAZZA", game.equippedArmor);
        drawEquippedSlot(canvas, slotEquipRelic, "RELIQUIA", game.equippedRelic);

        // Right Section: Backpack 8-Slot Grid
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(0xFFCBD5E1);
        paintText.setTextSize(14f);
        canvas.drawText("OGGETTI NELLO ZAINO (MAX 8)", 540f, 100f, paintText);

        for (int i = 0; i < 8; i++) {
            RectF slot = backpackSlots[i];
            boolean isSelected = (i == selectedBackpackIndex);

            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(isSelected ? 0xFF1E3A8A : 0xFF1E293B);
            canvas.drawRoundRect(slot, 6f, 6f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(isSelected ? 0xFF38BDF8 : 0xFF475569);
            paintStroke.setStrokeWidth(isSelected ? 2.5f : 1.5f);
            canvas.drawRoundRect(slot, 6f, 6f, paintStroke);

            if (i < game.backpack.size()) {
                Game.Item it = game.backpack.get(i);
                int rColor = game.getRarityColor(it.rarity);

                // Rarity border indicator
                paintStroke.setColor(rColor);
                paintStroke.setStrokeWidth(2f);
                canvas.drawRect(slot.left + 4f, slot.top + 4f, slot.right - 4f, slot.bottom - 4f, paintStroke);

                paintText.setTextAlign(Paint.Align.CENTER);
                paintText.setTextSize(10f);
                paintText.setColor(rColor);
                canvas.drawText(it.slot.toUpperCase(), slot.centerX(), slot.centerY() - 6f, paintText);

                paintText.setTextSize(9f);
                paintText.setColor(0xFFFFFFFF);
                String shortName = it.name.length() > 9 ? it.name.substring(0, 8) + ".." : it.name;
                canvas.drawText(shortName, slot.centerX(), slot.centerY() + 10f, paintText);
            }
        }

        // Details of selected backpack item
        if (selectedBackpackIndex >= 0 && selectedBackpackIndex < game.backpack.size()) {
            Game.Item sel = game.backpack.get(selectedBackpackIndex);
            int rColor = game.getRarityColor(sel.rarity);

            // Item Name & Rarity
            paintText.setTextAlign(Paint.Align.LEFT);
            paintText.setTextSize(15f);
            paintText.setFakeBoldText(true);
            paintText.setColor(rColor);
            canvas.drawText(sel.name + " (" + sel.getRarityName() + ")", 540f, 290f, paintText);

            // Stats comparison
            paintText.setTextSize(12f);
            paintText.setColor(0xFFE2E8F0);
            String statLine = "";
            if ("weapon".equals(sel.slot)) {
                statLine = "Danno: +" + sel.damageBonus + "   Crit: +" + (int)(sel.critBonus * 100) + "%";
            } else if ("armor".equals(sel.slot)) {
                statLine = "Difesa: +" + sel.defenseBonus + "   Salute: +" + sel.healthBonus;
            } else {
                statLine = "Velocita: +" + (int)sel.speedBonus + "   Crit: +" + (int)(sel.critBonus * 100) + "%";
            }
            canvas.drawText(statLine, 540f, 314f, paintText);

            paintText.setTextSize(11f);
            paintText.setColor(0xFF94A3B8);
            canvas.drawText(sel.desc, 540f, 336f, paintText);

            // Action Buttons: Equipaggia & Scarta
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF15803D); // Green
            canvas.drawRoundRect(btnEquipSelected, 6f, 6f, paintFill);
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setColor(0xFFFFFFFF);
            paintText.setTextSize(13f);
            canvas.drawText("EQUIPAGGIA", btnEquipSelected.centerX(), btnEquipSelected.centerY() + 5f, paintText);

            paintFill.setColor(0xFFB91C1C); // Red
            canvas.drawRoundRect(btnDropSelected, 6f, 6f, paintFill);
            canvas.drawText("SCARTA", btnDropSelected.centerX(), btnDropSelected.centerY() + 5f, paintText);
        }

        // Diver Global Stats Bar (Bottom of modal)
        paintStroke.setColor(0xFF334155);
        canvas.drawLine(modalRect.left + 20f, 430f, modalRect.right - 20f, 430f, paintStroke);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(0xFFFBBF24);
        paintText.setTextSize(13f);
        paintText.setFakeBoldText(true);
        String fullStats = "STATISTICHE:   DANNO: " + game.getAttackDamage() +
                "    DIFESA: " + game.getDefense() +
                "    SALUTE: " + game.getMaxHp() +
                "    VELOCITA: " + (int)game.getMoveSpeed() +
                "    CRITICO: " + (int)(game.getCritChance() * 100) + "%";
        canvas.drawText(fullStats, modalRect.left + 30f, 465f, paintText);
    }

    private void drawEquippedSlot(Canvas canvas, RectF r, String slotTitle, Game.Item item) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF1E293B);
        canvas.drawRoundRect(r, 6f, 6f, paintFill);

        int rColor = item != null ? game.getRarityColor(item.rarity) : 0xFF475569;
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(rColor);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(r, 6f, 6f, paintStroke);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(10f);
        paintText.setColor(0xFF94A3B8);
        canvas.drawText(slotTitle, r.left + 12f, r.top + 18f, paintText);

        if (item != null) {
            paintText.setTextSize(13f);
            paintText.setFakeBoldText(true);
            paintText.setColor(rColor);
            canvas.drawText(item.name, r.left + 12f, r.top + 36f, paintText);

            paintText.setTextSize(10f);
            paintText.setColor(0xFFCBD5E1);
            String statBrief = "";
            if ("weapon".equals(item.slot)) statBrief = "+" + item.damageBonus + " DANNO";
            else if ("armor".equals(item.slot)) statBrief = "+" + item.defenseBonus + " DEF, +" + item.healthBonus + " PV";
            else statBrief = "+" + (int)item.speedBonus + " VEL";
            canvas.drawText(statBrief, r.left + 12f, r.top + 49f, paintText);
        } else {
            paintText.setTextSize(12f);
            paintText.setColor(0xFF64748B);
            canvas.drawText("VUOTO", r.left + 12f, r.top + 36f, paintText);
        }
    }

    private void drawGameOverOverlay(Canvas canvas) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xD9020617);
        canvas.drawRect(0f, 0f, Game.SCREEN_W, Game.SCREEN_H, paintFill);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(0xFFEF4444);
        paintText.setTextSize(36f);
        paintText.setFakeBoldText(true);
        canvas.drawText("INGHIOTTITO DAGLI ABISSI", Game.SCREEN_W / 2f, 220f, paintText);

        paintText.setColor(0xFF94A3B8);
        paintText.setTextSize(16f);
        canvas.drawText("La pressione degli abissi ha spezzato il tuo scafandro.", Game.SCREEN_W / 2f, 260f, paintText);

        paintFill.setColor(0xFF1E3A8A);
        canvas.drawRoundRect(btnRestart, 8f, 8f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF38BDF8);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(btnRestart, 8f, 8f, paintStroke);

        paintText.setColor(0xFFFFFFFF);
        paintText.setTextSize(18f);
        canvas.drawText("RICOMINCIA SPEDIZIONE", btnRestart.centerX(), btnRestart.centerY() + 6f, paintText);
    }

    private void drawVictoryOverlay(Canvas canvas) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xD9020617);
        canvas.drawRect(0f, 0f, Game.SCREEN_W, Game.SCREEN_H, paintFill);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(0xFFFBBF24);
        paintText.setTextSize(36f);
        paintText.setFakeBoldText(true);
        canvas.drawText("TRIONFO NELL'ABISSO", Game.SCREEN_W / 2f, 220f, paintText);

        paintText.setColor(0xFFE2E8F0);
        paintText.setTextSize(16f);
        canvas.drawText("Hai sconfitto il Leviatano Primordiale e purificato le profondita!", Game.SCREEN_W / 2f, 260f, paintText);

        paintFill.setColor(0xFF15803D);
        canvas.drawRoundRect(btnRestart, 8f, 8f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF4ADE80);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(btnRestart, 8f, 8f, paintStroke);

        paintText.setColor(0xFFFFFFFF);
        paintText.setTextSize(18f);
        canvas.drawText("NUOVA SPEDIZIONE", btnRestart.centerX(), btnRestart.centerY() + 6f, paintText);
    }

    private void drawPauseOverlay(Canvas canvas) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xAA020617);
        canvas.drawRect(0f, 0f, Game.SCREEN_W, Game.SCREEN_H, paintFill);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(0xFFFBBF24);
        paintText.setTextSize(32f);
        paintText.setFakeBoldText(true);
        canvas.drawText("PAUSA", Game.SCREEN_W / 2f, 260f, paintText);
        paintText.setTextSize(15f);
        paintText.setColor(0xFF94A3B8);
        canvas.drawText("Tocca per riprendere", Game.SCREEN_W / 2f, 295f, paintText);
    }

    // =========================================================================
    // TOUCH INPUT (Point & Click to move, Drag steering, Skill buttons)
    // =========================================================================
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float rawX = event.getX();
        float rawY = event.getY();

        // Convert to virtual coordinate system (960x540)
        float vx = (rawX - offsetX) / viewScale;
        float vy = (rawY - offsetY) / viewScale;

        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            handleTouchDown(vx, vy);
            return true;
        } else if (action == MotionEvent.ACTION_MOVE) {
            handleTouchMove(vx, vy);
            return true;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            isDraggingToMove = false;
            return true;
        }

        return super.onTouchEvent(event);
    }

    private void handleTouchDown(float vx, float vy) {
        // Overlay handling
        if (game.gameOver || game.victory) {
            if (btnRestart.contains(vx, vy)) {
                restartGame();
            }
            return;
        }

        if (paused) {
            paused = false;
            return;
        }

        if (inventoryOpen) {
            handleInventoryTouch(vx, vy);
            return;
        }

        // Top bar buttons
        if (btnSound.contains(vx, vy)) {
            AudioHelper.toggleSound();
            AudioHelper.playClick();
            return;
        }

        // HUD Skill Belt Buttons
        if (btnAttack.contains(vx, vy)) {
            // Attack toward closest enemy or current facing
            float tx = game.playerX + (float) Math.cos(game.playerFacingAngle) * 200f;
            float ty = game.playerY + (float) Math.sin(game.playerFacingAngle) * 200f;
            game.performAttack(tx, ty);
            return;
        }

        if (btnWave.contains(vx, vy)) {
            game.performWave();
            return;
        }

        if (btnDash.contains(vx, vy)) {
            game.performDash();
            return;
        }

        if (btnPotion.contains(vx, vy)) {
            game.usePotion();
            return;
        }

        if (btnInventory.contains(vx, vy)) {
            toggleInventory();
            return;
        }

        // Point-and-Click / Enemy targeting in game world
        float worldX = vx + camX;
        float worldY = vy + camY;

        // Check if tapping on an enemy directly
        for (Game.Enemy e : game.enemies) {
            float dx = worldX - e.x;
            float dy = worldY - e.y;
            if (dx * dx + dy * dy < (e.radius + 15f) * (e.radius + 15f)) {
                // Target and attack enemy!
                game.performAttack(e.x, e.y);
                return;
            }
        }

        // Otherwise: Point and click to move!
        game.setPlayerTarget(worldX, worldY);
        isDraggingToMove = true;
    }

    private void handleTouchMove(float vx, float vy) {
        if (!inventoryOpen && !paused && !game.gameOver && !game.victory && isDraggingToMove) {
            float worldX = vx + camX;
            float worldY = vy + camY;
            game.setPlayerTarget(worldX, worldY);
        }
    }

    private void handleInventoryTouch(float vx, float vy) {
        if (btnCloseModal.contains(vx, vy)) {
            toggleInventory();
            return;
        }

        // Check backpack slots
        for (int i = 0; i < 8; i++) {
            if (backpackSlots[i].contains(vx, vy)) {
                if (i < game.backpack.size()) {
                    selectedBackpackIndex = i;
                    AudioHelper.playClick();
                }
                return;
            }
        }

        // Check equip button
        if (selectedBackpackIndex >= 0 && selectedBackpackIndex < game.backpack.size()) {
            if (btnEquipSelected.contains(vx, vy)) {
                game.equipBackpackItem(selectedBackpackIndex);
                selectedBackpackIndex = -1;
                return;
            }
            if (btnDropSelected.contains(vx, vy)) {
                game.dropBackpackItem(selectedBackpackIndex);
                selectedBackpackIndex = -1;
                return;
            }
        }
    }
}

