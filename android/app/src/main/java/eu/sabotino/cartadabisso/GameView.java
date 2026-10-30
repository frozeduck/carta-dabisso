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
    private float animTime = 0f;

    // Camera
    private float camX = 0f;
    private float camY = 0f;

    // Papercraft Palette Constants
    private static final int COL_CREAM = 0xFFE9DFBD;
    private static final int COL_EDGE = 0xFF143D43;
    private static final int COL_GOLD = 0xFFCFAD6C;
    private static final int COL_MINT = 0xFF76B7A0;
    private static final int COL_CORAL = 0xFFBD8272;
    private static final int COL_NAVY = 0xFF0B303B;
    private static final int COL_SHADOW = 0x5505232B;

    // Reusable Paints
    private final Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tempPath = new Path();
    private final Path shadowPath = new Path();
    private final RectF tempRect = new RectF();

    // Mobile Virtual Joystick (Left Thumb)
    private final float joyBaseX = 130f;
    private final float joyBaseY = 415f;
    private final float joyBaseRadius = 56f;
    private final float joyKnobRadius = 26f;
    private float joyKnobX = joyBaseX;
    private float joyKnobY = joyBaseY;
    private int joyPointerId = -1;

    // Mobile Action Button Cluster (Right Thumb)
    private final float btnAttackX = 855f, btnAttackY = 425f, btnAttackRadius = 44f;
    private final float btnWaveX = 750f, btnWaveY = 455f, btnWaveRadius = 28f;
    private final float btnDashX = 855f, btnDashY = 315f, btnDashRadius = 28f;
    private final float btnPotionX = 760f, btnPotionY = 365f, btnPotionRadius = 26f;
    private final float btnInventoryX = 685f, btnInventoryY = 475f, btnInventoryRadius = 24f;

    // Diablo Spheres (Upper Corners)
    private final float globeHpX = 55f, globeHpY = 55f, globeHpRadius = 34f;
    private final float globeTideX = 905f, globeTideY = 55f, globeTideRadius = 34f;

    // Sound button (Top center-right)
    private final RectF btnSound = new RectF(740f, 14f, 795f, 44f);

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
        joyPointerId = -1;
        joyKnobX = joyBaseX;
        joyKnobY = joyBaseY;
        game.joystickActive = false;
        AudioHelper.playClick();
    }

    @Override
    public void run() {
        while (running) {
            long nowNs = System.nanoTime();
            float dt = (nowNs - lastTimeNs) / 1_000_000_000f;
            lastTimeNs = nowNs;
            if (dt > 0.08f) dt = 0.08f;
            animTime += dt;

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

            long sleepMs = 16 - (System.nanoTime() - nowNs) / 1_000_000;
            if (sleepMs > 0) {
                SystemClock.sleep(sleepMs);
            }
        }
    }

    private void updateCamera() {
        float targetCamX = game.playerX - Game.SCREEN_W / 2f;
        float targetCamY = game.playerY - Game.SCREEN_H / 2f;

        targetCamX = Math.max(0f, Math.min(Game.MAP_W - Game.SCREEN_W, targetCamX));
        targetCamY = Math.max(0f, Math.min(Game.MAP_H - Game.SCREEN_H, targetCamY));

        camX += (targetCamX - camX) * 0.16f;
        camY += (targetCamY - camY) * 0.16f;

        if (game.trauma > 0.01f) {
            float shakeMag = game.trauma * game.trauma * 16f;
            camX += (float) Math.sin(animTime * 38.0) * shakeMag;
            camY += (float) Math.cos(animTime * 44.0) * shakeMag;
        }
    }

    // =========================================================================
    // PAPERCRAFT DRAWING HELPERS (Authentic Carta d'Abisso)
    // =========================================================================
    private void drawPaperPolygon(Canvas canvas, float[][] pts, int fillColor, boolean hasFold) {
        if (pts == null || pts.length < 3) return;

        // 1. Drop shadow offset by (3, 5)
        shadowPath.reset();
        shadowPath.moveTo(pts[0][0] + 3f, pts[0][1] + 5f);
        for (int i = 1; i < pts.length; i++) {
            shadowPath.lineTo(pts[i][0] + 3f, pts[i][1] + 5f);
        }
        shadowPath.close();
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_SHADOW);
        canvas.drawPath(shadowPath, paintFill);

        // 2. Main paper polygon
        tempPath.reset();
        tempPath.moveTo(pts[0][0], pts[0][1]);
        for (int i = 1; i < pts.length; i++) {
            tempPath.lineTo(pts[i][0], pts[i][1]);
        }
        tempPath.close();

        paintFill.setColor(fillColor);
        canvas.drawPath(tempPath, paintFill);

        // 3. Dark paper cutout edge stroke
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_EDGE);
        paintStroke.setStrokeWidth(2f);
        paintStroke.setStrokeJoin(Paint.Join.ROUND);
        paintStroke.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawPath(tempPath, paintStroke);

        // 4. Subtle paper fold highlight
        if (hasFold && pts.length >= 4) {
            paintStroke.setColor(0x33FFFFFF);
            paintStroke.setStrokeWidth(1.2f);
            canvas.drawLine(pts[0][0], pts[0][1], pts[2][0], pts[2][1], paintStroke);
        }
    }

    private void drawShadow(Canvas canvas, float x, float y, float rx, float ry) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_SHADOW);
        canvas.drawOval(x - rx, y - ry, x + rx, y + ry, paintFill);
    }

    private void drawScratch(Canvas canvas, float x, float y, float w, float h, int seed) {
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0x28163D42);
        paintStroke.setStrokeWidth(0.9f);
        for (int i = 0; i < 5; i++) {
            float px = x + (((i * 17 + seed * 3) % 31) / 31f) * w;
            float py = y + (((i * 11 + seed) % 29) / 29f) * h;
            canvas.drawLine(px, py, px + 5f, py - 2.5f, paintStroke);
        }
    }

    private void drawGame(Canvas canvas) {
        updateCamera();

        // Dark oceanic border
        canvas.drawColor(0xFF030712);

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(viewScale, viewScale);
        canvas.clipRect(0f, 0f, Game.SCREEN_W, Game.SCREEN_H);

        // 1. World Layer
        canvas.save();
        canvas.translate(-camX, -camY);
        drawWorld(canvas);
        canvas.restore();

        // 2. Diablo HUD + Mobile Controls Layer
        drawHudAndControls(canvas);

        // 3. Modals & Overlays
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
        // Multi-stop deep sea gradient
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(0, 0, 0, Game.MAP_H,
                new int[]{0xFF123F48, 0xFF0B303B, 0xFF061E2C},
                new float[]{0f, 0.52f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, Game.MAP_W, Game.MAP_H, paintFill);
        paintFill.setShader(null);

        // God Rays (soft oceanic light beams filtering from the surface)
        paintFill.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 6; i++) {
            float x = Game.MAP_W * (0.15f + i * 0.16f) + (float) Math.sin(animTime * 0.15f + i) * 35f;
            tempPath.reset();
            tempPath.moveTo(x - 30f, 0f);
            tempPath.lineTo(x + 25f, 0f);
            tempPath.lineTo(x + 260f, Game.MAP_H);
            tempPath.lineTo(x - 60f, Game.MAP_H);
            tempPath.close();

            paintFill.setShader(new LinearGradient(x, 0, x + 60f, Game.MAP_H,
                    0x12A4D9B2, 0x0078BBA3, Shader.TileMode.CLAMP));
            canvas.drawPath(tempPath, paintFill);
        }
        paintFill.setShader(null);

        // Seabed stone plateau
        float[][] seabedBorder = {
                {60f, 60f}, {Game.MAP_W - 60f, 60f},
                {Game.MAP_W - 60f, Game.MAP_H - 60f}, {60f, Game.MAP_H - 60f}
        };
        drawPaperPolygon(canvas, seabedBorder, 0xFF14353F, false);

        // Seabed sand texture dots
        paintFill.setColor(0x28244C46);
        for (int i = 0; i < 180; i++) {
            float sx = ((i * 197) % (int) (Game.MAP_W - 140)) + 70f;
            float sy = ((i * 113) % (int) (Game.MAP_H - 140)) + 70f;
            canvas.drawCircle(sx, sy, (i % 3 == 0) ? 2.5f : 1.4f, paintFill);
        }

        // Corals and ancient submerged stone architecture
        drawEnvironmentScenery(canvas);

        // Click Beacon (Diablo gold indicator)
        if (game.beaconTimer > 0f) {
            drawClickBeacon(canvas, game.beaconX, game.beaconY, game.beaconTimer);
        }

        // Depth portal archway
        if (game.portalOpen) {
            drawSunkenPortalArch(canvas, game.portalX, game.portalY);
        }

        // Floor Drops with Rarity Beams
        for (Game.Drop drop : game.drops) {
            drawDrop(canvas, drop);
        }

        // Enemies
        for (Game.Enemy enemy : game.enemies) {
            drawEnemy(canvas, enemy);
        }

        // Palombaro (Player)
        drawDiver(canvas);

        // Projectiles
        for (Game.Projectile p : game.projectiles) {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(p.color);
            canvas.drawCircle(p.x, p.y, p.radius, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_EDGE);
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

        // Underwater bubbles
        for (Game.Particle p : game.particles) {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(p.color);
            float alpha = p.life / p.maxLife;
            paintFill.setAlpha((int) (alpha * 200));
            canvas.drawCircle(p.x, p.y, p.size * alpha, paintFill);
        }
        paintFill.setAlpha(255);
    }

    private void drawEnvironmentScenery(Canvas canvas) {
        // Ancient sunken stone columns
        drawSunkenColumn(canvas, 240f, 220f, 1.0f);
        drawSunkenColumn(canvas, 1380f, 240f, 0.9f);
        drawSunkenColumn(canvas, 1500f, 850f, 0.85f);
        drawSunkenColumn(canvas, 260f, 880f, 0.95f);

        // Branching corals
        drawCoral(canvas, 420f, 360f, 0, 1.1f, 0.5f); // Coral pink staghorn
        drawCoral(canvas, 750f, 190f, 1, 1.2f, 1.2f); // Sage sea fan
        drawCoral(canvas, 1150f, 320f, 2, 0.95f, 2.0f); // Pipe sponge
        drawCoral(canvas, 600f, 820f, 3, 1.05f, 3.1f); // Deep sea anemones
        drawCoral(canvas, 1200f, 780f, 0, 1.0f, 4.4f); // Staghorn
    }

    private void drawCoral(Canvas canvas, float x, float y, int type, float scale, float phase) {
        canvas.save();
        canvas.translate(x, y);
        float sway = (float) Math.sin(animTime * 1.2 + phase) * 3f;
        canvas.rotate(sway);
        canvas.scale(scale, scale);

        drawShadow(canvas, 0f, 2f, 26f, 8f);

        if (type == 0) { // Staghorn coral in terracotta pink
            float[][] pts = {
                    {-9,0}, {-13,-31}, {-34,-46}, {-38,-69}, {-30,-72}, {-25,-53},
                    {-13,-47}, {-16,-90}, {-7,-100}, {-4,-70}, {7,-57}, {21,-65},
                    {25,-88}, {33,-90}, {33,-60}, {13,-44}, {11,-20}, {25,-32},
                    {38,-31}, {21,-14}, {10,1}
            };
            drawPaperPolygon(canvas, pts, COL_CORAL, true);
            drawScratch(canvas, -30f, -80f, 60f, 80f, (int)(phase * 10));
        } else if (type == 1) { // Sea fan in mint green
            float[][] pts = {
                    {-13,0}, {-18,-23}, {-28,-43}, {-24,-74}, {-15,-48}, {-6,-42},
                    {-13,-80}, {-1,-105}, {3,-69}, {11,-80}, {20,-103}, {23,-73},
                    {12,-48}, {17,-22}, {7,0}
            };
            drawPaperPolygon(canvas, pts, 0xFF547E70, true);
        } else if (type == 2) { // Organ pipe sponge
            for (int i = 0; i < 4; i++) {
                float ox = (i - 1.5f) * 12f;
                float h = 24f + (i % 2) * 16f;
                float[][] pts = {{-8 + ox, 0}, {-9 + ox, -h}, {ox, -h - 5}, {8 + ox, -h}, {6 + ox, 0}};
                drawPaperPolygon(canvas, pts, 0xFFBBA87B, false);
            }
        } else { // Sea anemone cluster
            float[][] pts = {{-30,0}, {-32,-17}, {-23,-32}, {-9,-21}, {2,-37}, {17,-30}, {25,-10}, {32,0}};
            drawPaperPolygon(canvas, pts, 0xFF476D66, true);
        }

        canvas.restore();
    }

    private void drawSunkenColumn(Canvas canvas, float x, float y, float s) {
        canvas.save();
        canvas.translate(x, y);
        canvas.scale(s, s);
        drawShadow(canvas, 0f, 0f, 38f, 11f);

        float[][] pts = {
                {-29,0}, {-32,-13}, {-24,-18}, {-21,-111}, {-31,-121},
                {-27,-135}, {27,-135}, {32,-120}, {22,-111}, {25,-18},
                {33,-12}, {29,0}
        };
        drawPaperPolygon(canvas, pts, 0xFF718977, true);
        drawScratch(canvas, -20f, -110f, 40f, 90f, 7);
        canvas.restore();
    }

    private void drawSunkenPortalArch(Canvas canvas, float x, float y) {
        canvas.save();
        canvas.translate(x, y);
        drawShadow(canvas, 0f, 0f, 90f, 22f);

        // Glowing emerald vortex
        float time = animTime * 3f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new RadialGradient(0, -70, 110, 0x88A3E1BB, 0x00A3E1BB, Shader.TileMode.CLAMP));
        canvas.drawCircle(0f, -70f, 110f, paintFill);
        paintFill.setShader(null);

        // Stone portal arch pillars
        float[][] pLeft = {{-80,0}, {-84,-127}, {-67,-171}, {-41,-191}, {-29,-167}, {-51,-139}, {-51,-4}};
        drawPaperPolygon(canvas, pLeft, 0xFF688574, true);

        float[][] pRight = {{48,0}, {50,-139}, {28,-166}, {41,-190}, {71,-167}, {84,-125}, {79,0}};
        drawPaperPolygon(canvas, pRight, 0xFF587C6E, true);

        float[][] archTop = {{-70,-163}, {-43,-199}, {0,-215}, {43,-198}, {72,-162}, {44,-151}, {18,-174}, {-17,-174}, {-43,-151}};
        drawPaperPolygon(canvas, archTop, 0xFF9DAF8B, true);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(13f);
        paintText.setFakeBoldText(true);
        canvas.drawText("DISCESA NELL'ABISSO", 0f, -230f, paintText);

        canvas.restore();
    }

    // =========================================================================
    // DIVER (PALOMBARO VINTAGE PAPERCRAFT)
    // =========================================================================
    private void drawDiver(Canvas canvas) {
        float px = game.playerX;
        float py = game.playerY;
        boolean moving = game.playerMoving;
        float facing = game.playerFacingAngle;

        float bob = moving ? (float) Math.sin(animTime * 12.0) * 3f : (float) Math.sin(animTime * 2.2) * 2f;
        float face = (float) Math.cos(facing) < 0 ? -1f : 1f;

        float sqX = game.attackAnim > 0 ? 1f + (float) Math.sin(game.attackAnim * Math.PI) * 0.22f : (game.dashAnim > 0 ? 1.22f : 1f);
        float sqY = game.attackAnim > 0 ? 1f - (float) Math.sin(game.attackAnim * Math.PI) * 0.16f : (game.dashAnim > 0 ? 0.84f : 1f);

        // Armor palette response
        int bodyCol = 0xFFE0C990; // Vintage canvas/leather
        int shadeCol = 0xFFBF9D66;
        int helmCol = 0xFFC8A466; // Brass porthole helmet
        if (game.equippedArmor != null) {
            if (game.equippedArmor.rarity == Game.RARITY_MAGIC) {
                bodyCol = 0xFF4A7C8C; shadeCol = 0xFF355D6B; helmCol = 0xFF76B7A0; // Cobalt
            } else if (game.equippedArmor.rarity == Game.RARITY_RARE) {
                bodyCol = 0xFFC79247; shadeCol = 0xFFA16F2A; helmCol = 0xFFE2B45A; // Bronze/Gold
            } else if (game.equippedArmor.rarity == Game.RARITY_ABYSSAL) {
                bodyCol = 0xFF6B4E7D; shadeCol = 0xFF4B325B; helmCol = 0xFF9F72BD; // Obsidian Purple
            }
        }

        canvas.save();
        canvas.translate(px, py);

        // Shadow
        drawShadow(canvas, 0f, 6f, 37f, 11f);

        canvas.translate(0f, bob);
        canvas.scale(face * sqX, sqY);

        // Oxygen Cylinders on back
        canvas.save();
        canvas.translate(-18f, -58f);
        canvas.rotate(-0.2f);
        float[][] tank = {{-15,20}, {-17,-11}, {-8,-20}, {9,-20}, {16,-6}, {11,21}};
        drawPaperPolygon(canvas, tank, 0xFF80937C, true);
        paintStroke.setColor(0xFFD2CFAA);
        paintStroke.setStrokeWidth(2f);
        canvas.drawLine(-12f, -8f, 10f, -8f, paintStroke);
        canvas.restore();

        // Stepping boots with walk animation
        float step = moving ? (float) Math.sin(animTime * 12.0) * 8f : 0f;
        canvas.save();
        canvas.translate(-12f, step * 0.3f);
        float[][] bootL = {{-10,-22}, {-9,-1}, {-20,5}, {-17,12}, {3,12}, {7,-20}};
        drawPaperPolygon(canvas, bootL, shadeCol, false);
        canvas.restore();

        canvas.save();
        canvas.translate(14f, -step * 0.3f);
        float[][] bootR = {{-8,-23}, {-6,1}, {-10,8}, {-7,13}, {17,12}, {18,5}, {8,-1}, {10,-24}};
        drawPaperPolygon(canvas, bootR, bodyCol, false);
        canvas.restore();

        // Diver Torso (vintage folded suit)
        float[][] suit = {{-27,-60}, {-18,-77}, {17,-77}, {29,-57}, {22,-24}, {4,-15}, {-22,-23}};
        drawPaperPolygon(canvas, suit, bodyCol, true);

        // Chest belt & harness
        paintStroke.setColor(0xFF7A7154);
        paintStroke.setStrokeWidth(3f);
        canvas.drawLine(-25f, -37f, 19f, -32f, paintStroke);

        // Arms & Shoulders
        float[][] shoulderL = {{-24,-61}, {-35,-54}, {-39,-31}, {-28,-25}, {-17,-49}};
        drawPaperPolygon(canvas, shoulderL, shadeCol, false);

        float[][] shoulderR = {{20,-63}, {31,-57}, {39,-39}, {29,-32}, {16,-48}};
        drawPaperPolygon(canvas, shoulderR, bodyCol, false);

        // Held Weapon (Arpione / Tridente / Lama)
        drawDiverWeapon(canvas);

        // Brass Diving Helmet
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(helmCol);
        canvas.drawCircle(0f, -81f, 32f, paintFill);
        paintStroke.setColor(COL_EDGE);
        paintStroke.setStrokeWidth(3f);
        canvas.drawCircle(0f, -81f, 32f, paintStroke);

        // Helmet porthole visor bezel
        paintFill.setColor(bodyCol);
        canvas.drawCircle(1f, -84f, 26f, paintFill);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawCircle(1f, -84f, 26f, paintStroke);

        // Glass porthole opening
        paintFill.setColor(0xFF193B43);
        canvas.drawCircle(5f, -84f, 20f, paintFill);
        paintFill.setColor(0xFF336169);
        canvas.drawCircle(8f, -87f, 15f, paintFill);

        // Porthole Protective Crossbar Grill
        paintStroke.setColor(0xBBBAA77);
        paintStroke.setStrokeWidth(2f);
        canvas.drawLine(-8f, -84f, 22f, -84f, paintStroke);
        canvas.drawLine(7f, -102f, 7f, -66f, paintStroke);

        // Glass reflection arc
        paintStroke.setColor(0x66FFFFFF);
        paintStroke.setStrokeWidth(2f);
        canvas.drawArc(1f, -94f, 17f, -76f, -120f, 70f, false, paintStroke);

        // Underwater Forward God Ray Light Cone from visor
        paintFill.setShader(new RadialGradient(10f, -84f, 90f, 0x22C3E9BC, 0x00B3E8BD, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, -140f, 180f, -30f, paintFill);
        paintFill.setShader(null);

        canvas.restore();
    }

    private void drawDiverWeapon(Canvas canvas) {
        canvas.save();
        canvas.translate(33f, -39f);

        float rot = game.attackAnim > 0 ?
                -1.05f + (float) Math.sin(game.attackAnim * Math.PI) * 1.4f : -0.6f;
        canvas.rotate((float) Math.toDegrees(rot));

        int shaftCol = 0xFFCDB078;
        int bladeCol = 0xFFE8E1C6;
        if (game.equippedWeapon != null) {
            bladeCol = game.getRarityColor(game.equippedWeapon.rarity);
        }

        // Shaft
        paintStroke.setColor(0xFF163C42);
        paintStroke.setStrokeWidth(5f);
        canvas.drawLine(0f, 15f, 0f, -58f, paintStroke);
        paintStroke.setColor(shaftCol);
        paintStroke.setStrokeWidth(3f);
        canvas.drawLine(0f, 15f, 0f, -58f, paintStroke);

        // Spearhead / Trident head
        if (game.equippedWeapon != null && game.equippedWeapon.rarity == Game.RARITY_RARE) { // Trident
            float[][] b1 = {{-15,-50}, {-12,-76}, {-8,-53}};
            drawPaperPolygon(canvas, b1, bladeCol, false);
            float[][] b2 = {{-6,-53}, {0,-82}, {6,-53}};
            drawPaperPolygon(canvas, b2, bladeCol, false);
            float[][] b3 = {{8,-53}, {12,-76}, {15,-50}};
            drawPaperPolygon(canvas, b3, bladeCol, false);
        } else { // Classic Harpoon
            float[][] head = {{-7,-53}, {0,-76}, {7,-53}, {0,-59}};
            drawPaperPolygon(canvas, head, bladeCol, false);
        }

        canvas.restore();
    }

    // =========================================================================
    // ABYSSAL FAUNA (CRABS, JELLIES, EELS, LEVIATHAN BOSS)
    // =========================================================================
    private void drawEnemy(Canvas canvas, Game.Enemy e) {
        // Red warning circle if winding up telegraphed slam
        if (e.isWindingUp) {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0x35EF4444);
            canvas.drawCircle(e.windupX, e.windupY, e.windupRadius, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFFEF4444);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawCircle(e.windupX, e.windupY, e.windupRadius, paintStroke);

            float prog = 1.0f - (e.windupTimer / e.windupDuration);
            canvas.drawCircle(e.windupX, e.windupY, e.windupRadius * prog, paintStroke);
        }

        canvas.save();
        canvas.translate(e.x, e.y);

        float sqX = 1f + e.hitSquash * 0.28f;
        float sqY = 1f - e.hitSquash * 0.22f;
        canvas.scale(sqX, sqY);

        if ("boss".equals(e.type)) {
            drawBossLeviathan(canvas, e);
        } else if ("jelly".equals(e.type)) {
            drawJellyfish(canvas, e);
        } else if ("eel".equals(e.type)) {
            drawAbyssalEel(canvas, e);
        } else {
            drawCrab(canvas, e);
        }

        // Health bar
        if (e.hp < e.maxHp && !"boss".equals(e.type)) {
            float barW = 44f;
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF173942);
            canvas.drawRect(-barW / 2f, -83f, barW / 2f, -79f, paintFill);

            float hpPct = (float) e.hp / e.maxHp;
            paintFill.setColor(0xFFDDB486);
            canvas.drawRect(-barW / 2f, -83f, -barW / 2f + barW * hpPct, -79f, paintFill);
        }

        canvas.restore();
    }

    private void drawCrab(Canvas canvas, Game.Enemy e) {
        drawShadow(canvas, 0f, 3f, 33f, 10f);
        float anim = (float) Math.sin(animTime * 7.0 + e.animTimer) * 4f;
        int fill = e.hitSquash > 0.05f ? 0xFFF4E5C0 : 0xFFB98070; // Coral terracotta

        // 6 Segmented walking legs & giant articulated claws
        for (int side : new int[]{-1, 1}) {
            canvas.save();
            canvas.scale(side, 1f);

            // Legs
            paintStroke.setColor(0xFF133E42);
            paintStroke.setStrokeWidth(6f);
            canvas.drawLine(15f, -9f, 31f, -4f, paintStroke);
            canvas.drawLine(31f, -4f, 39f, 4f, paintStroke);
            paintStroke.setColor(0xFFB9967A);
            paintStroke.setStrokeWidth(3f);
            canvas.drawLine(15f, -9f, 31f, -4f, paintStroke);
            canvas.drawLine(31f, -4f, 39f, 4f, paintStroke);

            // Articulated Claws (Chele giganti)
            canvas.save();
            canvas.translate(29f, -29f);
            canvas.rotate(0.1f + anim * 0.025f);
            float[][] claw = {{-9,9}, {-17,-4}, {-14,-20}, {-6,-28}, {-3,-11}, {5,-13}, {10,-28}, {18,-16}, {15,-2}, {4,12}};
            drawPaperPolygon(canvas, claw, fill, true);
            canvas.restore();

            canvas.restore();
        }

        // Crab Carapace
        float[][] shell = {{-26,-9}, {-29,-28}, {-17,-42}, {6,-45}, {27,-30}, {27,-9}, {5,0}};
        drawPaperPolygon(canvas, shell, fill, true);

        // Stalk Eyes
        for (int x : new int[]{-10, 12}) {
            paintStroke.setColor(COL_EDGE);
            paintStroke.setStrokeWidth(3f);
            canvas.drawLine(x, -34f, x - 2f, -47f, paintStroke);
            paintFill.setColor(0xFFE5DBAD);
            canvas.drawCircle(x - 2f, -49f, 5f, paintFill);
            paintFill.setColor(0xFF142F38);
            canvas.drawCircle(x - 2f, -49f, 2f, paintFill);
        }
        drawScratch(canvas, -20f, -36f, 40f, 30f, 3);
    }

    private void drawJellyfish(Canvas canvas, Game.Enemy e) {
        float bob = (float) Math.sin(animTime * 2.0 + e.animTimer) * 7f;
        drawShadow(canvas, 0f, 6f, 28f, 8f);

        canvas.save();
        canvas.translate(0f, -23f + bob);

        // 5 sinuous tentacles undulating with sine waves
        for (int i = 0; i < 5; i++) {
            float ox = (i - 2) * 9f;
            float s = (float) Math.sin(animTime * 2.5 + e.animTimer + i) * 7f;
            paintStroke.setColor(0xFF152F3B);
            paintStroke.setStrokeWidth(4.5f);
            canvas.drawLine(ox, -2f, ox + s, 16f, paintStroke);
            canvas.drawLine(ox + s, 16f, ox - s * 0.3f, 33f, paintStroke);
            paintStroke.setColor(0xFF9FBD9);
            paintStroke.setStrokeWidth(2.2f);
            canvas.drawLine(ox, -2f, ox + s, 16f, paintStroke);
            canvas.drawLine(ox + s, 16f, ox - s * 0.3f, 33f, paintStroke);
        }

        // Translucent scalloped bell
        float[][] bell = {{-34,-5}, {-31,-27}, {-17,-43}, {0,-48}, {19,-42}, {31,-25}, {35,-5}, {24,1}, {12,-3}, {1,3}, {-10,-3}, {-23,2}};
        int fill = e.hitSquash > 0.05f ? 0xFFF1E6CB : 0xFF8DB9A6;
        drawPaperPolygon(canvas, bell, fill, true);

        canvas.restore();
    }

    private void drawAbyssalEel(Canvas canvas, Game.Enemy e) {
        drawShadow(canvas, 0f, 5f, 35f, 8f);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_EDGE);
        paintStroke.setStrokeWidth(16f);
        paintStroke.setStrokeCap(Paint.Cap.ROUND);

        tempPath.reset();
        tempPath.moveTo(-e.radius * 1.3f, 0f);
        float s1 = (float) Math.sin(animTime * 6.0 + e.animTimer) * 16f;
        tempPath.quadTo(0f, s1, e.radius * 1.3f, 0f);
        canvas.drawPath(tempPath, paintStroke);

        paintStroke.setColor(0xFF38BDF8);
        paintStroke.setStrokeWidth(4f);
        canvas.drawPath(tempPath, paintStroke);
        paintStroke.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawBossLeviathan(Canvas canvas, Game.Enemy e) {
        float sway = (float) Math.sin(animTime * 1.6) * 4f;
        drawShadow(canvas, 0f, 9f, 105f, 26f);

        canvas.save();
        canvas.translate(0f, sway);

        // 7 writhing tentacles
        for (int i = 0; i < 7; i++) {
            float angle = (i - 3) * 0.3f;
            float tip = (float) Math.sin(animTime * 2.0 + i) * 14f;
            canvas.save();
            canvas.translate((i - 3) * 17f, -20f);
            canvas.rotate((float) Math.toDegrees(angle));
            float[][] tent = {{-10,0}, {-16,30}, {-8,62}, {tip,83}, {tip+14,78}, {3,49}, {13,9}};
            drawPaperPolygon(canvas, tent, (i % 2 == 0) ? 0xFF779B8E : 0xFFABC0A0, true);
            canvas.restore();
        }

        // Giant Head plate
        float[][] head = {{-83,-31}, {-91,-86}, {-67,-145}, {-18,-171}, {43,-158}, {83,-114}, {92,-53}, {50,-12}, {-23,-8}};
        int fill = e.hitSquash > 0.05f ? 0xFFFBEBBF : 0xFFC5BA91;
        drawPaperPolygon(canvas, head, fill, true);

        // Spiral ancient carvings
        paintStroke.setColor(0xFF496A62);
        paintStroke.setStrokeWidth(4f);
        tempPath.reset();
        for (float a = 0; a < Math.PI * 4; a += 0.1f) {
            float r = 5f + a * 4.2f;
            float sx = 6f + (float) Math.cos(a) * r;
            float sy = -89f + (float) Math.sin(a) * r;
            if (a == 0) tempPath.moveTo(sx, sy); else tempPath.lineTo(sx, sy);
        }
        canvas.drawPath(tempPath, paintStroke);

        // Cyclopean glowing eye
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF173D43);
        canvas.drawCircle(22f, -80f, 24f, paintFill);
        paintFill.setColor(0xFF8BC6AA);
        canvas.drawCircle(25f, -80f, 16f, paintFill);
        paintFill.setColor(0xFF17343D); // Slit pupil
        canvas.drawOval(26f, -92f, 32f, -68f, paintFill);

        drawScratch(canvas, -65f, -130f, 120f, 100f, 5);
        canvas.restore();
    }

    private void drawClickBeacon(Canvas canvas, float x, float y, float timer) {
        float radius = (1.0f - timer) * 44f;
        int alpha = (int) (timer * 255);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_GOLD);
        paintStroke.setStrokeWidth(3f);
        paintStroke.setAlpha(alpha);
        canvas.drawCircle(x, y, radius, paintStroke);
        paintStroke.setAlpha(255);
    }

    private void drawDrop(Canvas canvas, Game.Drop drop) {
        int rColor = game.getRarityColor(drop.item.rarity);
        float cy = drop.y + (float) Math.sin(drop.bobPhase) * 6f;

        // Rarity Beam
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(drop.x, cy, drop.x, cy - 220f,
                rColor & 0x77FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        tempRect.set(drop.x - 12f, cy - 220f, drop.x + 12f, cy);
        canvas.drawRect(tempRect, paintFill);
        paintFill.setShader(null);

        drawShadow(canvas, drop.x, drop.y, 16f, 5f);

        // Rune badge
        float[][] rune = {{drop.x, cy - 14f}, {drop.x + 10f, cy}, {drop.x, cy + 14f}, {drop.x - 10f, cy}};
        drawPaperPolygon(canvas, rune, rColor, false);

        // Text plaque
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(12f);
        paintText.setFakeBoldText(true);
        float tw = paintText.measureText(drop.item.name) + 14f;
        paintFill.setColor(0xDD0B1320);
        canvas.drawRoundRect(drop.x - tw / 2f, cy - 36f, drop.x + tw / 2f, cy - 18f, 4f, 4f, paintFill);
        paintText.setColor(rColor);
        canvas.drawText(drop.item.name, drop.x, cy - 24f, paintText);
    }

    // =========================================================================
    // HUD & MOBILE ACTION CONTROLS (Tasti movimento e attacco)
    // =========================================================================
    private void drawHudAndControls(Canvas canvas) {
        // Top Banner
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xCC0B1320);
        canvas.drawRoundRect(Game.SCREEN_W / 2f - 180f, 10f, Game.SCREEN_W / 2f + 180f, 44f, 8f, 8f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0x4438BDF8);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(Game.SCREEN_W / 2f - 180f, 10f, Game.SCREEN_W / 2f + 180f, 44f, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_CREAM);
        paintText.setTextSize(14f);
        paintText.setFakeBoldText(true);
        canvas.drawText(game.zoneName.toUpperCase() + "  -  " + game.depthMeters + "m",
                Game.SCREEN_W / 2f, 32f, paintText);

        // Sound Toggle
        paintFill.setColor(0xCC0F172A);
        canvas.drawRoundRect(btnSound, 6f, 6f, paintFill);
        paintText.setTextSize(10f);
        paintText.setColor(AudioHelper.isSoundEnabled() ? 0xFF22C55E : 0xFF94A3B8);
        canvas.drawText(AudioHelper.isSoundEnabled() ? "AUDIO" : "MUTO",
                btnSound.centerX(), btnSound.centerY() + 4f, paintText);

        // 1. DIABLO RED LIFE GLOBE (Upper-Left)
        drawRedLifeGlobe(canvas, globeHpX, globeHpY, globeHpRadius);

        // 2. DIABLO BLUE TIDE GLOBE (Upper-Right)
        drawBlueTideGlobe(canvas, globeTideX, globeTideY, globeTideRadius);

        // 3. VIRTUAL ANALOG MOVEMENT JOYSTICK (Left Thumb)
        drawVirtualJoystick(canvas);

        // 4. MOBILE ACTION BUTTON CLUSTER (Right Thumb)
        drawMobileActionButtons(canvas);
    }

    private void drawVirtualJoystick(Canvas canvas) {
        // Outer Base Ring
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x44020617);
        canvas.drawCircle(joyBaseX, joyBaseY, joyBaseRadius, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0x8838BDF8);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawCircle(joyBaseX, joyBaseY, joyBaseRadius, paintStroke);

        // Inner directional cross indicators
        paintStroke.setColor(0x3338BDF8);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawLine(joyBaseX - 35f, joyBaseY, joyBaseX + 35f, joyBaseY, paintStroke);
        canvas.drawLine(joyBaseX, joyBaseY - 35f, joyBaseX, joyBaseY + 35f, paintStroke);

        // Movable Knob
        paintFill.setColor(0xDD0F172A);
        canvas.drawCircle(joyKnobX, joyKnobY, joyKnobRadius, paintFill);

        paintStroke.setColor(0xFF38BDF8);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawCircle(joyKnobX, joyKnobY, joyKnobRadius, paintStroke);

        // Center dot
        paintFill.setColor(0xFF67E8F9);
        canvas.drawCircle(joyKnobX, joyKnobY, 5f, paintFill);
    }

    private void drawMobileActionButtons(Canvas canvas) {
        // --- 1. BIG MAIN ATTACK BUTTON [ARPIONE] ---
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xEE1E293B);
        canvas.drawCircle(btnAttackX, btnAttackY, btnAttackRadius, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_GOLD);
        paintStroke.setStrokeWidth(3.5f);
        canvas.drawCircle(btnAttackX, btnAttackY, btnAttackRadius, paintStroke);

        // Spearhead Icon inside attack button
        tempPath.reset();
        tempPath.moveTo(btnAttackX - 10f, btnAttackY + 12f);
        tempPath.lineTo(btnAttackX + 12f, btnAttackY - 10f);
        paintStroke.setColor(COL_CREAM);
        paintStroke.setStrokeWidth(3f);
        canvas.drawPath(tempPath, paintStroke);

        tempPath.reset();
        tempPath.moveTo(btnAttackX + 4f, btnAttackY - 16f);
        tempPath.lineTo(btnAttackX + 16f, btnAttackY - 16f);
        tempPath.lineTo(btnAttackX + 16f, btnAttackY - 4f);
        canvas.drawPath(tempPath, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(9f);
        paintText.setFakeBoldText(true);
        paintText.setColor(COL_GOLD);
        canvas.drawText("ATTACCO", btnAttackX, btnAttackY + 28f, paintText);

        // --- 2. SKILL [MAREA] (AoE Wave) ---
        float waveCd = game.waveCooldown / game.waveMaxCooldown;
        drawActionButton(canvas, btnWaveX, btnWaveY, btnWaveRadius, "MAREA", waveCd, 0xFF0284C7, () -> {
            // Cresting wave
            tempPath.reset();
            tempPath.moveTo(btnWaveX - 12f, btnWaveY + 3f);
            tempPath.quadTo(btnWaveX - 3f, btnWaveY - 10f, btnWaveX + 2f, btnWaveY);
            tempPath.quadTo(btnWaveX + 7f, btnWaveY - 5f, btnWaveX + 12f, btnWaveY + 3f);
            paintStroke.setColor(0xFF38BDF8);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawPath(tempPath, paintStroke);
        });

        // --- 3. SKILL [SCATTO] (Dash) ---
        float dashCd = game.dashCooldown / game.dashMaxCooldown;
        drawActionButton(canvas, btnDashX, btnDashY, btnDashRadius, "SCATTO", dashCd, 0xFFD97706, () -> {
            // Winged dash arrow
            paintStroke.setColor(0xFFFBBF24);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawLine(btnDashX - 10f, btnDashY, btnDashX + 10f, btnDashY, paintStroke);
            canvas.drawLine(btnDashX + 2f, btnDashY - 7f, btnDashX + 10f, btnDashY, paintStroke);
            canvas.drawLine(btnDashX + 2f, btnDashY + 7f, btnDashX + 10f, btnDashY, paintStroke);
        });

        // --- 4. SKILL [POZIONE] (Cura) ---
        drawActionButton(canvas, btnPotionX, btnPotionY, btnPotionRadius, "CURA", 0f, 0xFF15803D, () -> {
            // Vintage flask
            paintStroke.setColor(0xFF4ADE80);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRect(btnPotionX - 3f, btnPotionY - 10f, btnPotionX + 3f, btnPotionY - 6f, paintStroke);
            canvas.drawRoundRect(btnPotionX - 9f, btnPotionY - 6f, btnPotionX + 9f, btnPotionY + 9f, 4f, 4f, paintStroke);

            paintText.setTextAlign(Paint.Align.RIGHT);
            paintText.setColor(0xFFFFFFFF);
            paintText.setTextSize(9f);
            canvas.drawText("x" + game.potions, btnPotionX + 10f, btnPotionY + 8f, paintText);
        });

        // --- 5. INVENTARIO [ZAINO] ---
        drawActionButton(canvas, btnInventoryX, btnInventoryY, btnInventoryRadius, "ZAINO", 0f, 0xFF475569, () -> {
            // Satchel icon
            paintStroke.setColor(COL_GOLD);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRoundRect(btnInventoryX - 9f, btnInventoryY - 6f, btnInventoryX + 9f, btnInventoryY + 8f, 3f, 3f, paintStroke);
            canvas.drawLine(btnInventoryX - 9f, btnInventoryY, btnInventoryX + 9f, btnInventoryY, paintStroke);
        });
    }

    private void drawActionButton(Canvas canvas, float cx, float cy, float r, String label, float cdPct, int ringCol, Runnable iconDraw) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xDD0F172A);
        canvas.drawCircle(cx, cy, r, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(ringCol);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawCircle(cx, cy, r, paintStroke);

        iconDraw.run();

        // Cooldown radial sweep
        if (cdPct > 0.001f) {
            paintFill.setColor(0xAA020617);
            canvas.drawArc(cx - r, cy - r, cx + r, cy + r, -90f, cdPct * 360f, true, paintFill);
        }

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(8f);
        paintText.setColor(0xFFCBD5E1);
        canvas.drawText(label, cx, cy + r - 3f, paintText);
    }

    private void drawRedLifeGlobe(Canvas canvas, float cx, float cy, float r) {
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(3.5f);
        paintStroke.setColor(0xFF7F1D1D);
        canvas.drawCircle(cx, cy, r, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF0F172A);
        canvas.drawCircle(cx, cy, r - 2f, paintFill);

        // Fluid fill
        float pct = Math.max(0f, Math.min(1.0f, (float) game.hp / game.getMaxHp()));
        float top = (cy + r - 2f) - (2f * (r - 2f) * pct);

        canvas.save();
        tempPath.reset();
        tempPath.addCircle(cx, cy, r - 2f, Path.Direction.CW);
        canvas.clipPath(tempPath);

        paintFill.setColor(0xFFDC2626);
        canvas.drawRect(cx - r, top, cx + r, cy + r, paintFill);

        // Glass highlight
        paintStroke.setColor(0x66FFFFFF);
        paintStroke.setStrokeWidth(2f);
        canvas.drawArc(cx - r + 5f, cy - r + 5f, cx + r - 5f, cy + r - 5f, -120f, 60f, false, paintStroke);
        canvas.restore();

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(10f);
        paintText.setFakeBoldText(true);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText(game.hp + " / " + game.getMaxHp(), cx, cy + 4f, paintText);
        paintText.setTextSize(8f);
        paintText.setColor(0xFFFCA5A5);
        canvas.drawText("VITA", cx, cy - 10f, paintText);
    }

    private void drawBlueTideGlobe(Canvas canvas, float cx, float cy, float r) {
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(3.5f);
        paintStroke.setColor(0xFF075985);
        canvas.drawCircle(cx, cy, r, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF0F172A);
        canvas.drawCircle(cx, cy, r - 2f, paintFill);

        // Fluid fill
        float pct = Math.max(0f, Math.min(1.0f, game.tide / game.maxTide));
        float top = (cy + r - 2f) - (2f * (r - 2f) * pct);

        canvas.save();
        tempPath.reset();
        tempPath.addCircle(cx, cy, r - 2f, Path.Direction.CW);
        canvas.clipPath(tempPath);

        paintFill.setColor(0xFF0284C7);
        canvas.drawRect(cx - r, top, cx + r, cy + r, paintFill);

        // Glass highlight
        paintStroke.setColor(0x66FFFFFF);
        paintStroke.setStrokeWidth(2f);
        canvas.drawArc(cx - r + 5f, cy - r + 5f, cx + r - 5f, cy + r - 5f, -120f, 60f, false, paintStroke);
        canvas.restore();

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(10f);
        paintText.setFakeBoldText(true);
        paintText.setColor(0xFFFFFFFF);
        canvas.drawText((int) game.tide + " / " + (int) game.maxTide, cx, cy + 4f, paintText);
        paintText.setTextSize(8f);
        paintText.setColor(0xFF7DD3FC);
        canvas.drawText("MAREA", cx, cy - 10f, paintText);
    }

    // =========================================================================
    // INVENTORY MODAL & OVERLAYS
    // =========================================================================
    private void drawInventoryModal(Canvas canvas) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xCC020617);
        canvas.drawRect(0f, 0f, Game.SCREEN_W, Game.SCREEN_H, paintFill);

        float[][] box = {
                {modalRect.left, modalRect.top}, {modalRect.right, modalRect.top},
                {modalRect.right, modalRect.bottom}, {modalRect.left, modalRect.bottom}
        };
        drawPaperPolygon(canvas, box, 0xFF0F172A, false);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(18f);
        paintText.setFakeBoldText(true);
        canvas.drawText("EQUIPAGGIAMENTO E REPERTI ABISSALI", modalRect.left + 24f, modalRect.top + 34f, paintText);

        paintFill.setColor(0xFF334155);
        canvas.drawRoundRect(btnCloseModal, 4f, 4f, paintFill);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_CREAM);
        paintText.setTextSize(16f);
        canvas.drawText("X", btnCloseModal.centerX(), btnCloseModal.centerY() + 5f, paintText);

        drawEquippedSlot(canvas, slotEquipWeapon, "ARMA", game.equippedWeapon);
        drawEquippedSlot(canvas, slotEquipArmor, "CORAZZA", game.equippedArmor);
        drawEquippedSlot(canvas, slotEquipRelic, "RELIQUIA", game.equippedRelic);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(0xFFCBD5E1);
        paintText.setTextSize(14f);
        canvas.drawText("ZAINO (8 SLOT)", 540f, 100f, paintText);

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
                paintStroke.setColor(rColor);
                paintStroke.setStrokeWidth(2f);
                canvas.drawRect(slot.left + 4f, slot.top + 4f, slot.right - 4f, slot.bottom - 4f, paintStroke);

                paintText.setTextAlign(Paint.Align.CENTER);
                paintText.setTextSize(10f);
                paintText.setColor(rColor);
                canvas.drawText(it.slot.toUpperCase(), slot.centerX(), slot.centerY() - 6f, paintText);

                paintText.setTextSize(9f);
                paintText.setColor(0xFFFFFFFF);
                String sn = it.name.length() > 9 ? it.name.substring(0, 8) + ".." : it.name;
                canvas.drawText(sn, slot.centerX(), slot.centerY() + 10f, paintText);
            }
        }

        if (selectedBackpackIndex >= 0 && selectedBackpackIndex < game.backpack.size()) {
            Game.Item sel = game.backpack.get(selectedBackpackIndex);
            int rColor = game.getRarityColor(sel.rarity);

            paintText.setTextAlign(Paint.Align.LEFT);
            paintText.setTextSize(15f);
            paintText.setFakeBoldText(true);
            paintText.setColor(rColor);
            canvas.drawText(sel.name + " (" + sel.getRarityName() + ")", 540f, 290f, paintText);

            paintText.setTextSize(12f);
            paintText.setColor(0xFFE2E8F0);
            String statLine = "weapon".equals(sel.slot) ? "Danno: +" + sel.damageBonus :
                    "armor".equals(sel.slot) ? "Difesa: +" + sel.defenseBonus + "  Salute: +" + sel.healthBonus :
                            "Velocita: +" + (int)sel.speedBonus;
            canvas.drawText(statLine, 540f, 314f, paintText);

            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF15803D);
            canvas.drawRoundRect(btnEquipSelected, 6f, 6f, paintFill);
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setColor(0xFFFFFFFF);
            paintText.setTextSize(13f);
            canvas.drawText("EQUIPAGGIA", btnEquipSelected.centerX(), btnEquipSelected.centerY() + 5f, paintText);

            paintFill.setColor(0xFFB91C1C);
            canvas.drawRoundRect(btnDropSelected, 6f, 6f, paintFill);
            canvas.drawText("SCARTA", btnDropSelected.centerX(), btnDropSelected.centerY() + 5f, paintText);
        }

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(13f);
        paintText.setFakeBoldText(true);
        String fullStats = "STATISTICHE:   DANNO: " + game.getAttackDamage() +
                "    DIFESA: " + game.getDefense() +
                "    SALUTE: " + game.getMaxHp() +
                "    VELOCITA: " + (int)game.getMoveSpeed();
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
        paintText.setTextSize(34f);
        paintText.setFakeBoldText(true);
        canvas.drawText("INGHIOTTITO DAGLI ABISSI", Game.SCREEN_W / 2f, 220f, paintText);

        paintFill.setColor(0xFF1E3A8A);
        canvas.drawRoundRect(btnRestart, 8f, 8f, paintFill);
        paintText.setColor(COL_CREAM);
        paintText.setTextSize(17f);
        canvas.drawText("RICOMINCIA SPEDIZIONE", btnRestart.centerX(), btnRestart.centerY() + 6f, paintText);
    }

    private void drawVictoryOverlay(Canvas canvas) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xD9020617);
        canvas.drawRect(0f, 0f, Game.SCREEN_W, Game.SCREEN_H, paintFill);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(34f);
        paintText.setFakeBoldText(true);
        canvas.drawText("TRIONFO NELL'ABISSO", Game.SCREEN_W / 2f, 220f, paintText);

        paintFill.setColor(0xFF15803D);
        canvas.drawRoundRect(btnRestart, 8f, 8f, paintFill);
        paintText.setColor(COL_CREAM);
        paintText.setTextSize(17f);
        canvas.drawText("NUOVA SPEDIZIONE", btnRestart.centerX(), btnRestart.centerY() + 6f, paintText);
    }

    private void drawPauseOverlay(Canvas canvas) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xAA020617);
        canvas.drawRect(0f, 0f, Game.SCREEN_W, Game.SCREEN_H, paintFill);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(32f);
        paintText.setFakeBoldText(true);
        canvas.drawText("PAUSA", Game.SCREEN_W / 2f, 260f, paintText);
    }

    // =========================================================================
    // MULTI-TOUCH HANDLING (Movement Joystick + Action Cluster simultaneously)
    // =========================================================================
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();

        if (inventoryOpen) {
            if (action == MotionEvent.ACTION_DOWN) {
                float vx = (event.getX() - offsetX) / viewScale;
                float vy = (event.getY() - offsetY) / viewScale;
                handleInventoryTouch(vx, vy);
            }
            return true;
        }

        if (game.gameOver || game.victory) {
            if (action == MotionEvent.ACTION_DOWN) {
                float vx = (event.getX() - offsetX) / viewScale;
                float vy = (event.getY() - offsetY) / viewScale;
                if (btnRestart.contains(vx, vy)) restartGame();
            }
            return true;
        }

        if (paused) {
            if (action == MotionEvent.ACTION_DOWN) paused = false;
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int pointerId = event.getPointerId(actionIndex);
                float vx = (event.getX(actionIndex) - offsetX) / viewScale;
                float vy = (event.getY(actionIndex) - offsetY) / viewScale;
                handlePointerDown(pointerId, vx, vy);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                int pointerCount = event.getPointerCount();
                for (int i = 0; i < pointerCount; i++) {
                    int pointerId = event.getPointerId(i);
                    float vx = (event.getX(i) - offsetX) / viewScale;
                    float vy = (event.getY(i) - offsetY) / viewScale;
                    handlePointerMove(pointerId, vx, vy);
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                int pointerId = event.getPointerId(actionIndex);
                handlePointerUp(pointerId);
                break;
            }
        }

        return true;
    }

    private void handlePointerDown(int pointerId, float vx, float vy) {
        // Top buttons
        if (btnSound.contains(vx, vy)) {
            AudioHelper.toggleSound();
            AudioHelper.playClick();
            return;
        }

        // Left half of screen: Movement Joystick
        if (vx < 420f && vy > 120f) {
            joyPointerId = pointerId;
            updateJoystick(vx, vy);
            return;
        }

        // Right side: Action Button Cluster
        float dAttack = distSq(vx, vy, btnAttackX, btnAttackY);
        if (dAttack < btnAttackRadius * btnAttackRadius * 1.5f) {
            // Auto-aim attack
            game.performAttack(-1f, -1f);
            return;
        }

        float dWave = distSq(vx, vy, btnWaveX, btnWaveY);
        if (dWave < btnWaveRadius * btnWaveRadius * 1.5f) {
            game.performWave();
            return;
        }

        float dDash = distSq(vx, vy, btnDashX, btnDashY);
        if (dDash < btnDashRadius * btnDashRadius * 1.5f) {
            game.performDash();
            return;
        }

        float dPotion = distSq(vx, vy, btnPotionX, btnPotionY);
        if (dPotion < btnPotionRadius * btnPotionRadius * 1.5f) {
            game.usePotion();
            return;
        }

        float dInv = distSq(vx, vy, btnInventoryX, btnInventoryY);
        if (dInv < btnInventoryRadius * btnInventoryRadius * 1.5f) {
            toggleInventory();
            return;
        }

        // Tap in world (Target enemy or point & click move)
        float wx = vx + camX;
        float wy = vy + camY;

        for (Game.Enemy e : game.enemies) {
            if (e.hp <= 0) continue;
            float de = distSq(wx, wy, e.x, e.y);
            if (de < (e.radius + 20f) * (e.radius + 20f)) {
                game.performAttack(e.x, e.y);
                return;
            }
        }

        // Point & click move fallback
        game.setPlayerTarget(wx, wy);
    }

    private void handlePointerMove(int pointerId, float vx, float vy) {
        if (pointerId == joyPointerId) {
            updateJoystick(vx, vy);
        }
    }

    private void handlePointerUp(int pointerId) {
        if (pointerId == joyPointerId) {
            joyPointerId = -1;
            joyKnobX = joyBaseX;
            joyKnobY = joyBaseY;
            game.joystickX = 0f;
            game.joystickY = 0f;
            game.joystickActive = false;
        }
    }

    private void updateJoystick(float vx, float vy) {
        float dx = vx - joyBaseX;
        float dy = vy - joyBaseY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > joyBaseRadius) {
            joyKnobX = joyBaseX + (dx / dist) * joyBaseRadius;
            joyKnobY = joyBaseY + (dy / dist) * joyBaseRadius;
        } else {
            joyKnobX = vx;
            joyKnobY = vy;
        }

        game.joystickX = (joyKnobX - joyBaseX) / joyBaseRadius;
        game.joystickY = (joyKnobY - joyBaseY) / joyBaseRadius;
        game.joystickActive = true;
    }

    private float distSq(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private void handleInventoryTouch(float vx, float vy) {
        if (btnCloseModal.contains(vx, vy)) {
            toggleInventory();
            return;
        }

        for (int i = 0; i < 8; i++) {
            if (backpackSlots[i].contains(vx, vy)) {
                if (i < game.backpack.size()) {
                    selectedBackpackIndex = i;
                    AudioHelper.playClick();
                }
                return;
            }
        }

        if (selectedBackpackIndex >= 0 && selectedBackpackIndex < game.backpack.size()) {
            if (btnEquipSelected.contains(vx, vy)) {
                game.equipBackpackItem(selectedBackpackIndex);
                selectedBackpackIndex = -1;
                return;
            }
            if (btnDropSelected.contains(vx, vy)) {
                game.dropBackpackItem(selectedBackpackIndex);
                selectedBackpackIndex = -1;
            }
        }
    }
}
