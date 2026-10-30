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
    # Bioluminescent sea anemone cluster
    w, h = 360, 360
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Base rock / pedestal
    draw.ellipse([100, 280, 260, 340], fill=(71, 85, 105, 255), outline=COL_INK, width=8)

    # Swaying tentacles
    for i in range(9):
        tx = 120 + i * 15
        curve_x = tx + (i - 4) * 8
        pts = [(tx, 290), (curve_x, 220), (curve_x + (i - 4) * 6, 140)]
        draw.line(pts, fill=COL_INK, width=16)
        draw.line(pts, fill=(236, 72, 153, 255) if i % 2 == 0 else (192, 132, 252, 255), width=10)
        # Glowing tip bulb
        draw.ellipse([pts[-1][0] - 10, pts[-1][1] - 10, pts[-1][0] + 10, pts[-1][1] + 10], fill=COL_GOLD, outline=COL_INK, width=4)

    sticker = apply_smooth_sticker(im, stroke_width=12, target_size=(180, 180))
    sticker.save(out_dir / 'anemone.png')
    print('Generato anemone.png (supersampled Lanczos)')

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
    print('Tutti gli asset grafici Paper Mario PNG generati con successo e anti-aliasing liscio!')
