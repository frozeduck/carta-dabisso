#!/usr/bin/env python3
import math
import pathlib
from PIL import Image, ImageDraw, ImageFilter

out_dir = pathlib.Path(__file__).resolve().parent.parent / 'android/app/src/main/res/drawable'
out_dir.mkdir(parents=True, exist_ok=True)

COL_WHITE = (255, 255, 255, 255)
COL_INK = (24, 24, 27, 255)
COL_GOLD = (251, 191, 36, 255)
COL_CORAL = (248, 113, 113, 255)
COL_MINT = (74, 222, 128, 255)
COL_AQUA = (56, 189, 248, 255)
COL_CANVAS = (253, 224, 71, 255)
COL_BOOT = (120, 53, 15, 255)

def apply_smooth_sticker(im_hi, stroke_width=12, target_size=(256, 256)):
    """Applies anti-aliased, rounded Paper Mario white die-cut sticker outline and downsamples with Lanczos."""
    alpha = im_hi.split()[-1]
    w, h = im_hi.size

    # Dilation for sticker margin
    dilated = alpha.filter(ImageFilter.MaxFilter(stroke_width * 2 + 1))
    # Gaussian blur to round sharp/jagged boxy corners
    dilated = dilated.filter(ImageFilter.GaussianBlur(radius=stroke_width * 0.42))
    # Sharpen edge while keeping subpixel anti-aliasing gradient
    dilated = dilated.point(lambda p: 255 if p > 72 else int((p / 72.0) ** 1.6 * 255))

    border = Image.new('RGBA', (w, h), COL_WHITE)
    border.putalpha(dilated)

    composited = Image.alpha_composite(border, im_hi)
    return composited.resize(target_size, Image.Resampling.LANCZOS)

def gen_palombaro():
    w, h = 512, 512
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Oxygen tanks on back (scaled 2x)
    draw.rounded_rectangle([90, 180, 170, 350], radius=32, fill=(52, 211, 153, 255), outline=COL_INK, width=8)
    draw.rounded_rectangle([104, 210, 156, 236], radius=8, fill=(16, 185, 129, 255), outline=COL_INK, width=6)
    draw.ellipse([116, 160, 144, 188], fill=COL_GOLD, outline=COL_INK, width=6)

    # Heavy diver boots
    draw.rounded_rectangle([140, 380, 230, 460], radius=20, fill=COL_BOOT, outline=COL_INK, width=8)
    draw.rounded_rectangle([270, 380, 360, 460], radius=20, fill=(146, 64, 14, 255), outline=COL_INK, width=8)

    # Canvas diving suit body
    draw.rounded_rectangle([150, 230, 350, 400], radius=48, fill=COL_CANVAS, outline=COL_INK, width=10)
    # Chest harness & brass badge
    draw.line([150, 300, 350, 310], fill=COL_INK, width=10)
    draw.ellipse([230, 284, 270, 324], fill=COL_GOLD, outline=COL_INK, width=8)
    draw.ellipse([244, 298, 256, 310], fill=COL_WHITE)

    # Harpoon weapon in hand
    draw.line([330, 370, 430, 120], fill=(120, 53, 15, 255), width=14)
    draw.line([330, 370, 430, 120], fill=COL_INK, width=6)
    # Harpoon head
    draw.polygon([(430, 120), (400, 156), (456, 140)], fill=(241, 245, 249, 255), outline=COL_INK)
    draw.ellipse([424, 108, 444, 128], fill=COL_WHITE)

    # Big round Paper Mario brass diving helmet
    draw.ellipse([140, 70, 360, 290], fill=(245, 158, 11, 255), outline=COL_INK, width=12)

    # Rivets around helmet
    for angle in [math.pi * 0.8, math.pi, math.pi * 1.2, math.pi * 1.4, math.pi * 1.6]:
        rx = 250 + math.cos(angle) * 100
        ry = 180 + math.sin(angle) * 100
        draw.ellipse([rx - 8, ry - 8, rx + 8, ry + 8], fill=COL_GOLD, outline=COL_INK, width=4)

    # Porthole visor bezel
    draw.ellipse([184, 114, 316, 246], fill=(30, 41, 59, 255), outline=COL_INK, width=10)
    draw.ellipse([196, 126, 304, 234], fill=(56, 189, 248, 255), outline=COL_INK, width=6)

    # Crossbars
    draw.line([196, 180, 304, 180], fill=COL_GOLD, width=8)
    draw.line([250, 126, 250, 234], fill=COL_GOLD, width=8)

    # Glare highlights
    draw.polygon([(208, 152), (232, 148), (216, 192), (196, 192)], fill=COL_WHITE)
    draw.polygon([(272, 148), (288, 146), (280, 172), (264, 172)], fill=COL_WHITE)

    sticker = apply_smooth_sticker(im, stroke_width=14, target_size=(256, 256))
    sticker.save(out_dir / 'palombaro.png')
    print('Generato palombaro.png (supersampled Lanczos)')

def gen_granchio():
    w, h = 512, 512
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Jointed legs
    for side in [-1, 1]:
        cx = 256 + side * 80
        draw.line([cx, 320, cx + side * 80, 340], fill=COL_INK, width=16)
        draw.line([cx + side * 80, 340, cx + side * 120, 410], fill=COL_INK, width=16)
        draw.line([cx, 320, cx + side * 80, 340], fill=COL_CORAL, width=8)
        draw.line([cx + side * 80, 340, cx + side * 120, 410], fill=COL_CORAL, width=8)

    # Snip-snip claws
    for side in [-1, 1]:
        cx = 256 + side * 130
        draw.line([256 + side * 90, 280, cx, 220], fill=COL_INK, width=16)
        draw.line([256 + side * 90, 280, cx, 220], fill=COL_CORAL, width=8)
        pts = [(cx, 220), (cx + side * 50, 160), (cx + side * 70, 230), (cx + side * 30, 250)]
        draw.polygon(pts, fill=(239, 68, 68, 255), outline=COL_INK)
        draw.line([cx + side * 30, 190, cx + side * 60, 190], fill=COL_INK, width=8)

    # Carapace (Round Paper Mario crab body)
    draw.ellipse([130, 240, 382, 390], fill=COL_CORAL, outline=COL_INK, width=12)
    draw.ellipse([160, 270, 352, 350], fill=(252, 165, 165, 255))

    # Goofy Cartoon Eyes with white eyeballs & black pupils
    for side in [-1, 1]:
        ex = 256 + side * 56
        draw.line([ex, 250, ex, 180], fill=COL_INK, width=12)
        draw.ellipse([ex - 36, 130, ex + 36, 202], fill=COL_WHITE, outline=COL_INK, width=10)
        draw.ellipse([ex - 16, 150, ex + 16, 182], fill=COL_INK)
        draw.ellipse([ex - 8, 154, ex + 4, 166], fill=COL_WHITE)

    # Crab smile
    draw.arc([220, 310, 292, 350], start=0, end=180, fill=COL_INK, width=8)

    sticker = apply_smooth_sticker(im, stroke_width=14, target_size=(256, 256))
    sticker.save(out_dir / 'granchio.png')
    print('Generato granchio.png (supersampled Lanczos)')

def gen_medusa():
    w, h = 512, 512
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Crepe-paper fluttering tentacles
    for i in range(5):
        tx = 160 + i * 48
        points = [(tx, 290), (tx + 16, 350), (tx - 16, 410), (tx + 10, 470)]
        draw.line(points, fill=COL_INK, width=16)
        draw.line(points, fill=(186, 230, 253, 255), width=8)

    # Scalloped Paper Mario Blooper/Jelly Bell
    draw.ellipse([110, 120, 402, 310], fill=COL_AQUA, outline=COL_INK, width=12)
    draw.ellipse([140, 150, 372, 270], fill=(125, 211, 252, 255))

    # Cute cartoon eyes
    for side in [-1, 1]:
        ex = 256 + side * 48
        draw.ellipse([ex - 24, 200, ex + 24, 252], fill=COL_WHITE, outline=COL_INK, width=8)
        draw.ellipse([ex - 10, 214, ex + 10, 238], fill=COL_INK)
        draw.ellipse([ex - 4, 218, ex + 4, 226], fill=COL_WHITE)

    sticker = apply_smooth_sticker(im, stroke_width=14, target_size=(256, 256))
    sticker.save(out_dir / 'medusa.png')
    print('Generato medusa.png (supersampled Lanczos)')

def gen_boss_leviatano():
    w, h = 640, 640
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # 7 Layered Cardboard Tentacles
    for i in range(7):
        tx = 320 + (i - 3) * 56
        pts = [(tx, 340), (tx + 24, 440), (tx - 20, 540), (tx + int(math.sin(i) * 30), 610)]
        draw.line(pts, fill=COL_INK, width=28)
        draw.line(pts, fill=(52, 211, 153, 255) if i % 2 == 0 else (16, 185, 129, 255), width=16)
        draw.ellipse([pts[1][0] - 16, pts[1][1] - 16, pts[1][0] + 16, pts[1][1] + 16], fill=COL_GOLD, outline=COL_INK, width=6)

    # Boss Head
    draw.ellipse([120, 100, 520, 390], fill=COL_GOLD, outline=COL_INK, width=14)
    draw.ellipse([160, 130, 480, 350], fill=(245, 158, 11, 255))

    # Ancient spiral shell carving
    for a in range(25):
        rad = a * 0.25
        r = 20 + a * 7.0
        sx = 270 + math.cos(rad) * r
        sy = 240 + math.sin(rad) * r
        draw.point((sx, sy), fill=COL_INK)

    # Giant Cyclopean Cartoon Boss Eye
    draw.ellipse([320, 170, 460, 310], fill=COL_WHITE, outline=COL_INK, width=12)
    draw.ellipse([350, 196, 436, 284], fill=(249, 115, 22, 255), outline=COL_INK, width=6)
    draw.ellipse([380, 210, 410, 270], fill=COL_INK)
    draw.ellipse([360, 204, 376, 220], fill=COL_WHITE)

    # Spiky Crest Fin
    draw.polygon([(220, 110), (260, 30), (300, 104), (340, 36), (380, 108)], fill=COL_CORAL, outline=COL_INK, width=8)

    sticker = apply_smooth_sticker(im, stroke_width=16, target_size=(320, 320))
    sticker.save(out_dir / 'boss_leviatano.png')
    print('Generato boss_leviatano.png (supersampled Lanczos)')

def gen_coralli():
    # Coral 1: Pink Staghorn (rendered at 360x360 -> downsampled to 180x180)
    im1 = Image.new('RGBA', (360, 360), (0, 0, 0, 0))
    d1 = ImageDraw.Draw(im1)
    pts1 = [(180, 320), (140, 220), (80, 180), (70, 110), (110, 120), (150, 170),
            (170, 60), (210, 50), (210, 150), (250, 110), (290, 120), (260, 190), (210, 320)]
    d1.polygon(pts1, fill=COL_CORAL, outline=COL_INK, width=8)
    d1.ellipse([140, 140, 160, 160], fill=COL_WHITE)
    d1.ellipse([230, 150, 250, 170], fill=COL_WHITE)
    d1.ellipse([184, 90, 200, 106], fill=COL_WHITE)
    s1 = apply_smooth_sticker(im1, stroke_width=12, target_size=(180, 180))
    s1.save(out_dir / 'corallo_rosso.png')

    # Coral 2: Mint Sea Fan (rendered at 360x360 -> downsampled to 180x180)
    im2 = Image.new('RGBA', (360, 360), (0, 0, 0, 0))
    d2 = ImageDraw.Draw(im2)
    pts2 = [(180, 320), (130, 240), (90, 160), (120, 90), (160, 140), (180, 60),
            (210, 60), (230, 130), (270, 90), (300, 150), (260, 230), (210, 320)]
    d2.polygon(pts2, fill=COL_MINT, outline=COL_INK, width=8)
    d2.line([180, 320, 194, 90], fill=(34, 197, 94, 255), width=6)
    s2 = apply_smooth_sticker(im2, stroke_width=12, target_size=(180, 180))
    s2.save(out_dir / 'corallo_verde.png')
    print('Generati coralli PNG (supersampled Lanczos)')

def gen_conchiglia():
    # Golden spiral shell (currency drop & HUD icon)
    w, h = 256, 256
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    pts = [(128, 210), (60, 160), (45, 110), (65, 60), (128, 45), (191, 60), (211, 110), (196, 160)]
    draw.polygon(pts, fill=COL_GOLD, outline=COL_INK, width=10)
    for angle_deg in [70, 90, 110]:
        rad = math.radians(angle_deg)
        tx = 128 + int(math.cos(rad) * 75)
        ty = 130 - int(math.sin(rad) * 75)
        draw.line([128, 205, tx, ty], fill=(217, 119, 6, 255), width=6)
    draw.ellipse([145, 70, 175, 100], fill=COL_WHITE)
    draw.ellipse([100, 140, 115, 155], fill=COL_WHITE)

    sticker = apply_smooth_sticker(im, stroke_width=12, target_size=(128, 128))
    sticker.save(out_dir / 'conchiglia.png')
    print('Generata conchiglia.png (supersampled Lanczos)')

def gen_paguro_bottegaio():
    # Mastro Paguro - Merchant Hermit Crab
    w, h = 512, 512
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Big Spiral Shell on back (swirl of amber and gold)
    draw.ellipse([180, 110, 430, 360], fill=(217, 119, 6, 255), outline=COL_INK, width=14)
    draw.ellipse([210, 140, 400, 330], fill=COL_GOLD, outline=COL_INK, width=10)
    draw.ellipse([250, 180, 360, 290], fill=(253, 230, 138, 255), outline=COL_INK, width=8)

    # Merchant pack strapped on top of shell with ropes
    draw.line([240, 140, 370, 330], fill=COL_BOOT, width=8)
    draw.rounded_rectangle([290, 80, 390, 160], radius=16, fill=(52, 211, 153, 255), outline=COL_INK, width=10)
    draw.line([290, 120, 390, 120], fill=COL_GOLD, width=6)

    # Crab legs & body popping out of shell
    for i in range(3):
        lx = 140 - i * 20
        ly = 320 + i * 35
        draw.line([190, 310, lx, ly], fill=COL_INK, width=14)
        draw.line([190, 310, lx, ly], fill=COL_CORAL, width=8)

    # Body
    draw.ellipse([110, 240, 250, 370], fill=COL_CORAL, outline=COL_INK, width=12)

    # Little waving merchant claw
    draw.line([130, 320, 70, 270], fill=COL_INK, width=14)
    draw.line([130, 320, 70, 270], fill=COL_CORAL, width=8)
    draw.ellipse([45, 230, 85, 280], fill=(239, 68, 68, 255), outline=COL_INK, width=8)

    # Cute merchant face with wire spectacles!
    draw.ellipse([130, 190, 175, 240], fill=COL_WHITE, outline=COL_INK, width=8)
    draw.ellipse([175, 190, 220, 240], fill=COL_WHITE, outline=COL_INK, width=8)
    draw.ellipse([130, 190, 175, 240], fill=None, outline=COL_GOLD, width=6)
    draw.ellipse([175, 190, 220, 240], fill=None, outline=COL_GOLD, width=6)
    draw.line([175, 215, 180, 215], fill=COL_GOLD, width=6)
    draw.ellipse([145, 205, 160, 225], fill=COL_INK)
    draw.ellipse([190, 205, 205, 225], fill=COL_INK)
    draw.arc([150, 280, 200, 320], start=0, end=180, fill=COL_INK, width=6)

    sticker = apply_smooth_sticker(im, stroke_width=14, target_size=(256, 256))
    sticker.save(out_dir / 'paguro_bottegaio.png')
    print('Generato paguro_bottegaio.png (supersampled Lanczos)')

def gen_murena():
    # Abyssal Moray Eel (Murena dei Fondali)
    w, h = 512, 512
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Dorsal fin along serpentine back
    fin_pts = [(100, 280), (140, 200), (220, 160), (320, 150), (410, 210), (460, 290), (410, 260), (320, 180), (220, 190), (140, 230), (100, 280)]
    draw.polygon(fin_pts, fill=(52, 211, 153, 255), outline=COL_INK)

    # Sinuous moray eel body with accordion curves
    body_pts = [(80, 310), (140, 240), (240, 210), (350, 230), (440, 300),
                (420, 370), (330, 310), (220, 290), (140, 330), (70, 370)]
    draw.polygon(body_pts, fill=(147, 51, 234, 255), outline=COL_INK)

    # Golden belly stripe
    belly_pts = [(100, 340), (160, 290), (250, 270), (340, 285), (410, 340),
                 (380, 360), (310, 310), (220, 300), (140, 330)]
    draw.polygon(belly_pts, fill=COL_GOLD)

    # Round Head
    draw.ellipse([50, 270, 180, 390], fill=(168, 85, 247, 255), outline=COL_INK, width=12)

    # Wide grinning open mouth
    draw.polygon([(60, 340), (120, 340), (100, 380)], fill=(225, 29, 72, 255), outline=COL_INK)
    # Cute sharp triangle fangs
    draw.polygon([(70, 340), (76, 354), (82, 340)], fill=COL_WHITE)
    draw.polygon([(86, 340), (92, 354), (98, 340)], fill=COL_WHITE)
    draw.polygon([(78, 374), (84, 360), (90, 374)], fill=COL_WHITE)

    # Big googly cartoon eyes with shiny pupils
    draw.ellipse([90, 275, 140, 325], fill=COL_WHITE, outline=COL_INK, width=8)
    draw.ellipse([100, 285, 125, 310], fill=COL_INK)
    draw.ellipse([104, 288, 114, 298], fill=COL_WHITE)

    # Cute rosy cheek
    draw.ellipse([135, 330, 155, 350], fill=(244, 114, 182, 200))

    sticker = apply_smooth_sticker(im, stroke_width=14, target_size=(256, 256))
    sticker.save(out_dir / 'murena.png')
    print('Generato murena.png (supersampled Lanczos)')

def gen_stella_marina():
    # Cute cartoon starfish for seabed decoration
    w, h = 256, 256
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # 5-pointed star
    pts = []
    cx, cy = 128, 128
    for i in range(10):
        rad = math.radians(i * 36 - 90)
        r = 100 if i % 2 == 0 else 46
        pts.append((cx + int(math.cos(rad) * r), cy + int(math.sin(rad) * r)))
    draw.polygon(pts, fill=(251, 146, 60, 255), outline=COL_INK, width=8)

    # Cute polka dots on arms
    for arm in range(5):
        rad = math.radians(arm * 72 - 90)
        dot_x = cx + int(math.cos(rad) * 65)
        dot_y = cy + int(math.sin(rad) * 65)
        draw.ellipse([dot_x - 8, dot_y - 8, dot_x + 8, dot_y + 8], fill=COL_GOLD)

    # Starfish happy eyes
    draw.ellipse([110, 112, 124, 128], fill=COL_WHITE, outline=COL_INK, width=4)
    draw.ellipse([132, 112, 146, 128], fill=COL_WHITE, outline=COL_INK, width=4)
    draw.ellipse([115, 117, 122, 125], fill=COL_INK)
    draw.ellipse([137, 117, 144, 125], fill=COL_INK)
    # Smile
    draw.arc([118, 128, 138, 142], start=0, end=180, fill=COL_INK, width=4)

    sticker = apply_smooth_sticker(im, stroke_width=10, target_size=(128, 128))
    sticker.save(out_dir / 'stella_marina.png')
    print('Generata stella_marina.png (supersampled Lanczos)')

def gen_anemone():
    # Charming Paper Mario style soft sea anemone cluster
    w, h = 360, 360
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Rounded sandy base rock
    draw.ellipse([90, 270, 270, 335], fill=(217, 119, 6, 255), outline=COL_INK, width=7)
    draw.ellipse([110, 280, 250, 325], fill=(251, 191, 36, 255))

    # Stitches on rock
    for sx in range(120, 240, 16):
        draw.line([(sx, 305), (sx + 8, 305)], fill=COL_INK, width=3)

    # Layered petal-like tentacles (back layer then front layer)
    tentacle_colors = [
        (244, 114, 182, 255), # Coral Pink
        (192, 132, 252, 255), # Lavender
        (56, 189, 248, 255),  # Cyan
        (74, 222, 128, 255),  # Mint
        (251, 146, 60, 255)   # Peach
    ]

    # Back petals
    for i, angle in enumerate([-45, -25, 0, 25, 45]):
        rad = math.radians(angle)
        base_x = 180 + math.sin(rad) * 40
        tip_x = 180 + math.sin(rad) * 110
        tip_y = 190 - math.cos(rad) * 85
        col = tentacle_colors[i % len(tentacle_colors)]
        # Petal polygon
        pts = [(base_x - 12, 280), (tip_x - 14, tip_y + 15), (tip_x, tip_y), (tip_x + 14, tip_y + 15), (base_x + 12, 280)]
        draw.polygon(pts, fill=col, outline=COL_INK)
        draw.ellipse([tip_x - 8, tip_y - 8, tip_x + 8, tip_y + 8], fill=COL_WHITE, outline=COL_INK, width=3)

    # Front petals
    for i, angle in enumerate([-35, -15, 15, 35]):
        rad = math.radians(angle)
        base_x = 180 + math.sin(rad) * 35
        tip_x = 180 + math.sin(rad) * 95
        tip_y = 215 - math.cos(rad) * 75
        col = tentacle_colors[(i + 2) % len(tentacle_colors)]
        pts = [(base_x - 10, 285), (tip_x - 12, tip_y + 12), (tip_x, tip_y), (tip_x + 12, tip_y + 12), (base_x + 10, 285)]
        draw.polygon(pts, fill=col, outline=COL_INK)
        draw.ellipse([tip_x - 6, tip_y - 6, tip_x + 6, tip_y + 6], fill=COL_GOLD, outline=COL_INK, width=3)

    sticker = apply_smooth_sticker(im, stroke_width=12, target_size=(180, 180))
    sticker.save(out_dir / 'anemone.png')
    print('Generato anemone.png (supersampled Lanczos)')

def gen_pesce_lanterna():
    # Cartoon Paper Mario Anglerfish / Pesce Lanterna degli Abissi
    w, h = 512, 512
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Glowing lantern light aura (concentric translucent circles)
    aura_cx, aura_cy = 130, 110
    draw.ellipse([aura_cx - 70, aura_cy - 70, aura_cx + 70, aura_cy + 70], fill=(56, 189, 248, 60))
    draw.ellipse([aura_cx - 45, aura_cy - 45, aura_cx + 45, aura_cy + 45], fill=(254, 240, 138, 110))
    draw.ellipse([aura_cx - 24, aura_cy - 24, aura_cx + 24, aura_cy + 24], fill=(253, 224, 71, 255), outline=COL_INK, width=6)
    draw.ellipse([aura_cx - 10, aura_cy - 10, aura_cx + 4, aura_cy + 4], fill=COL_WHITE)

    # Curved stalk/esca from head
    stalk_pts = [(240, 210), (220, 150), (170, 120), (134, 120), (130, 134), (160, 136), (206, 166), (226, 220)]
    draw.polygon(stalk_pts, fill=(15, 118, 110, 255), outline=COL_INK)

    # Caudal fin (tail fin) - Paper fan shape
    tail_pts = [(400, 280), (475, 210), (455, 280), (485, 350), (400, 310)]
    draw.polygon(tail_pts, fill=(13, 148, 136, 255), outline=COL_INK)

    # Dorsal fin
    dorsal_pts = [(280, 195), (340, 140), (370, 205)]
    draw.polygon(dorsal_pts, fill=(20, 184, 166, 255), outline=COL_INK)

    # Main rounded body
    body_box = [150, 180, 420, 400]
    draw.ellipse(body_box, fill=(19, 78, 74, 255), outline=COL_INK, width=12)

    # Belly patch (lighter teal)
    belly_box = [180, 270, 380, 390]
    draw.ellipse(belly_box, fill=(45, 212, 191, 220))

    # Pectoral side fin
    pec_pts = [(260, 290), (310, 310), (275, 345)]
    draw.polygon(pec_pts, fill=(94, 234, 212, 255), outline=COL_INK)

    # Massive predatory underbite grin
    mouth_pts = [(160, 290), (260, 280), (250, 360), (170, 375)]
    draw.polygon(mouth_pts, fill=(225, 29, 72, 255), outline=COL_INK, width=8)

    # Sharp triangle teeth
    for tx in [180, 205, 230]:
        draw.polygon([(tx, 282), (tx + 8, 304), (tx + 16, 282)], fill=COL_WHITE, outline=COL_INK)
    for bx in [170, 195, 220, 240]:
        draw.polygon([(bx, 370), (bx + 8, 340), (bx + 16, 370)], fill=COL_WHITE, outline=COL_INK)

    # Big round cartoon eye
    draw.ellipse([200, 205, 265, 270], fill=COL_WHITE, outline=COL_INK, width=8)
    draw.ellipse([210, 215, 245, 250], fill=(245, 158, 11, 255))
    draw.ellipse([218, 223, 238, 243], fill=COL_INK)
    draw.ellipse([222, 225, 228, 231], fill=COL_WHITE)

    # Rosy sticker cheek
    draw.ellipse([250, 260, 280, 285], fill=(251, 146, 60, 180))

    sticker = apply_smooth_sticker(im, stroke_width=14, target_size=(256, 256))
    sticker.save(out_dir / 'pesce_lanterna.png')
    print('Generato pesce_lanterna.png (supersampled Lanczos)')

def gen_fabbro_astice():
    # Mastro Astice: Cartoon Lobster Blacksmith
    w, h = 512, 512
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Anvil on right
    anvil_pts = [(300, 360), (460, 360), (440, 460), (320, 460)]
    draw.polygon(anvil_pts, fill=(71, 85, 105, 255), outline=COL_INK, width=8)
    draw.polygon([(280, 340), (480, 340), (460, 370), (300, 370)], fill=(148, 163, 184, 255), outline=COL_INK, width=8)

    # Sparks above anvil
    for spx, spy in [(360, 310), (400, 290), (430, 320), (380, 270)]:
        draw.polygon([(spx, spy - 15), (spx + 5, spy - 5), (spx + 15, spy), (spx + 5, spy + 5), (spx, spy + 15), (spx - 5, spy + 5), (spx - 15, spy), (spx - 5, spy - 5)], fill=COL_GOLD, outline=COL_INK)

    # Lobster body (red/orange)
    draw.ellipse([140, 200, 320, 420], fill=(225, 29, 72, 255), outline=COL_INK, width=12)
    # Apron (leather brown)
    draw.polygon([(170, 260), (290, 260), (310, 410), (150, 410)], fill=(180, 83, 9, 255), outline=COL_INK, width=8)
    draw.line([(200, 200), (200, 260)], fill=COL_INK, width=8)
    draw.line([(260, 200), (260, 260)], fill=COL_INK, width=8)

    # Left claw resting
    draw.ellipse([60, 240, 160, 340], fill=(244, 63, 94, 255), outline=COL_INK, width=10)
    # Right arm holding iron hammer raised over anvil
    draw.line([(280, 250), (380, 200)], fill=(225, 29, 72, 255), width=24)
    # Hammer handle & head
    draw.line([(360, 240), (420, 140)], fill=(120, 53, 15, 255), width=14)
    draw.polygon([(390, 120), (450, 150), (430, 180), (370, 150)], fill=(100, 116, 139, 255), outline=COL_INK, width=6)

    # Head & big friendly eyes
    draw.ellipse([170, 150, 290, 250], fill=(225, 29, 72, 255), outline=COL_INK, width=10)
    draw.ellipse([190, 140, 225, 185], fill=COL_WHITE, outline=COL_INK, width=6)
    draw.ellipse([235, 140, 270, 185], fill=COL_WHITE, outline=COL_INK, width=6)
    draw.ellipse([200, 150, 220, 175], fill=COL_INK)
    draw.ellipse([245, 150, 265, 175], fill=COL_INK)
    draw.arc([205, 195, 255, 235], start=0, end=180, fill=COL_INK, width=6)

    # Antennae
    draw.line([(200, 150), (140, 70)], fill=(225, 29, 72, 255), width=8)
    draw.line([(260, 150), (320, 70)], fill=(225, 29, 72, 255), width=8)

    sticker = apply_smooth_sticker(im, stroke_width=14, target_size=(256, 256))
    sticker.save(out_dir / 'fabbro_astice.png')
    print('Generato fabbro_astice.png (supersampled Lanczos)')

def gen_cassa_tesoro():
    # Shared Expedition Stash Chest
    w, h = 512, 512
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Chest Base
    draw.rounded_rectangle([90, 220, 422, 420], radius=16, fill=(146, 64, 14, 255), outline=COL_INK, width=12)
    # Brass bands
    draw.rectangle([140, 220, 180, 420], fill=COL_GOLD, outline=COL_INK, width=6)
    draw.rectangle([332, 220, 372, 420], fill=COL_GOLD, outline=COL_INK, width=6)
    # Lock plate & keyhole
    draw.ellipse([236, 260, 276, 300], fill=COL_GOLD, outline=COL_INK, width=6)
    draw.ellipse([250, 272, 262, 284], fill=COL_INK)
    draw.polygon([(252, 280), (260, 280), (258, 292), (254, 292)], fill=COL_INK)

    # Open/Arched Lid with treasure glow
    draw.polygon([(70, 220), (100, 130), (412, 130), (442, 220)], fill=(180, 83, 9, 255), outline=COL_INK, width=12)
    draw.polygon([(140, 130), (180, 130), (180, 220), (140, 220)], fill=COL_GOLD, outline=COL_INK, width=6)
    draw.polygon([(332, 130), (372, 130), (372, 220), (332, 220)], fill=COL_GOLD, outline=COL_INK, width=6)

    # Glowing gems / pearls spilling over edge
    draw.ellipse([200, 200, 240, 240], fill=(56, 189, 248, 255), outline=COL_INK, width=6)
    draw.ellipse([260, 190, 310, 235], fill=(234, 179, 8, 255), outline=COL_INK, width=6)
    draw.ellipse([320, 205, 355, 240], fill=(168, 85, 247, 255), outline=COL_INK, width=6)
    # Sparkles
    for spx, spy in [(170, 170), (250, 150), (360, 160)]:
        draw.polygon([(spx, spy - 12), (spx + 4, spy - 4), (spx + 12, spy), (spx + 4, spy + 4), (spx, spy + 12), (spx - 4, spy + 4), (spx - 12, spy), (spx - 4, spy - 4)], fill=COL_WHITE)

    sticker = apply_smooth_sticker(im, stroke_width=14, target_size=(256, 256))
    sticker.save(out_dir / 'cassa_tesoro.png')
    print('Generata cassa_tesoro.png (supersampled Lanczos)')

def gen_portale_citta():
    # Diablo Town Portal Rune
    w, h = 512, 512
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    cx, cy = 256, 256
    draw.ellipse([70, 70, 442, 442], fill=(2, 132, 199, 120), outline=COL_INK, width=14)
    draw.ellipse([110, 110, 402, 402], fill=(56, 189, 248, 180), outline=COL_GOLD, width=10)
    draw.ellipse([160, 160, 352, 352], fill=(186, 230, 253, 255), outline=COL_WHITE, width=8)
    draw.ellipse([210, 210, 302, 302], fill=COL_WHITE)

    for arm in range(4):
        angle_offset = arm * (math.pi / 2.0)
        pts = []
        for step in range(25):
            t = step / 24.0
            rad = angle_offset + t * 4.0
            r = 30 + t * 150
            pts.append((cx + int(math.cos(rad) * r), cy + int(math.sin(rad) * r)))
        draw.line(pts, fill=COL_WHITE, width=8)

    sticker = apply_smooth_sticker(im, stroke_width=14, target_size=(256, 256))
    sticker.save(out_dir / 'portale_citta.png')
    print('Generato portale_citta.png (supersampled Lanczos)')

def gen_anziano_tartaruga():
    # Elder Sage Sea Turtle (Lore & Questgiver)
    w, h = 512, 512
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Shell
    draw.ellipse([110, 180, 410, 430], fill=(22, 101, 52, 255), outline=COL_INK, width=12)
    for scx, scy in [(210, 260), (310, 260), (260, 340)]:
        draw.polygon([(scx, scy - 30), (scx + 28, scy - 12), (scx + 28, scy + 20), (scx, scy + 35), (scx - 28, scy + 20), (scx - 28, scy - 12)], fill=(34, 197, 94, 255), outline=COL_INK, width=6)

    # Head
    draw.ellipse([210, 90, 310, 210], fill=(74, 222, 128, 255), outline=COL_INK, width=10)
    # Round brass spectacles
    draw.ellipse([215, 125, 255, 165], fill=(255, 255, 255, 200), outline=COL_GOLD, width=6)
    draw.ellipse([265, 125, 305, 165], fill=(255, 255, 255, 200), outline=COL_GOLD, width=6)
    draw.line([(255, 145), (265, 145)], fill=COL_GOLD, width=6)
    draw.ellipse([230, 140, 242, 152], fill=COL_INK)
    draw.ellipse([280, 140, 292, 152], fill=COL_INK)
    draw.arc([235, 165, 285, 190], start=0, end=180, fill=COL_INK, width=5)

    # Parchment scroll held in front
    draw.rounded_rectangle([190, 360, 330, 460], radius=8, fill=(254, 243, 199, 255), outline=COL_INK, width=6)
    draw.line([(210, 390), (310, 390)], fill=COL_INK, width=4)
    draw.line([(210, 415), (290, 415)], fill=COL_INK, width=4)

    sticker = apply_smooth_sticker(im, stroke_width=14, target_size=(256, 256))
    sticker.save(out_dir / 'anziano_tartaruga.png')
    print('Generato anziano_tartaruga.png (supersampled Lanczos)')

def gen_app_icon():
    # Builds both standard squircle and round adaptive Paper Mario app icons
    w, h = 1024, 1024

    def build_icon(is_round=False):
        im = Image.new('RGBA', (w, h), (0, 0, 0, 0))

        # Mask for squircle or circle
        mask = Image.new('L', (w, h), 0)
        dmask = ImageDraw.Draw(mask)
        if is_round:
            dmask.ellipse([32, 32, 992, 992], fill=255)
        else:
            dmask.rounded_rectangle([32, 32, 992, 992], radius=216, fill=255)

        # 1. Background Content (Ocean + Dunes + God rays)
        bg = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        dbg = ImageDraw.Draw(bg)

        # Vertical ocean watercolor gradient
        top_col = (14, 165, 233)
        mid_col = (2, 132, 199)
        bot_col = (8, 45, 56)
        for y in range(h):
            t = y / float(h)
            if t < 0.5:
                f = t / 0.5
                r = int(top_col[0] * (1 - f) + mid_col[0] * f)
                g = int(top_col[1] * (1 - f) + mid_col[1] * f)
                b = int(top_col[2] * (1 - f) + mid_col[2] * f)
            else:
                f = (t - 0.5) / 0.5
                r = int(mid_col[0] * (1 - f) + bot_col[0] * f)
                g = int(mid_col[1] * (1 - f) + bot_col[1] * f)
                b = int(mid_col[2] * (1 - f) + bot_col[2] * f)
            dbg.line([(0, y), (w, y)], fill=(r, g, b, 255))

        # Soft God Rays from surface
        dbg.polygon([(240, 32), (400, 32), (580, 992), (360, 992)], fill=(253, 224, 71, 42))
        dbg.polygon([(460, 32), (590, 32), (790, 992), (620, 992)], fill=(253, 224, 71, 32))

        # Wave foam current trim (crepe paper ripples)
        wave_pts = []
        for x in range(32, 993, 20):
            wy = 490 + math.sin(x * 0.015) * 25
            wave_pts.append((x, wy))
        wave_poly = [(32, 600)] + wave_pts + [(992, 600)]
        dbg.polygon(wave_poly, fill=(255, 255, 255, 30))
        for i in range(len(wave_pts) - 1):
            dbg.line([wave_pts[i], wave_pts[i+1]], fill=(255, 255, 255, 80), width=6)

        # Golden Sand Dunes at bottom
        dune_pts = []
        for x in range(32, 993, 20):
            dy = 750 + math.sin(x * 0.009) * 35
            dune_pts.append((x, dy))
        dune_poly = [(32, 992)] + dune_pts + [(992, 992)]
        dbg.polygon(dune_poly, fill=(253, 230, 138, 255))

        # Dune shadow & gradient
        for y in range(780, 993):
            f = (y - 780) / 212.0
            r = int(253 * (1 - f) + 217 * f)
            g = int(230 * (1 - f) + 119 * f)
            b = int(138 * (1 - f) + 6 * f)
            dbg.line([(32, y), (992, y)], fill=(r, g, b, 140))

        for i in range(len(dune_pts) - 1):
            dbg.line([dune_pts[i], dune_pts[i+1]], fill=COL_WHITE, width=10)
            dbg.line([dune_pts[i], dune_pts[i+1]], fill=COL_INK, width=4)

        # Dashed sewing stitches along the dune
        for x in range(50, 980, 40):
            dy = 770 + math.sin(x * 0.009) * 35
            dbg.line([(x, dy), (x + 22, dy)], fill=(180, 83, 9, 230), width=5)

        # Pink Coral on the left dune
        coral_pts = [(180, 770), (140, 680), (90, 640), (80, 580), (115, 590), (150, 640),
                     (170, 530), (210, 520), (210, 620), (250, 580), (280, 600), (250, 660), (200, 770)]
        dbg.polygon(coral_pts, fill=COL_CORAL, outline=COL_INK, width=6)
        dbg.ellipse([145, 615, 165, 635], fill=COL_WHITE)
        dbg.ellipse([225, 625, 245, 645], fill=COL_WHITE)
        dbg.ellipse([185, 565, 200, 580], fill=COL_WHITE)

        # Cute Starfish on the right dune
        scx, scy = 800, 820
        s_pts = []
        for i in range(10):
            rad = math.radians(i * 36 - 90)
            r = 75 if i % 2 == 0 else 34
            s_pts.append((scx + int(math.cos(rad) * r), scy + int(math.sin(rad) * r)))
        dbg.polygon(s_pts, fill=(251, 146, 60, 255), outline=COL_INK, width=6)
        dbg.ellipse([scx - 14, scy - 8, scx - 4, scy + 4], fill=COL_INK)
        dbg.ellipse([scx + 4, scy - 8, scx + 14, scy + 4], fill=COL_INK)
        dbg.arc([scx - 8, scy + 2, scx + 8, scy + 14], start=0, end=180, fill=COL_INK, width=4)

        # Air Bubbles rising
        for bx, by, br in [(180, 320, 26), (230, 260, 16), (820, 390, 30), (860, 320, 18), (760, 220, 22)]:
            dbg.ellipse([bx - br, by - br, bx + br, by + br], fill=(255, 255, 255, 70), outline=COL_WHITE, width=4)
            dbg.arc([bx - br + 4, by - br + 4, bx + br - 4, by + br - 4], start=210, end=330, fill=COL_WHITE, width=4)

        # 2. Hero Palombaro Sprite with Die-Cut Sticker
        hero = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        dhero = ImageDraw.Draw(hero)

        pcx, pcy = 512, 450

        # Oxygen tanks peeking from behind
        dhero.rounded_rectangle([pcx - 220, pcy - 90, pcx - 130, pcy + 170], radius=38, fill=(52, 211, 153, 255), outline=COL_INK, width=12)
        dhero.rounded_rectangle([pcx + 130, pcy - 90, pcx + 220, pcy + 170], radius=38, fill=(52, 211, 153, 255), outline=COL_INK, width=12)
        dhero.ellipse([pcx - 200, pcy - 120, pcx - 150, pcy - 80], fill=COL_GOLD, outline=COL_INK, width=8)
        dhero.ellipse([pcx + 150, pcy - 120, pcx + 200, pcy - 80], fill=COL_GOLD, outline=COL_INK, width=8)

        # Diving Suit Torso & Shoulders
        dhero.rounded_rectangle([pcx - 170, pcy + 70, pcx + 170, pcy + 300], radius=65, fill=COL_CANVAS, outline=COL_INK, width=14)
        dhero.line([pcx - 140, pcy + 130, pcx + 140, pcy + 130], fill=COL_INK, width=16)
        dhero.ellipse([pcx - 42, pcy + 96, pcx + 42, pcy + 164], fill=COL_GOLD, outline=COL_INK, width=10)
        dhero.ellipse([pcx - 18, pcy + 115, pcx + 18, pcy + 145], fill=COL_WHITE)

        # Harpoon spear in hand angled upwards
        dhero.line([pcx + 140, pcy + 220, pcx + 270, pcy - 190], fill=(120, 53, 15, 255), width=22)
        dhero.line([pcx + 140, pcy + 220, pcx + 270, pcy - 190], fill=COL_INK, width=8)
        dhero.polygon([(pcx + 270, pcy - 190), (pcx + 220, pcy - 140), (pcx + 305, pcy - 165)], fill=(241, 245, 249, 255), outline=COL_INK, width=5)
        dhero.ellipse([(pcx + 260, pcy - 210), (pcx + 290, pcy - 180)], fill=COL_WHITE)

        # Big Round Paper Mario Brass Diving Helmet
        dhero.ellipse([pcx - 195, pcy - 215, pcx + 195, pcy + 145], fill=(245, 158, 11, 255), outline=COL_INK, width=16)

        # Brass Rivets around helmet rim
        for deg in range(30, 160, 28):
            rad = math.radians(deg + 90)
            rx = pcx + math.cos(rad) * 162
            ry = pcy - 35 + math.sin(rad) * 162
            dhero.ellipse([rx - 12, ry - 12, rx + 12, ry + 12], fill=COL_GOLD, outline=COL_INK, width=6)
        for deg in range(30, 160, 28):
            rad = math.radians(-deg + 90)
            rx = pcx + math.cos(rad) * 162
            ry = pcy - 35 + math.sin(rad) * 162
            dhero.ellipse([rx - 12, ry - 12, rx + 12, ry + 12], fill=COL_GOLD, outline=COL_INK, width=6)

        # Porthole Visor Outer Bezel
        dhero.ellipse([pcx - 125, pcy - 135, pcx + 125, pcy + 75], fill=(30, 41, 59, 255), outline=COL_INK, width=14)
        dhero.ellipse([pcx - 110, pcy - 120, pcx + 110, pcy + 60], fill=(56, 189, 248, 255), outline=COL_INK, width=8)

        # Visor Crossbars
        dhero.line([pcx - 110, pcy - 30, pcx + 110, pcy - 30], fill=COL_GOLD, width=12)
        dhero.line([pcx, pcy - 120, pcx, pcy + 60], fill=COL_GOLD, width=12)
        dhero.line([pcx - 110, pcy - 30, pcx + 110, pcy - 30], fill=COL_INK, width=4)
        dhero.line([pcx, pcy - 120, pcx, pcy + 60], fill=COL_INK, width=4)

        # Glare Reflections
        dhero.polygon([(pcx - 85, pcy - 85), (pcx - 45, pcy - 92), (pcx - 65, pcy - 35), (pcx - 100, pcy - 35)], fill=COL_WHITE)
        dhero.polygon([(pcx + 35, pcy - 92), (pcx + 65, pcy - 95), (pcx + 55, pcy - 45), (pcx + 30, pcy - 45)], fill=COL_WHITE)

        # Die-cut sticker outline
        alpha = hero.split()[-1]
        dilated = alpha.filter(ImageFilter.MaxFilter(25 * 2 + 1))
        dilated = dilated.filter(ImageFilter.GaussianBlur(radius=25 * 0.42))
        dilated = dilated.point(lambda p: 255 if p > 72 else int((p / 72.0) ** 1.6 * 255))
        border = Image.new('RGBA', (w, h), COL_WHITE)
        border.putalpha(dilated)
        hero_sticker = Image.alpha_composite(border, hero)

        # Floor / drop shadow for hero
        shadow = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        dshadow = ImageDraw.Draw(shadow)
        dshadow.ellipse([pcx - 220, pcy + 160, pcx + 220, pcy + 250], fill=(0, 0, 0, 110))
        shadow = shadow.filter(ImageFilter.GaussianBlur(radius=16))

        # Composite Background + Shadow + Hero
        comp = Image.alpha_composite(bg, shadow)
        comp = Image.alpha_composite(comp, hero_sticker)

        # Apply outer squircle / circle mask
        final_icon = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        final_icon.paste(comp, (0, 0), mask)

        dfinal = ImageDraw.Draw(final_icon)
        if is_round:
            dfinal.ellipse([32, 32, 992, 992], outline=COL_WHITE, width=28)
            dfinal.ellipse([18, 18, 1006, 1006], outline=COL_INK, width=8)
        else:
            dfinal.rounded_rectangle([32, 32, 992, 992], radius=216, outline=COL_WHITE, width=28)
            dfinal.rounded_rectangle([18, 18, 1006, 1006], radius=230, outline=COL_INK, width=8)

        return final_icon

    sq_master = build_icon(is_round=False)
    rd_master = build_icon(is_round=True)

    res_root = out_dir.parent
    densities = {
        'drawable': 192,
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192
    }

    for folder, size in densities.items():
        target_dir = res_root / folder
        target_dir.mkdir(parents=True, exist_ok=True)
        sq_img = sq_master.resize((size, size), Image.Resampling.LANCZOS)
        rd_img = rd_master.resize((size, size), Image.Resampling.LANCZOS)
        sq_img.save(target_dir / 'ic_launcher.png')
        rd_img.save(target_dir / 'ic_launcher_round.png')

    print('Generate icone launcher Paper Mario PNG ad alta risoluzione (drawable e mipmap-*)')

if __name__ == '__main__':
    gen_palombaro()
    gen_granchio()
    gen_medusa()
    gen_boss_leviatano()
    gen_murena()
    gen_coralli()
    gen_conchiglia()
    gen_paguro_bottegaio()
    gen_stella_marina()
    gen_anemone()
    gen_pesce_lanterna()
    gen_fabbro_astice()
    gen_cassa_tesoro()
    gen_portale_citta()
    gen_anziano_tartaruga()
    gen_app_icon()
    print('Tutti gli asset grafici Paper Mario PNG generati con successo e anti-aliasing liscio!')
