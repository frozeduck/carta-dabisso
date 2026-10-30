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

    // Paper Mario Smooth 2D Flip Animation
    private float diverFlipScale = 1f;

    // Cartoon Paper Mario Palette
    private static final int COL_STICKER = 0xFFFFFFFF; // Iconic white sticker die-cut border
    private static final int COL_INK = 0xFF18181B;     // Thick comic ink contour
    private static final int COL_CREAM = 0xFFFDFBF7;   // Warm paper sheet
    private static final int COL_SHADOW = 0x40020617;  // Soft cardboard floor drop shadow
    private static final int COL_GOLD = 0xFFFBBF24;
    private static final int COL_CORAL = 0xFFF87171;   // Cartoon bright red/coral
    private static final int COL_MINT = 0xFF4ADE80;    // Cartoon mint paper
    private static final int COL_AQUA = 0xFF38BDF8;    // Storybook oceanic cyan

    // Reusable Paints
    private final Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path tempPath = new Path();
    private final Path stickerPath = new Path();
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

            // Paper Mario 2D paper-flip interpolation
            float targetFlip = (float) Math.cos(game.playerFacingAngle) < 0 ? -1f : 1f;
            diverFlipScale += (targetFlip - diverFlipScale) * 0.28f;

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
    // PAPER MARIO STICKER CUTOUT RENDERERS
    // =========================================================================
    private void drawPaperCutout(Canvas canvas, float[][] pts, int fillColor, boolean whiteStickerBorder) {
        if (pts == null || pts.length < 3) return;

        tempPath.reset();
        tempPath.moveTo(pts[0][0], pts[0][1]);
        for (int i = 1; i < pts.length; i++) {
            tempPath.lineTo(pts[i][0], pts[i][1]);
        }
        tempPath.close();

        // 1. Paper drop shadow on floor
        shadowPath.reset();
        shadowPath.moveTo(pts[0][0] + 3f, pts[0][1] + 5f);
        for (int i = 1; i < pts.length; i++) {
            shadowPath.lineTo(pts[i][0] + 3f, pts[i][1] + 5f);
        }
        shadowPath.close();
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_SHADOW);
        canvas.drawPath(shadowPath, paintFill);

        // 2. White Paper Mario Sticker Die-Cut Border (Outer Thick White Lip)
        if (whiteStickerBorder) {
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(7f);
            paintStroke.setStrokeJoin(Paint.Join.ROUND);
            paintStroke.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawPath(tempPath, paintStroke);
        }

        // 3. Crisp Cartoon Black Ink Outline
        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(3f);
        canvas.drawPath(tempPath, paintStroke);

        // 4. Vibrant Cartoon Paper Fill
        paintFill.setColor(fillColor);
        canvas.drawPath(tempPath, paintFill);

        // 5. Subtle Origami Paper Fold Highlight
        if (pts.length >= 4) {
            paintStroke.setColor(0x44FFFFFF);
            paintStroke.setStrokeWidth(1.8f);
            canvas.drawLine(pts[0][0], pts[0][1], pts[2][0], pts[2][1], paintStroke);
        }
    }

    private void drawFloorShadow(Canvas canvas, float x, float y, float rx, float ry) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_SHADOW);
        canvas.drawOval(x - rx, y - ry, x + rx, y + ry, paintFill);
    }

    private void drawGame(Canvas canvas) {
        updateCamera();

        // Dark oceanic border
        canvas.drawColor(0xFF020B14);

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
        // Pop-Up Storybook Ocean Stage (Cardboard Gradient)
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(0, 0, 0, Game.MAP_H,
                new int[]{0xFF0E4A59, 0xFF082D38, 0xFF031620},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, Game.MAP_W, Game.MAP_H, paintFill);
        paintFill.setShader(null);

        // Paper Mario Layered Cardboard Waves in background
        drawStorybookWaves(canvas);

        // Soft God Rays filtering down like theatre spotlights
        for (int i = 0; i < 5; i++) {
            float x = Game.MAP_W * (0.18f + i * 0.18f) + (float) Math.sin(animTime * 0.2f + i) * 30f;
            tempPath.reset();
            tempPath.moveTo(x - 20f, 0f);
            tempPath.lineTo(x + 35f, 0f);
            tempPath.lineTo(x + 220f, Game.MAP_H);
            tempPath.lineTo(x - 45f, Game.MAP_H);
            tempPath.close();

            paintFill.setShader(new LinearGradient(x, 0, x + 60f, Game.MAP_H,
                    0x16A7F3D0, 0x00A7F3D0, Shader.TileMode.CLAMP));
            canvas.drawPath(tempPath, paintFill);
        }
        paintFill.setShader(null);

        // Main Cardboard Seabed Plateau (with white sticker seam)
        float[][] seabed = {
                {50f, 50f}, {Game.MAP_W - 50f, 50f},
                {Game.MAP_W - 50f, Game.MAP_H - 50f}, {50f, Game.MAP_H - 50f}
        };
        drawPaperCutout(canvas, seabed, 0xFF11323B, false);

        // Corrugated paper edge stitches
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0x334ADE80);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawRect(65f, 65f, Game.MAP_W - 65f, Game.MAP_H - 65f, paintStroke);

        // Cardboard Corals & Pop-up Scenery
        drawPaperCoralsAndRuins(canvas);

        // Click Beacon (Diablo gold star ring)
        if (game.beaconTimer > 0f) {
            drawClickBeacon(canvas, game.beaconX, game.beaconY, game.beaconTimer);
        }

        // Pop-up Archway Portal
        if (game.portalOpen) {
            drawStorybookPortalArch(canvas, game.portalX, game.portalY);
        }

        // Floor Drops with Paper Mario Rarity Beams
        for (Game.Drop drop : game.drops) {
            drawDrop(canvas, drop);
        }

        // Paper Mario Enemies (Crabs, Jellies, Eels, Leviathan)
        for (Game.Enemy enemy : game.enemies) {
            drawEnemy(canvas, enemy);
        }

        // Paper Mario Palombaro
        drawPaperDiver(canvas);

        // Projectiles (Cartoon Harpoon & Energy bubbles)
        for (Game.Projectile p : game.projectiles) {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(p.color);
            canvas.drawCircle(p.x, p.y, p.radius, paintFill);

            // White cartoon shine
            paintFill.setColor(0xCCFFFFFF);
            canvas.drawCircle(p.x - p.radius * 0.3f, p.y - p.radius * 0.3f, p.radius * 0.4f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawCircle(p.x, p.y, p.radius, paintStroke);
        }

        // Comic-style floating damage numbers & popups
        for (Game.Floater f : game.floaters) {
            drawComicFloater(canvas, f);
        }

        // Paper bubbles
        for (Game.Particle p : game.particles) {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(p.color);
            float alpha = p.life / p.maxLife;
            paintFill.setAlpha((int) (alpha * 220));
            canvas.drawCircle(p.x, p.y, p.size * alpha, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0x88FFFFFF);
            paintStroke.setStrokeWidth(1.2f);
            canvas.drawCircle(p.x, p.y, p.size * alpha, paintStroke);
        }
        paintFill.setAlpha(255);
    }

    private void drawStorybookWaves(Canvas canvas) {
        // Scalloped cardboard wave strips in background
        float waveOffset = (float) Math.sin(animTime * 0.8) * 20f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x221E40AF);

        tempPath.reset();
        tempPath.moveTo(0f, 180f);
        for (float x = 0; x <= Game.MAP_W + 80f; x += 100f) {
            float midX = x + 50f + waveOffset;
            float endX = x + 100f + waveOffset;
            tempPath.quadTo(midX, 150f, endX, 180f);
        }
        tempPath.lineTo(Game.MAP_W, 0f);
        tempPath.lineTo(0f, 0f);
        tempPath.close();
        canvas.drawPath(tempPath, paintFill);
    }

    private void drawPaperCoralsAndRuins(Canvas canvas) {
        // Pop-up Cardboard Corals with white die-cut sticker borders
        drawCartoonCoral(canvas, 420f, 360f, 0, 1.15f, 0.5f); // Red staghorn
        drawCartoonCoral(canvas, 750f, 190f, 1, 1.25f, 1.2f); // Sage origami fan
        drawCartoonCoral(canvas, 1150f, 320f, 2, 1.05f, 2.0f); // Pipe sponge
        drawCartoonCoral(canvas, 600f, 820f, 0, 1.1f, 3.1f);
        drawCartoonCoral(canvas, 1220f, 780f, 1, 1.15f, 4.4f);

        // Sunken cardboard columns
        drawStorybookColumn(canvas, 240f, 220f, 1.0f);
        drawStorybookColumn(canvas, 1380f, 240f, 0.95f);
    }

    private void drawCartoonCoral(Canvas canvas, float x, float y, int type, float scale, float phase) {
        canvas.save();
        canvas.translate(x, y);
        float sway = (float) Math.sin(animTime * 1.5 + phase) * 3f;
        canvas.rotate(sway);
        canvas.scale(scale, scale);

        drawFloorShadow(canvas, 0f, 2f, 28f, 9f);

        if (type == 0) { // Staghorn coral in bright cartoon coral pink with white sticker border
            float[][] pts = {
                    {-9,0}, {-13,-31}, {-34,-46}, {-38,-69}, {-30,-72}, {-25,-53},
                    {-13,-47}, {-16,-90}, {-7,-100}, {-4,-70}, {7,-57}, {21,-65},
                    {25,-88}, {33,-90}, {33,-60}, {13,-44}, {11,-20}, {25,-32},
                    {38,-31}, {21,-14}, {10,1}
            };
            drawPaperCutout(canvas, pts, COL_CORAL, true);
        } else if (type == 1) { // Sea fan in Paper Mario mint
            float[][] pts = {
                    {-13,0}, {-18,-23}, {-28,-43}, {-24,-74}, {-15,-48}, {-6,-42},
                    {-13,-80}, {-1,-105}, {3,-69}, {11,-80}, {20,-103}, {23,-73},
                    {12,-48}, {17,-22}, {7,0}
            };
            drawPaperCutout(canvas, pts, COL_MINT, true);
        } else { // Pipe sponge in golden yellow
            for (int i = 0; i < 4; i++) {
                float ox = (i - 1.5f) * 14f;
                float h = 28f + (i % 2) * 18f;
                float[][] pts = {{-8 + ox, 0}, {-9 + ox, -h}, {ox, -h - 5}, {8 + ox, -h}, {6 + ox, 0}};
                drawPaperCutout(canvas, pts, COL_GOLD, true);
            }
        }
        canvas.restore();
    }

    private void drawStorybookColumn(Canvas canvas, float x, float y, float s) {
        canvas.save();
        canvas.translate(x, y);
        canvas.scale(s, s);
        drawFloorShadow(canvas, 0f, 0f, 38f, 11f);

        float[][] pts = {
                {-29,0}, {-32,-13}, {-24,-18}, {-21,-111}, {-31,-121},
                {-27,-135}, {27,-135}, {32,-120}, {22,-111}, {25,-18},
                {33,-12}, {29,0}
        };
        drawPaperCutout(canvas, pts, 0xFF6EE7B7, true);
        canvas.restore();
    }

    private void drawStorybookPortalArch(Canvas canvas, float x, float y) {
        canvas.save();
        canvas.translate(x, y);
        drawFloorShadow(canvas, 0f, 0f, 95f, 24f);

        // Glowing comic spiral portal
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new RadialGradient(0, -70, 110, 0x996EE7B7, 0x006EE7B7, Shader.TileMode.CLAMP));
        canvas.drawCircle(0f, -70f, 110f, paintFill);
        paintFill.setShader(null);

        float[][] pLeft = {{-80,0}, {-84,-127}, {-67,-171}, {-41,-191}, {-29,-167}, {-51,-139}, {-51,-4}};
        drawPaperCutout(canvas, pLeft, 0xFF34D399, true);

        float[][] pRight = {{48,0}, {50,-139}, {28,-166}, {41,-190}, {71,-167}, {84,-125}, {79,0}};
        drawPaperCutout(canvas, pRight, 0xFF10B981, true);

        float[][] archTop = {{-70,-163}, {-43,-199}, {0,-215}, {43,-198}, {72,-162}, {44,-151}, {18,-174}, {-17,-174}, {-43,-151}};
        drawPaperCutout(canvas, archTop, COL_GOLD, true);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(14f);
        paintText.setFakeBoldText(true);
        canvas.drawText("DISCESA NELL'ABISSO", 0f, -230f, paintText);

        canvas.restore();
    }

    // =========================================================================
    // DIVER (CARTOON PAPER MARIO DIVER CUTOUT)
    // =========================================================================
    private void drawPaperDiver(Canvas canvas) {
        float px = game.playerX;
        float py = game.playerY;
        boolean moving = game.playerMoving;

        // Paper Mario Bouncing Walk Hop & Tilt
        float hopY = moving ? -Math.abs((float) Math.sin(animTime * 14.0)) * 7f : (float) Math.sin(animTime * 2.2) * 2f;
        float tilt = moving ? (float) Math.sin(animTime * 14.0) * 5f : 0f;

        // Smooth 2D Paper-Flip
        float flip = diverFlipScale;

        // Attack & Dash Squash/Stretch
        float sqX = game.attackAnim > 0 ? 1f + (float) Math.sin(game.attackAnim * Math.PI) * 0.25f : (game.dashAnim > 0 ? 1.25f : 1f);
        float sqY = game.attackAnim > 0 ? 1f - (float) Math.sin(game.attackAnim * Math.PI) * 0.18f : (game.dashAnim > 0 ? 0.82f : 1f);

        int suitColor = 0xFFFBBF24; // Classic bright Paper Mario gold/canvas
        int helmetColor = 0xFFF59E0B;
        if (game.equippedArmor != null) {
            if (game.equippedArmor.rarity == Game.RARITY_MAGIC) {
                suitColor = 0xFF38BDF8; helmetColor = 0xFF0284C7; // Cyan Cobalt
            } else if (game.equippedArmor.rarity == Game.RARITY_RARE) {
                suitColor = 0xFFF97316; helmetColor = 0xFFEA580C; // Fiery Amber
            } else if (game.equippedArmor.rarity == Game.RARITY_ABYSSAL) {
                suitColor = 0xFFC084FC; helmetColor = 0xFF9333EA; // Amethyst Abyssal
            }
        }

        canvas.save();
        canvas.translate(px, py);

        // Ground Drop Shadow
        drawFloorShadow(canvas, 0f, 6f, 38f, 12f);

        canvas.translate(0f, hopY);
        canvas.rotate(tilt);
        canvas.scale(flip * sqX, sqY);

        // Oxygen Cylinders on back with white sticker border
        canvas.save();
        canvas.translate(-20f, -56f);
        canvas.rotate(-0.25f);
        float[][] tank = {{-16,22}, {-18,-12}, {-9,-22}, {10,-22}, {17,-8}, {12,23}};
        drawPaperCutout(canvas, tank, 0xFF34D399, true);
        canvas.restore();

        // Oversized Paper Mario Diver Boots (Stepping animation)
        float step = moving ? (float) Math.sin(animTime * 14.0) * 10f : 0f;
        canvas.save();
        canvas.translate(-14f, step * 0.3f);
        float[][] bootL = {{-12,-20}, {-10,0}, {-22,6}, {-18,14}, {4,14}, {8,-18}};
        drawPaperCutout(canvas, bootL, 0xFF78350F, true);
        canvas.restore();

        canvas.save();
        canvas.translate(16f, -step * 0.3f);
        float[][] bootR = {{-10,-20}, {-8,2}, {-12,9}, {-8,14}, {18,14}, {20,6}, {10,0}, {12,-22}};
        drawPaperCutout(canvas, bootR, 0xFF92400E, true);
        canvas.restore();

        // Chubby Cartoon Diver Torso with white sticker border
        float[][] suit = {{-28,-60}, {-19,-78}, {18,-78}, {30,-57}, {23,-24}, {4,-15}, {-23,-23}};
        drawPaperCutout(canvas, suit, suitColor, true);

        // Chest harness and shiny brass button
        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(3f);
        canvas.drawLine(-25f, -37f, 20f, -32f, paintStroke);
        paintFill.setColor(COL_GOLD);
        canvas.drawCircle(0f, -35f, 5f, paintFill);
        canvas.drawCircle(0f, -35f, 5f, paintStroke);

        // Cute Held Weapon (Paper Mario Hammer/Spear style)
        drawPaperDiverWeapon(canvas);

        // Big Round Cartoon Diving Helmet (Mario-proportions!)
        // 1. Thick white sticker border around helmet
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(8f);
        canvas.drawCircle(0f, -82f, 34f, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(helmetColor);
        canvas.drawCircle(0f, -82f, 34f, paintFill);

        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(3.5f);
        canvas.drawCircle(0f, -82f, 34f, paintStroke);

        // Helmet Rivets / Screws (Cartoon Paper craft style)
        for (int i = 0; i < 5; i++) {
            float angle = (float) (-Math.PI * 0.8 + i * Math.PI * 0.4);
            float rx = (float) Math.cos(angle) * 30f;
            float ry = -82f + (float) Math.sin(angle) * 30f;
            paintFill.setColor(COL_GOLD);
            canvas.drawCircle(rx, ry, 3f, paintFill);
            canvas.drawCircle(rx, ry, 3f, paintStroke);
        }

        // Oversized Porthole Visor
        paintFill.setColor(0xFF1E293B);
        canvas.drawCircle(6f, -82f, 22f, paintFill);
        canvas.drawCircle(6f, -82f, 22f, paintStroke);

        paintFill.setColor(0xFF38BDF8);
        canvas.drawCircle(8f, -84f, 17f, paintFill);

        // Protective Crossbar Grill
        paintStroke.setColor(COL_GOLD);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawLine(-8f, -82f, 22f, -82f, paintStroke);
        canvas.drawLine(7f, -100f, 7f, -65f, paintStroke);

        // Comic-style White Glare Streaks on Visor
        paintFill.setColor(COL_STICKER);
        tempPath.reset();
        tempPath.moveTo(0f, -92f);
        tempPath.lineTo(8f, -92f);
        tempPath.lineTo(2f, -76f);
        tempPath.lineTo(-6f, -76f);
        tempPath.close();
        canvas.drawPath(tempPath, paintFill);

        canvas.restore();
    }

    private void drawPaperDiverWeapon(Canvas canvas) {
        canvas.save();
        canvas.translate(34f, -38f);

        float rot = game.attackAnim > 0 ?
                -1.15f + (float) Math.sin(game.attackAnim * Math.PI) * 1.6f : -0.6f;
        canvas.rotate((float) Math.toDegrees(rot));

        int bladeCol = 0xFFF1F5F9;
        if (game.equippedWeapon != null) {
            bladeCol = game.getRarityColor(game.equippedWeapon.rarity);
        }

        // Thick Cartoon Shaft
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(9f);
        canvas.drawLine(0f, 18f, 0f, -62f, paintStroke);

        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(4.5f);
        canvas.drawLine(0f, 18f, 0f, -62f, paintStroke);

        paintStroke.setColor(COL_GOLD);
        paintStroke.setStrokeWidth(3f);
        canvas.drawLine(0f, 18f, 0f, -62f, paintStroke);

        // Cartoon Spearhead / Trident with white die-cut sticker
        if (game.equippedWeapon != null && game.equippedWeapon.rarity == Game.RARITY_RARE) { // Trident
            float[][] b1 = {{-16,-52}, {-13,-80}, {-9,-55}};
            drawPaperCutout(canvas, b1, bladeCol, true);
            float[][] b2 = {{-7,-55}, {0,-88}, {7,-55}};
            drawPaperCutout(canvas, b2, bladeCol, true);
            float[][] b3 = {{9,-55}, {13,-80}, {16,-52}};
            drawPaperCutout(canvas, b3, bladeCol, true);
        } else { // Classic Oversized Cartoon Harpoon
            float[][] head = {{-9,-54}, {0,-82}, {9,-54}, {0,-62}};
            drawPaperCutout(canvas, head, bladeCol, true);
        }

        canvas.restore();
    }

    // =========================================================================
    // PAPER MARIO ENEMIES (CARTOON STICKER CRABS, JELLIES, LEVIATHAN)
    // =========================================================================
    private void drawEnemy(Canvas canvas, Game.Enemy e) {
        if (e.isWindingUp) {
            // Comic Rubber-Stamp Warning Circle
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0x35EF4444);
            canvas.drawCircle(e.windupX, e.windupY, e.windupRadius, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFFEF4444);
            paintStroke.setStrokeWidth(3.5f);
            canvas.drawCircle(e.windupX, e.windupY, e.windupRadius, paintStroke);

            float prog = 1.0f - (e.windupTimer / e.windupDuration);
            canvas.drawCircle(e.windupX, e.windupY, e.windupRadius * prog, paintStroke);
        }

        canvas.save();
        canvas.translate(e.x, e.y);

        float sqX = 1f + e.hitSquash * 0.35f;
        float sqY = 1f - e.hitSquash * 0.25f;
        canvas.scale(sqX, sqY);

        if ("boss".equals(e.type)) {
            drawPaperBossLeviathan(canvas, e);
        } else if ("jelly".equals(e.type)) {
            drawPaperJellyfish(canvas, e);
        } else if ("eel".equals(e.type)) {
            drawPaperAbyssalEel(canvas, e);
        } else {
            drawPaperCrab(canvas, e);
        }

        // Cartoon Health Bar
        if (e.hp < e.maxHp && !"boss".equals(e.type)) {
            float barW = 48f;
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(COL_STICKER);
            canvas.drawRoundRect(-barW / 2f - 2f, -86f, barW / 2f + 2f, -76f, 3f, 3f, paintFill);

            paintFill.setColor(COL_INK);
            canvas.drawRect(-barW / 2f, -84f, barW / 2f, -78f, paintFill);

            float hpPct = (float) e.hp / e.maxHp;
            paintFill.setColor(COL_CORAL);
            canvas.drawRect(-barW / 2f, -84f, -barW / 2f + barW * hpPct, -78f, paintFill);
        }

        canvas.restore();
    }

    private void drawPaperCrab(Canvas canvas, Game.Enemy e) {
        drawFloorShadow(canvas, 0f, 3f, 35f, 11f);

        // Goofy Cartoon Paper Mario Crab!
        float wobble = (float) Math.sin(animTime * 8.0 + e.animTimer) * 5f;
        int fill = e.hitSquash > 0.05f ? 0xFFFEF08A : COL_CORAL; // Flash bright yellow on hit

        // Claws (Scissors snip-snip with white sticker border)
        for (int side : new int[]{-1, 1}) {
            canvas.save();
            canvas.scale(side, 1f);

            // Jointed legs
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(8f);
            canvas.drawLine(16f, -9f, 33f, -4f, paintStroke);
            canvas.drawLine(33f, -4f, 41f, 5f, paintStroke);

            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(3.5f);
            canvas.drawLine(16f, -9f, 33f, -4f, paintStroke);
            canvas.drawLine(33f, -4f, 41f, 5f, paintStroke);

            // Snapping cartoon pincer claws
            canvas.save();
            canvas.translate(31f, -31f);
            canvas.rotate(0.12f + wobble * 0.03f);
            float[][] claw = {{-10,10}, {-18,-4}, {-15,-22}, {-6,-30}, {-3,-12}, {6,-14}, {11,-30}, {19,-18}, {16,-2}, {4,13}};
            drawPaperCutout(canvas, claw, fill, true);
            canvas.restore();

            canvas.restore();
        }

        // Crab Carapace with white sticker border
        float[][] shell = {{-28,-10}, {-31,-30}, {-18,-45}, {7,-48}, {29,-32}, {29,-10}, {6,0}};
        drawPaperCutout(canvas, shell, fill, true);

        // Big Goofy Cartoon Eyes with white eyeballs and black pupils!
        for (int x : new int[]{-11, 13}) {
            // Stalk
            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(4f);
            canvas.drawLine(x, -36f, x - 2f, -50f, paintStroke);

            // Big White Cartoon Eyeball
            paintFill.setColor(COL_STICKER);
            canvas.drawCircle(x - 2f, -52f, 7f, paintFill);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawCircle(x - 2f, -52f, 7f, paintStroke);

            // Black pupil looking toward player
            paintFill.setColor(COL_INK);
            canvas.drawCircle(x - 2f, -52f, 3.5f, paintFill);

            // Cute white twinkle highlight
            paintFill.setColor(COL_STICKER);
            canvas.drawCircle(x - 3f, -54f, 1.5f, paintFill);
        }
    }

    private void drawPaperJellyfish(Canvas canvas, Game.Enemy e) {
        float bob = (float) Math.sin(animTime * 2.2 + e.animTimer) * 8f;
        drawFloorShadow(canvas, 0f, 6f, 30f, 9f);

        canvas.save();
        canvas.translate(0f, -24f + bob);

        // 5 fluttering crepe-paper tentacles
        for (int i = 0; i < 5; i++) {
            float ox = (i - 2) * 10f;
            float s = (float) Math.sin(animTime * 3.0 + e.animTimer + i) * 8f;

            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(7f);
            canvas.drawLine(ox, -2f, ox + s, 18f, paintStroke);
            canvas.drawLine(ox + s, 18f, ox - s * 0.3f, 36f, paintStroke);

            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(3f);
            canvas.drawLine(ox, -2f, ox + s, 18f, paintStroke);
            canvas.drawLine(ox + s, 18f, ox - s * 0.3f, 36f, paintStroke);
        }

        // Scalloped Blooper/Jelly Bell with white die-cut sticker border
        float[][] bell = {{-36,-6}, {-33,-29}, {-18,-45}, {0,-50}, {20,-44}, {33,-27}, {37,-6}, {25,2}, {13,-3}, {1,4}, {-11,-3}, {-24,3}};
        int fill = e.hitSquash > 0.05f ? 0xFFFEF08A : COL_AQUA;
        drawPaperCutout(canvas, bell, fill, true);

        // Cute cartoon eyes on the jellyfish
        for (int x : new int[]{-10, 10}) {
            paintFill.setColor(COL_STICKER);
            canvas.drawCircle(x, -18f, 5f, paintFill);
            paintStroke.setStrokeWidth(2f);
            canvas.drawCircle(x, -18f, 5f, paintStroke);
            paintFill.setColor(COL_INK);
            canvas.drawCircle(x, -18f, 2.5f, paintFill);
        }

        canvas.restore();
    }

    private void drawPaperAbyssalEel(Canvas canvas, Game.Enemy e) {
        drawFloorShadow(canvas, 0f, 5f, 38f, 9f);

        // Accordion Paper Eel
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(22f);
        paintStroke.setStrokeCap(Paint.Cap.ROUND);

        tempPath.reset();
        tempPath.moveTo(-e.radius * 1.3f, 0f);
        float s1 = (float) Math.sin(animTime * 6.5 + e.animTimer) * 18f;
        tempPath.quadTo(0f, s1, e.radius * 1.3f, 0f);
        canvas.drawPath(tempPath, paintStroke);

        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(16f);
        canvas.drawPath(tempPath, paintStroke);

        paintStroke.setColor(COL_AQUA);
        paintStroke.setStrokeWidth(10f);
        canvas.drawPath(tempPath, paintStroke);
        paintStroke.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawPaperBossLeviathan(Canvas canvas, Game.Enemy e) {
        float sway = (float) Math.sin(animTime * 1.8) * 5f;
        drawFloorShadow(canvas, 0f, 10f, 115f, 28f);

        canvas.save();
        canvas.translate(0f, sway);

        // 7 multi-layered pop-up tentacles with corrugated cardboard suction cups
        for (int i = 0; i < 7; i++) {
            float angle = (i - 3) * 0.32f;
            float tip = (float) Math.sin(animTime * 2.2 + i) * 16f;
            canvas.save();
            canvas.translate((i - 3) * 18f, -22f);
            canvas.rotate((float) Math.toDegrees(angle));
            float[][] tent = {{-11,0}, {-17,32}, {-9,65}, {tip,86}, {tip+15,81}, {4,51}, {14,10}};
            drawPaperCutout(canvas, tent, (i % 2 == 0) ? 0xFF34D399 : 0xFF10B981, true);
            canvas.restore();
        }

        // Giant Cardboard Monster Head with white sticker die-cut border
        float[][] head = {{-86,-33}, {-95,-90}, {-70,-150}, {-19,-176}, {45,-162}, {86,-118}, {95,-55}, {52,-13}, {-24,-9}};
        int fill = e.hitSquash > 0.05f ? 0xFFFEF08A : COL_GOLD;
        drawPaperCutout(canvas, head, fill, true);

        // Hypnotic spiral markings
        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(4.5f);
        tempPath.reset();
        for (float a = 0; a < Math.PI * 4; a += 0.1f) {
            float r = 5f + a * 4.4f;
            float sx = 6f + (float) Math.cos(a) * r;
            float sy = -92f + (float) Math.sin(a) * r;
            if (a == 0) tempPath.moveTo(sx, sy); else tempPath.lineTo(sx, sy);
        }
        canvas.drawPath(tempPath, paintStroke);

        // Big Menacing Cartoon Eye with white eyeball & yellow iris
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_STICKER);
        canvas.drawCircle(22f, -82f, 26f, paintFill);
        paintStroke.setStrokeWidth(3.5f);
        canvas.drawCircle(22f, -82f, 26f, paintStroke);

        paintFill.setColor(0xFFF97316); // Amber iris
        canvas.drawCircle(25f, -82f, 17f, paintFill);

        paintFill.setColor(COL_INK); // Cat slit pupil
        canvas.drawOval(26f, -94f, 32f, -70f, paintFill);

        canvas.restore();
    }

    private void drawClickBeacon(Canvas canvas, float x, float y, float timer) {
        float radius = (1.0f - timer) * 45f;
        int alpha = (int) (timer * 255);

        // Paper Mario Star Beacon
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(6f);
        paintStroke.setAlpha(alpha);
        canvas.drawCircle(x, y, radius, paintStroke);

        paintStroke.setColor(COL_GOLD);
        paintStroke.setStrokeWidth(3f);
        canvas.drawCircle(x, y, radius, paintStroke);
        paintStroke.setAlpha(255);
    }

    private void drawDrop(Canvas canvas, Game.Drop drop) {
        int rColor = game.getRarityColor(drop.item.rarity);
        float cy = drop.y + (float) Math.sin(drop.bobPhase) * 6f;

        // Cartoon Rarity Beam
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(drop.x, cy, drop.x, cy - 220f,
                rColor & 0x77FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        tempRect.set(drop.x - 14f, cy - 220f, drop.x + 14f, cy);
        canvas.drawRect(tempRect, paintFill);
        paintFill.setShader(null);

        drawFloorShadow(canvas, drop.x, drop.y, 18f, 6f);

        // Paper Badge with white sticker border
        float[][] badge = {{drop.x, cy - 15f}, {drop.x + 12f, cy}, {drop.x, cy + 15f}, {drop.x - 12f, cy}};
        drawPaperCutout(canvas, badge, rColor, true);

        // Pop-up Comic Name Plaque
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(12f);
        paintText.setFakeBoldText(true);
        float tw = paintText.measureText(drop.item.name) + 16f;

        paintFill.setColor(COL_STICKER);
        canvas.drawRoundRect(drop.x - tw / 2f - 2f, cy - 38f, drop.x + tw / 2f + 2f, cy - 18f, 5f, 5f, paintFill);

        paintFill.setColor(COL_INK);
        canvas.drawRoundRect(drop.x - tw / 2f, cy - 36f, drop.x + tw / 2f, cy - 20f, 4f, 4f, paintFill);

        paintText.setColor(rColor);
        canvas.drawText(drop.item.name, drop.x, cy - 25f, paintText);
    }

    private void drawComicFloater(Canvas canvas, Game.Floater f) {
        // Comic-book pop typography: thick sticker border + shadow
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(f.isCrit ? 26f : 18f);
        paintText.setFakeBoldText(true);

        // Outer white comic outline
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(f.isCrit ? 7f : 5f);
        canvas.drawText(f.text, f.x, f.y, paintStroke);

        // Dark ink stroke
        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(f.isCrit ? 4f : 3f);
        canvas.drawText(f.text, f.x, f.y, paintStroke);

        // Bright comic fill
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(f.color);
        canvas.drawText(f.text, f.x, f.y, paintFill);
    }

    // =========================================================================
    // HUD & MOBILE CONTROLS (CLASSIC ACTION DUAL-THUMB)
    // =========================================================================
    private void drawHudAndControls(Canvas canvas) {
        // Top Storybook Banner
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_STICKER);
        canvas.drawRoundRect(Game.SCREEN_W / 2f - 182f, 8f, Game.SCREEN_W / 2f + 182f, 46f, 10f, 10f, paintFill);

        paintFill.setColor(0xFF0F172A);
        canvas.drawRoundRect(Game.SCREEN_W / 2f - 180f, 10f, Game.SCREEN_W / 2f + 180f, 44f, 8f, 8f, paintFill);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(14f);
        paintText.setFakeBoldText(true);
        canvas.drawText(game.zoneName.toUpperCase() + "  -  " + game.depthMeters + "m",
                Game.SCREEN_W / 2f, 32f, paintText);

        // Sound Toggle
        paintFill.setColor(COL_STICKER);
        canvas.drawRoundRect(btnSound.left - 2f, btnSound.top - 2f, btnSound.right + 2f, btnSound.bottom + 2f, 8f, 8f, paintFill);
        paintFill.setColor(0xFF1E293B);
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
        // Outer Base Ring with white sticker rim
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(5f);
        canvas.drawCircle(joyBaseX, joyBaseY, joyBaseRadius, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x880F172A);
        canvas.drawCircle(joyBaseX, joyBaseY, joyBaseRadius, paintFill);

        paintStroke.setColor(COL_AQUA);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawCircle(joyBaseX, joyBaseY, joyBaseRadius, paintStroke);

        // Directional cross notches
        paintStroke.setColor(0x4438BDF8);
        canvas.drawLine(joyBaseX - 35f, joyBaseY, joyBaseX + 35f, joyBaseY, paintStroke);
        canvas.drawLine(joyBaseX, joyBaseY - 35f, joyBaseX, joyBaseY + 35f, paintStroke);

        // Movable Knob
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(5f);
        canvas.drawCircle(joyKnobX, joyKnobY, joyKnobRadius, paintStroke);

        paintFill.setColor(0xEE1E293B);
        canvas.drawCircle(joyKnobX, joyKnobY, joyKnobRadius, paintFill);

        paintStroke.setColor(COL_AQUA);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawCircle(joyKnobX, joyKnobY, joyKnobRadius, paintStroke);

        paintFill.setColor(COL_GOLD);
        canvas.drawCircle(joyKnobX, joyKnobY, 5f, paintFill);
    }

    private void drawMobileActionButtons(Canvas canvas) {
        // --- 1. BIG MAIN ATTACK BUTTON [ARPIONE] ---
        // White Sticker Border
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(7f);
        canvas.drawCircle(btnAttackX, btnAttackY, btnAttackRadius, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xEE1E293B);
        canvas.drawCircle(btnAttackX, btnAttackY, btnAttackRadius, paintFill);

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
            paintStroke.setColor(COL_GOLD);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawLine(btnDashX - 10f, btnDashY, btnDashX + 10f, btnDashY, paintStroke);
            canvas.drawLine(btnDashX + 2f, btnDashY - 7f, btnDashX + 10f, btnDashY, paintStroke);
            canvas.drawLine(btnDashX + 2f, btnDashY + 7f, btnDashX + 10f, btnDashY, paintStroke);
        });

        // --- 4. SKILL [POZIONE] (Cura) ---
        drawActionButton(canvas, btnPotionX, btnPotionY, btnPotionRadius, "CURA", 0f, 0xFF15803D, () -> {
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
            paintStroke.setColor(COL_GOLD);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRoundRect(btnInventoryX - 9f, btnInventoryY - 6f, btnInventoryX + 9f, btnInventoryY + 8f, 3f, 3f, paintStroke);
            canvas.drawLine(btnInventoryX - 9f, btnInventoryY, btnInventoryX + 9f, btnInventoryY, paintStroke);
        });
    }

    private void drawActionButton(Canvas canvas, float cx, float cy, float r, String label, float cdPct, int ringCol, Runnable iconDraw) {
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(5f);
        canvas.drawCircle(cx, cy, r, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xEE0F172A);
        canvas.drawCircle(cx, cy, r, paintFill);

        paintStroke.setColor(ringCol);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawCircle(cx, cy, r, paintStroke);

        iconDraw.run();

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
        paintStroke.setStrokeWidth(5f);
        paintStroke.setColor(COL_STICKER);
        canvas.drawCircle(cx, cy, r, paintStroke);

        paintStroke.setStrokeWidth(2.5f);
        paintStroke.setColor(0xFF7F1D1D);
        canvas.drawCircle(cx, cy, r, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF0F172A);
        canvas.drawCircle(cx, cy, r - 2f, paintFill);

        float pct = Math.max(0f, Math.min(1.0f, (float) game.hp / game.getMaxHp()));
        float top = (cy + r - 2f) - (2f * (r - 2f) * pct);

        canvas.save();
        tempPath.reset();
        tempPath.addCircle(cx, cy, r - 2f, Path.Direction.CW);
        canvas.clipPath(tempPath);

        paintFill.setColor(0xFFDC2626);
        canvas.drawRect(cx - r, top, cx + r, cy + r, paintFill);

        paintStroke.setColor(0x88FFFFFF);
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
        paintStroke.setStrokeWidth(5f);
        paintStroke.setColor(COL_STICKER);
        canvas.drawCircle(cx, cy, r, paintStroke);

        paintStroke.setStrokeWidth(2.5f);
        paintStroke.setColor(0xFF075985);
        canvas.drawCircle(cx, cy, r, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF0F172A);
        canvas.drawCircle(cx, cy, r - 2f, paintFill);

        float pct = Math.max(0f, Math.min(1.0f, game.tide / game.maxTide));
        float top = (cy + r - 2f) - (2f * (r - 2f) * pct);

        canvas.save();
        tempPath.reset();
        tempPath.addCircle(cx, cy, r - 2f, Path.Direction.CW);
        canvas.clipPath(tempPath);

        paintFill.setColor(0xFF0284C7);
        canvas.drawRect(cx - r, top, cx + r, cy + r, paintFill);

        paintStroke.setColor(0x88FFFFFF);
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
        drawPaperCutout(canvas, box, 0xFF0F172A, true);

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
    // MULTI-TOUCH (Dual-Thumb classic mobile controls)
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
        if (btnSound.contains(vx, vy)) {
            AudioHelper.toggleSound();
            AudioHelper.playClick();
            return;
        }

        // Left half: Movement Joystick
        if (vx < 420f && vy > 120f) {
            joyPointerId = pointerId;
            updateJoystick(vx, vy);
            return;
        }

        // Right side: Action Button Cluster
        float dAttack = distSq(vx, vy, btnAttackX, btnAttackY);
        if (dAttack < btnAttackRadius * btnAttackRadius * 1.5f) {
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
