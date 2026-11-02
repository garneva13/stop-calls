"""Preview of the Quick Settings tile (how it looks in the shade).

    python tools/render_tile_preview.py
"""

import os

from PIL import Image, ImageDraw

from render_mockup import dp, font

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(ROOT, "docs")

BG = (28, 28, 28)
TILE_OFF = (58, 58, 60)
TILE_ON = (11, 87, 208)
TEXT = (235, 235, 235)
MUTED = (170, 170, 170)


def shield(draw, cx, cy, size, color):
    half = size / 2
    top = cy - half
    body = size * 0.52
    draw.rounded_rectangle([cx - half * 0.8, top, cx + half * 0.8, top + body],
                           radius=dp(5), outline=color, width=dp(3))
    draw.polygon([(cx - half * 0.8, top + body - dp(2)),
                  (cx + half * 0.8, top + body - dp(2)),
                  (cx, cy + half)], outline=color, width=dp(3))


def tile(draw, x, y, size, active, caption):
    draw.rounded_rectangle([x, y, x + size, y + size], radius=dp(26),
                           fill=TILE_ON if active else TILE_OFF)
    shield(draw, x + size // 2, y + size // 2, size * 0.46, (255, 255, 255))
    draw.text((x + size // 2, y + size + dp(10)), caption, font=font(11),
              fill=TEXT if active else MUTED, anchor="ma")


def main():
    w, h = dp(340), dp(190)
    img = Image.new("RGB", (w, h), BG)
    draw = ImageDraw.Draw(img)
    size = dp(100)
    tile(draw, dp(24), dp(20), size, True, "Включён")
    tile(draw, dp(160), dp(20), size, False, "Выключен")
    draw.text((dp(24), dp(150)), "Плитка «Фильтр звонков» в шторке",
              font=font(12), fill=MUTED, anchor="la")
    img.save(os.path.join(DOCS, "tile-preview.png"), optimize=True)
    print(os.path.join(DOCS, "tile-preview.png"), img.size)


if __name__ == "__main__":
    main()
