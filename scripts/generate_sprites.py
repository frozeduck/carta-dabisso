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

def apply_sticker_outline(im, stroke_width=6, stroke_color=(255, 255, 255, 255)):
    """Creates the signature Paper Mario thick white die-cut sticker border."""
    # Alpha mask dilation
    alpha = im.split()[-1]
    w, h = im.size
    dilated = alpha.filter(ImageFilter.MaxFilter(stroke_width * 2 + 1))
    
    border = Image.new('RGBA', (w, h), stroke_color)
    border.putalpha(dilated)
    
    # Composite original on top of border
    result = Image.alpha_composite(border, im)
    return result

def gen_palombaro():
    w, h = 256, 256
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Oxygen tanks on back
    draw.rounded_rectangle([45, 90, 85, 175], radius=16, fill=(52, 211, 153, 255), outline=COL_INK, width=4)
    draw.rounded_rectangle([52, 105, 78, 118], radius=4, fill=(16, 185, 129, 255), outline=COL_INK, width=3)
    draw.ellipse([58, 80, 72, 94], fill=COL_GOLD, outline=COL_INK, width=3)

    # Boots
    draw.rounded_rectangle([70, 190, 115, 230], radius=10, fill=COL_BOOT, outline=COL_INK, width=4)
    draw.rounded_rectangle([135, 190, 180, 230], radius=10, fill=(146, 64, 14, 255), outline=COL_INK, width=4)

    # Diving suit body
    draw.rounded_rectangle([75, 115, 175, 200], radius=24, fill=COL_CANVAS, outline=COL_INK, width=5)
    # Chest harness & brass badge
    draw.line([75, 150, 175, 155], fill=COL_INK, width=5)
    draw.ellipse([115, 142, 135, 162], fill=COL_GOLD, outline=COL_INK, width=4)
    draw.ellipse([122, 149, 128, 155], fill=COL_WHITE)

    # Harpoon weapon in hand
    draw.line([165, 185, 215, 60], fill=(120, 53, 15, 255), width=7)
    draw.line([165, 185, 215, 60], fill=COL_INK, width=3)
    # Harpoon head (triangle)
    draw.polygon([(215, 60), (200, 78), (228, 70)], fill=(241, 245, 249, 255), outline=COL_INK)
    # Shiny star glint on harpoon tip
    draw.ellipse([212, 54, 222, 64], fill=COL_WHITE)

    # Big round Paper Mario brass diving helmet
    draw.ellipse([70, 35, 180, 145], fill=(245, 158, 11, 255), outline=COL_INK, width=6)
    
    # Helmet rivets
    for angle in [math.pi * 0.8, math.pi, math.pi * 1.2, math.pi * 1.4, math.pi * 1.6]:
        rx = 125 + math.cos(angle) * 50
        ry = 90 + math.sin(angle) * 50
        draw.ellipse([rx-4, ry-4, rx+4, ry+4], fill=COL_GOLD, outline=COL_INK, width=2)

    # Porthole visor bezel
    draw.ellipse([92, 57, 158, 123], fill=(30, 41, 59, 255), outline=COL_INK, width=5)
    draw.ellipse([98, 63, 152, 117], fill=(56, 189, 248, 255), outline=COL_INK, width=3)

    # Protective visor crossbars
    draw.line([98, 90, 152, 90], fill=COL_GOLD, width=4)
    draw.line([125, 63, 125, 117], fill=COL_GOLD, width=4)

    # Double comic glare reflections
    draw.polygon([(104, 76), (116, 74), (108, 96), (98, 96)], fill=COL_WHITE)
    draw.polygon([(136, 74), (144, 73), (140, 86), (132, 86)], fill=COL_WHITE)

    # Apply signature thick white Paper Mario die-cut sticker border!
    sticker = apply_sticker_outline(im, stroke_width=7, stroke_color=COL_WHITE)
    sticker.save(out_dir / 'palombaro.png')
    print('Salvato palombaro.png')

def gen_granchio():
    w, h = 256, 256
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Jointed legs
    for side in [-1, 1]:
        cx = 128 + side * 40
        draw.line([cx, 160, cx + side * 40, 170], fill=COL_INK, width=8)
        draw.line([cx + side * 40, 170, cx + side * 60, 205], fill=COL_INK, width=8)
        draw.line([cx, 160, cx + side * 40, 170], fill=COL_CORAL, width=4)
        draw.line([cx + side * 40, 170, cx + side * 60, 205], fill=COL_CORAL, width=4)

    # Big snip-snip claws
    for side in [-1, 1]:
        cx = 128 + side * 65
        # Claw arm
        draw.line([128 + side * 45, 140, cx, 110], fill=COL_INK, width=8)
        draw.line([128 + side * 45, 140, cx, 110], fill=COL_CORAL, width=4)
        # Snapping claw head
        pts = [(cx, 110), (cx + side * 25, 80), (cx + side * 35, 115), (cx + side * 15, 125)]
        draw.polygon(pts, fill=(239, 68, 68, 255), outline=COL_INK)
        # Inner pincer
        draw.line([cx + side * 15, 95, cx + side * 30, 95], fill=COL_INK, width=4)

    # Carapace (Round Paper Mario crab body)
    draw.ellipse([65, 120, 191, 195], fill=COL_CORAL, outline=COL_INK, width=6)
    draw.ellipse([80, 135, 176, 175], fill=(252, 165, 165, 255))

    # Big Goofy Cartoon Eyes with white eyeballs & black pupils!
    for side in [-1, 1]:
        ex = 128 + side * 28
        # Eye stalk
        draw.line([ex, 125, ex, 90], fill=COL_INK, width=6)
        # Eyeball
        draw.ellipse([ex - 18, 65, ex + 18, 101], fill=COL_WHITE, outline=COL_INK, width=5)
        # Pupil
        draw.ellipse([ex - 8, 75, ex + 8, 91], fill=COL_INK)
        # Twinkle
        draw.ellipse([ex - 4, 77, ex + 2, 83], fill=COL_WHITE)

    # Happy little crab smile
    draw.arc([110, 155, 146, 175], start=0, end=180, fill=COL_INK, width=4)

    sticker = apply_sticker_outline(im, stroke_width=7, stroke_color=COL_WHITE)
    sticker.save(out_dir / 'granchio.png')
    print('Salvato granchio.png')

def gen_medusa():
    w, h = 256, 256
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # Fluttering crepe-paper tentacles
    for i in range(5):
        tx = 80 + i * 24
        points = [(tx, 145), (tx + 8, 175), (tx - 8, 205), (tx + 5, 235)]
        draw.line(points, fill=COL_INK, width=8)
        draw.line(points, fill=(186, 230, 253, 255), width=4)

    # Scalloped Paper Mario Blooper/Jelly Bell
    draw.ellipse([55, 60, 201, 155], fill=COL_AQUA, outline=COL_INK, width=6)
    draw.ellipse([70, 75, 186, 135], fill=(125, 211, 252, 255))

    # Cute cartoon eyes
    for side in [-1, 1]:
        ex = 128 + side * 24
        draw.ellipse([ex - 12, 100, ex + 12, 126], fill=COL_WHITE, outline=COL_INK, width=4)
        draw.ellipse([ex - 5, 107, ex + 5, 119], fill=COL_INK)
        draw.ellipse([ex - 2, 109, ex + 2, 113], fill=COL_WHITE)

    sticker = apply_sticker_outline(im, stroke_width=7, stroke_color=COL_WHITE)
    sticker.save(out_dir / 'medusa.png')
    print('Salvato medusa.png')

def gen_boss_leviatano():
    w, h = 320, 320
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(im)

    # 7 Layered Cardboard Tentacles
    for i in range(7):
        angle = (i - 3) * 0.3
        tx = 160 + (i - 3) * 28
        pts = [(tx, 170), (tx + 12, 220), (tx - 10, 270), (tx + int(math.sin(i) * 15), 305)]
        draw.line(pts, fill=COL_INK, width=14)
        draw.line(pts, fill=(52, 211, 153, 255) if i % 2 == 0 else (16, 185, 129, 255), width=8)
        # Suction cup rings
        draw.ellipse([pts[1][0] - 8, pts[1][1] - 8, pts[1][0] + 8, pts[1][1] + 8], fill=COL_GOLD, outline=COL_INK, width=3)

    # Monumental Cardboard Boss Head
    draw.ellipse([60, 50, 260, 195], fill=COL_GOLD, outline=COL_INK, width=7)
    draw.ellipse([80, 65, 240, 175], fill=(245, 158, 11, 255))

    # Ancient spiral shell carving
    for a in range(25):
        rad = a * 0.25
        r = 10 + a * 3.5
        sx = 135 + math.cos(rad) * r
        sy = 120 + math.sin(rad) * r
        draw.point((sx, sy), fill=COL_INK)

    # Giant Cyclopean Cartoon Boss Eye
    draw.ellipse([160, 85, 230, 155], fill=COL_WHITE, outline=COL_INK, width=6)
    draw.ellipse([175, 98, 218, 142], fill=(249, 115, 22, 255), outline=COL_INK, width=3) # Amber iris
    draw.ellipse([190, 105, 205, 135], fill=COL_INK) # Cat slit pupil
    draw.ellipse([180, 102, 188, 110], fill=COL_WHITE) # Shiny glint

    # Spiky Cardboard Crest Fin
    draw.polygon([(110, 55), (130, 15), (150, 52), (170, 18), (190, 54)], fill=COL_CORAL, outline=COL_INK, width=4)

    sticker = apply_sticker_outline(im, stroke_width=8, stroke_color=COL_WHITE)
    sticker.save(out_dir / 'boss_leviatano.png')
    print('Salvato boss_leviatano.png')

def gen_coralli():
    # Coral 1: Pink Staghorn
    im1 = Image.new('RGBA', (180, 180), (0, 0, 0, 0))
    d1 = ImageDraw.Draw(im1)
    pts1 = [(90, 160), (70, 110), (40, 90), (35, 55), (55, 60), (75, 85),
            (85, 30), (105, 25), (105, 75), (125, 55), (145, 60), (130, 95), (105, 160)]
    d1.polygon(pts1, fill=COL_CORAL, outline=COL_INK, width=4)
    # White decorative polka dots
    d1.ellipse([70, 70, 80, 80], fill=COL_WHITE)
    d1.ellipse([115, 75, 125, 85], fill=COL_WHITE)
    d1.ellipse([92, 45, 100, 53], fill=COL_WHITE)
    s1 = apply_sticker_outline(im1, stroke_width=6, stroke_color=COL_WHITE)
    s1.save(out_dir / 'corallo_rosso.png')

    # Coral 2: Mint Sea Fan
    im2 = Image.new('RGBA', (180, 180), (0, 0, 0, 0))
    d2 = ImageDraw.Draw(im2)
    pts2 = [(90, 160), (65, 120), (45, 80), (60, 45), (80, 70), (90, 30),
            (105, 30), (115, 65), (135, 45), (150, 75), (130, 115), (105, 160)]
    d2.polygon(pts2, fill=COL_MINT, outline=COL_INK, width=4)
    d2.line([90, 160, 97, 45], fill=(34, 197, 94, 255), width=3)
    s2 = apply_sticker_outline(im2, stroke_width=6, stroke_color=COL_WHITE)
    s2.save(out_dir / 'corallo_verde.png')
    print('Salvati coralli PNG.')

if __name__ == '__main__':
    gen_palombaro()
    gen_granchio()
    gen_medusa()
    gen_boss_leviatano()
    gen_coralli()
    print('Tutti gli asset grafici Paper Mario PNG generati con successo!')

