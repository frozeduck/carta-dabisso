package eu.sabotino.cartadabisso;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.util.List;

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

    // Generated Paper Mario PNG Bitmaps
    private Bitmap bmpDiver;
    private Bitmap bmpCrab;
    private Bitmap bmpJelly;
    private Bitmap bmpBoss;
    private Bitmap bmpCoralRed;
    private Bitmap bmpCoralGreen;
    private Bitmap bmpShell;
    private Bitmap bmpMerchant;

    // Modals: Perk Selection Card Rects (3 cards)
    private final RectF[] perkCardRects = new RectF[]{
            new RectF(70f, 100f, 320f, 440f),
            new RectF(355f, 100f, 605f, 440f),
            new RectF(640f, 100f, 890f, 440f)
    };

    // Modals: Merchant Shop Dialog Rects
    private final RectF shopModalRect = new RectF(120f, 45f, 840f, 495f);
    private final RectF btnCloseShop = new RectF(795f, 55f, 830f, 90f);
    private final RectF shopSlotPotion = new RectF(150f, 130f, 810f, 220f);
    private final RectF shopSlotBlessing = new RectF(150f, 235f, 810f, 325f);
    private final RectF shopSlotCoffer = new RectF(150f, 340f, 810f, 430f);

    // Cartoon Paper Mario Palette
    private static final int COL_STICKER = 0xFFFFFFFF; // Iconic white sticker die-cut border
    private static final int COL_INK = 0xFF18181B;     // Thick comic ink contour
    private static final int COL_CREAM = 0xFFFDFBF7;   // Warm paper sheet
    private static final int COL_CANVAS = 0xFFFDE047;  // Warm canvas/gold
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
    private final Path shadowPath = new Path();
    private final RectF tempRect = new RectF();

    // Mobile Dynamic Virtual Joystick (Left Thumb)
    private final float joyDefaultBaseX = 140f;
    private final float joyDefaultBaseY = 415f;
    private float joyCurrentBaseX = joyDefaultBaseX;
    private float joyCurrentBaseY = joyDefaultBaseY;
    private final float joyBaseRadius = 56f;
    private final float joyKnobRadius = 26f;
    private float joyKnobX = joyDefaultBaseX;
    private float joyKnobY = joyDefaultBaseY;
    private int joyPointerId = -1;
    private boolean joyTouched = false;

    // Mobile Action Button Cluster (Right Thumb)
    private final float btnAttackX = 855f, btnAttackY = 425f, btnAttackRadius = 44f;
    private final float btnWaveX = 750f, btnWaveY = 455f, btnWaveRadius = 28f;
    private final float btnDashX = 855f, btnDashY = 315f, btnDashRadius = 28f;
    private final float btnPotionX = 760f, btnPotionY = 365f, btnPotionRadius = 26f;
    private final float btnInventoryX = 685f, btnInventoryY = 475f, btnInventoryRadius = 24f;
    private final float btnSkillTreeX = 615f, btnSkillTreeY = 475f, btnSkillTreeRadius = 24f;

    // Diablo Spheres (Upper Corners)
    private final float globeHpX = 55f, globeHpY = 55f, globeHpRadius = 34f;
    private final float globeTideX = 905f, globeTideY = 55f, globeTideRadius = 34f;

    // Sound button (Top center-right)
    private final RectF btnSound = new RectF(740f, 14f, 795f, 44f);

    // Skill Tree Modal Rectangles & Classes
    public boolean skillTreeOpen = false;
    public String selectedSkillId = "hunt_pierce";
    private final RectF modalSkillTreeRect = new RectF(55f, 25f, 905f, 515f);
    private final RectF btnCloseSkillTree = new RectF(860f, 35f, 895f, 68f);
    private final RectF btnRespec = new RectF(660f, 35f, 845f, 68f);
    private final RectF tabHunter = new RectF(70f, 35f, 245f, 68f);
    private final RectF tabJuggernaut = new RectF(255f, 35f, 435f, 68f);
    private final RectF tabMystic = new RectF(445f, 35f, 615f, 68f);

    // Tree coordinates (genuine visual tree topology)
    private final float treeRootX = 480f, treeRootY = 320f, treeRootRadius = 26f;
    private final float[] treeB1X = new float[]{270f, 200f, 200f};
    private final float[] treeB1Y = new float[]{255f, 170f, 85f};
    private final float[] treeB2X = new float[]{690f, 760f, 760f};
    private final float[] treeB2Y = new float[]{255f, 170f, 85f};
    private final float treeNodeRadius = 23f;
    private final RectF btnUpgradeSelectedSkill = new RectF(680f, 395f, 885f, 490f);
    private final RectF skillInspectRect = new RectF(70f, 385f, 890f, 500f);

    // Merchant interact HUD button (when near Mastro Paguro)
    private final RectF btnMerchantInteract = new RectF(575f, 14f, 730f, 46f);

    // Inventory Modal Rectangles
    private final RectF modalRect = new RectF(100f, 45f, 860f, 495f);
    private final RectF btnCloseModal = new RectF(815f, 55f, 850f, 85f);
    private final RectF slotEquipWeapon = new RectF(130f, 120f, 310f, 175f);
    private final RectF slotEquipArmor = new RectF(130f, 185f, 310f, 240f);
    private final RectF slotEquipRelic = new RectF(130f, 250f, 310f, 305f);

    // Backpack 8-slot grid (4x2)
    private final RectF[] backpackSlots = new RectF[8];
    private final RectF btnEquipSelected = new RectF(560f, 385f, 690f, 425f);
    private final RectF btnDropSelected = new RectF(705f, 385f, 825f, 425f);

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
        loadBitmaps(context);
    }

    private void loadBitmaps(Context context) {
        try {
            bmpDiver = BitmapFactory.decodeResource(context.getResources(), R.drawable.palombaro);
            bmpCrab = BitmapFactory.decodeResource(context.getResources(), R.drawable.granchio);
            bmpJelly = BitmapFactory.decodeResource(context.getResources(), R.drawable.medusa);
            bmpBoss = BitmapFactory.decodeResource(context.getResources(), R.drawable.boss_leviatano);
            bmpCoralRed = BitmapFactory.decodeResource(context.getResources(), R.drawable.corallo_rosso);
            bmpCoralGreen = BitmapFactory.decodeResource(context.getResources(), R.drawable.corallo_verde);
            bmpShell = BitmapFactory.decodeResource(context.getResources(), R.drawable.conchiglia);
            bmpMerchant = BitmapFactory.decodeResource(context.getResources(), R.drawable.paguro_bottegaio);
        } catch (Exception ignored) {
        }
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
        float startX = 350f;
        float startY = 120f;
        float slotW = 110f;
        float slotH = 58f;
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
        if (inventoryOpen) {
            skillTreeOpen = false;
        }
        selectedBackpackIndex = -1;
        AudioHelper.playClick();
    }

    public void toggleSkillTree() {
        skillTreeOpen = !skillTreeOpen;
        if (skillTreeOpen) {
            inventoryOpen = false;
        }
        AudioHelper.playClick();
    }

    private void resetJoystick() {
        joyPointerId = -1;
        joyTouched = false;
        joyCurrentBaseX = joyDefaultBaseX;
        joyCurrentBaseY = joyDefaultBaseY;
        joyKnobX = joyDefaultBaseX;
        joyKnobY = joyDefaultBaseY;
        game.joystickX = 0f;
        game.joystickY = 0f;
        game.joystickActive = false;
        game.playerMoving = false;
        game.playerTargetX = game.playerX;
        game.playerTargetY = game.playerY;
    }

    public void restartGame() {
        game.initPlayer();
        game.loadZone(1);
        game.gameOver = false;
        game.victory = false;
        inventoryOpen = false;
        resetJoystick();
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

    private void drawPaperCutout(Canvas canvas, float[][] pts, int fillColor, boolean whiteStickerBorder) {
        if (pts == null || pts.length < 3) return;

        tempPath.reset();
        tempPath.moveTo(pts[0][0], pts[0][1]);
        for (int i = 1; i < pts.length; i++) {
            tempPath.lineTo(pts[i][0], pts[i][1]);
        }
        tempPath.close();

        shadowPath.reset();
        shadowPath.moveTo(pts[0][0] + 3f, pts[0][1] + 5f);
        for (int i = 1; i < pts.length; i++) {
            shadowPath.lineTo(pts[i][0] + 3f, pts[i][1] + 5f);
        }
        shadowPath.close();
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_SHADOW);
        canvas.drawPath(shadowPath, paintFill);

        if (whiteStickerBorder) {
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(7f);
            paintStroke.setStrokeJoin(Paint.Join.ROUND);
            paintStroke.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawPath(tempPath, paintStroke);
        }

        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(3f);
        canvas.drawPath(tempPath, paintStroke);

        paintFill.setColor(fillColor);
        canvas.drawPath(tempPath, paintFill);

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
        if (skillTreeOpen) {
            drawSkillTreeModal(canvas);
        } else if (game.perkChoicePending) {
            drawPerkSelectionModal(canvas);
        } else if (game.merchantShopOpen) {
            drawMerchantShopModal(canvas);
        } else if (inventoryOpen) {
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
        // Pop-Up Storybook Ocean Stage
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(0, 0, 0, Game.MAP_H,
                new int[]{0xFF0E4A59, 0xFF082D38, 0xFF031620},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, Game.MAP_W, Game.MAP_H, paintFill);
        paintFill.setShader(null);

        drawStorybookWaves(canvas);

        // Soft God Rays
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

        // Cardboard Seabed Plateau
        float[][] seabed = {
                {50f, 50f}, {Game.MAP_W - 50f, 50f},
                {Game.MAP_W - 50f, Game.MAP_H - 50f}, {50f, Game.MAP_H - 50f}
        };
        drawPaperCutout(canvas, seabed, 0xFF11323B, false);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0x334ADE80);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawRect(65f, 65f, Game.MAP_W - 65f, Game.MAP_H - 65f, paintStroke);

        drawPaperCoralsAndRuins(canvas);

        if (game.merchantPresent) {
            drawPaperMerchant(canvas);
        }

        if (game.beaconTimer > 0f) {
            drawClickBeacon(canvas, game.beaconX, game.beaconY, game.beaconTimer);
        }

        if (game.portalOpen) {
            drawStorybookPortalArch(canvas, game.portalX, game.portalY);
        }

        for (Game.Drop drop : game.drops) {
            drawDrop(canvas, drop);
        }

        for (Game.Enemy enemy : game.enemies) {
            drawEnemy(canvas, enemy);
        }

        drawPaperDiver(canvas);

        for (Game.Projectile p : game.projectiles) {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(p.color);
            canvas.drawCircle(p.x, p.y, p.radius, paintFill);

            paintFill.setColor(0xCCFFFFFF);
            canvas.drawCircle(p.x - p.radius * 0.3f, p.y - p.radius * 0.3f, p.radius * 0.4f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawCircle(p.x, p.y, p.radius, paintStroke);
        }

        for (Game.Floater f : game.floaters) {
            drawComicFloater(canvas, f);
        }

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
        // Decorative perimeter corals only — combat arena (x: 250..1350, y: 220..900) remains completely clear
        drawCartoonCoral(canvas, 160f, 160f, 0, 0.30f, 0.5f);
        drawCartoonCoral(canvas, 520f, 130f, 1, 0.28f, 1.2f);
        drawCartoonCoral(canvas, 1080f, 130f, 0, 0.30f, 2.0f);
        drawCartoonCoral(canvas, 1440f, 160f, 1, 0.28f, 3.1f);
        drawCartoonCoral(canvas, 160f, 960f, 1, 0.30f, 4.4f);
        drawCartoonCoral(canvas, 1440f, 960f, 0, 0.28f, 1.8f);

        drawStorybookColumn(canvas, 200f, 200f, 0.65f);
        drawStorybookColumn(canvas, 1400f, 200f, 0.65f);
    }

    private void drawCartoonCoral(Canvas canvas, float x, float y, int type, float scale, float phase) {
        canvas.save();
        canvas.translate(x, y);
        float sway = (float) Math.sin(animTime * 1.5 + phase) * 3f;
        canvas.rotate(sway);
        canvas.scale(scale, scale);

        drawFloorShadow(canvas, 0f, 3f, 18f, 6f);

        Bitmap coralBmp = (type == 0) ? bmpCoralRed : bmpCoralGreen;
        if (coralBmp != null) {
            canvas.drawBitmap(coralBmp, -coralBmp.getWidth() / 2f, -coralBmp.getHeight() + 10f, null);
        } else {
            float[][] pts = {{-9,0}, {-13,-31}, {-34,-46}, {-38,-69}, {-30,-72}, {-25,-53},
                    {-13,-47}, {-16,-90}, {-7,-100}, {-4,-70}, {7,-57}, {21,-65},
                    {25,-88}, {33,-90}, {33,-60}, {13,-44}, {11,-20}, {25,-32},
                    {38,-31}, {21,-14}, {10,1}};
            drawPaperCutout(canvas, pts, (type == 0) ? COL_CORAL : COL_MINT, true);
        }
        canvas.restore();
    }

    private void drawStorybookColumn(Canvas canvas, float x, float y, float s) {
        canvas.save();
        canvas.translate(x, y);
        canvas.scale(s, s);
        drawFloorShadow(canvas, 0f, 0f, 24f, 8f);

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
    // PALOMBARO (PAPER MARIO STICKER CUTOUT SPRITE)
    // =========================================================================
    private void drawPaperDiver(Canvas canvas) {
        float px = game.playerX;
        float py = game.playerY;
        boolean moving = game.playerMoving;

        float hopY = moving ? -Math.abs((float) Math.sin(animTime * 14.0)) * 7f : (float) Math.sin(animTime * 2.2) * 2f;
        float tilt = moving ? (float) Math.sin(animTime * 14.0) * 5f : 0f;
        float flip = diverFlipScale;

        float sqX = game.attackAnim > 0 ? 1f + (float) Math.sin(game.attackAnim * Math.PI) * 0.25f : (game.dashAnim > 0 ? 1.25f : 1f);
        float sqY = game.attackAnim > 0 ? 1f - (float) Math.sin(game.attackAnim * Math.PI) * 0.18f : (game.dashAnim > 0 ? 0.82f : 1f);

        canvas.save();
        canvas.translate(px, py);

        drawFloorShadow(canvas, 0f, 2f, 20f, 7f);

        canvas.translate(0f, hopY);
        canvas.rotate(tilt);
        canvas.scale(flip * sqX, sqY);

        if (bmpDiver != null) {
            float s = 0.21f;
            canvas.scale(s, s);
            canvas.drawBitmap(bmpDiver, -bmpDiver.getWidth() / 2f, -220f, null);
            drawDiverEquipment(canvas);
        } else {
            // Procedural fallback with white sticker border
            float[][] suit = {{-15,-30}, {-10,-40}, {10,-40}, {16,-28}, {12,-12}, {2,-8}, {-12,-12}};
            drawPaperCutout(canvas, suit, COL_CANVAS, true);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(5f);
            canvas.drawCircle(0f, -44f, 18f, paintStroke);

            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFFF59E0B);
            canvas.drawCircle(0f, -44f, 18f, paintFill);

            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawCircle(0f, -44f, 18f, paintStroke);
        }

        canvas.restore();
    }

    private void drawDiverEquipment(Canvas canvas) {
        // Local space: diver center is x: 0, y: -110, feet at y: 0, top of helmet at y: -220
        // 1. ARMOR CHESTPLATE & PAULDRONS (Visual aesthetic changes based on equipped armor)
        if (game.equippedArmor != null) {
            String setName = game.equippedArmor.setName;
            int armorCol = 0xFFF87171; // Corallo (Red)
            int accentCol = 0xFFFFFFFF;
            if ("Marea".equals(setName)) {
                armorCol = 0xFF0284C7; // Marea (Cyan)
                accentCol = 0xFF38BDF8;
            } else if ("Abisso".equals(setName)) {
                armorCol = 0xFF7E22CE; // Abisso (Purple)
                accentCol = 0xFFC084FC;
            } else if (game.equippedArmor.rarity == Game.RARITY_COMMON) {
                armorCol = 0xFFD97706; // Bronze / leather
                accentCol = 0xFFFDE047;
            }

            // Chestplate cutout over diver torso
            float[][] chest = {
                    {-32f, -112f}, {24f, -112f},
                    {28f, -62f}, {-4f, -48f}, {-34f, -62f}
            };
            drawPaperCutout(canvas, chest, armorCol, true);

            // Inner chestplate decorative plate
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(accentCol);
            if ("Abisso".equals(setName)) {
                // Golden abyssal eye
                paintFill.setColor(COL_GOLD);
                canvas.drawCircle(-4f, -80f, 7f, paintFill);
                paintFill.setColor(COL_INK);
                canvas.drawCircle(-4f, -80f, 3.5f, paintFill);
            } else if ("Marea".equals(setName)) {
                // Wave rune arc
                paintStroke.setStyle(Paint.Style.STROKE);
                paintStroke.setColor(COL_STICKER);
                paintStroke.setStrokeWidth(3f);
                canvas.drawArc(-18f, -92f, 10f, -68f, 0f, 180f, false, paintStroke);
            } else {
                // Coral studs
                paintFill.setColor(COL_STICKER);
                canvas.drawCircle(-18f, -80f, 4f, paintFill);
                canvas.drawCircle(10f, -80f, 4f, paintFill);
            }

            // Left & Right shoulder pauldrons
            float[][] pauldronLeft = {{-48f, -120f}, {-28f, -120f}, {-30f, -96f}, {-52f, -96f}};
            float[][] pauldronRight = {{20f, -120f}, {40f, -120f}, {42f, -96f}, {22f, -96f}};
            drawPaperCutout(canvas, pauldronLeft, armorCol, true);
            drawPaperCutout(canvas, pauldronRight, armorCol, true);

            // Visor tint overlay on helmet
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor("Marea".equals(setName) ? 0x6638BDF8 : ("Abisso".equals(setName) ? 0x66A855F7 : 0x44F87171));
            canvas.drawCircle(-4f, -155f, 22f, paintFill);
        }

        // 2. WEAPON IN FRONT HAND (Thrusts forward dynamically during attackAnim)
        if (game.equippedWeapon != null) {
            String setName = game.equippedWeapon.setName;
            int wepCol = 0xFFB45309; // Copper
            if ("Corallo".equals(setName)) wepCol = 0xFFEF4444; // Coral
            else if ("Marea".equals(setName)) wepCol = 0xFF0284C7; // Tide
            else if ("Abisso".equals(setName)) wepCol = 0xFF6B21A8; // Void

            // Attack thrust translation during weapon swing
            float thrust = game.attackAnim > 0f ? (float) Math.sin(game.attackAnim * Math.PI) * 45f : 0f;

            canvas.save();
            canvas.translate(-50f - thrust, -90f);
            canvas.rotate(-15f + (thrust * 0.4f));

            // Harpoon / Spear shaft
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(9f);
            canvas.drawLine(0f, 75f, 0f, -75f, paintStroke);

            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(6f);
            canvas.drawLine(0f, 75f, 0f, -75f, paintStroke);

            paintStroke.setColor(wepCol);
            paintStroke.setStrokeWidth(3.5f);
            canvas.drawLine(0f, 75f, 0f, -75f, paintStroke);

            // Spearhead based on weapon type
            if ("Marea".equals(setName)) {
                // 3-pronged Trident head
                float[][] tridentLeft = {{-16f, -70f}, {-16f, -110f}, {-11f, -110f}, {-8f, -70f}};
                float[][] tridentCenter = {{-5f, -70f}, {0f, -125f}, {5f, -70f}};
                float[][] tridentRight = {{8f, -70f}, {11f, -110f}, {16f, -110f}, {16f, -70f}};
                drawPaperCutout(canvas, tridentLeft, 0xFF38BDF8, true);
                drawPaperCutout(canvas, tridentCenter, 0xFF38BDF8, true);
                drawPaperCutout(canvas, tridentRight, 0xFF38BDF8, true);
            } else if ("Abisso".equals(setName)) {
                // Curved scythe-harpoon head
                float[][] scythe = {{0f, -70f}, {-28f, -95f}, {-18f, -125f}, {8f, -85f}};
                drawPaperCutout(canvas, scythe, 0xFFC084FC, true);
            } else if ("Corallo".equals(setName)) {
                // Jagged serrated coral spearhead
                float[][] coralTip = {{0f, -125f}, {14f, -95f}, {6f, -90f}, {12f, -75f}, {0f, -70f}, {-12f, -75f}, {-6f, -90f}, {-14f, -95f}};
                drawPaperCutout(canvas, coralTip, 0xFFF87171, true);
            } else {
                // Standard Barbed Harpoon
                float[][] spear = {{0f, -115f}, {10f, -85f}, {3f, -80f}, {-3f, -80f}, {-10f, -85f}};
                drawPaperCutout(canvas, spear, 0xFFCBD5E1, true);
            }

            canvas.restore();
        }

        // 3. FLOATING RELIC COMPANION CHARM (Bobs smoothly near shoulder)
        if (game.equippedRelic != null) {
            String setName = game.equippedRelic.setName;
            float bobRelic = (float) Math.sin(animTime * 3.5) * 8f;
            canvas.save();
            canvas.translate(46f, -145f + bobRelic);

            int rCol = "Marea".equals(setName) ? 0xFF38BDF8 : ("Abisso".equals(setName) ? 0xFFA855F7 : 0xFFFBBF24);
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(COL_STICKER);
            canvas.drawCircle(0f, 0f, 13f, paintFill);

            paintFill.setColor(rCol);
            canvas.drawCircle(0f, 0f, 10f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawCircle(0f, 0f, 10f, paintStroke);

            paintFill.setColor(COL_STICKER);
            canvas.drawCircle(-3f, -3f, 3.5f, paintFill);

            canvas.restore();
        }
    }

    // =========================================================================
    // ENEMIES (PAPER MARIO CUTOUT SPRITES)
    // =========================================================================
    private void drawEnemy(Canvas canvas, Game.Enemy e) {
        if (e.isWindingUp) {
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

        // Clear Diablo-like Enemy Health Bar above head (Always visible so player tracks enemy HP)
        if (!"boss".equals(e.type)) {
            float barW = Math.max(38f, e.radius * 2.2f);
            float barH = 7f;
            float barY = -e.radius - 22f;

            // White sticker border
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(COL_STICKER);
            canvas.drawRoundRect(-barW / 2f - 2f, barY - barH / 2f - 2f, barW / 2f + 2f, barY + barH / 2f + 2f, 4f, 4f, paintFill);

            // Dark slate background
            paintFill.setColor(0xFF0F172A);
            canvas.drawRoundRect(-barW / 2f, barY - barH / 2f, barW / 2f, barY + barH / 2f, 3f, 3f, paintFill);

            // Red life bar fill
            float hpPct = Math.max(0f, Math.min(1.0f, (float) e.hp / e.maxHp));
            paintFill.setColor(e.isWindingUp ? 0xFFF59E0B : 0xFFEF4444);
            canvas.drawRect(-barW / 2f, barY - barH / 2f, -barW / 2f + (barW * hpPct), barY + barH / 2f, paintFill);

            // Inner ink border
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(1.2f);
            canvas.drawRoundRect(-barW / 2f, barY - barH / 2f, barW / 2f, barY + barH / 2f, 3f, 3f, paintStroke);

            // Miniature HP text: e.g. "45 / 75"
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(8f);
            paintText.setColor(COL_CREAM);
            paintText.setFakeBoldText(true);
            canvas.drawText(e.hp + " / " + e.maxHp, 0f, barY + 3f, paintText);
        }

        canvas.restore();
    }

    private void drawPaperCrab(Canvas canvas, Game.Enemy e) {
        drawFloorShadow(canvas, 0f, 2f, 18f, 6f);

        if (bmpCrab != null) {
            float s = 0.18f;
            canvas.scale(s, s);
            canvas.drawBitmap(bmpCrab, -bmpCrab.getWidth() / 2f, -200f, null);
        } else {
            float[][] shell = {{-16,-6}, {-18,-18}, {-10,-26}, {4,-28}, {16,-18}, {16,-6}, {4,0}};
            drawPaperCutout(canvas, shell, COL_CORAL, true);
        }
    }

    private void drawPaperJellyfish(Canvas canvas, Game.Enemy e) {
        float bob = (float) Math.sin(animTime * 2.2 + e.animTimer) * 5f;
        drawFloorShadow(canvas, 0f, 2f, 15f, 5f);

        canvas.save();
        canvas.translate(0f, -14f + bob);

        if (bmpJelly != null) {
            float s = 0.18f;
            canvas.scale(s, s);
            canvas.drawBitmap(bmpJelly, -bmpJelly.getWidth() / 2f, -150f, null);
        } else {
            float[][] bell = {{-20,-4}, {-18,-16}, {-10,-25}, {0,-28}, {11,-24}, {18,-15}, {20,-4}, {14,1}, {7,-2}, {0,2}, {-6,-2}, {-13,2}};
            drawPaperCutout(canvas, bell, COL_AQUA, true);
        }
        canvas.restore();
    }

    private void drawPaperAbyssalEel(Canvas canvas, Game.Enemy e) {
        drawFloorShadow(canvas, 0f, 4f, 24f, 7f);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(16f);
        paintStroke.setStrokeCap(Paint.Cap.ROUND);

        tempPath.reset();
        tempPath.moveTo(-e.radius * 1.3f, 0f);
        float s1 = (float) Math.sin(animTime * 6.5 + e.animTimer) * 12f;
        tempPath.quadTo(0f, s1, e.radius * 1.3f, 0f);
        canvas.drawPath(tempPath, paintStroke);

        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(11f);
        canvas.drawPath(tempPath, paintStroke);

        paintStroke.setColor(COL_AQUA);
        paintStroke.setStrokeWidth(7f);
        canvas.drawPath(tempPath, paintStroke);
        paintStroke.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawPaperBossLeviathan(Canvas canvas, Game.Enemy e) {
        float sway = (float) Math.sin(animTime * 1.8) * 5f;
        drawFloorShadow(canvas, 0f, 6f, 65f, 18f);

        canvas.save();
        canvas.translate(0f, sway);

        if (bmpBoss != null) {
            float s = 0.40f;
            canvas.scale(s, s);
            canvas.drawBitmap(bmpBoss, -bmpBoss.getWidth() / 2f, -280f, null);
        } else {
            float[][] head = {{-50,-20}, {-55,-54}, {-40,-90}, {-11,-105}, {27,-97}, {51,-70}, {57,-33}, {31,-8}, {-14,-5}};
            drawPaperCutout(canvas, head, COL_GOLD, true);
        }
        canvas.restore();
    }

    private void drawClickBeacon(Canvas canvas, float x, float y, float timer) {
        float radius = (1.0f - timer) * 45f;
        int alpha = (int) (timer * 255);

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
        if (drop.isShell()) {
            float cy = drop.y + (float) Math.sin(drop.bobPhase) * 5f;
            drawFloorShadow(canvas, drop.x, drop.y, 14f, 5f);
            canvas.save();
            canvas.translate(drop.x, cy);
            if (bmpShell != null) {
                float s = 0.22f;
                canvas.scale(s, s);
                canvas.drawBitmap(bmpShell, -bmpShell.getWidth() / 2f, -bmpShell.getHeight() / 2f, null);
            } else {
                paintFill.setStyle(Paint.Style.FILL);
                paintFill.setColor(COL_GOLD);
                canvas.drawCircle(0f, 0f, 8f, paintFill);
            }
            canvas.restore();

            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(10f);
            paintText.setFakeBoldText(true);
            paintText.setColor(COL_GOLD);
            canvas.drawText("+" + drop.shellAmount, drop.x, cy - 12f, paintText);
            return;
        }

        if (drop.item == null) return;
        int rColor = game.getRarityColor(drop.item.rarity);
        float cy = drop.y + (float) Math.sin(drop.bobPhase) * 6f;

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(drop.x, cy, drop.x, cy - 220f,
                rColor & 0x77FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        tempRect.set(drop.x - 14f, cy - 220f, drop.x + 14f, cy);
        canvas.drawRect(tempRect, paintFill);
        paintFill.setShader(null);

        drawFloorShadow(canvas, drop.x, drop.y, 18f, 6f);

        float[][] badge = {{drop.x, cy - 15f}, {drop.x + 12f, cy}, {drop.x, cy + 15f}, {drop.x - 12f, cy}};
        drawPaperCutout(canvas, badge, rColor, true);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(12f);
        paintText.setFakeBoldText(true);
        String dropLabel = "[" + drop.item.getSlotTitle() + "] " + drop.item.name;
        float tw = paintText.measureText(dropLabel) + 16f;

        paintFill.setColor(COL_STICKER);
        canvas.drawRoundRect(drop.x - tw / 2f - 2f, cy - 38f, drop.x + tw / 2f + 2f, cy - 18f, 5f, 5f, paintFill);

        paintFill.setColor(COL_INK);
        canvas.drawRoundRect(drop.x - tw / 2f, cy - 36f, drop.x + tw / 2f, cy - 20f, 4f, 4f, paintFill);

        paintText.setColor(rColor);
        canvas.drawText(dropLabel, drop.x, cy - 25f, paintText);
    }

    private void drawComicFloater(Canvas canvas, Game.Floater f) {
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(f.isCrit ? 26f : 18f);
        paintText.setFakeBoldText(true);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(f.isCrit ? 7f : 5f);
        canvas.drawText(f.text, f.x, f.y, paintStroke);

        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(f.isCrit ? 4f : 3f);
        canvas.drawText(f.text, f.x, f.y, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(f.color);
        canvas.drawText(f.text, f.x, f.y, paintFill);
    }

    // =========================================================================
    // HUD & MOBILE CONTROLS
    // =========================================================================
    private void drawHudAndControls(Canvas canvas) {
        // Conchiglie counter (Golden Shell currency badge)
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_STICKER);
        canvas.drawRoundRect(165f - 2f, 8f, 260f + 2f, 46f, 10f, 10f, paintFill);
        paintFill.setColor(0xFF0F172A);
        canvas.drawRoundRect(165f, 10f, 260f, 44f, 8f, 8f, paintFill);

        if (bmpShell != null) {
            float s = 0.16f;
            canvas.save();
            canvas.translate(182f, 27f);
            canvas.scale(s, s);
            canvas.drawBitmap(bmpShell, -bmpShell.getWidth() / 2f, -bmpShell.getHeight() / 2f, null);
            canvas.restore();
        }

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(13f);
        paintText.setFakeBoldText(true);
        canvas.drawText(String.valueOf(game.shells), 198f, 32f, paintText);

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

        paintFill.setColor(COL_STICKER);
        canvas.drawRoundRect(btnSound.left - 2f, btnSound.top - 2f, btnSound.right + 2f, btnSound.bottom + 2f, 8f, 8f, paintFill);
        paintFill.setColor(0xFF1E293B);
        canvas.drawRoundRect(btnSound, 6f, 6f, paintFill);
        paintText.setTextSize(10f);
        paintText.setColor(AudioHelper.isSoundEnabled() ? 0xFF22C55E : 0xFF94A3B8);
        canvas.drawText(AudioHelper.isSoundEnabled() ? "AUDIO" : "MUTO",
                btnSound.centerX(), btnSound.centerY() + 4f, paintText);

        drawRedLifeGlobe(canvas, globeHpX, globeHpY, globeHpRadius);
        drawBlueTideGlobe(canvas, globeTideX, globeTideY, globeTideRadius);

        // Diablo Boss Health Bar (if Boss is alive in current zone)
        for (Game.Enemy e : game.enemies) {
            if ("boss".equals(e.type) && e.hp > 0) {
                drawDiabloBossBar(canvas, e);
                break;
            }
        }

        drawVirtualJoystick(canvas);
        drawMobileActionButtons(canvas);
    }

    private void drawDiabloBossBar(Canvas canvas, Game.Enemy boss) {
        float cx = Game.SCREEN_W / 2f;
        float barY = 62f;
        float barW = 340f;
        float barH = 22f;

        // Sticker outline
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_STICKER);
        canvas.drawRoundRect(cx - barW / 2f - 3f, barY - barH / 2f - 3f, cx + barW / 2f + 3f, barY + barH / 2f + 3f, 8f, 8f, paintFill);

        // Frame
        paintFill.setColor(0xFF0F172A);
        canvas.drawRoundRect(cx - barW / 2f, barY - barH / 2f, cx + barW / 2f, barY + barH / 2f, 6f, 6f, paintFill);

        // Health fill
        float pct = Math.max(0f, Math.min(1.0f, (float) boss.hp / boss.maxHp));
        paintFill.setColor(0xFFDC2626);
        canvas.drawRoundRect(cx - barW / 2f + 2f, barY - barH / 2f + 2f, cx - barW / 2f + 2f + (barW - 4f) * pct, barY + barH / 2f - 2f, 4f, 4f, paintFill);

        // Comic border
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(cx - barW / 2f, barY - barH / 2f, cx + barW / 2f, barY + barH / 2f, 6f, 6f, paintStroke);

        // Boss Title & HP text
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(11f);
        paintText.setFakeBoldText(true);
        paintText.setColor(COL_GOLD);
        canvas.drawText("👑 LEVIATANO DEGLI ABISSI: " + boss.hp + " / " + boss.maxHp + " PV", cx, barY + 4f, paintText);
    }

    private void drawVirtualJoystick(Canvas canvas) {
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(5f);
        canvas.drawCircle(joyCurrentBaseX, joyCurrentBaseY, joyBaseRadius, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(joyTouched ? 0x990F172A : 0x440F172A);
        canvas.drawCircle(joyCurrentBaseX, joyCurrentBaseY, joyBaseRadius, paintFill);

        paintStroke.setColor(joyTouched ? COL_AQUA : 0x5538BDF8);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawCircle(joyCurrentBaseX, joyCurrentBaseY, joyBaseRadius, paintStroke);

        paintStroke.setColor(0x4438BDF8);
        canvas.drawLine(joyCurrentBaseX - 35f, joyCurrentBaseY, joyCurrentBaseX + 35f, joyCurrentBaseY, paintStroke);
        canvas.drawLine(joyCurrentBaseX, joyCurrentBaseY - 35f, joyCurrentBaseX, joyCurrentBaseY + 35f, paintStroke);

        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(5f);
        canvas.drawCircle(joyKnobX, joyKnobY, joyKnobRadius, paintStroke);

        paintFill.setColor(joyTouched ? 0xFF1E293B : 0x881E293B);
        canvas.drawCircle(joyKnobX, joyKnobY, joyKnobRadius, paintFill);

        paintStroke.setColor(joyTouched ? COL_AQUA : 0x6638BDF8);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawCircle(joyKnobX, joyKnobY, joyKnobRadius, paintStroke);

        paintFill.setColor(joyTouched ? COL_GOLD : 0x88FBBF24);
        canvas.drawCircle(joyKnobX, joyKnobY, 5f, paintFill);
    }

    private void drawMobileActionButtons(Canvas canvas) {
        // Merchant HUD interact pill (se il palombaro è vicino a Mastro Paguro)
        boolean merchantNear = game.merchantPresent && distSq(game.playerX, game.playerY, game.merchantX, game.merchantY) < 180f * 180f;
        if (merchantNear) {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0x88000000);
            canvas.drawRoundRect(btnMerchantInteract.left + 2f, btnMerchantInteract.top + 3f, btnMerchantInteract.right + 2f, btnMerchantInteract.bottom + 3f, 8f, 8f, paintFill);

            paintFill.setColor(COL_STICKER);
            canvas.drawRoundRect(btnMerchantInteract.left - 2f, btnMerchantInteract.top - 2f, btnMerchantInteract.right + 2f, btnMerchantInteract.bottom + 2f, 8f, 8f, paintFill);

            paintFill.setColor(0xFF78350F);
            canvas.drawRoundRect(btnMerchantInteract, 6f, 6f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_GOLD);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRoundRect(btnMerchantInteract, 6f, 6f, paintStroke);

            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(11f);
            paintText.setFakeBoldText(true);
            paintText.setColor(COL_GOLD);
            canvas.drawText("🛒 APRI BOTTEGA PAGURO", btnMerchantInteract.centerX(), btnMerchantInteract.centerY() + 4.5f, paintText);
        }

        // 1. ATTACCO (Tasto Principale Grande)
        drawActionButton(canvas, btnAttackX, btnAttackY, btnAttackRadius, "ATTACCO", 0f, COL_GOLD, 0xFFDC2626, 0xFF7F1D1D, () -> {
            // Lame incrociate con riflesso metallico
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(7f);
            canvas.drawLine(btnAttackX - 16f, btnAttackY + 16f, btnAttackX + 16f, btnAttackY - 16f, paintStroke);
            canvas.drawLine(btnAttackX + 16f, btnAttackY + 16f, btnAttackX - 16f, btnAttackY - 16f, paintStroke);

            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(4.5f);
            canvas.drawLine(btnAttackX - 16f, btnAttackY + 16f, btnAttackX + 16f, btnAttackY - 16f, paintStroke);
            canvas.drawLine(btnAttackX + 16f, btnAttackY + 16f, btnAttackX - 16f, btnAttackY - 16f, paintStroke);

            paintStroke.setColor(0xFFF1F5F9);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawLine(btnAttackX - 15f, btnAttackY + 15f, btnAttackX + 15f, btnAttackY - 15f, paintStroke);
            canvas.drawLine(btnAttackX + 15f, btnAttackY + 15f, btnAttackX - 15f, btnAttackY - 15f, paintStroke);

            // Elsa dorata centrale
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(COL_GOLD);
            canvas.drawCircle(btnAttackX, btnAttackY, 4.5f, paintFill);
        });

        // 2. MAREA (Onda d'Urto AoE)
        float waveCd = game.waveCooldown / game.waveMaxCooldown;
        drawActionButton(canvas, btnWaveX, btnWaveY, btnWaveRadius, "MAREA", waveCd, 0xFF38BDF8, 0xFF0284C7, 0xFF0C4A6E, () -> {
            tempPath.reset();
            tempPath.moveTo(btnWaveX - 13f, btnWaveY + 4f);
            tempPath.quadTo(btnWaveX - 4f, btnWaveY - 11f, btnWaveX + 2f, btnWaveY);
            tempPath.quadTo(btnWaveX + 8f, btnWaveY - 6f, btnWaveX + 13f, btnWaveY + 4f);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(4.5f);
            canvas.drawPath(tempPath, paintStroke);

            paintStroke.setColor(0xFF38BDF8);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawPath(tempPath, paintStroke);

            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFFE0F2FE);
            canvas.drawCircle(btnWaveX - 4f, btnWaveY - 11f, 2.5f, paintFill);
            canvas.drawCircle(btnWaveX + 8f, btnWaveY - 6f, 2.5f, paintFill);
        });

        // 3. SCATTO (Schivata Rapida)
        float dashCd = game.dashCooldown / game.dashMaxCooldown;
        drawActionButton(canvas, btnDashX, btnDashY, btnDashRadius, "SCATTO", dashCd, 0xFFFDE047, 0xFFF59E0B, 0xFF78350F, () -> {
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(5f);
            canvas.drawLine(btnDashX - 11f, btnDashY, btnDashX + 11f, btnDashY, paintStroke);
            canvas.drawLine(btnDashX + 2f, btnDashY - 8f, btnDashX + 11f, btnDashY, paintStroke);
            canvas.drawLine(btnDashX + 2f, btnDashY + 8f, btnDashX + 11f, btnDashY, paintStroke);

            paintStroke.setColor(COL_GOLD);
            paintStroke.setStrokeWidth(2.8f);
            canvas.drawLine(btnDashX - 11f, btnDashY, btnDashX + 11f, btnDashY, paintStroke);
            canvas.drawLine(btnDashX + 2f, btnDashY - 8f, btnDashX + 11f, btnDashY, paintStroke);
            canvas.drawLine(btnDashX + 2f, btnDashY + 8f, btnDashX + 11f, btnDashY, paintStroke);
        });

        // 4. CURA (Pozione Alchemica)
        drawActionButton(canvas, btnPotionX, btnPotionY, btnPotionRadius, "CURA", 0f, 0xFF4ADE80, 0xFF16A34A, 0xFF14532D, () -> {
            // Fiala pozione
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF4ADE80);
            canvas.drawRoundRect(btnPotionX - 8f, btnPotionY - 6f, btnPotionX + 8f, btnPotionY + 8f, 4f, 4f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRect(btnPotionX - 3f, btnPotionY - 11f, btnPotionX + 3f, btnPotionY - 6f, paintStroke);
            canvas.drawRoundRect(btnPotionX - 8f, btnPotionY - 6f, btnPotionX + 8f, btnPotionY + 8f, 4f, 4f, paintStroke);

            // Tappo di sughero
            paintFill.setColor(0xFFB45309);
            canvas.drawRect(btnPotionX - 2.5f, btnPotionY - 11f, btnPotionX + 2.5f, btnPotionY - 8f, paintFill);

            // Contatore pozioni
            paintText.setTextAlign(Paint.Align.RIGHT);
            paintText.setColor(0xFFFFFFFF);
            paintText.setTextSize(9.5f);
            paintText.setFakeBoldText(true);
            canvas.drawText("x" + game.potions, btnPotionX + 10f, btnPotionY + 9f, paintText);
        });

        // 5. ZAINO (Inventario & Reperti)
        drawActionButton(canvas, btnInventoryX, btnInventoryY, btnInventoryRadius, "ZAINO", 0f, COL_GOLD, 0xFF475569, 0xFF1E293B, () -> {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF92400E);
            canvas.drawRoundRect(btnInventoryX - 9f, btnInventoryY - 6f, btnInventoryX + 9f, btnInventoryY + 8f, 3f, 3f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRoundRect(btnInventoryX - 9f, btnInventoryY - 6f, btnInventoryX + 9f, btnInventoryY + 8f, 3f, 3f, paintStroke);

            paintStroke.setColor(COL_GOLD);
            paintStroke.setStrokeWidth(1.8f);
            canvas.drawLine(btnInventoryX - 9f, btnInventoryY, btnInventoryX + 9f, btnInventoryY, paintStroke);
            canvas.drawCircle(btnInventoryX, btnInventoryY, 2.5f, paintFill);
        });

        // 6. ALBERO (Albero Abilità & Classi)
        drawActionButton(canvas, btnSkillTreeX, btnSkillTreeY, btnSkillTreeRadius, "ALBERO", 0f, 0xFFC084FC, 0xFF9333EA, 0xFF581C87, () -> {
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_GOLD);
            paintStroke.setStrokeWidth(2f);
            canvas.drawLine(btnSkillTreeX, btnSkillTreeY + 8f, btnSkillTreeX, btnSkillTreeY - 6f, paintStroke);
            canvas.drawLine(btnSkillTreeX, btnSkillTreeY - 1f, btnSkillTreeX - 7f, btnSkillTreeY - 7f, paintStroke);
            canvas.drawLine(btnSkillTreeX, btnSkillTreeY - 1f, btnSkillTreeX + 7f, btnSkillTreeY - 7f, paintStroke);

            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFFFDE047);
            canvas.drawCircle(btnSkillTreeX, btnSkillTreeY - 8f, 3f, paintFill);
            canvas.drawCircle(btnSkillTreeX - 8f, btnSkillTreeY - 8f, 2.5f, paintFill);
            canvas.drawCircle(btnSkillTreeX + 8f, btnSkillTreeY - 8f, 2.5f, paintFill);
        });

        // Badge circolare rosso se ci sono punti abilità da spendere
        if (game.skillPoints > 0) {
            float badgeX = btnSkillTreeX + 13f;
            float badgeY = btnSkillTreeY - 13f;
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFFEF4444);
            canvas.drawCircle(badgeX, badgeY, 9f, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(2f);
            canvas.drawCircle(badgeX, badgeY, 9f, paintStroke);
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(9.5f);
            paintText.setColor(COL_STICKER);
            paintText.setFakeBoldText(true);
            canvas.drawText(String.valueOf(game.skillPoints), badgeX, badgeY + 3.5f, paintText);
        }
    }

    private void drawActionButton(Canvas canvas, float cx, float cy, float r, String label, float cdPct, int ringCol, int bgTop, int bgBot, Runnable iconDraw) {
        // 1. Ombra a terra morbida
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x66000000);
        canvas.drawCircle(cx + 2.5f, cy + 4f, r, paintFill);

        // 2. Contorno spesso Paper Mario bianco sticker
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(5f);
        canvas.drawCircle(cx, cy, r, paintStroke);

        // 3. Bordo interno inchiostro scuro
        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(2f);
        canvas.drawCircle(cx, cy, r - 1f, paintStroke);

        // 4. Riempimento tattile a gemma / gradiente
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(cx, cy - r, cx, cy + r, bgTop, bgBot, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r - 2f, paintFill);
        paintFill.setShader(null);

        // 5. Riflesso speculare superiore (gloss 3D)
        paintFill.setColor(0x33FFFFFF);
        canvas.drawArc(cx - r + 3f, cy - r + 3f, cx + r - 3f, cy + r - 3f, 200f, 140f, true, paintFill);

        // 6. Anello decorativo colorato
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(ringCol);
        paintStroke.setStrokeWidth(2.2f);
        canvas.drawCircle(cx, cy, r - 3f, paintStroke);

        // 7. Disegno icona
        iconDraw.run();

        // 8. Cooldown sweep overlay
        if (cdPct > 0.001f) {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xCC020617);
            canvas.drawArc(cx - r, cy - r, cx + r, cy + r, -90f, cdPct * 360f, true, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFFFCD34D);
            paintStroke.setStrokeWidth(2f);
            float endAngle = -90f + cdPct * 360f;
            float ex = cx + (float) Math.cos(Math.toRadians(endAngle)) * (r - 4f);
            float ey = cy + (float) Math.sin(Math.toRadians(endAngle)) * (r - 4f);
            canvas.drawLine(cx, cy, ex, ey, paintStroke);
        }

        // 9. Targhetta Sticker Dark sottostante con testo nitido ad alto contrasto
        float pillW = paintText.measureText(label) + 14f;
        float pillH = 15f;
        float pillY = cy + r + 3f;

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_STICKER);
        canvas.drawRoundRect(cx - pillW / 2f - 1.5f, pillY - 1.5f, cx + pillW / 2f + 1.5f, pillY + pillH + 1.5f, 4f, 4f, paintFill);

        paintFill.setColor(0xEE0B132B);
        canvas.drawRoundRect(cx - pillW / 2f, pillY, cx + pillW / 2f, pillY + pillH, 3f, 3f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(ringCol);
        paintStroke.setStrokeWidth(1.2f);
        canvas.drawRoundRect(cx - pillW / 2f, pillY, cx + pillW / 2f, pillY + pillH, 3f, 3f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(9f);
        paintText.setFakeBoldText(true);
        paintText.setColor(COL_CREAM);
        canvas.drawText(label, cx, pillY + 11f, paintText);
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

        Game.Item selItem = (selectedBackpackIndex >= 0 && selectedBackpackIndex < game.backpack.size()) ?
                game.backpack.get(selectedBackpackIndex) : null;
        String targetedSlot = selItem != null ? selItem.slot : null;

        drawEquippedSlot(canvas, slotEquipWeapon, "ARMA", game.equippedWeapon, "weapon".equals(targetedSlot));
        drawEquippedSlot(canvas, slotEquipArmor, "CORAZZA", game.equippedArmor, "armor".equals(targetedSlot));
        drawEquippedSlot(canvas, slotEquipRelic, "RELIQUIA", game.equippedRelic, "relic".equals(targetedSlot));

        // Active Set Bonuses info box on the left below equipment
        float setBoxY = 320f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF1E293B);
        canvas.drawRoundRect(130f, setBoxY, 310f, setBoxY + 115f, 6f, 6f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF475569);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(130f, setBoxY, 310f, setBoxY + 115f, 6f, 6f, paintStroke);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(10f);
        paintText.setFakeBoldText(true);
        paintText.setColor(COL_GOLD);
        canvas.drawText("BONUS SET (2+ PEZZI):", 140f, setBoxY + 18f, paintText);

        drawSetBonusLine(canvas, 140f, setBoxY + 38f, "Set Corallo", "+35 PV, +5 Dif", game.isSetBonusActive("Corallo"));
        drawSetBonusLine(canvas, 140f, setBoxY + 62f, "Set Marea", "+30 Marea, -20% Cd", game.isSetBonusActive("Marea"));
        drawSetBonusLine(canvas, 140f, setBoxY + 86f, "Set Abisso", "+22% Dmg, +12% Crit", game.isSetBonusActive("Abisso"));

        // Backpack Grid Header
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(0xFFCBD5E1);
        paintText.setTextSize(13f);
        paintText.setFakeBoldText(true);
        canvas.drawText("ZAINO (8 SLOT) - TOCCA PER CONFRONTARE", 350f, 100f, paintText);

        for (int i = 0; i < 8; i++) {
            RectF slot = backpackSlots[i];
            boolean isSelected = (i == selectedBackpackIndex);

            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(isSelected ? 0xFF1E3A8A : 0xFF1E293B);
            canvas.drawRoundRect(slot, 6f, 6f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(isSelected ? COL_GOLD : 0xFF475569);
            paintStroke.setStrokeWidth(isSelected ? 2.5f : 1.5f);
            canvas.drawRoundRect(slot, 6f, 6f, paintStroke);

            if (i < game.backpack.size()) {
                Game.Item it = game.backpack.get(i);
                int rColor = game.getRarityColor(it.rarity);

                // Slot category pill badge
                int pillBg = "weapon".equals(it.slot) ? 0x4438BDF8 : ("armor".equals(it.slot) ? 0x444ADE80 : 0x44C084FC);
                paintFill.setColor(pillBg);
                canvas.drawRoundRect(slot.left + 4f, slot.top + 4f, slot.right - 4f, slot.top + 18f, 3f, 3f, paintFill);

                paintText.setTextAlign(Paint.Align.CENTER);
                paintText.setTextSize(9f);
                paintText.setFakeBoldText(true);
                paintText.setColor(rColor);
                canvas.drawText(it.getSlotTitle(), slot.centerX(), slot.top + 14f, paintText);

                paintText.setTextSize(9f);
                paintText.setColor(0xFFFFFFFF);
                paintText.setFakeBoldText(false);
                String sn = it.name.length() > 14 ? it.name.substring(0, 13) + ".." : it.name;
                canvas.drawText(sn, slot.centerX(), slot.centerY() + 8f, paintText);

                if (it.setName != null) {
                    paintText.setTextSize(8f);
                    paintText.setColor(COL_GOLD);
                    canvas.drawText(it.setName, slot.centerX(), slot.bottom - 4f, paintText);
                }
            }
        }

        // Comparison & Detail Box (Diablo comparison card)
        RectF compRect = new RectF(350f, 260f, 850f, 440f);
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF131D2E);
        canvas.drawRoundRect(compRect, 8f, 8f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(selItem != null ? game.getRarityColor(selItem.rarity) : 0xFF334155);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(compRect, 8f, 8f, paintStroke);

        if (selItem != null) {
            Game.Item eq = game.getEquippedForSlot(selItem.slot);
            int rColor = game.getRarityColor(selItem.rarity);

            // Item Name & Category
            paintText.setTextAlign(Paint.Align.LEFT);
            paintText.setTextSize(14f);
            paintText.setFakeBoldText(true);
            paintText.setColor(rColor);
            canvas.drawText(selItem.name + " (" + selItem.getRarityName() + ")", compRect.left + 14f, compRect.top + 22f, paintText);

            paintText.setTextSize(11f);
            paintText.setColor(0xFF38BDF8);
            paintText.setFakeBoldText(false);
            canvas.drawText("CATEGORIA: " + selItem.getCategoryName(), compRect.left + 14f, compRect.top + 38f, paintText);

            paintText.setColor(selItem.setName != null ? COL_GOLD : 0xFF94A3B8);
            canvas.drawText("SET: " + (selItem.setName != null ? "Set " + selItem.setName : "Nessun Set"), compRect.left + 260f, compRect.top + 38f, paintText);

            // Divider line
            paintStroke.setColor(0x44FFFFFF);
            paintStroke.setStrokeWidth(1f);
            canvas.drawLine(compRect.left + 14f, compRect.top + 45f, compRect.right - 14f, compRect.top + 45f, paintStroke);

            // Comparison Header
            paintText.setTextSize(11f);
            paintText.setColor(COL_GOLD);
            paintText.setFakeBoldText(true);
            canvas.drawText("CONFRONTO CON: " + (eq != null ? eq.name + " (Equipaggiato)" : "Slot Vuoto"), compRect.left + 14f, compRect.top + 62f, paintText);

            // Diablo Stat Comparison with Deltas
            float statY = compRect.top + 80f;
            paintText.setTextSize(11f);
            paintText.setFakeBoldText(false);

            if ("weapon".equals(selItem.slot)) {
                int eqDmg = eq != null ? eq.damageBonus : 0;
                int dDmg = selItem.damageBonus - eqDmg;
                drawDiabloStatRow(canvas, compRect.left + 14f, statY, "Danno Base:", "+" + selItem.damageBonus, dDmg);

                float eqCrit = eq != null ? eq.critBonus : 0f;
                int dCrit = (int) ((selItem.critBonus - eqCrit) * 100f);
                drawDiabloStatRow(canvas, compRect.left + 14f, statY + 18f, "Critico:", "+" + (int)(selItem.critBonus * 100) + "%", dCrit);
            } else if ("armor".equals(selItem.slot)) {
                int eqDef = eq != null ? eq.defenseBonus : 0;
                int dDef = selItem.defenseBonus - eqDef;
                drawDiabloStatRow(canvas, compRect.left + 14f, statY, "Difesa:", "+" + selItem.defenseBonus, dDef);

                int eqHp = eq != null ? eq.healthBonus : 0;
                int dHp = selItem.healthBonus - eqHp;
                drawDiabloStatRow(canvas, compRect.left + 14f, statY + 18f, "Salute Massima:", "+" + selItem.healthBonus, dHp);
            } else if ("relic".equals(selItem.slot)) {
                float eqSpd = eq != null ? eq.speedBonus : 0f;
                int dSpd = (int)(selItem.speedBonus - eqSpd);
                drawDiabloStatRow(canvas, compRect.left + 14f, statY, "Velocità Movimento:", "+" + (int)selItem.speedBonus, dSpd);

                float eqCrit = eq != null ? eq.critBonus : 0f;
                int dCrit = (int)((selItem.critBonus - eqCrit) * 100f);
                drawDiabloStatRow(canvas, compRect.left + 14f, statY + 18f, "Critico:", "+" + (int)(selItem.critBonus * 100) + "%", dCrit);
            }

            // Action Buttons
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF15803D);
            canvas.drawRoundRect(btnEquipSelected, 6f, 6f, paintFill);
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setColor(0xFFFFFFFF);
            paintText.setTextSize(12f);
            paintText.setFakeBoldText(true);
            canvas.drawText("EQUIPAGGIA", btnEquipSelected.centerX(), btnEquipSelected.centerY() + 4f, paintText);

            paintFill.setColor(0xFFB91C1C);
            canvas.drawRoundRect(btnDropSelected, 6f, 6f, paintFill);
            canvas.drawText("SCARTA", btnDropSelected.centerX(), btnDropSelected.centerY() + 4f, paintText);
        } else {
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(12f);
            paintText.setColor(0xFF94A3B8);
            paintText.setFakeBoldText(false);
            canvas.drawText("Tocca un oggetto nello zaino per esaminarne categoria e statistiche", compRect.centerX(), compRect.centerY() - 10f, paintText);
            canvas.drawText("e visualizzare il confronto automatico con l'equipaggiamento attivo.", compRect.centerX(), compRect.centerY() + 12f, paintText);
        }

        // Overall stats bar
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(13f);
        paintText.setFakeBoldText(true);
        String fullStats = "STATISTICHE:   DANNO: " + game.getAttackDamage() +
                "    DIFESA: " + game.getDefense() +
                "    SALUTE: " + game.getMaxHp() +
                "    VELOCITA: " + (int)game.getMoveSpeed() +
                "    CRIT: " + (int)(game.getCritChance() * 100) + "%";
        canvas.drawText(fullStats, modalRect.left + 30f, 470f, paintText);
    }

    private void drawSetBonusLine(Canvas canvas, float x, float y, String name, String bonus, boolean active) {
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(10f);
        paintText.setColor(active ? 0xFF4ADE80 : 0xFF64748B);
        paintText.setFakeBoldText(active);
        canvas.drawText(name + ": " + bonus + (active ? " [ATTIVO]" : " (2pz)"), x, y, paintText);
    }

    private void drawDiabloStatRow(Canvas canvas, float x, float y, String statName, String val, int delta) {
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(0xFFE2E8F0);
        canvas.drawText(statName + " " + val, x, y, paintText);

        float nameW = paintText.measureText(statName + " " + val);
        if (delta > 0) {
            paintText.setColor(0xFF4ADE80); // Green for improvement
            canvas.drawText("  (+" + delta + " PIÙ FORTE)", x + nameW, y, paintText);
        } else if (delta < 0) {
            paintText.setColor(0xFFF87171); // Red for downgrade
            canvas.drawText("  (" + delta + " PIÙ DEBOLE)", x + nameW, y, paintText);
        } else {
            paintText.setColor(0xFF94A3B8);
            canvas.drawText("  (= IDENTICO)", x + nameW, y, paintText);
        }
    }

    private void drawEquippedSlot(Canvas canvas, RectF r, String slotTitle, Game.Item item, boolean isTargeted) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(isTargeted ? 0xFF1E3A8A : 0xFF1E293B);
        canvas.drawRoundRect(r, 6f, 6f, paintFill);

        int rColor = item != null ? game.getRarityColor(item.rarity) : 0xFF475569;
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(isTargeted ? COL_GOLD : rColor);
        paintStroke.setStrokeWidth(isTargeted ? 3f : 2f);
        canvas.drawRoundRect(r, 6f, 6f, paintStroke);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(10f);
        paintText.setColor(isTargeted ? COL_GOLD : 0xFF94A3B8);
        canvas.drawText(slotTitle + (isTargeted ? " (SOSTITUISCE)" : ""), r.left + 12f, r.top + 18f, paintText);

        if (item != null) {
            paintText.setTextSize(13f);
            paintText.setFakeBoldText(true);
            paintText.setColor(rColor);
            canvas.drawText(item.name, r.left + 12f, r.top + 36f, paintText);
            if (item.setName != null) {
                paintText.setTextSize(9f);
                paintText.setColor(COL_GOLD);
                canvas.drawText("[Set " + item.setName + "]", r.right - 70f, r.top + 18f, paintText);
            }
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

    private void drawPaperMerchant(Canvas canvas) {
        float x = game.merchantX;
        float y = game.merchantY;
        drawFloorShadow(canvas, x, y, 22f, 7f);

        float sway = (float) Math.sin(animTime * 1.8) * 3f;
        canvas.save();
        canvas.translate(x, y - 6f + sway);

        if (bmpMerchant != null) {
            float s = 0.22f;
            canvas.scale(s, s);
            canvas.drawBitmap(bmpMerchant, -bmpMerchant.getWidth() / 2f, -bmpMerchant.getHeight() + 20f, null);
        } else {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(COL_GOLD);
            canvas.drawCircle(0f, -20f, 18f, paintFill);
        }
        canvas.restore();

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xDD0F172A);
        canvas.drawRoundRect(x - 52f, y - 56f, x + 52f, y - 36f, 6f, 6f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_GOLD);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(x - 52f, y - 56f, x + 52f, y - 36f, 6f, 6f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(10f);
        paintText.setFakeBoldText(true);
        canvas.drawText("BOTTEGA [TOCCA]", x, y - 42f, paintText);
    }

    // =========================================================================
    // MODALE ALBERO DELLE ABILITÀ E CLASSI DI COMBATTIMENTO
    // =========================================================================
    private void drawSkillTreeModal(Canvas canvas) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xEE020B14);
        canvas.drawRect(0f, 0f, Game.SCREEN_W, Game.SCREEN_H, paintFill);

        // Backdrop panel (pergamena / ardesia marina)
        float[][] box = {
                {modalSkillTreeRect.left, modalSkillTreeRect.top}, {modalSkillTreeRect.right, modalSkillTreeRect.top},
                {modalSkillTreeRect.right, modalSkillTreeRect.bottom}, {modalSkillTreeRect.left, modalSkillTreeRect.bottom}
        };
        drawPaperCutout(canvas, box, 0xFF081325, true);

        // Header Class Tabs
        drawClassTab(canvas, tabHunter, "CACCIATORE", Game.CLASS_HUNTER.equals(game.playerClass), 0xFF0284C7);
        drawClassTab(canvas, tabJuggernaut, "CUSTODE", Game.CLASS_JUGGERNAUT.equals(game.playerClass), 0xFFD97706);
        drawClassTab(canvas, tabMystic, "MISTICO", Game.CLASS_MYSTIC.equals(game.playerClass), 0xFF7E22CE);

        // Respec button
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF7F1D1D);
        canvas.drawRoundRect(btnRespec, 6f, 6f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFFF87171);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(btnRespec, 6f, 6f, paintStroke);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(11f);
        paintText.setFakeBoldText(true);
        canvas.drawText("RESET (RESPEC)", btnRespec.centerX(), btnRespec.centerY() + 4f, paintText);

        // Close button
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF334155);
        canvas.drawRoundRect(btnCloseSkillTree, 4f, 4f, paintFill);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_CREAM);
        paintText.setTextSize(16f);
        canvas.drawText("X", btnCloseSkillTree.centerX(), btnCloseSkillTree.centerY() + 5.5f, paintText);

        // Subheader: Branch Titles & Available Points Pill
        String b1Title = Game.CLASS_HUNTER.equals(game.playerClass) ? "RAMO 1: BALISTICA PESANTE"
                : (Game.CLASS_JUGGERNAUT.equals(game.playerClass) ? "RAMO 1: BASTIONE DI CORALLO" : "RAMO 1: SIGNORE DELLE TEMPESTE");
        String b2Title = Game.CLASS_HUNTER.equals(game.playerClass) ? "RAMO 2: AGILITÀ IDRODINAMICA"
                : (Game.CLASS_JUGGERNAUT.equals(game.playerClass) ? "RAMO 2: MAGLIO SISMICO" : "RAMO 2: DRENAGGIO OSCURO");

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(11f);
        paintText.setFakeBoldText(true);
        paintText.setColor(0xFF38BDF8);
        canvas.drawText(b1Title, 220f, 66f, paintText);
        paintText.setColor(0xFFFBBF24);
        canvas.drawText(b2Title, 740f, 66f, paintText);

        // Skill points pill at top center
        float spPillW = 150f;
        float spPillH = 22f;
        float spPillX = 480f - spPillW / 2f;
        float spPillY = 46f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(game.skillPoints > 0 ? 0xFF14532D : 0xFF1E293B);
        canvas.drawRoundRect(spPillX, spPillY, spPillX + spPillW, spPillY + spPillH, 6f, 6f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(game.skillPoints > 0 ? 0xFF4ADE80 : 0xFF475569);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(spPillX, spPillY, spPillX + spPillW, spPillY + spPillH, 6f, 6f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(10.5f);
        paintText.setFakeBoldText(true);
        paintText.setColor(game.skillPoints > 0 ? 0xFF86EFAC : COL_CREAM);
        canvas.drawText("PUNTI ABILITÀ: " + game.skillPoints, 480f, spPillY + 15f, paintText);

        // Retrieve Skills
        List<Game.Skill> skills = game.getSkillsForClass(game.playerClass);
        Game.Skill sB1T1 = null, sB1T2 = null, sB1T3 = null;
        Game.Skill sB2T1 = null, sB2T2 = null, sB2T3 = null;
        for (Game.Skill s : skills) {
            if (s.branch == 1) {
                if (s.tier == 1) sB1T1 = s;
                else if (s.tier == 2) sB1T2 = s;
                else if (s.tier == 3) sB1T3 = s;
            } else if (s.branch == 2) {
                if (s.tier == 1) sB2T1 = s;
                else if (s.tier == 2) sB2T2 = s;
                else if (s.tier == 3) sB2T3 = s;
            }
        }

        // --- DISEGNO DEI RAMI DELL'ALBERO (Conduits / Limbs) ---
        // Ramo 1 (Sinistra): Radice -> B1T1 -> B1T2 -> B1T3
        drawTreeConduit(canvas, treeRootX, treeRootY, treeB1X[0], treeB1Y[0], sB1T1 != null && game.hasSkill(sB1T1.id));
        drawTreeConduit(canvas, treeB1X[0], treeB1Y[0], treeB1X[1], treeB1Y[1], sB1T2 != null && game.hasSkill(sB1T2.id));
        drawTreeConduit(canvas, treeB1X[1], treeB1Y[1], treeB1X[2], treeB1Y[2], sB1T3 != null && game.hasSkill(sB1T3.id));

        // Ramo 2 (Destra): Radice -> B2T1 -> B2T2 -> B2T3
        drawTreeConduit(canvas, treeRootX, treeRootY, treeB2X[0], treeB2Y[0], sB2T1 != null && game.hasSkill(sB2T1.id));
        drawTreeConduit(canvas, treeB2X[0], treeB2Y[0], treeB2X[1], treeB2Y[1], sB2T2 != null && game.hasSkill(sB2T2.id));
        drawTreeConduit(canvas, treeB2X[1], treeB2Y[1], treeB2X[2], treeB2Y[2], sB2T3 != null && game.hasSkill(sB2T3.id));

        // --- NODO CENTRALE DELLA RADICE DELL'ARCHETIPO (TRUNK BASE) ---
        drawTreeRootNode(canvas, treeRootX, treeRootY, treeRootRadius);

        // --- NODI CIRCOLARI DELLE ABILITÀ (MEDAGLIONI RUNICI) ---
        if (sB1T1 != null) drawTreeNode(canvas, treeB1X[0], treeB1Y[0], treeNodeRadius, sB1T1);
        if (sB1T2 != null) drawTreeNode(canvas, treeB1X[1], treeB1Y[1], treeNodeRadius, sB1T2);
        if (sB1T3 != null) drawTreeNode(canvas, treeB1X[2], treeB1Y[2], treeNodeRadius, sB1T3);

        if (sB2T1 != null) drawTreeNode(canvas, treeB2X[0], treeB2Y[0], treeNodeRadius, sB2T1);
        if (sB2T2 != null) drawTreeNode(canvas, treeB2X[1], treeB2Y[1], treeNodeRadius, sB2T2);
        if (sB2T3 != null) drawTreeNode(canvas, treeB2X[2], treeB2Y[2], treeNodeRadius, sB2T3);

        // --- SCHEDA INFERIORE DI DETTAGLIO E ATTIVAZIONE (INSPECTION BAR) ---
        Game.Skill selSkill = game.getSkillById(selectedSkillId);
        if (selSkill == null && !skills.isEmpty()) {
            selSkill = skills.get(0);
            selectedSkillId = selSkill.id;
        }
        if (selSkill != null) {
            drawSkillInspectionBar(canvas, selSkill);
        }
    }

    private void drawTreeConduit(Canvas canvas, float x1, float y1, float x2, float y2, boolean active) {
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF1E293B);
        paintStroke.setStrokeWidth(9f);
        canvas.drawLine(x1, y1, x2, y2, paintStroke);

        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(6.5f);
        canvas.drawLine(x1, y1, x2, y2, paintStroke);

        paintStroke.setColor(active ? COL_GOLD : 0xFF334155);
        paintStroke.setStrokeWidth(active ? 3.5f : 2f);
        canvas.drawLine(x1, y1, x2, y2, paintStroke);
    }

    private void drawTreeRootNode(Canvas canvas, float cx, float cy, float r) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_STICKER);
        canvas.drawCircle(cx, cy, r + 2.5f, paintFill);

        paintFill.setColor(0xFF0F172A);
        canvas.drawCircle(cx, cy, r, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_GOLD);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawCircle(cx, cy, r, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(9f);
        paintText.setFakeBoldText(true);
        canvas.drawText("RADICE", cx, cy - 2f, paintText);
        paintText.setColor(COL_CREAM);
        paintText.setTextSize(8f);
        paintText.setFakeBoldText(false);
        String clzTitle = Game.CLASS_HUNTER.equals(game.playerClass) ? "Cacciatore"
                : (Game.CLASS_JUGGERNAUT.equals(game.playerClass) ? "Custode" : "Mistico");
        canvas.drawText(clzTitle, cx, cy + 9f, paintText);
    }

    private void drawTreeNode(Canvas canvas, float cx, float cy, float r, Game.Skill s) {
        boolean isSelected = s.id.equals(selectedSkillId);
        boolean unlocked = game.hasSkill(s.id);
        boolean canUnlock = game.canUpgradeSkill(s.id);

        if (isSelected) {
            float pulse = 0.85f + (float) Math.sin(animTime * 5.0) * 0.15f;
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0x88FDE047);
            paintStroke.setStrokeWidth(6f);
            canvas.drawCircle(cx, cy, (r + 7f) * pulse, paintStroke);
        }

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_STICKER);
        canvas.drawCircle(cx, cy, r + 2.5f, paintFill);

        int nodeBg = unlocked ? 0xFF064E3B : (canUnlock ? 0xFF1E3A8A : 0xFF0F172A);
        paintFill.setColor(nodeBg);
        canvas.drawCircle(cx, cy, r, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(unlocked ? COL_GOLD : (canUnlock ? 0xFF38BDF8 : 0xFF475569));
        paintStroke.setStrokeWidth(isSelected ? 3f : (unlocked ? 2.2f : 1.5f));
        canvas.drawCircle(cx, cy, r, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(8.5f);
        paintText.setFakeBoldText(true);
        paintText.setColor(unlocked ? 0xFF86EFAC : (canUnlock ? 0xFF93C5FD : 0xFF94A3B8));
        canvas.drawText(s.badge, cx, cy + 3f, paintText);

        float tagX = cx < 480f ? cx - r - 12f : cx + r + 12f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(unlocked ? 0xFF14532D : 0xFF1E293B);
        canvas.drawRoundRect(tagX - 10f, cy - 8f, tagX + 10f, cy + 8f, 3f, 3f, paintFill);
        paintText.setTextSize(7.5f);
        paintText.setColor(unlocked ? 0xFF86EFAC : 0xFFCBD5E1);
        canvas.drawText("T" + s.tier, tagX, cy + 3f, paintText);

        paintText.setTextSize(8.5f);
        paintText.setColor(unlocked ? 0xFF86EFAC : (canUnlock ? 0xFFFCD34D : 0xFF64748B));
        String statusText = unlocked ? "ATTIVO" : (canUnlock ? "+ SBLOCCA" : "BLOCCATO");
        canvas.drawText(statusText, cx, cy + r + 12f, paintText);
    }

    private void drawSkillInspectionBar(Canvas canvas, Game.Skill s) {
        boolean unlocked = game.hasSkill(s.id);
        boolean canUnlock = game.canUpgradeSkill(s.id);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_STICKER);
        canvas.drawRoundRect(skillInspectRect.left - 2f, skillInspectRect.top - 2f, skillInspectRect.right + 2f, skillInspectRect.bottom + 2f, 10f, 10f, paintFill);

        paintFill.setColor(0xFF0F172A);
        canvas.drawRoundRect(skillInspectRect, 8f, 8f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(unlocked ? COL_GOLD : (canUnlock ? 0xFF38BDF8 : 0xFF334155));
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(skillInspectRect, 8f, 8f, paintStroke);

        // Sinistra: Titolo e Grado
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(14f);
        paintText.setFakeBoldText(true);
        paintText.setColor(COL_GOLD);
        canvas.drawText(s.name, skillInspectRect.left + 16f, skillInspectRect.top + 26f, paintText);

        paintText.setTextSize(10f);
        paintText.setColor(0xFF94A3B8);
        paintText.setFakeBoldText(false);
        String subHeader = "Ramo " + s.branch + " • Tier " + s.tier + " • Grado: " + (unlocked ? "1/1" : "0/1");
        canvas.drawText(subHeader, skillInspectRect.left + 16f, skillInspectRect.top + 42f, paintText);

        float bPillW = paintText.measureText(s.badge) + 16f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(s.color);
        canvas.drawRoundRect(skillInspectRect.left + 16f, skillInspectRect.top + 52f, skillInspectRect.left + 16f + bPillW, skillInspectRect.top + 70f, 4f, 4f, paintFill);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_INK);
        paintText.setTextSize(9.5f);
        paintText.setFakeBoldText(true);
        canvas.drawText(s.badge, skillInspectRect.left + 16f + bPillW / 2f, skillInspectRect.top + 65f, paintText);

        // Centro: Descrizione
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(10.5f);
        paintText.setColor(0xFFE2E8F0);
        paintText.setFakeBoldText(false);
        drawWrappedText(canvas, s.desc, skillInspectRect.left + 230f, skillInspectRect.top + 30f, 15f, 420f, paintText);

        if (s.requiredSkillId != null && !game.hasSkill(s.requiredSkillId)) {
            Game.Skill reqSkill = game.getSkillById(s.requiredSkillId);
            String reqName = reqSkill != null ? reqSkill.name : "Tier " + (s.tier - 1);
            paintText.setColor(0xFFF87171);
            paintText.setTextSize(9.5f);
            canvas.drawText("⚠ Richiede prima: " + reqName, skillInspectRect.left + 230f, skillInspectRect.bottom - 16f, paintText);
        }

        // Destra: Tasto Azione Grande
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(unlocked ? 0xFF14532D : (canUnlock ? 0xFF0284C7 : 0xFF1E293B));
        canvas.drawRoundRect(btnUpgradeSelectedSkill, 8f, 8f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(unlocked ? 0xFF22C55E : (canUnlock ? COL_GOLD : 0xFF475569));
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(btnUpgradeSelectedSkill, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setFakeBoldText(true);
        if (unlocked) {
            paintText.setTextSize(12f);
            paintText.setColor(0xFF86EFAC);
            canvas.drawText("TALENTO ATTIVO ✓", btnUpgradeSelectedSkill.centerX(), btnUpgradeSelectedSkill.centerY() - 2f, paintText);
            paintText.setTextSize(9f);
            paintText.setColor(0xFFBBF7D0);
            paintText.setFakeBoldText(false);
            canvas.drawText("Già Acquisito", btnUpgradeSelectedSkill.centerX(), btnUpgradeSelectedSkill.centerY() + 14f, paintText);
        } else if (canUnlock) {
            paintText.setTextSize(12.5f);
            paintText.setColor(COL_STICKER);
            canvas.drawText("+ SBLOCCA TALENTO", btnUpgradeSelectedSkill.centerX(), btnUpgradeSelectedSkill.centerY() - 3f, paintText);
            paintText.setTextSize(9f);
            paintText.setColor(0xFFFEF08A);
            paintText.setFakeBoldText(false);
            canvas.drawText("Costa 1 Punto Abilità", btnUpgradeSelectedSkill.centerX(), btnUpgradeSelectedSkill.centerY() + 14f, paintText);
        } else {
            paintText.setTextSize(11f);
            paintText.setColor(0xFF94A3B8);
            canvas.drawText("BLOCCATO", btnUpgradeSelectedSkill.centerX(), btnUpgradeSelectedSkill.centerY() - 3f, paintText);
            paintText.setTextSize(8.5f);
            paintText.setFakeBoldText(false);
            String reason = (s.requiredSkillId != null && !game.hasSkill(s.requiredSkillId))
                    ? "Richiede Tier " + (s.tier - 1)
                    : "Punti esauriti";
            canvas.drawText(reason, btnUpgradeSelectedSkill.centerX(), btnUpgradeSelectedSkill.centerY() + 13f, paintText);
        }
    }

    private void drawClassTab(Canvas canvas, RectF r, String title, boolean active, int accentCol) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(active ? accentCol : 0xFF1E293B);
        canvas.drawRoundRect(r, 6f, 6f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(active ? COL_GOLD : 0xFF475569);
        paintStroke.setStrokeWidth(active ? 2.5f : 1.2f);
        canvas.drawRoundRect(r, 6f, 6f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(active ? COL_STICKER : 0xFF94A3B8);
        paintText.setTextSize(12f);
        paintText.setFakeBoldText(true);
        canvas.drawText(title, r.centerX(), r.centerY() + 4.5f, paintText);
    }

    private void drawWrappedText(Canvas canvas, String text, float leftX, float topY, float lineSpacing, float maxW, Paint paint) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float y = topY;
        for (String w : words) {
            String test = line.length() == 0 ? w : line + " " + w;
            if (paint.measureText(test) > maxW) {
                canvas.drawText(line.toString(), leftX, y, paint);
                line = new StringBuilder(w);
                y += lineSpacing;
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) {
            canvas.drawText(line.toString(), leftX, y, paint);
        }
    }

    private void drawPerkSelectionModal(Canvas canvas) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xEE020B14);
        canvas.drawRect(0f, 0f, Game.SCREEN_W, Game.SCREEN_H, paintFill);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(22f);
        paintText.setFakeBoldText(true);
        canvas.drawText("LIVELLO " + game.level + "! SCEGLI UN TALENTO ABISSALE", Game.SCREEN_W / 2f, 50f, paintText);

        paintText.setTextSize(12f);
        paintText.setColor(COL_CREAM);
        paintText.setFakeBoldText(false);
        canvas.drawText("Tocca una carta per sbloccare la tua nuova abilità permanente", Game.SCREEN_W / 2f, 75f, paintText);

        List<Game.Perk> choices = game.pendingPerkChoices;
        for (int i = 0; i < choices.size() && i < perkCardRects.length; i++) {
            Game.Perk p = choices.get(i);
            RectF r = perkCardRects[i];

            paintFill.setColor(COL_STICKER);
            canvas.drawRoundRect(r.left - 4f, r.top - 4f, r.right + 4f, r.bottom + 4f, 14f, 14f, paintFill);

            paintFill.setColor(0xFF0F1E2E);
            canvas.drawRoundRect(r, 12f, 12f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(3f);
            canvas.drawRoundRect(r, 12f, 12f, paintStroke);

            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(p.color);
            canvas.drawRoundRect(r.left + 20f, r.top + 16f, r.right - 20f, r.top + 46f, 8f, 8f, paintFill);
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setColor(COL_INK);
            paintText.setTextSize(11f);
            paintText.setFakeBoldText(true);
            canvas.drawText(p.badge, r.centerX(), r.top + 35f, paintText);

            paintText.setColor(COL_CREAM);
            paintText.setTextSize(15f);
            canvas.drawText(p.title, r.centerX(), r.top + 78f, paintText);

            paintStroke.setColor(0x44FFFFFF);
            paintStroke.setStrokeWidth(1.5f);
            canvas.drawLine(r.left + 24f, r.top + 95f, r.right - 24f, r.top + 95f, paintStroke);

            paintText.setColor(0xFFCBD5E1);
            paintText.setTextSize(11f);
            paintText.setFakeBoldText(false);
            drawWrappedCardText(canvas, p.desc, r.centerX(), r.top + 130f, r.width() - 32f);

            float btnY = r.bottom - 50f;
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(p.color);
            canvas.drawRoundRect(r.left + 25f, btnY, r.right - 25f, btnY + 34f, 8f, 8f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRoundRect(r.left + 25f, btnY, r.right - 25f, btnY + 34f, 8f, 8f, paintStroke);

            paintText.setColor(COL_INK);
            paintText.setTextSize(13f);
            paintText.setFakeBoldText(true);
            canvas.drawText("SCEGLI", r.centerX(), btnY + 22f, paintText);
        }
    }

    private void drawWrappedCardText(Canvas canvas, String text, float cx, float topY, float maxW) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float y = topY;
        for (String w : words) {
            String test = line.length() == 0 ? w : line + " " + w;
            if (paintText.measureText(test) > maxW) {
                canvas.drawText(line.toString(), cx, y, paintText);
                line = new StringBuilder(w);
                y += 18f;
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) {
            canvas.drawText(line.toString(), cx, y, paintText);
        }
    }

    private void drawMerchantShopModal(Canvas canvas) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xDD020B14);
        canvas.drawRect(0f, 0f, Game.SCREEN_W, Game.SCREEN_H, paintFill);

        paintFill.setColor(COL_STICKER);
        canvas.drawRoundRect(shopModalRect.left - 4f, shopModalRect.top - 4f, shopModalRect.right + 4f, shopModalRect.bottom + 4f, 16f, 16f, paintFill);

        paintFill.setColor(0xFF0F1E2E);
        canvas.drawRoundRect(shopModalRect, 14f, 14f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(3f);
        canvas.drawRoundRect(shopModalRect, 14f, 14f, paintStroke);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(18f);
        paintText.setFakeBoldText(true);
        canvas.drawText("BOTTEGA DI MASTRO PAGURO", shopModalRect.left + 28f, shopModalRect.top + 38f, paintText);

        paintText.setTextAlign(Paint.Align.RIGHT);
        paintText.setTextSize(14f);
        canvas.drawText("Conchiglie: " + game.shells, shopModalRect.right - 80f, shopModalRect.top + 38f, paintText);
        if (bmpShell != null) {
            tempRect.set(shopModalRect.right - 72f, shopModalRect.top + 20f, shopModalRect.right - 46f, shopModalRect.top + 46f);
            canvas.drawBitmap(bmpShell, null, tempRect, null);
        }

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFEF4444);
        canvas.drawRoundRect(btnCloseShop, 6f, 6f, paintFill);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_STICKER);
        paintText.setTextSize(16f);
        canvas.drawText("X", btnCloseShop.centerX(), btnCloseShop.centerY() + 6f, paintText);

        drawShopItem(canvas, shopSlotPotion, "Fiala di Cura Rinforzata", "+1 Pozione di cura istantanea (PV massimi ripristinati)", 15,
                game.shells >= 15 && game.potions < game.maxPotions, 0xFF4ADE80);

        drawShopItem(canvas, shopSlotBlessing, "Benedizione delle Maree", "Ripristina al 100% la Marea e guarisce +30 PV", 25,
                game.shells >= 25, 0xFF38BDF8);

        drawShopItem(canvas, shopSlotCoffer, "Scrigno Misterioso dei Fondali", "Genera un equipaggiamento Raro o Abissale per lo zaino", 55,
                game.shells >= 55 && game.backpack.size() < 8, 0xFFC084FC);
    }

    private void drawShopItem(Canvas canvas, RectF r, String title, String desc, int cost, boolean canBuy, int accentCol) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF1E293B);
        canvas.drawRoundRect(r, 10f, 10f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(canBuy ? accentCol : 0x4464748B);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(r, 10f, 10f, paintStroke);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(canBuy ? COL_CREAM : 0xFF94A3B8);
        paintText.setTextSize(15f);
        paintText.setFakeBoldText(true);
        canvas.drawText(title, r.left + 20f, r.top + 32f, paintText);

        paintText.setTextSize(11f);
        paintText.setColor(0xFF94A3B8);
        paintText.setFakeBoldText(false);
        canvas.drawText(desc, r.left + 20f, r.top + 58f, paintText);

        float btnW = 130f;
        float btnH = 40f;
        float bx = r.right - btnW - 20f;
        float by = r.centerY() - btnH / 2f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(canBuy ? 0xFF10B981 : 0xFF334155);
        canvas.drawRoundRect(bx, by, bx + btnW, by + btnH, 8f, 8f, paintFill);

        paintStroke.setColor(canBuy ? COL_STICKER : 0xFF475569);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(bx, by, bx + btnW, by + btnH, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(canBuy ? COL_STICKER : 0xFF64748B);
        paintText.setTextSize(12f);
        paintText.setFakeBoldText(true);
        canvas.drawText("ACQUISTA (" + cost + ")", bx + btnW / 2f, by + 25f, paintText);
    }

    private void handlePerkSelectionTouch(float vx, float vy) {
        List<Game.Perk> choices = game.pendingPerkChoices;
        for (int i = 0; i < choices.size() && i < perkCardRects.length; i++) {
            if (perkCardRects[i].contains(vx, vy)) {
                game.selectPerk(choices.get(i).id);
                AudioHelper.playLevelUp();
                return;
            }
        }
    }

    private void handleMerchantShopTouch(float vx, float vy) {
        if (btnCloseShop.contains(vx, vy)) {
            game.merchantShopOpen = false;
            AudioHelper.playClick();
            return;
        }

        if (shopSlotPotion.contains(vx, vy)) {
            game.buyPotionFromMerchant();
            return;
        }

        if (shopSlotBlessing.contains(vx, vy)) {
            game.buyTideBlessing();
            return;
        }

        if (shopSlotCoffer.contains(vx, vy)) {
            game.buyMysteryCoffer();
            return;
        }
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();

        if (skillTreeOpen) {
            if (action == MotionEvent.ACTION_DOWN) {
                float vx = (event.getX() - offsetX) / viewScale;
                float vy = (event.getY() - offsetY) / viewScale;
                handleSkillTreeTouch(vx, vy);
            }
            return true;
        }

        if (game.perkChoicePending) {
            if (action == MotionEvent.ACTION_DOWN) {
                float vx = (event.getX() - offsetX) / viewScale;
                float vy = (event.getY() - offsetY) / viewScale;
                handlePerkSelectionTouch(vx, vy);
            }
            return true;
        }

        if (game.merchantShopOpen) {
            if (action == MotionEvent.ACTION_DOWN) {
                float vx = (event.getX() - offsetX) / viewScale;
                float vy = (event.getY() - offsetY) / viewScale;
                handleMerchantShopTouch(vx, vy);
            }
            return true;
        }

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

            case MotionEvent.ACTION_POINTER_UP: {
                int pointerId = event.getPointerId(actionIndex);
                handlePointerUp(pointerId);
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                int pointerId = event.getPointerId(actionIndex);
                handlePointerUp(pointerId);
                resetJoystick();
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

        float wx = vx + camX;
        float wy = vy + camY;

        // 1. Tasto HUD interazione Mastro Paguro (se il giocatore è nei paraggi)
        boolean merchantNear = game.merchantPresent && distSq(game.playerX, game.playerY, game.merchantX, game.merchantY) < 180f * 180f;
        if (merchantNear && btnMerchantInteract.contains(vx, vy)) {
            game.merchantShopOpen = true;
            AudioHelper.playClick();
            return;
        }

        // 2. Tocco diretto su Mastro Paguro nel mondo di gioco (priorità assoluta!)
        if (game.merchantPresent) {
            float dm = distSq(wx, wy, game.merchantX, game.merchantY);
            if (dm < 100f * 100f) {
                game.merchantShopOpen = true;
                AudioHelper.playClick();
                return;
            }
        }

        // 3. Tasti Azione Mobile (Destra)
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

        float dSkill = distSq(vx, vy, btnSkillTreeX, btnSkillTreeY);
        if (dSkill < btnSkillTreeRadius * btnSkillTreeRadius * 1.5f) {
            toggleSkillTree();
            return;
        }

        float dInv = distSq(vx, vy, btnInventoryX, btnInventoryY);
        if (dInv < btnInventoryRadius * btnInventoryRadius * 1.5f) {
            toggleInventory();
            return;
        }

        // 4. Raccolta Loot a terra con tocco diretto
        for (Game.Drop d : game.drops) {
            float dd = distSq(wx, wy, d.x, d.y);
            if (dd < 65f * 65f) {
                game.setPlayerTarget(d.x, d.y);
                return;
            }
        }

        // 5. Joystick analogico dinamico (metà sinistra dello schermo, lontano da entità)
        if (vx < 380f && vy > 110f) {
            if (joyPointerId == -1) {
                joyPointerId = pointerId;
                joyTouched = true;
                joyCurrentBaseX = vx;
                joyCurrentBaseY = vy;
                joyKnobX = vx;
                joyKnobY = vy;
                game.joystickX = 0f;
                game.joystickY = 0f;
                game.joystickActive = false;
                game.playerMoving = false;
                game.playerTargetX = game.playerX;
                game.playerTargetY = game.playerY;
            }
            return;
        }

        // 6. Attacco mirato a nemico
        for (Game.Enemy e : game.enemies) {
            if (e.hp <= 0) continue;
            float de = distSq(wx, wy, e.x, e.y);
            if (de < (e.radius + 20f) * (e.radius + 20f)) {
                game.performAttack(e.x, e.y);
                return;
            }
        }

        // 7. Navigazione point-and-click
        game.setPlayerTarget(wx, wy);
    }

    private void handlePointerMove(int pointerId, float vx, float vy) {
        if (pointerId == joyPointerId) {
            updateJoystick(vx, vy);
        }
    }

    private void handlePointerUp(int pointerId) {
        if (pointerId == joyPointerId) {
            resetJoystick();
        }
    }

    private void updateJoystick(float vx, float vy) {
        float dx = vx - joyCurrentBaseX;
        float dy = vy - joyCurrentBaseY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > 8f) {
            float clampedDist = Math.min(dist, joyBaseRadius);
            joyKnobX = joyCurrentBaseX + (dx / dist) * clampedDist;
            joyKnobY = joyCurrentBaseY + (dy / dist) * clampedDist;
            game.joystickX = (joyKnobX - joyCurrentBaseX) / joyBaseRadius;
            game.joystickY = (joyKnobY - joyCurrentBaseY) / joyBaseRadius;
            game.joystickActive = true;
            game.playerMoving = true;
            game.playerTargetX = game.playerX;
            game.playerTargetY = game.playerY;
        } else {
            joyKnobX = joyCurrentBaseX;
            joyKnobY = joyCurrentBaseY;
            game.joystickX = 0f;
            game.joystickY = 0f;
            game.joystickActive = false;
            game.playerMoving = false;
            game.playerTargetX = game.playerX;
            game.playerTargetY = game.playerY;
        }
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

    private void handleSkillTreeTouch(float vx, float vy) {
        if (btnCloseSkillTree.contains(vx, vy)) {
            skillTreeOpen = false;
            AudioHelper.playClick();
            return;
        }

        if (btnRespec.contains(vx, vy)) {
            game.respecSkills();
            return;
        }

        if (tabHunter.contains(vx, vy)) {
            game.selectClass(Game.CLASS_HUNTER);
            selectedSkillId = "hunt_pierce";
            return;
        }
        if (tabJuggernaut.contains(vx, vy)) {
            game.selectClass(Game.CLASS_JUGGERNAUT);
            selectedSkillId = "jug_armor";
            return;
        }
        if (tabMystic.contains(vx, vy)) {
            game.selectClass(Game.CLASS_MYSTIC);
            selectedSkillId = "mys_vortex";
            return;
        }

        // Tasto di sblocco/attivazione del talento selezionato nella scheda inferiore
        if (btnUpgradeSelectedSkill.contains(vx, vy)) {
            if (game.canUpgradeSkill(selectedSkillId)) {
                game.upgradeSkill(selectedSkillId);
            } else {
                AudioHelper.playClick();
            }
            return;
        }

        // Tocco sui nodi runici dell'albero per selezionare l'abilità
        List<Game.Skill> skills = game.getSkillsForClass(game.playerClass);
        for (int i = 0; i < 3; i++) {
            float d1 = distSq(vx, vy, treeB1X[i], treeB1Y[i]);
            if (d1 < (treeNodeRadius + 14f) * (treeNodeRadius + 14f)) {
                for (Game.Skill s : skills) {
                    if (s.branch == 1 && s.tier == (i + 1)) {
                        selectedSkillId = s.id;
                        AudioHelper.playClick();
                        return;
                    }
                }
            }
            float d2 = distSq(vx, vy, treeB2X[i], treeB2Y[i]);
            if (d2 < (treeNodeRadius + 14f) * (treeNodeRadius + 14f)) {
                for (Game.Skill s : skills) {
                    if (s.branch == 2 && s.tier == (i + 1)) {
                        selectedSkillId = s.id;
                        AudioHelper.playClick();
                        return;
                    }
                }
            }
        }
    }
}
