"""Preview of the quick-action tiles with the supplied icons.

    python tools/render_tiles.py
"""

import os

from PIL import Image, ImageDraw

from render_mockup import dp, font

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(ROOT, "docs")

BG = (15, 18, 22)
CARD = (26, 30, 35)
STROKE = (42, 48, 54)
TEXT = (231, 234, 238)
SECOND = (154, 165, 177)
ACCENT = (168, 199, 250)
WARN = (245, 176, 76)

TILES = (
    ("Pause.png", "Пауза", "Временно отключить", False),
    ("Skip.png", "Пропустить\nследующий", "Пропустить один звонок", True),
    ("Schedule.png", "Расписание", "Настроить время работы", False),
)


def wrap(draw, text, fnt, width, max_lines=2):
    words = text.split()
    lines = []
    current = ""
    for word in words:
        probe = (current + " " + word).strip()
        if draw.textlength(probe, font=fnt) <= width or not current:
            current = probe
        else:
            lines.append(current)
            current = word
    lines.append(current)
    return lines[:max_lines]


def main():
    w, h = dp(360), dp(190)
    img = Image.new("RGB", (w, h), BG)
    draw = ImageDraw.Draw(img)

    margin = dp(16)
    gap = dp(8)
    tile_w = (w - margin * 2 - gap * 2) // 3
    tile_h = dp(160)
    top = dp(16)

    for index, (icon_file, title, subtitle, active) in enumerate(TILES):
        x = margin + index * (tile_w + gap)
        draw.rounded_rectangle([x, top, x + tile_w, top + tile_h], radius=dp(22),
                               fill=CARD, outline=ACCENT if active else STROKE,
                               width=dp(2) if active else dp(1))
        icon = Image.open(os.path.join(DOCS, icon_file)).convert("RGBA")
        size = dp(56)
        icon = icon.resize((size, size), Image.LANCZOS)
        img.paste(icon, (x + (tile_w - size) // 2, top + dp(14)), icon)

        inner = tile_w - dp(12)
        title_font = font(13, True)
        y = top + dp(14) + size + dp(10)
        for line in wrap(draw, title, title_font, inner):
            draw.text((x + tile_w // 2, y), line, font=title_font,
                      fill=ACCENT if active else TEXT, anchor="ma")
            y += dp(17)

        sub_font = font(11)
        y += dp(3)
        for line in wrap(draw, subtitle, sub_font, inner):
            draw.text((x + tile_w // 2, y), line, font=sub_font, fill=SECOND, anchor="ma")
            y += dp(14)

    img.save(os.path.join(DOCS, "tiles-preview.png"), optimize=True)
    print(os.path.join(DOCS, "tiles-preview.png"))


if __name__ == "__main__":
    main()
