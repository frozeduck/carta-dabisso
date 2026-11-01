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
    private Bitmap bmpEel;
    private Bitmap bmpStarfish;
    private Bitmap bmpAnemone;
    private Bitmap bmpAngler;

    // Modals: Perk Selection Card Rects (3 cards)
    private final RectF[] perkCardRects = new RectF[]{
            new RectF(70f, 100f, 320f, 440f),
            new RectF(355f, 100f, 605f, 440f),
            new RectF(640f, 100f, 890f, 440f)
    };

    // Modals: Merchant Shop Dialog Rects & Exit Button
    public int shopTab = 0; // 0 = Provviste, 1 = Altare Reliquie Permanenti
    private final RectF shopModalRect = new RectF(120f, 25f, 840f, 515f);
    private final RectF btnCloseShop = new RectF(785f, 15f, 835f, 65f);
    private final RectF btnShopExit = new RectF(360f, 465f, 600f, 508f);
    private final RectF shopTabProvviste = new RectF();
    private final RectF shopTabAltare = new RectF();

    private final RectF shopSlotPotion = new RectF(150f, 125f, 810f, 222f);
    private final RectF shopSlotBlessing = new RectF(150f, 235f, 810f, 332f);
    private final RectF shopSlotCoffer = new RectF(150f, 345f, 810f, 442f);

    private final RectF metaSlotVitality = new RectF();
    private final RectF metaSlotTide = new RectF();
    private final RectF metaSlotPotion = new RectF();
    private final RectF metaSlotMagnet = new RectF();
    private final RectF btnEndless = new RectF();

    // Dynamic Fullscreen Adaptive Widescreen (Galaxy Z Flip 7, 22:9, 20:9, 16:9 - ZERO Black Bars)
    public static final float VIRTUAL_H = 540f;
    public float screenW = 960f;
    public float screenH = 540f;

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
    private float joyDefaultBaseX = 140f;
    private float joyDefaultBaseY = 415f;
    private float joyCurrentBaseX = joyDefaultBaseX;
    private float joyCurrentBaseY = joyDefaultBaseY;
    private final float joyBaseRadius = 56f;
    private final float joyKnobRadius = 26f;
    private float joyKnobX = joyDefaultBaseX;
    private float joyKnobY = joyDefaultBaseY;
    private int joyPointerId = -1;
    private boolean joyTouched = false;

    // Mobile Action Button Cluster (Right Thumb - Dynamic edge anchoring)
    private float btnAttackX = 860f, btnAttackY = 420f, btnAttackRadius = 44f;
    private float btnWaveX = 750f, btnWaveY = 440f, btnWaveRadius = 28f;
    private float btnDashX = 860f, btnDashY = 300f, btnDashRadius = 28f;
    private float btnPotionX = 750f, btnPotionY = 330f, btnPotionRadius = 26f;
    private float btnInventoryX = 655f, btnInventoryY = 460f, btnInventoryRadius = 24f;
    private float btnSkillTreeX = 575f, btnSkillTreeY = 460f, btnSkillTreeRadius = 24f;

    // Diablo Spheres (Upper Corners)
    private float globeHpX = 55f, globeHpY = 55f, globeHpRadius = 34f;
    private float globeTideX = 905f, globeTideY = 55f, globeTideRadius = 34f;

    // Sound button (Top center-right)
    private final RectF btnSound = new RectF(810f, 12f, 860f, 44f);

    // Skill Tree Modal Rectangles & Classes (Completely non-overlapping geometry)
    public boolean skillTreeOpen = false;
    public String selectedSkillId = "hunt_pierce";
    private final RectF modalSkillTreeRect = new RectF(50f, 15f, 910f, 525f);
    private final RectF btnCloseSkillTree = new RectF(845f, 18f, 895f, 70f);
    private final RectF btnRespec = new RectF(670f, 26f, 825f, 64f);
    private final RectF tabHunter = new RectF(65f, 26f, 195f, 64f);
    private final RectF tabJuggernaut = new RectF(205f, 26f, 335f, 64f);
    private final RectF tabMystic = new RectF(345f, 26f, 475f, 64f);
    private final RectF spPillRect = new RectF(490f, 26f, 655f, 64f);

    // Tree coordinates (genuine visual tree topology with branch banners)
    private float treeRootX = 480f, treeRootY = 320f, treeRootRadius = 24f;
    private final float[] treeB1X = new float[]{340f, 245f, 175f};
    private final float[] treeB1Y = new float[]{250f, 185f, 135f};
    private final float[] treeB2X = new float[]{620f, 715f, 785f};
    private final float[] treeB2Y = new float[]{250f, 185f, 135f};
    private float treeNodeRadius = 22f;
    private final RectF btnUpgradeSelectedSkill = new RectF(670f, 385f, 880f, 490f);
    private final RectF skillInspectRect = new RectF(65f, 365f, 895f, 508f);

    // Merchant interact HUD button (when near Mastro Paguro - dedicated slot)
    private final RectF btnMerchantInteract = new RectF(635f, 12f, 795f, 44f);

    // Inventory Modal Rectangles
    private final RectF modalRect = new RectF(100f, 20f, 860f, 520f);
    private final RectF btnCloseModal = new RectF(810f, 15f, 855f, 68f);
    private final RectF slotEquipWeapon = new RectF(130f, 110f, 310f, 165f);
    private final RectF slotEquipArmor = new RectF(130f, 175f, 310f, 230f);
    private final RectF slotEquipRelic = new RectF(130f, 240f, 310f, 295f);

    // Backpack 8-slot grid (4x2)
    private final RectF[] backpackSlots = new RectF[8];
    private final RectF btnEquipSelected = new RectF(560f, 395f, 690f, 440f);
    private final RectF btnDropSelected = new RectF(705f, 395f, 825f, 440f);

    // Game Over / Victory Buttons
    private final RectF btnRestart = new RectF(330f, 375f, 630f, 430f);

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
            bmpEel = BitmapFactory.decodeResource(context.getResources(), R.drawable.murena);
            bmpStarfish = BitmapFactory.decodeResource(context.getResources(), R.drawable.stella_marina);
            bmpAnemone = BitmapFactory.decodeResource(context.getResources(), R.drawable.anemone);
            bmpAngler = BitmapFactory.decodeResource(context.getResources(), R.drawable.pesce_lanterna);
        } catch (Exception ignored) {
        }
    }

    private void setupSoundBridge() {
        game.setSoundListener(new Game.SoundListener() {
            @Override public void onHit() { AudioHelper.playHit(); HapticHelper.lightTap(); }
            @Override public void onHeavyHit() { AudioHelper.playHeavyHit(); HapticHelper.heavyImpact(); }
            @Override public void onWave() { AudioHelper.playWave(); }
            @Override public void onDash() { AudioHelper.playDash(); HapticHelper.lightTap(); }
            @Override public void onPotion() { AudioHelper.playPotion(); }
            @Override public void onLoot(int rarity) { AudioHelper.playLoot(rarity); }
            @Override public void onClick() { AudioHelper.playClick(); }
            @Override public void onLevelUp() { AudioHelper.playLevelUp(); HapticHelper.victoryFanfare(); }
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
        if (h <= 0 || w <= 0) return;
        viewScale = (float) h / VIRTUAL_H;
        screenW = Math.max(960f, (float) w / viewScale);
        screenH = VIRTUAL_H;
        offsetX = 0f;
        offsetY = 0f;
        updateLayoutPositions();
    }

    private void updateLayoutPositions() {
        globeHpX = 55f;
        globeHpY = 55f;
        globeTideX = screenW - 55f;
        globeTideY = 55f;

        btnSound.set(screenW - 170f, 12f, screenW - 110f, 44f);
        btnMerchantInteract.set(screenW / 2f + 155f, 12f, screenW / 2f + 325f, 44f);

        btnAttackX = screenW - 95f;
        btnAttackY = 420f;
        btnDashX = screenW - 95f;
        btnDashY = 300f;
        btnWaveX = screenW - 195f;
        btnWaveY = 440f;
        btnPotionX = screenW - 195f;
        btnPotionY = 330f;
        btnInventoryX = screenW - 285f;
        btnInventoryY = 460f;
        btnSkillTreeX = screenW - 365f;
        btnSkillTreeY = 460f;

        float cx = screenW / 2f;

        // 1. Skill Tree Modal (Spazioso e widescreen-adaptive, zero collisioni)
        float treeW = Math.min(screenW - 40f, 1020f);
        float treeL = cx - treeW / 2f;
        float treeR = treeL + treeW;
        modalSkillTreeRect.set(treeL, 12f, treeR, 528f);

        // Header Row: 3 Archetype Tabs, Skill Points Badge, Respec Button, Close Seal
        float tabW = 120f;
        float tabH = 34f;
        tabHunter.set(treeL + 20f, 22f, treeL + 20f + tabW, 22f + tabH);
        tabJuggernaut.set(treeL + 150f, 22f, treeL + 150f + tabW, 22f + tabH);
        tabMystic.set(treeL + 280f, 22f, treeL + 280f + tabW, 22f + tabH);

        spPillRect.set(cx + 20f, 22f, cx + 175f, 22f + tabH);
        btnRespec.set(treeR - 220f, 22f, treeR - 85f, 22f + tabH);
        btnCloseSkillTree.set(treeR - 70f, 14f, treeR - 18f, 66f);

        treeRootX = cx;
        treeRootY = 320f;
        treeB1X[0] = cx - 130f; treeB1Y[0] = 250f;
        treeB1X[1] = cx - 210f; treeB1Y[1] = 190f;
        treeB1X[2] = cx - 280f; treeB1Y[2] = 135f;

        treeB2X[0] = cx + 130f; treeB2Y[0] = 250f;
        treeB2X[1] = cx + 210f; treeB2Y[1] = 190f;
        treeB2X[2] = cx + 280f; treeB2Y[2] = 135f;

        skillInspectRect.set(treeL + 20f, 365f, treeR - 20f, 512f);
        btnUpgradeSelectedSkill.set(treeR - 250f, 385f, treeR - 35f, 495f);

        // 2. Merchant Shop Modal
        float shopW = Math.min(screenW - 60f, 840f);
        float shopL = cx - shopW / 2f;
        float shopR = shopL + shopW;
        shopModalRect.set(shopL, 20f, shopR, 520f);
        btnCloseShop.set(shopR - 65f, 15f, shopR - 10f, 68f);
        btnShopExit.set(cx - 130f, 465f, cx + 130f, 508f);

        shopTabProvviste.set(shopL + 30f, 62f, cx - 10f, 102f);
        shopTabAltare.set(cx + 10f, 62f, shopR - 70f, 102f);

        shopSlotPotion.set(shopL + 30f, 155f, shopR - 30f, 245f);
        shopSlotBlessing.set(shopL + 30f, 255f, shopR - 30f, 345f);
        shopSlotCoffer.set(shopL + 30f, 355f, shopR - 30f, 445f);

        metaSlotVitality.set(shopL + 30f, 155f, shopR - 30f, 222f);
        metaSlotTide.set(shopL + 30f, 230f, shopR - 30f, 297f);
        metaSlotPotion.set(shopL + 30f, 305f, shopR - 30f, 372f);
        metaSlotMagnet.set(shopL + 30f, 380f, shopR - 30f, 447f);

        // 3. Inventory Modal
        float invW = Math.min(screenW - 60f, 860f);
        float invL = cx - invW / 2f;
        float invR = invL + invW;
        modalRect.set(invL, 20f, invR, 520f);
        btnCloseModal.set(invR - 65f, 15f, invR - 10f, 68f);

        slotEquipWeapon.set(invL + 30f, 110f, invL + 230f, 165f);
        slotEquipArmor.set(invL + 30f, 175f, invL + 230f, 230f);
        slotEquipRelic.set(invL + 30f, 240f, invL + 230f, 295f);

        float bagStartX = invL + 260f;
        float bagStartY = 110f;
        float slotW = (invW - 290f - 36f) / 4f;
        float slotH = 58f;
        for (int i = 0; i < 8; i++) {
            int col = i % 4;
            int row = i / 4;
            float left = bagStartX + col * (slotW + 12f);
            float top = bagStartY + row * (slotH + 12f);
            backpackSlots[i] = new RectF(left, top, left + slotW, top + slotH);
        }

        btnEquipSelected.set(invR - 300f, 395f, invR - 165f, 440f);
        btnDropSelected.set(invR - 150f, 395f, invR - 35f, 440f);

        // 4. Perk Selection Cards
        float perkCardW = (Math.min(screenW - 60f, 900f) - 60f) / 3f;
        float perkStart = cx - (perkCardW * 1.5f + 20f);
        for (int i = 0; i < 3; i++) {
            float left = perkStart + i * (perkCardW + 20f);
            perkCardRects[i].set(left, 100f, left + perkCardW, 440f);
        }

        // 5. Game Over / Victory
        btnRestart.set(cx - 150f, 400f, cx + 150f, 455f);
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

    public void startEndlessDive() {
        game.victory = false;
        game.loadZone(5);
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
        float targetCamX = game.playerX - screenW / 2f;
        float targetCamY = game.playerY - screenH / 2f;

        targetCamX = Math.max(0f, Math.min(Game.MAP_W - screenW, targetCamX));
        targetCamY = Math.max(0f, Math.min(Game.MAP_H - screenH, targetCamY));

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
        canvas.scale(viewScale, viewScale);
        canvas.clipRect(0f, 0f, screenW, screenH);

        // 1. World Layer
        canvas.save();
        canvas.translate(-camX, -camY);
        drawWorld(canvas);
        canvas.restore();

        boolean anyModalOpen = skillTreeOpen || game.perkChoicePending ||
                game.merchantShopOpen || inventoryOpen || game.gameOver || game.victory || paused;

        // 2. Diablo HUD + Mobile Controls Layer (disegnati SOLO durante l'esplorazione attiva!)
        if (!anyModalOpen) {
            drawHudAndControls(canvas);
        }

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
        // 1. Zone-Themed Watercolor Ocean Gradients
        int colTop, colMid, colBot;
        if (game.zone == 2) {
            // Fossa delle Tenebre (Abyssal indigo to dark violet)
            colTop = 0xFF1E1B4B;
            colMid = 0xFF31103F;
            colBot = 0xFF1E1E38;
        } else if (game.zone == 3) {
            // Dorsale Vulcanica (Volcanic obsidian to burning magma twilight)
            colTop = 0xFF1C0A0A;
            colMid = 0xFF450A0A;
            colBot = 0xFF7C2D12;
        } else if (game.zone == 4) {
            // Santuario del Kraken (Royal oceanic amethyst to luminous emerald deeps)
            colTop = 0xFF0B091A;
            colMid = 0xFF0F766E;
            colBot = 0xFF042F2E;
        } else if (game.zone >= 5) {
            // Abisso Senza Fine (Deep cosmic abyssal void)
            colTop = 0xFF020617;
            colMid = 0xFF0B132B;
            colBot = 0xFF1C2541;
        } else {
            // Barriera Corallina (Sunny Caribbean cyan/aquamarine to rich tropical teal)
            colTop = 0xFF0284C7;
            colMid = 0xFF0369A1;
            colBot = 0xFF0E4A59;
        }

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setShader(new LinearGradient(0, 0, 0, Game.MAP_H,
                new int[]{colTop, colMid, colBot},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, Game.MAP_W, Game.MAP_H, paintFill);
        paintFill.setShader(null);

        // 2. Parallax Animated Storybook Paper Waves
        drawStorybookWaves(canvas);

        // 3. Golden Sun Rays / Geothermal Vents & Motes & Bubbles
        drawSunRaysAndMotes(canvas);

        // 4. Cardboard Sandy Seabed with Dunes, Starfish, Shells & Anemones
        drawSandySeabedWithDunes(canvas);

        // 5. Perimeter Corals & Theatrical Stagecraft Ruins
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
        // Multi-tier animated wave currents with cream paper scallop foam ripples
        for (int layer = 0; layer < 3; layer++) {
            float speed = (layer == 0) ? 0.35f : (layer == 1 ? 0.6f : 0.85f);
            float offset = (float) Math.sin(animTime * speed) * 35f;
            float baseY = 160f + layer * 280f;
            int waveColor = (layer == 0) ? 0x22FFFFFF : (layer == 1 ? 0x1A38BDF8 : 0x180284C7);

            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(waveColor);

            tempPath.reset();
            tempPath.moveTo(0f, baseY + 60f);
            for (float x = 0; x <= Game.MAP_W + 120f; x += 120f) {
                float midX = x + 60f + offset;
                float endX = x + 120f + offset;
                float crest = baseY + (float) Math.sin(x * 0.02f + animTime * 1.2f) * 12f;
                tempPath.quadTo(midX, crest - 18f, endX, baseY + 20f);
            }
            tempPath.lineTo(Game.MAP_W, baseY + 70f);
            tempPath.lineTo(0f, baseY + 70f);
            tempPath.close();
            canvas.drawPath(tempPath, paintFill);

            // Paper Scallop Foam Trim
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0x44FFFFFF);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawPath(tempPath, paintStroke);
        }
    }

    private void drawSunRaysAndMotes(Canvas canvas) {
        // Warm God Rays
        for (int i = 0; i < 5; i++) {
            float x = Game.MAP_W * (0.15f + i * 0.18f) + (float) Math.sin(animTime * 0.3f + i) * 25f;
            tempPath.reset();
            tempPath.moveTo(x - 20f, 0f);
            tempPath.lineTo(x + 40f, 0f);
            tempPath.lineTo(x + 220f, Game.MAP_H);
            tempPath.lineTo(x - 45f, Game.MAP_H);
            tempPath.close();

            paintFill.setShader(new LinearGradient(x, 0, x + 80f, Game.MAP_H,
                    0x26FDE047, 0x00FDE047, Shader.TileMode.CLAMP));
            canvas.drawPath(tempPath, paintFill);
        }
        paintFill.setShader(null);

        // Floating Golden Plankton / Sun Motes
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x88FDE047);
        for (int i = 0; i < 16; i++) {
            float mx = (float) ((i * 113.7f + animTime * 14f) % Game.MAP_W);
            float my = (float) ((i * 71.3f + Math.sin(animTime * 0.8f + i) * 30f) % (Game.MAP_H - 120f));
            float sz = 2.5f + (float) Math.sin(animTime * 2.0f + i) * 1f;
            canvas.drawCircle(mx, my, sz, paintFill);
        }

        // Rising Paper Air Bubbles with Glare Crescents
        for (int i = 0; i < 10; i++) {
            float bx = (float) ((i * 179.3f + Math.sin(animTime * 1.5f + i) * 20f) % Game.MAP_W);
            float by = (float) (Game.MAP_H - ((animTime * 45f + i * 110f) % Game.MAP_H));
            float br = 4f + (i % 4) * 2.5f;

            paintFill.setColor(0x44FFFFFF);
            canvas.drawCircle(bx, by, br, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xAAFFFFFF);
            paintStroke.setStrokeWidth(1.4f);
            canvas.drawCircle(bx, by, br, paintStroke);

            // Glare arc
            paintStroke.setColor(0xEEFFFFFF);
            paintStroke.setStrokeWidth(1.2f);
            canvas.drawArc(bx - br + 1.5f, by - br + 1.5f, bx + br - 1.5f, by + br - 1.5f, -120f, 60f, false, paintStroke);
        }
    }

    private void drawSandySeabedWithDunes(Canvas canvas) {
        float seabedTop = Game.MAP_H - 240f;

        // Sand Base (warm golden sand: #FDE68A / #F59E0B)
        // Dune layer 1 (Background Dune)
        tempPath.reset();
        tempPath.moveTo(0f, seabedTop + 70f);
        for (float x = 0f; x <= Game.MAP_W + 200f; x += 200f) {
            float waveH = (float) Math.sin(x * 0.008f) * 28f;
            tempPath.quadTo(x + 100f, seabedTop + 45f + waveH, x + 200f, seabedTop + 70f + waveH);
        }
        tempPath.lineTo(Game.MAP_W, Game.MAP_H);
        tempPath.lineTo(0f, Game.MAP_H);
        tempPath.close();

        // Shadow under dune
        shadowPath.reset();
        shadowPath.addPath(tempPath, 0f, 6f);
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x33000000);
        canvas.drawPath(shadowPath, paintFill);

        // Dune fill
        paintFill.setShader(new LinearGradient(0, seabedTop + 40f, 0, Game.MAP_H,
                new int[]{0xFFFDE68A, 0xFFFBBF24, 0xFFD97706},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawPath(tempPath, paintFill);
        paintFill.setShader(null);

        // White Papercraft torn-edge trim on dune ridge
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(4.5f);
        canvas.drawPath(tempPath, paintStroke);

        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(2f);
        canvas.drawPath(tempPath, paintStroke);

        // Dashed sewing stitches along the dune edge (Paper Mario craftwork)
        paintStroke.setColor(0x88B45309);
        paintStroke.setStrokeWidth(2f);
        for (float x = 20f; x < Game.MAP_W; x += 36f) {
            float waveH = (float) Math.sin(x * 0.008f) * 28f;
            float sy = seabedTop + 78f + waveH;
            canvas.drawLine(x, sy, x + 18f, sy, paintStroke);
        }

        // Seabed Decorations: Starfish, Shells, Anemones
        drawSeabedDecorations(canvas, seabedTop);
    }

    private void drawSeabedDecorations(Canvas canvas, float seabedTop) {
        // Starfish scattered naturally across sandy dunes (non affollano il centro)
        if (bmpStarfish != null) {
            float[][] starPositions = {
                    {220f, seabedTop + 140f, 0.26f, 0.3f},
                    {540f, seabedTop + 160f, 0.28f, 1.2f},
                    {1260f, seabedTop + 160f, 0.28f, 2.1f},
                    {1580f, seabedTop + 140f, 0.26f, 3.4f}
            };
            for (float[] sp : starPositions) {
                canvas.save();
                canvas.translate(sp[0], sp[1]);
                canvas.rotate((float) Math.sin(animTime * 0.5 + sp[3]) * 6f);
                canvas.scale(sp[2], sp[2]);
                drawFloorShadow(canvas, 0f, 4f, 24f, 8f);
                canvas.drawBitmap(bmpStarfish, -bmpStarfish.getWidth() / 2f, -bmpStarfish.getHeight() / 2f, null);
                canvas.restore();
            }
        }

        // Bioluminescent Swaying Anemones (Paper Mario garden perimeter framing)
        if (bmpAnemone != null) {
            float[][] anemPositions = {
                    {240f, seabedTop + 95f, 0.22f, 0.5f},
                    {450f, seabedTop + 115f, 0.24f, 1.7f},
                    {1350f, seabedTop + 115f, 0.24f, 2.8f},
                    {1560f, seabedTop + 95f, 0.22f, 0.2f}
            };
            for (float[] ap : anemPositions) {
                canvas.save();
                canvas.translate(ap[0], ap[1]);
                float sway = (float) Math.sin(animTime * 1.8 + ap[3]) * 4f;
                canvas.rotate(sway);
                canvas.scale(ap[2], ap[2]);
                drawFloorShadow(canvas, 0f, 4f, 20f, 7f);
                canvas.drawBitmap(bmpAnemone, -bmpAnemone.getWidth() / 2f, -bmpAnemone.getHeight() + 25f, null);
                canvas.restore();
            }
        }

        // Spiral Seashells
        if (bmpShell != null) {
            float[][] shellPositions = {
                    {340f, seabedTop + 155f, 0.18f, 25f},
                    {750f, seabedTop + 165f, 0.16f, -15f},
                    {1150f, seabedTop + 165f, 0.16f, 15f},
                    {1460f, seabedTop + 150f, 0.18f, 40f}
            };
            for (float[] shp : shellPositions) {
                canvas.save();
                canvas.translate(shp[0], shp[1]);
                canvas.rotate(shp[3]);
                canvas.scale(shp[2], shp[2]);
                drawFloorShadow(canvas, 0f, 3f, 14f, 5f);
                canvas.drawBitmap(bmpShell, -bmpShell.getWidth() / 2f, -bmpShell.getHeight() / 2f, null);
                canvas.restore();
            }
        }
    }

    private void drawPaperCoralsAndRuins(Canvas canvas) {
        // Decorative perimeter corals only — framing stage wings and seabed
        drawCartoonCoral(canvas, 140f, 160f, 0, 0.28f, 0.5f);
        drawCartoonCoral(canvas, 1660f, 160f, 1, 0.28f, 2.0f);
        drawCartoonCoral(canvas, 110f, 520f, 1, 0.32f, 3.1f);
        drawCartoonCoral(canvas, 1690f, 520f, 0, 0.32f, 1.8f);
        drawCartoonCoral(canvas, 180f, 920f, 0, 0.36f, 4.4f);
        drawCartoonCoral(canvas, 1620f, 920f, 1, 0.36f, 1.2f);

        drawStorybookColumn(canvas, 180f, 240f, 0.55f);
        drawStorybookColumn(canvas, 1620f, 240f, 0.55f);
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
        } else if ("angler".equals(e.type)) {
            drawPaperAnglerfish(canvas, e);
        } else {
            drawPaperCrab(canvas, e);
        }

        // Paper Mario Heart Badge & HP Bar above enemy head
        if (!"boss".equals(e.type)) {
            float barW = Math.max(46f, e.radius * 2.3f);
            float barH = 8f;
            float barY = -e.radius - 22f;

            // Soft shadow
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0x55000000);
            canvas.drawRoundRect(-barW / 2f + 1f, barY - barH / 2f + 2f, barW / 2f + 1f, barY + barH / 2f + 2f, 4f, 4f, paintFill);

            // White die-cut sticker contour
            paintFill.setColor(COL_STICKER);
            canvas.drawRoundRect(-barW / 2f - 2f, barY - barH / 2f - 2f, barW / 2f + 2f, barY + barH / 2f + 2f, 5f, 5f, paintFill);

            // Dark ink background
            paintFill.setColor(0xFF1E293B);
            canvas.drawRoundRect(-barW / 2f, barY - barH / 2f, barW / 2f, barY + barH / 2f, 4f, 4f, paintFill);

            // Red life bar fill (amber if winding up an attack)
            float hpPct = Math.max(0f, Math.min(1.0f, (float) e.hp / e.maxHp));
            paintFill.setColor(e.isWindingUp ? 0xFFF59E0B : 0xFFEF4444);
            canvas.drawRoundRect(-barW / 2f + 1f, barY - barH / 2f + 1f, -barW / 2f + 1f + ((barW - 2f) * hpPct), barY + barH / 2f - 1f, 3f, 3f, paintFill);

            // Inner ink border
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(1.2f);
            canvas.drawRoundRect(-barW / 2f, barY - barH / 2f, barW / 2f, barY + barH / 2f, 4f, 4f, paintStroke);

            // Paper Mario cute Heart badge at the left
            drawHeart(canvas, -barW / 2f - 4f, barY, 4.5f, 0xFFEF4444);

            // HP text: e.g. "45 / 75"
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(8f);
            paintText.setColor(COL_CREAM);
            paintText.setFakeBoldText(true);
            canvas.drawText(e.hp + " / " + e.maxHp, 2f, barY + 3f, paintText);

            // When hit, draw cartoon impact stars!
            if (e.hitSquash > 0.05f) {
                drawCartoonHitStars(canvas, 0f, barY - 10f, e.hitSquash);
            }
        }

        canvas.restore();
    }

    private void drawHeart(Canvas canvas, float cx, float cy, float r, int fillColor) {
        tempPath.reset();
        tempPath.moveTo(cx, cy + r * 0.7f);
        tempPath.cubicTo(cx - r * 1.3f, cy - r * 0.3f, cx - r * 0.9f, cy - r * 1.2f, cx, cy - r * 0.3f);
        tempPath.cubicTo(cx + r * 0.9f, cy - r * 1.2f, cx + r * 1.3f, cy - r * 0.3f, cx, cy + r * 0.7f);
        tempPath.close();

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawPath(tempPath, paintStroke);

        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(1.2f);
        canvas.drawPath(tempPath, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(fillColor);
        canvas.drawPath(tempPath, paintFill);
    }

    private void drawCartoonHitStars(Canvas canvas, float cx, float cy, float intensity) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_GOLD);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(1.2f);

        for (int i = 0; i < 3; i++) {
            float angle = (i * 1.3f) + animTime * 8f;
            float dist = 14f + intensity * 12f;
            float sx = cx + (float) Math.cos(angle) * dist;
            float sy = cy + (float) Math.sin(angle) * dist * 0.6f;
            float sz = 4f * intensity;

            tempPath.reset();
            tempPath.moveTo(sx, sy - sz);
            tempPath.lineTo(sx + sz * 0.35f, sy - sz * 0.35f);
            tempPath.lineTo(sx + sz, sy);
            tempPath.lineTo(sx + sz * 0.35f, sy + sz * 0.35f);
            tempPath.lineTo(sx, sy + sz);
            tempPath.lineTo(sx - sz * 0.35f, sy + sz * 0.35f);
            tempPath.lineTo(sx - sz, sy);
            tempPath.lineTo(sx - sz * 0.35f, sy - sz * 0.35f);
            tempPath.close();

            canvas.drawPath(tempPath, paintFill);
            canvas.drawPath(tempPath, paintStroke);
        }
    }

    private void drawPaperCrab(Canvas canvas, Game.Enemy e) {
        drawFloorShadow(canvas, 0f, 2f, 18f, 6f);

        // Comical side-to-side wobble walking animation
        float wobble = (float) Math.sin(animTime * 11.0 + e.animTimer) * 6f;
        float scuttleY = Math.abs((float) Math.sin(animTime * 11.0 + e.animTimer)) * 3.5f;
        canvas.translate(0f, -scuttleY);
        canvas.rotate(wobble);

        if (bmpCrab != null) {
            float s = 0.18f;
            canvas.scale(s, s);
            canvas.drawBitmap(bmpCrab, -bmpCrab.getWidth() / 2f, -bmpCrab.getHeight() + 25f, null);
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

        // Pulsating accordion bell squash & stretch motion
        float pulse = (float) Math.sin(animTime * 3.8 + e.animTimer);
        float jSqX = 1f + pulse * 0.12f;
        float jSqY = 1f - pulse * 0.12f;
        canvas.scale(jSqX, jSqY);

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

        // Sinuous undulating eel movement
        float undulate = (float) Math.sin(animTime * 5.5 + e.animTimer) * 5.5f;
        float bob = (float) Math.sin(animTime * 2.0 + e.animTimer) * 3f;

        canvas.save();
        canvas.translate(0f, bob);
        canvas.rotate(undulate);

        if (bmpEel != null) {
            float s = 0.22f;
            canvas.scale(s, s);
            canvas.drawBitmap(bmpEel, -bmpEel.getWidth() / 2f, -bmpEel.getHeight() / 2f - 10f, null);
        } else {
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
        canvas.restore();
    }

    private void drawPaperAnglerfish(Canvas canvas, Game.Enemy e) {
        drawFloorShadow(canvas, 0f, 6f, 26f, 9f);

        float rot = (float) Math.sin(animTime * 3.5f + e.animTimer) * 6f;
        float bob = (float) Math.sin(animTime * 2.2f + e.animTimer) * 3f;

        canvas.save();
        canvas.translate(0f, bob);
        canvas.rotate(rot);

        boolean facingLeft = e.vx < 0f;
        if (facingLeft) {
            canvas.scale(-1f, 1f);
        }

        // Bioluminescent lantern glow in the deep sea!
        float glowPulse = 0.85f + (float) Math.sin(animTime * 6f + e.animTimer) * 0.25f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x3538BDF8);
        canvas.drawCircle(-e.radius * 0.65f, -e.radius * 0.75f, 28f * glowPulse, paintFill);
        paintFill.setColor(0x65FEF08A);
        canvas.drawCircle(-e.radius * 0.65f, -e.radius * 0.75f, 15f * glowPulse, paintFill);

        if (bmpAngler != null) {
            float s = 0.23f;
            canvas.scale(s, s);
            canvas.drawBitmap(bmpAngler, -bmpAngler.getWidth() / 2f, -bmpAngler.getHeight() / 2f, null);
        } else {
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF0F766E);
            canvas.drawCircle(0f, 0f, e.radius, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(3f);
            canvas.drawCircle(0f, 0f, e.radius, paintStroke);
        }
        canvas.restore();
    }

    private void drawPaperBossLeviathan(Canvas canvas, Game.Enemy e) {
        float sway = (float) Math.sin(animTime * 1.8) * 5f;
        float pulse = 1f + (float) Math.sin(animTime * 2.5) * 0.05f;
        drawFloorShadow(canvas, 0f, 6f, 65f, 18f);

        canvas.save();
        canvas.translate(0f, sway);
        canvas.scale(pulse, pulse);

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
        // Conchiglie counter (Golden Shell currency badge - Slot 110f .. 215f)
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_STICKER);
        canvas.drawRoundRect(108f, 10f, 217f, 46f, 8f, 8f, paintFill);
        paintFill.setColor(0xFF0F172A);
        canvas.drawRoundRect(110f, 12f, 215f, 44f, 7f, 7f, paintFill);

        if (bmpShell != null) {
            float s = 0.16f;
            canvas.save();
            canvas.translate(130f, 28f);
            canvas.scale(s, s);
            canvas.drawBitmap(bmpShell, -bmpShell.getWidth() / 2f, -bmpShell.getHeight() / 2f, null);
            canvas.restore();
        }

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(13f);
        paintText.setFakeBoldText(true);
        canvas.drawText(String.valueOf(game.shells), 150f, 33f, paintText);

        // Zone Title Banner (Centrato dinamicamente rispetto a screenW)
        float bannerHalfW = 140f;
        float bCenter = screenW / 2f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_STICKER);
        canvas.drawRoundRect(bCenter - bannerHalfW - 2f, 10f, bCenter + bannerHalfW + 2f, 46f, 8f, 8f, paintFill);

        paintFill.setColor(0xFF0F172A);
        canvas.drawRoundRect(bCenter - bannerHalfW, 12f, bCenter + bannerHalfW, 44f, 7f, 7f, paintFill);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(13.5f);
        paintText.setFakeBoldText(true);
        canvas.drawText(game.zoneName.toUpperCase() + "  -  " + game.depthMeters + "m",
                bCenter, 33f, paintText);

        // Sound Toggle Button (Slot 810f .. 860f)
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
        float cx = screenW / 2f;
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
    // UNIVERSAL STORYBOOK PARCHMENT & NAUTICAL CARD PRESENTATION
    // =========================================================================
    private void drawStorybookParchment(Canvas canvas, RectF r, String title, RectF closeBtn) {
        // 1. Soft Cardboard Drop Shadow
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x55000000);
        canvas.drawRoundRect(r.left + 5f, r.top + 7f, r.right + 5f, r.bottom + 7f, 16f, 16f, paintFill);

        // 2. Thick Die-Cut White Sticker Edge
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(8f);
        canvas.drawRoundRect(r, 16f, 16f, paintStroke);

        // 3. Comic Inking Contour
        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(3.5f);
        canvas.drawRoundRect(r, 16f, 16f, paintStroke);

        // 4. Warm Antique Storybook Parchment Background (100% solida e opaca, zero trasparenze indesiderate)
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFFEF3C7);
        canvas.drawRoundRect(r, 15f, 15f, paintFill);

        paintFill.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom,
                new int[]{0xFFFFFDF5, 0xFFFEF3C7, 0xFFFDE68A},
                new float[]{0f, 0.7f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(r, 15f, 15f, paintFill);
        paintFill.setShader(null);

        // 5. Paper Stitches / Dashed Border
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0x66B45309);
        paintStroke.setStrokeWidth(1.8f);
        for (float x = r.left + 24f; x < r.right - 24f; x += 16f) {
            canvas.drawLine(x, r.top + 10f, x + 8f, r.top + 10f, paintStroke);
            canvas.drawLine(x, r.bottom - 10f, x + 8f, r.bottom - 10f, paintStroke);
        }
        for (float y = r.top + 24f; y < r.bottom - 24f; y += 16f) {
            canvas.drawLine(r.left + 10f, y, r.left + 10f, y + 8f, paintStroke);
            canvas.drawLine(r.right - 10f, y, r.right - 10f, y + 8f, paintStroke);
        }

        // 6. Brass Corner Brackets & Rivets
        drawBrassCorner(canvas, r.left + 10f, r.top + 10f, 0f);
        drawBrassCorner(canvas, r.right - 10f, r.top + 10f, 90f);
        drawBrassCorner(canvas, r.right - 10f, r.bottom - 10f, 180f);
        drawBrassCorner(canvas, r.left + 10f, r.bottom - 10f, 270f);

        // 7. Carved Driftwood Title Banner
        if (title != null && !title.isEmpty()) {
            float bannerW = Math.min(r.width() - 140f, paintText.measureText(title) + 50f);
            float bx = r.left + 24f;
            float by = r.top + 16f;
            float bh = 34f;

            paintFill.setColor(0x33000000);
            canvas.drawRoundRect(bx + 2f, by + 3f, bx + bannerW + 2f, by + bh + 3f, 8f, 8f, paintFill);

            paintFill.setShader(new LinearGradient(bx, by, bx, by + bh,
                    new int[]{0xFF78350F, 0xFF451A03},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(bx, by, bx + bannerW, by + bh, 7f, 7f, paintFill);
            paintFill.setShader(null);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(2.5f);
            canvas.drawRoundRect(bx, by, bx + bannerW, by + bh, 7f, 7f, paintStroke);

            paintStroke.setColor(COL_GOLD);
            paintStroke.setStrokeWidth(1.2f);
            canvas.drawRoundRect(bx + 2f, by + 2f, bx + bannerW - 2f, by + bh - 2f, 5f, 5f, paintStroke);

            paintText.setTextAlign(Paint.Align.LEFT);
            paintText.setColor(COL_GOLD);
            paintText.setTextSize(15f);
            paintText.setFakeBoldText(true);
            canvas.drawText(title, bx + 16f, by + 22f, paintText);
        }

        // 8. Close Button (Wax Seal 'X')
        if (closeBtn != null) {
            drawCloseWaxSeal(canvas, closeBtn);
        }
    }

    private void drawCloseWaxSeal(Canvas canvas, RectF r) {
        float cx = r.centerX();
        float cy = r.centerY();
        float radius = Math.min(r.width(), r.height()) / 2f;

        // Cardboard shadow
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x55000000);
        canvas.drawCircle(cx + 3f, cy + 4f, radius, paintFill);

        // White die-cut sticker rim
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(5f);
        canvas.drawCircle(cx, cy, radius, paintStroke);

        // Deep crimson wax seal body
        paintFill.setColor(0xFFDC2626);
        canvas.drawCircle(cx, cy, radius, paintFill);

        // Inner ink rim
        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(2f);
        canvas.drawCircle(cx, cy, radius, paintStroke);

        // Inner gold highlight ring
        paintStroke.setColor(COL_GOLD);
        paintStroke.setStrokeWidth(1.2f);
        canvas.drawCircle(cx, cy, radius - 4f, paintStroke);

        // Bold white ✕
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_STICKER);
        paintText.setTextSize(radius * 1.15f);
        paintText.setFakeBoldText(true);
        canvas.drawText("✕", cx, cy + (radius * 0.4f), paintText);
    }

    private void drawBrassCorner(Canvas canvas, float cx, float cy, float rotation) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(rotation);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_GOLD);
        paintStroke.setStrokeWidth(3f);
        canvas.drawLine(0f, 0f, 16f, 0f, paintStroke);
        canvas.drawLine(0f, 0f, 0f, 16f, paintStroke);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_INK);
        canvas.drawCircle(4f, 4f, 2.5f, paintFill);
        paintFill.setColor(COL_GOLD);
        canvas.drawCircle(4f, 4f, 1.8f, paintFill);

        canvas.restore();
    }

    // =========================================================================
    // INVENTORY MODAL (OPENED EXPLORER CAPTAIN'S LOGBOOK)
    // =========================================================================
    private void drawInventoryModal(Canvas canvas) {
        // Dim overlay
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xB3020B14);
        canvas.drawRect(0f, 0f, screenW, screenH, paintFill);

        // Storybook Parchment Frame
        drawStorybookParchment(canvas, modalRect, "DIARIO DI BORDO & REPERTI", btnCloseModal);

        Game.Item selItem = (selectedBackpackIndex >= 0 && selectedBackpackIndex < game.backpack.size()) ?
                game.backpack.get(selectedBackpackIndex) : null;
        String targetedSlot = selItem != null ? selItem.slot : null;

        float invL = modalRect.left;
        float invR = modalRect.right;

        // Left Page: Equipped Gear in Carved Driftwood Slots
        drawEquippedSlot(canvas, slotEquipWeapon, "ARMA", game.equippedWeapon, "weapon".equals(targetedSlot));
        drawEquippedSlot(canvas, slotEquipArmor, "CORAZZA", game.equippedArmor, "armor".equals(targetedSlot));
        drawEquippedSlot(canvas, slotEquipRelic, "RELIQUIA", game.equippedRelic, "relic".equals(targetedSlot));

        // Active Set Bonuses scroll on warm parchment
        float setBoxY = 320f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFFEF3C7);
        canvas.drawRoundRect(invL + 30f, setBoxY, invL + 230f, setBoxY + 115f, 8f, 8f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFFD97706);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(invL + 30f, setBoxY, invL + 230f, setBoxY + 115f, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(10f);
        paintText.setFakeBoldText(true);
        paintText.setColor(0xFF78350F);
        canvas.drawText("BONUS SET (2+ PEZZI):", invL + 40f, setBoxY + 18f, paintText);

        drawSetBonusLine(canvas, invL + 40f, setBoxY + 38f, "Set Corallo", "+35 PV, +5 Dif", game.isSetBonusActive("Corallo"));
        drawSetBonusLine(canvas, invL + 40f, setBoxY + 62f, "Set Marea", "+30 Marea, -20% Cd", game.isSetBonusActive("Marea"));
        drawSetBonusLine(canvas, invL + 40f, setBoxY + 86f, "Set Abisso", "+22% Dmg, +12% Crit", game.isSetBonusActive("Abisso"));

        // Right Page: Backpack 8-Slot Grid Header
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(0xFF78350F);
        paintText.setTextSize(12.5f);
        paintText.setFakeBoldText(true);
        canvas.drawText("ZAINO (8 SLOT) — TOCCA PER CONFRONTARE", invL + 260f, 100f, paintText);

        for (int i = 0; i < 8; i++) {
            RectF slot = backpackSlots[i];
            boolean isSelected = (i == selectedBackpackIndex);

            // Slot Background (warm oak plank)
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(isSelected ? 0xFFFEF08A : 0xFFFFFBEB);
            canvas.drawRoundRect(slot, 6f, 6f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(isSelected ? 0xFFD97706 : 0xFFCBD5E1);
            paintStroke.setStrokeWidth(isSelected ? 3f : 1.5f);
            canvas.drawRoundRect(slot, 6f, 6f, paintStroke);

            if (i < game.backpack.size()) {
                Game.Item it = game.backpack.get(i);
                int rColor = game.getRarityColor(it.rarity);

                // Slot category pill badge
                int pillBg = "weapon".equals(it.slot) ? 0x330284C7 : ("armor".equals(it.slot) ? 0x3316A34A : 0x339333EA);
                paintFill.setColor(pillBg);
                canvas.drawRoundRect(slot.left + 4f, slot.top + 4f, slot.right - 4f, slot.top + 18f, 3f, 3f, paintFill);

                paintText.setTextAlign(Paint.Align.CENTER);
                paintText.setTextSize(9f);
                paintText.setFakeBoldText(true);
                paintText.setColor(rColor);
                canvas.drawText(it.getSlotTitle(), slot.centerX(), slot.top + 14f, paintText);

                paintText.setTextSize(9f);
                paintText.setColor(COL_INK);
                paintText.setFakeBoldText(false);
                String sn = it.name.length() > 14 ? it.name.substring(0, 13) + ".." : it.name;
                canvas.drawText(sn, slot.centerX(), slot.centerY() + 8f, paintText);

                if (it.setName != null) {
                    paintText.setTextSize(8f);
                    paintText.setColor(0xFFB45309);
                    paintText.setFakeBoldText(true);
                    canvas.drawText(it.setName, slot.centerX(), slot.bottom - 4f, paintText);
                }
            }
        }

        // Right Page Bottom: Comparison & Detail Scroll
        RectF compRect = new RectF(invL + 260f, 260f, invR - 30f, 440f);
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFFEF3C7);
        canvas.drawRoundRect(compRect, 8f, 8f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(selItem != null ? game.getRarityColor(selItem.rarity) : 0xFFD97706);
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
            paintText.setColor(0xFF0284C7);
            paintText.setFakeBoldText(false);
            canvas.drawText("CATEGORIA: " + selItem.getCategoryName(), compRect.left + 14f, compRect.top + 38f, paintText);

            paintText.setColor(selItem.setName != null ? 0xFFB45309 : 0xFF64748B);
            canvas.drawText("SET: " + (selItem.setName != null ? "Set " + selItem.setName : "Nessun Set"), compRect.left + 260f, compRect.top + 38f, paintText);

            // Divider line
            paintStroke.setColor(0x33000000);
            paintStroke.setStrokeWidth(1f);
            canvas.drawLine(compRect.left + 14f, compRect.top + 45f, compRect.right - 14f, compRect.top + 45f, paintStroke);

            // Comparison Header
            paintText.setTextSize(11f);
            paintText.setColor(0xFF78350F);
            paintText.setFakeBoldText(true);
            canvas.drawText("CONFRONTO CON: " + (eq != null ? eq.name + " (Equipaggiato)" : "Slot Vuoto"), compRect.left + 14f, compRect.top + 62f, paintText);

            // Stat Comparison with Deltas
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
            paintFill.setColor(0xFF16A34A);
            canvas.drawRoundRect(btnEquipSelected, 6f, 6f, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(1.5f);
            canvas.drawRoundRect(btnEquipSelected, 6f, 6f, paintStroke);

            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setColor(COL_STICKER);
            paintText.setTextSize(12f);
            paintText.setFakeBoldText(true);
            canvas.drawText("EQUIPAGGIA", btnEquipSelected.centerX(), btnEquipSelected.centerY() + 4f, paintText);

            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFFDC2626);
            canvas.drawRoundRect(btnDropSelected, 6f, 6f, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(1.5f);
            canvas.drawRoundRect(btnDropSelected, 6f, 6f, paintStroke);

            canvas.drawText("SCARTA", btnDropSelected.centerX(), btnDropSelected.centerY() + 4f, paintText);
        } else {
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(12f);
            paintText.setColor(0xFF78350F);
            paintText.setFakeBoldText(false);
            canvas.drawText("Tocca un oggetto nello zaino per esaminarne categoria e statistiche", compRect.centerX(), compRect.centerY() - 10f, paintText);
            canvas.drawText("e visualizzare il confronto automatico con l'equipaggiamento attivo.", compRect.centerX(), compRect.centerY() + 12f, paintText);
        }

        // Overall stats banner at bottom
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(0xFF78350F);
        paintText.setTextSize(13f);
        paintText.setFakeBoldText(true);
        String fullStats = "STATISTICHE:   DANNO: " + game.getAttackDamage() +
                "    DIFESA: " + game.getDefense() +
                "    SALUTE: " + game.getMaxHp() +
                "    VELOCITA: " + (int)game.getMoveSpeed() +
                "    CRIT: " + (int)(game.getCritChance() * 100) + "%";
        canvas.drawText(fullStats, modalRect.left + 30f, 470f, paintText);
    }

    private void drawEquippedSlot(Canvas canvas, RectF r, String slotTitle, Game.Item item, boolean isTargeted) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(isTargeted ? 0xFFFEF08A : 0xFFFFFBEB);
        canvas.drawRoundRect(r, 6f, 6f, paintFill);

        int rColor = item != null ? game.getRarityColor(item.rarity) : 0xFF94A3B8;
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(isTargeted ? 0xFFD97706 : rColor);
        paintStroke.setStrokeWidth(isTargeted ? 3f : 2f);
        canvas.drawRoundRect(r, 6f, 6f, paintStroke);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(10f);
        paintText.setColor(isTargeted ? 0xFFB45309 : 0xFF78350F);
        canvas.drawText(slotTitle + (isTargeted ? " (SOSTITUISCE)" : ""), r.left + 12f, r.top + 18f, paintText);

        if (item != null) {
            paintText.setTextSize(13f);
            paintText.setFakeBoldText(true);
            paintText.setColor(rColor);
            canvas.drawText(item.name, r.left + 12f, r.top + 36f, paintText);
            if (item.setName != null) {
                paintText.setTextSize(9f);
                paintText.setColor(0xFFB45309);
                canvas.drawText("[Set " + item.setName + "]", r.right - 70f, r.top + 18f, paintText);
            }
        } else {
            paintText.setTextSize(12f);
            paintText.setColor(0xFF94A3B8);
            canvas.drawText("VUOTO", r.left + 12f, r.top + 36f, paintText);
        }
    }

    private void drawSetBonusLine(Canvas canvas, float x, float y, String name, String bonus, boolean active) {
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(10f);
        paintText.setColor(active ? 0xFF16A34A : 0xFF94A3B8);
        paintText.setFakeBoldText(active);
        canvas.drawText(name + ": " + bonus + (active ? " [ATTIVO]" : " (2pz)"), x, y, paintText);
    }

    private void drawDiabloStatRow(Canvas canvas, float x, float y, String statName, String val, int delta) {
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(COL_INK);
        canvas.drawText(statName + " " + val, x, y, paintText);

        float nameW = paintText.measureText(statName + " " + val);
        if (delta > 0) {
            paintText.setColor(0xFF16A34A); // Green improvement
            canvas.drawText("  (▲ MIGLIORE +" + delta + ")", x + nameW, y, paintText);
        } else if (delta < 0) {
            paintText.setColor(0xFFDC2626); // Red downgrade
            canvas.drawText("  (▼ INFERIORE " + delta + ")", x + nameW, y, paintText);
        } else {
            paintText.setColor(0xFF64748B);
            canvas.drawText("  (= IDENTICO)", x + nameW, y, paintText);
        }
    }

    private void drawGameOverOverlay(Canvas canvas) {
        float cx = screenW / 2f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xEE020617);
        canvas.drawRect(0f, 0f, screenW, screenH, paintFill);

        float boxW = Math.min(screenW - 60f, 660f);
        RectF goBox = new RectF(cx - boxW / 2f, 50f, cx + boxW / 2f, 490f);
        drawStorybookParchment(canvas, goBox, "SPEDIZIONE CONCLUSA", null);

        // Titolo Sconfitta ad alto contrasto
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(0xFFDC2626);
        paintText.setTextSize(26f);
        paintText.setFakeBoldText(true);
        canvas.drawText("INGHIOTTITO DAGLI ABISSI", cx, goBox.top + 80f, paintText);

        // Testo descrittivo in inchiostro scuro ultra-leggibile su pergamena chiara
        paintText.setColor(COL_INK);
        paintText.setTextSize(12.5f);
        paintText.setFakeBoldText(false);
        canvas.drawText("La pressione oceanica ha avuto la meglio sul tuo scafandro.", cx, goBox.top + 112f, paintText);
        canvas.drawText("Le tue conchiglie e i tesori raccolti giacciono nel fondale marino.", cx, goBox.top + 134f, paintText);

        // Cartellino riepilogo spedizione
        float cardW = boxW - 60f;
        float cardL = cx - cardW / 2f, cardT = goBox.top + 160f, cardR = cx + cardW / 2f, cardB = goBox.top + 270f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFFFFBEB);
        canvas.drawRoundRect(cardL, cardT, cardR, cardB, 8f, 8f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFFD97706);
        paintStroke.setStrokeWidth(1.8f);
        canvas.drawRoundRect(cardL, cardT, cardR, cardB, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(12f);
        paintText.setColor(0xFF78350F);
        paintText.setFakeBoldText(true);
        canvas.drawText("RIEPILOGO DELLA DISCESA", cx, cardT + 26f, paintText);

        paintText.setTextSize(12.5f);
        paintText.setColor(COL_INK);
        paintText.setFakeBoldText(false);
        canvas.drawText("Profondità Raggiunta: " + game.depthMeters + "m  •  Record Storico: " + game.maxDepthRecord + "m", cx, cardT + 48f, paintText);
        canvas.drawText("Conchiglie Raccolte: " + game.shells + "  •  Banca Totale: " + game.totalShellsBank, cx, cardT + 70f, paintText);
        canvas.drawText("Immersioni Affrontate: " + game.totalDivesCount + "  •  Leviatani Battuti: " + game.bossesDefeatedCount, cx, cardT + 92f, paintText);

        // Tasto Riavvio Verde Smeraldo
        btnRestart.set(cx - 150f, goBox.bottom - 65f, cx + 150f, goBox.bottom - 18f);
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF16A34A);
        canvas.drawRoundRect(btnRestart, 8f, 8f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawRoundRect(btnRestart, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_STICKER);
        paintText.setTextSize(15f);
        paintText.setFakeBoldText(true);
        canvas.drawText("↺ RITENTA L'IMMERSIONE", btnRestart.centerX(), btnRestart.centerY() + 5.5f, paintText);
    }

    private void drawVictoryOverlay(Canvas canvas) {
        float cx = screenW / 2f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xEE020617);
        canvas.drawRect(0f, 0f, screenW, screenH, paintFill);

        float boxW = Math.min(screenW - 60f, 660f);
        RectF vicBox = new RectF(cx - boxW / 2f, 50f, cx + boxW / 2f, 490f);
        drawStorybookParchment(canvas, vicBox, "VITTORIA LEGGENDARIA", null);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(0xFFD97706);
        paintText.setTextSize(26f);
        paintText.setFakeBoldText(true);
        canvas.drawText("DOMINATORE DEGLI ABISSI!", cx, vicBox.top + 80f, paintText);

        paintText.setColor(COL_INK);
        paintText.setTextSize(12.5f);
        paintText.setFakeBoldText(false);
        canvas.drawText("Hai sconfitto il Leviatano e conquistato i segreti del fondale!", cx, vicBox.top + 112f, paintText);
        canvas.drawText("La tua leggenda è incisa sulla Carta d'Abisso.", cx, vicBox.top + 134f, paintText);

        // Cartellino trionfo
        float cardW = boxW - 60f;
        float cardL = cx - cardW / 2f, cardT = vicBox.top + 160f, cardR = cx + cardW / 2f, cardB = vicBox.top + 270f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFFFFBEB);
        canvas.drawRoundRect(cardL, cardT, cardR, cardB, 8f, 8f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFFD97706);
        paintStroke.setStrokeWidth(1.8f);
        canvas.drawRoundRect(cardL, cardT, cardR, cardB, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(12f);
        paintText.setColor(0xFF78350F);
        paintText.setFakeBoldText(true);
        canvas.drawText("IMPRESA DEGLI ABISSI COMPLETATA", cx, cardT + 26f, paintText);

        paintText.setTextSize(12.5f);
        paintText.setColor(COL_INK);
        paintText.setFakeBoldText(false);
        canvas.drawText("Profondità Finale: " + game.depthMeters + "m  •  Record Storico: " + game.maxDepthRecord + "m", cx, cardT + 48f, paintText);
        canvas.drawText("Conchiglie Salve: " + game.shells + "  •  Banca Totale: " + game.totalShellsBank, cx, cardT + 70f, paintText);
        canvas.drawText("Spedizioni Concluse: " + game.totalDivesCount + "  •  Leviatani Sconfitti: " + game.bossesDefeatedCount, cx, cardT + 92f, paintText);

        float btnW = 210f;
        btnRestart.set(cx - btnW - 12f, vicBox.bottom - 65f, cx - 12f, vicBox.bottom - 18f);
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF16A34A);
        canvas.drawRoundRect(btnRestart, 8f, 8f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawRoundRect(btnRestart, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_STICKER);
        paintText.setTextSize(13f);
        paintText.setFakeBoldText(true);
        canvas.drawText("NUOVA SPEDIZIONE", btnRestart.centerX(), btnRestart.centerY() + 5f, paintText);

        btnEndless.set(cx + 12f, vicBox.bottom - 65f, cx + btnW + 12f, vicBox.bottom - 18f);
        paintFill.setColor(0xFF0284C7);
        canvas.drawRoundRect(btnEndless, 8f, 8f, paintFill);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawRoundRect(btnEndless, 8f, 8f, paintStroke);

        paintText.setColor(COL_STICKER);
        paintText.setTextSize(13f);
        paintText.setFakeBoldText(true);
        canvas.drawText("⚡ ABISSO SENZA FINE", btnEndless.centerX(), btnEndless.centerY() + 5f, paintText);
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
        float cx = screenW / 2f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xDE020B14);
        canvas.drawRect(0f, 0f, screenW, screenH, paintFill);

        // Storybook Nautical Chart Parchment (Solida, opaca e die-cut - nessun titolo conflittuale)
        drawStorybookParchment(canvas, modalSkillTreeRect, "", null);

        // Header Class Tabs (Hunter, Juggernaut, Mystic) - coordinate pulite non sovrapposte
        drawClassTab(canvas, tabHunter, "🏹 CACCIATORE", Game.CLASS_HUNTER.equals(game.playerClass), 0xFF0284C7);
        drawClassTab(canvas, tabJuggernaut, "🛡 CUSTODE", Game.CLASS_JUGGERNAUT.equals(game.playerClass), 0xFFD97706);
        drawClassTab(canvas, tabMystic, "🔮 MISTICO", Game.CLASS_MYSTIC.equals(game.playerClass), 0xFF7E22CE);

        // Skill points pill at top center (Golden wax seal badge)
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(game.skillPoints > 0 ? 0xFF16A34A : 0xFF78350F);
        canvas.drawRoundRect(spPillRect, 6f, 6f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(spPillRect, 6f, 6f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(10.5f);
        paintText.setFakeBoldText(true);
        paintText.setColor(COL_STICKER);
        canvas.drawText("⭐ PUNTI: " + game.skillPoints, spPillRect.centerX(), spPillRect.centerY() + 4f, paintText);

        // Respec button (Wax seal style)
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFDC2626);
        canvas.drawRoundRect(btnRespec, 6f, 6f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(btnRespec, 6f, 6f, paintStroke);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_STICKER);
        paintText.setTextSize(10.5f);
        paintText.setFakeBoldText(true);
        canvas.drawText("↺ RESET (RESPEC)", btnRespec.centerX(), btnRespec.centerY() + 4f, paintText);

        // Tasto di Chiusura 'X' (Sigillo in ceralacca rossa prominente)
        drawCloseWaxSeal(canvas, btnCloseSkillTree);

        // Subheader: Branch Titles in Dedicated Decorative Banners (sotto le tab, zero collisioni!)
        String b1Title = Game.CLASS_HUNTER.equals(game.playerClass) ? "RAMO 1: BALISTICA PESANTE"
                : (Game.CLASS_JUGGERNAUT.equals(game.playerClass) ? "RAMO 1: BASTIONE DI CORALLO" : "RAMO 1: SIGNORE DELLE TEMPESTE");
        String b2Title = Game.CLASS_HUNTER.equals(game.playerClass) ? "RAMO 2: AGILITÀ IDRODINAMICA"
                : (Game.CLASS_JUGGERNAUT.equals(game.playerClass) ? "RAMO 2: MAGLIO SISMICO" : "RAMO 2: DRENAGGIO OSCURO");

        float b1CenterX = cx - 210f;
        float b2CenterX = cx + 210f;
        float bannerHalfW = 125f;

        // Ramo 1 Banner (Sinistra)
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x220284C7);
        canvas.drawRoundRect(b1CenterX - bannerHalfW, 68f, b1CenterX + bannerHalfW, 94f, 6f, 6f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFF0284C7);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(b1CenterX - bannerHalfW, 68f, b1CenterX + bannerHalfW, 94f, 6f, 6f, paintStroke);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(10.5f);
        paintText.setFakeBoldText(true);
        paintText.setColor(0xFF0369A1);
        canvas.drawText(b1Title, b1CenterX, 85f, paintText);

        // Ramo 2 Banner (Destra)
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0x22D97706);
        canvas.drawRoundRect(b2CenterX - bannerHalfW, 68f, b2CenterX + bannerHalfW, 94f, 6f, 6f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(0xFFD97706);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(b2CenterX - bannerHalfW, 68f, b2CenterX + bannerHalfW, 94f, 6f, 6f, paintStroke);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(10.5f);
        paintText.setFakeBoldText(true);
        paintText.setColor(0xFFB45309);
        canvas.drawText(b2Title, b2CenterX, 85f, paintText);

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
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(9f);
        canvas.drawLine(x1, y1, x2, y2, paintStroke);

        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(6f);
        canvas.drawLine(x1, y1, x2, y2, paintStroke);

        paintStroke.setColor(active ? 0xFFF59E0B : 0xFFCBD5E1);
        paintStroke.setStrokeWidth(active ? 3.5f : 2f);
        canvas.drawLine(x1, y1, x2, y2, paintStroke);
    }

    private void drawTreeRootNode(Canvas canvas, float cx, float cy, float r) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_STICKER);
        canvas.drawCircle(cx, cy, r + 3f, paintFill);

        paintFill.setColor(0xFF78350F);
        canvas.drawCircle(cx, cy, r, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_INK);
        paintStroke.setStrokeWidth(2f);
        canvas.drawCircle(cx, cy, r, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(9f);
        paintText.setFakeBoldText(true);
        canvas.drawText("RADICE", cx, cy - 2f, paintText);
        paintText.setColor(COL_STICKER);
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
            float pulse = 0.88f + (float) Math.sin(animTime * 6.0) * 0.12f;
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xCCF59E0B);
            paintStroke.setStrokeWidth(6.5f);
            canvas.drawCircle(cx, cy, (r + 8f) * pulse, paintStroke);
        }

        // Sticker Die-cut Border
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_STICKER);
        canvas.drawCircle(cx, cy, r + 4f, paintFill);

        // Background
        int nodeBg = unlocked ? 0xFF16A34A : (canUnlock ? 0xFF0284C7 : 0xFFF1F5F9);
        paintFill.setColor(nodeBg);
        canvas.drawCircle(cx, cy, r, paintFill);

        // Inner Ink contour
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(isSelected ? COL_GOLD : COL_INK);
        paintStroke.setStrokeWidth(isSelected ? 3.5f : (unlocked ? 2.5f : 1.8f));
        canvas.drawCircle(cx, cy, r, paintStroke);

        // Badge short label inside circle
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(9f);
        paintText.setFakeBoldText(true);
        paintText.setColor(unlocked || canUnlock ? COL_STICKER : 0xFF475569);
        canvas.drawText(s.badge, cx, cy + 3.5f, paintText);

        // Pinned Tier Pill at top-right of medallion (Zero vertical text collisions!)
        float tagX = cx + r - 2f;
        float tagY = cy - r + 2f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(COL_STICKER);
        canvas.drawCircle(tagX, tagY, 10f, paintFill);
        paintFill.setColor(unlocked ? 0xFF15803D : (canUnlock ? 0xFF0369A1 : 0xFF64748B));
        canvas.drawCircle(tagX, tagY, 8f, paintFill);
        paintText.setTextSize(8f);
        paintText.setColor(COL_STICKER);
        paintText.setFakeBoldText(true);
        canvas.drawText("T" + s.tier, tagX, tagY + 3f, paintText);

        // Unlocked Checkmark pill at bottom-right
        if (unlocked) {
            float chkX = cx + r - 2f;
            float chkY = cy + r - 2f;
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(COL_STICKER);
            canvas.drawCircle(chkX, chkY, 9f, paintFill);
            paintFill.setColor(0xFF16A34A);
            canvas.drawCircle(chkX, chkY, 7f, paintFill);
            paintText.setTextSize(8f);
            paintText.setColor(COL_STICKER);
            canvas.drawText("✓", chkX, chkY + 3f, paintText);
        }
    }

    private void drawSkillInspectionBar(Canvas canvas, Game.Skill s) {
        boolean unlocked = game.hasSkill(s.id);
        boolean canUnlock = game.canUpgradeSkill(s.id);

        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFFFFBEB);
        canvas.drawRoundRect(skillInspectRect, 8f, 8f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(3f);
        canvas.drawRoundRect(skillInspectRect, 8f, 8f, paintStroke);

        paintStroke.setColor(unlocked ? 0xFF16A34A : (canUnlock ? 0xFF0284C7 : 0xFFD97706));
        paintStroke.setStrokeWidth(1.8f);
        canvas.drawRoundRect(skillInspectRect, 8f, 8f, paintStroke);

        // Titolo e Grado
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(14f);
        paintText.setFakeBoldText(true);
        paintText.setColor(0xFF78350F);
        canvas.drawText(s.name, skillInspectRect.left + 16f, skillInspectRect.top + 26f, paintText);

        paintText.setTextSize(10f);
        paintText.setColor(0xFFB45309);
        paintText.setFakeBoldText(false);
        String subHeader = "Ramo " + s.branch + " • Tier " + s.tier + " • Grado: " + (unlocked ? "1/1 (ATTIVO)" : "0/1 (NON APPRESO)");
        canvas.drawText(subHeader, skillInspectRect.left + 16f, skillInspectRect.top + 42f, paintText);

        float bPillW = paintText.measureText(s.badge) + 16f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(s.color);
        canvas.drawRoundRect(skillInspectRect.left + 16f, skillInspectRect.top + 52f, skillInspectRect.left + 16f + bPillW, skillInspectRect.top + 72f, 4f, 4f, paintFill);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_STICKER);
        paintText.setTextSize(9.5f);
        paintText.setFakeBoldText(true);
        canvas.drawText(s.badge, skillInspectRect.left + 16f + bPillW / 2f, skillInspectRect.top + 66f, paintText);

        // Centro: Descrizione
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setTextSize(10.5f);
        paintText.setColor(COL_INK);
        paintText.setFakeBoldText(false);
        float descMaxW = btnUpgradeSelectedSkill.left - (skillInspectRect.left + 230f) - 20f;
        drawWrappedText(canvas, s.desc, skillInspectRect.left + 230f, skillInspectRect.top + 28f, 15f, Math.max(260f, descMaxW), paintText);

        if (s.requiredSkillId != null && !game.hasSkill(s.requiredSkillId)) {
            Game.Skill reqSkill = game.getSkillById(s.requiredSkillId);
            String reqName = reqSkill != null ? reqSkill.name : "Tier " + (s.tier - 1);
            paintText.setColor(0xFFDC2626);
            paintText.setTextSize(9.5f);
            canvas.drawText("⚠ Richiede prima: " + reqName, skillInspectRect.left + 230f, skillInspectRect.bottom - 16f, paintText);
        }

        // Destra: Tasto Azione Grande
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(unlocked ? 0xFF16A34A : (canUnlock ? 0xFF0284C7 : 0xFFE2E8F0));
        canvas.drawRoundRect(btnUpgradeSelectedSkill, 8f, 8f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(btnUpgradeSelectedSkill, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(unlocked || canUnlock ? COL_STICKER : 0xFF64748B);
        paintText.setTextSize(12f);
        paintText.setFakeBoldText(true);

        if (unlocked) {
            canvas.drawText("TALENTO ATTIVO ✓", btnUpgradeSelectedSkill.centerX(), btnUpgradeSelectedSkill.centerY() + 4.5f, paintText);
        } else if (canUnlock) {
            canvas.drawText("+ SBLOCCA TALENTO", btnUpgradeSelectedSkill.centerX(), btnUpgradeSelectedSkill.centerY() - 4f, paintText);
            paintText.setTextSize(10f);
            canvas.drawText("(Costo: 1 Punto)", btnUpgradeSelectedSkill.centerX(), btnUpgradeSelectedSkill.centerY() + 14f, paintText);
        } else {
            canvas.drawText("BLOCCATO", btnUpgradeSelectedSkill.centerX(), btnUpgradeSelectedSkill.centerY() - 4f, paintText);
            paintText.setTextSize(9.5f);
            String reason = game.skillPoints <= 0 ? "Punti esauriti" : "Richiede Tier prec.";
            canvas.drawText(reason, btnUpgradeSelectedSkill.centerX(), btnUpgradeSelectedSkill.centerY() + 13f, paintText);
        }
    }

    private void drawClassTab(Canvas canvas, RectF r, String title, boolean active, int accentCol) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(active ? accentCol : 0xFFFEF3C7);
        canvas.drawRoundRect(r, 6f, 6f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(active ? COL_STICKER : 0xFFD97706);
        paintStroke.setStrokeWidth(active ? 2.5f : 1.2f);
        canvas.drawRoundRect(r, 6f, 6f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(active ? COL_STICKER : 0xFF78350F);
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
        float cx = screenW / 2f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xB3020B14);
        canvas.drawRect(0f, 0f, screenW, screenH, paintFill);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_GOLD);
        paintText.setTextSize(22f);
        paintText.setFakeBoldText(true);
        canvas.drawText("LIVELLO " + game.level + "! SCEGLI UN TALENTO ABISSALE", cx, 50f, paintText);

        paintText.setTextSize(12f);
        paintText.setColor(COL_CREAM);
        paintText.setFakeBoldText(false);
        canvas.drawText("Tocca una carta per sbloccare la tua nuova abilità permanente", cx, 75f, paintText);

        List<Game.Perk> choices = game.pendingPerkChoices;
        for (int i = 0; i < choices.size() && i < perkCardRects.length; i++) {
            Game.Perk p = choices.get(i);
            RectF r = perkCardRects[i];

            // Card Shadow
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0x44000000);
            canvas.drawRoundRect(r.left + 4f, r.top + 6f, r.right + 4f, r.bottom + 6f, 14f, 14f, paintFill);

            // Card White Die-Cut Sticker
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(7f);
            canvas.drawRoundRect(r, 14f, 14f, paintStroke);

            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(3f);
            canvas.drawRoundRect(r, 14f, 14f, paintStroke);

            // Card Warm Cream Paper Sheet
            paintFill.setColor(0xFFFFFBEB);
            canvas.drawRoundRect(r, 12f, 12f, paintFill);

            // Top Badge Pill
            paintFill.setColor(p.color);
            canvas.drawRoundRect(r.left + 20f, r.top + 16f, r.right - 20f, r.top + 46f, 8f, 8f, paintFill);
            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(1.5f);
            canvas.drawRoundRect(r.left + 20f, r.top + 16f, r.right - 20f, r.top + 46f, 8f, 8f, paintStroke);

            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setColor(COL_STICKER);
            paintText.setTextSize(11f);
            paintText.setFakeBoldText(true);
            canvas.drawText(p.badge, r.centerX(), r.top + 35f, paintText);

            paintText.setColor(0xFF78350F);
            paintText.setTextSize(15f);
            canvas.drawText(p.title, r.centerX(), r.top + 78f, paintText);

            paintStroke.setColor(0xFFD97706);
            paintStroke.setStrokeWidth(1.2f);
            canvas.drawLine(r.left + 24f, r.top + 95f, r.right - 24f, r.top + 95f, paintStroke);

            paintText.setColor(COL_INK);
            paintText.setTextSize(11f);
            paintText.setFakeBoldText(false);
            drawWrappedCardText(canvas, p.desc, r.centerX(), r.top + 130f, r.width() - 32f);

            float btnY = r.bottom - 50f;
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFF16A34A);
            canvas.drawRoundRect(r.left + 25f, btnY, r.right - 25f, btnY + 34f, 8f, 8f, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_STICKER);
            paintStroke.setStrokeWidth(2f);
            canvas.drawRoundRect(r.left + 25f, btnY, r.right - 25f, btnY + 34f, 8f, 8f, paintStroke);

            paintText.setColor(COL_STICKER);
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

    private void drawMerchantAwning(Canvas canvas, RectF r) {
        float awningH = 36f;
        float stripeW = 32f;
        int stripeIndex = 0;

        for (float x = r.left + 12f; x < r.right - 12f; x += stripeW) {
            float sx2 = Math.min(x + stripeW, r.right - 12f);
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor((stripeIndex % 2 == 0) ? 0xFFEF4444 : 0xFFFFFBEB);

            tempPath.reset();
            tempPath.moveTo(x, r.top + 6f);
            tempPath.lineTo(sx2, r.top + 6f);
            tempPath.lineTo(sx2, r.top + 6f + awningH);
            tempPath.quadTo((x + sx2) / 2f, r.top + 6f + awningH + 8f, x, r.top + 6f + awningH);
            tempPath.close();
            canvas.drawPath(tempPath, paintFill);

            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(COL_INK);
            paintStroke.setStrokeWidth(1.4f);
            canvas.drawPath(tempPath, paintStroke);

            stripeIndex++;
        }
    }

    private void drawMerchantShopModal(Canvas canvas) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xDE020B14);
        canvas.drawRect(0f, 0f, screenW, screenH, paintFill);

        // Storybook Parchment Frame (senza close button qui per disegnarlo SOPRA la tenda!)
        drawStorybookParchment(canvas, shopModalRect, "BOTTEGA DI MASTRO PAGURO", null);

        // Striped Nautical Awning Canopy
        drawMerchantAwning(canvas, shopModalRect);

        // Sigillo di chiusura 'X' disegnato SOPRA la tenda per massima visibilità e accessibilità!
        drawCloseWaxSeal(canvas, btnCloseShop);

        // Tabs
        drawClassTab(canvas, shopTabProvviste, "PROVVISTE SPEDIZIONE", shopTab == 0, 0xFF16A34A);
        drawClassTab(canvas, shopTabAltare, "ALTARE DEGLI ABISSI", shopTab == 1, 0xFFD97706);

        // Conchiglie counter pill
        paintText.setTextAlign(Paint.Align.RIGHT);
        paintText.setTextSize(13f);
        paintText.setColor(0xFF78350F);
        paintText.setFakeBoldText(true);
        String shellStr = "Conchiglie: " + game.shells + (game.totalShellsBank > 0 ? " (Banca: " + game.totalShellsBank + ")" : "");
        canvas.drawText(shellStr, shopModalRect.right - 85f, shopModalRect.top + 34f, paintText);
        if (bmpShell != null) {
            tempRect.set(shopModalRect.right - 78f, shopModalRect.top + 16f, shopModalRect.right - 50f, shopModalRect.top + 44f);
            canvas.drawBitmap(bmpShell, null, tempRect, null);
        }

        // Friendly Paguro Portrait & Greeting Bubble
        if (bmpMerchant != null) {
            float s = 0.22f;
            canvas.save();
            canvas.translate(shopModalRect.left + 50f, shopModalRect.top + 124f);
            canvas.scale(s, s);
            canvas.drawBitmap(bmpMerchant, -bmpMerchant.getWidth() / 2f, -bmpMerchant.getHeight() / 2f, null);
            canvas.restore();

            // Speech bubble
            paintFill.setStyle(Paint.Style.FILL);
            paintFill.setColor(0xFFFFFBEB);
            canvas.drawRoundRect(shopModalRect.left + 95f, shopModalRect.top + 106f, shopModalRect.right - 30f, shopModalRect.top + 144f, 8f, 8f, paintFill);
            paintStroke.setStyle(Paint.Style.STROKE);
            paintStroke.setColor(0xFFD97706);
            paintStroke.setStrokeWidth(1.5f);
            canvas.drawRoundRect(shopModalRect.left + 95f, shopModalRect.top + 106f, shopModalRect.right - 30f, shopModalRect.top + 144f, 8f, 8f, paintStroke);

            paintText.setTextAlign(Paint.Align.LEFT);
            paintText.setColor(0xFF78350F);
            paintText.setTextSize(10.5f);
            paintText.setFakeBoldText(false);
            if (shopTab == 0) {
                canvas.drawText("«Rifornimenti rapidi per la spedizione attuale. Pozioni, benedizioni e forzieri!»", shopModalRect.left + 105f, shopModalRect.top + 128f, paintText);
            } else {
                canvas.drawText("«L'Altare Ancestrale risuona con le conchiglie salvate. Forgia poteri permanenti!»", shopModalRect.left + 105f, shopModalRect.top + 128f, paintText);
            }
        }

        if (shopTab == 0) {
            drawShopItem(canvas, shopSlotPotion, "Fiala di Cura Rinforzata", "+1 Pozione di cura istantanea (ripristina tutti i PV)", 15,
                    game.shells >= 15 && game.potions < game.maxPotions, 0xFF16A34A, "ACQUISTA");

            drawShopItem(canvas, shopSlotBlessing, "Benedizione delle Maree", "Ripristina al 100% la Marea e guarisce +30 PV", 25,
                    game.shells >= 25, 0xFF0284C7, "ACQUISTA");

            drawShopItem(canvas, shopSlotCoffer, "Scrigno Misterioso dei Fondali", "Genera un equipaggiamento Raro o Abissale per lo zaino", 55,
                    game.shells >= 55 && game.backpack.size() < 8, 0xFF9333EA, "ACQUISTA");
        } else {
            int costVit = game.getMetaPerkCost("vitality");
            drawShopItem(canvas, metaSlotVitality, "Vitalità Sommersa (+25 PV Base) — Livello " + game.metaVitalityLevel + "/5",
                    "Aumenta permanentemente la salute massima per tutte le future spedizioni", costVit,
                    game.canUpgradeMetaPerk("vitality"), 0xFF16A34A, "POTENZIA");

            int costTide = game.getMetaPerkCost("tide");
            drawShopItem(canvas, metaSlotTide, "Flusso della Marea (+20 Marea Base) — Livello " + game.metaTideLevel + "/5",
                    "Aumenta permanentemente la riserva di energia vitale per le abilità marine", costTide,
                    game.canUpgradeMetaPerk("tide"), 0xFF0284C7, "POTENZIA");

            int costPot = game.getMetaPerkCost("potion");
            drawShopItem(canvas, metaSlotPotion, "Scorta di Pozioni (+1 Fiala Iniziale) — Livello " + game.metaPotionLevel + "/2",
                    "Inizia ogni spedizione negli abissi con una fiala di cura aggiuntiva", costPot,
                    game.canUpgradeMetaPerk("potion"), 0xFFF59E0B, "POTENZIA");

            int costMag = game.getMetaPerkCost("magnet");
            drawShopItem(canvas, metaSlotMagnet, "Calamita Abissale (+35% Raggio Raccolta) — Livello " + game.metaMagnetLevel + "/3",
                    "Attira magneticamente le conchiglie e i tesori da maggiore distanza", costMag,
                    game.canUpgradeMetaPerk("magnet"), 0xFF9333EA, "POTENZIA");
        }

        // Tasto di uscita chiaro in basso
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF7F1D1D);
        canvas.drawRoundRect(btnShopExit, 8f, 8f, paintFill);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(2.5f);
        canvas.drawRoundRect(btnShopExit, 8f, 8f, paintStroke);
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_STICKER);
        paintText.setTextSize(12f);
        paintText.setFakeBoldText(true);
        canvas.drawText("✕ ESCI DALLA BOTTEGA", btnShopExit.centerX(), btnShopExit.centerY() + 4.5f, paintText);
    }

    private void drawShopItem(Canvas canvas, RectF r, String title, String desc, int cost, boolean canBuy, int accentCol, String actionLabel) {
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFFFEF3C7);
        canvas.drawRoundRect(r, 8f, 8f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(canBuy ? accentCol : 0xFFCBD5E1);
        paintStroke.setStrokeWidth(1.8f);
        canvas.drawRoundRect(r, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.setColor(canBuy ? 0xFF78350F : 0xFF94A3B8);
        paintText.setTextSize(12.5f);
        paintText.setFakeBoldText(true);
        canvas.drawText(title, r.left + 16f, r.top + 26f, paintText);

        paintText.setTextSize(10.5f);
        paintText.setColor(COL_INK);
        paintText.setFakeBoldText(false);
        canvas.drawText(desc, r.left + 16f, r.top + 48f, paintText);

        float btnW = 145f;
        float btnH = 34f;
        float bx = r.right - btnW - 16f;
        float by = r.centerY() - btnH / 2f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(canBuy ? 0xFF16A34A : 0xFFCBD5E1);
        canvas.drawRoundRect(bx, by, bx + btnW, by + btnH, 8f, 8f, paintFill);

        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(1.5f);
        canvas.drawRoundRect(bx, by, bx + btnW, by + btnH, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(canBuy ? COL_STICKER : 0xFF64748B);
        paintText.setTextSize(11f);
        paintText.setFakeBoldText(true);
        String label = (cost >= 999) ? "MASSIMO" : (actionLabel + " (" + cost + ")");
        canvas.drawText(label, bx + btnW / 2f, by + 21f, paintText);
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
        if (btnCloseShop.contains(vx, vy) || btnShopExit.contains(vx, vy) || !shopModalRect.contains(vx, vy)) {
            game.merchantShopOpen = false;
            AudioHelper.playClick();
            return;
        }

        if (shopTabProvviste.contains(vx, vy)) {
            shopTab = 0;
            AudioHelper.playClick();
            return;
        }

        if (shopTabAltare.contains(vx, vy)) {
            shopTab = 1;
            AudioHelper.playClick();
            return;
        }

        if (shopTab == 0) {
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
        } else {
            if (metaSlotVitality.contains(vx, vy)) {
                game.upgradeMetaPerk("vitality");
                return;
            }
            if (metaSlotTide.contains(vx, vy)) {
                game.upgradeMetaPerk("tide");
                return;
            }
            if (metaSlotPotion.contains(vx, vy)) {
                game.upgradeMetaPerk("potion");
                return;
            }
            if (metaSlotMagnet.contains(vx, vy)) {
                game.upgradeMetaPerk("magnet");
                return;
            }
        }
    }

    private void drawPauseOverlay(Canvas canvas) {
        float cx = screenW / 2f;
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xCC020617);
        canvas.drawRect(0f, 0f, screenW, screenH, paintFill);

        RectF pBox = new RectF(cx - 220f, 150f, cx + 220f, 390f);
        drawStorybookParchment(canvas, pBox, "IMMERSIONE IN PAUSA", null);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(0xFF78350F);
        paintText.setTextSize(22f);
        paintText.setFakeBoldText(true);
        canvas.drawText("PAUSA DI BORDO", cx, 235f, paintText);

        paintText.setColor(COL_INK);
        paintText.setTextSize(13f);
        paintText.setFakeBoldText(false);
        canvas.drawText("Il tempo si ferma nelle correnti sottomarine...", cx, 275f, paintText);

        // Resume pill
        paintFill.setStyle(Paint.Style.FILL);
        paintFill.setColor(0xFF16A34A);
        canvas.drawRoundRect(cx - 110f, 315f, cx + 110f, 355f, 8f, 8f, paintFill);

        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setColor(COL_STICKER);
        paintStroke.setStrokeWidth(2f);
        canvas.drawRoundRect(cx - 110f, 315f, cx + 110f, 355f, 8f, 8f, paintStroke);

        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setColor(COL_STICKER);
        paintText.setTextSize(14f);
        paintText.setFakeBoldText(true);
        canvas.drawText("TOCCA PER RIPRENDERE", cx, 340f, paintText);
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
                if (btnRestart.contains(vx, vy)) {
                    restartGame();
                } else if (game.victory && btnEndless.contains(vx, vy)) {
                    startEndlessDive();
                }
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
        if (btnCloseModal.contains(vx, vy) || !modalRect.contains(vx, vy)) {
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
        if (btnCloseSkillTree.contains(vx, vy) || !modalSkillTreeRect.contains(vx, vy)) {
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
