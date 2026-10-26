"""Render button and statistics-screen options for the CallGuard redesign.

    python tools/render_buttons.py

Outputs: docs/options-buttons.png, docs/options-stats.png
"""

import os

from PIL import Image, ImageDraw

from render_mockup import (
    BG, CARD, SECOND, STROKE, TEXT,
    dp, font,
)

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "docs")
ACCENT = (11, 87, 208)
ACCENT_SOFT = (227, 236, 251)
MUTED_SOFT = (238, 240, 243)
BAR_LIGHT = (196, 214, 248)


def glyph(draw, cx, cy, size, kind, color):
    s = size / 2.0
    w = max(2, int(size / 9))
    if kind == "clock":
        draw.ellipse([cx - s, cy - s, cx + s, cy + s], outline=color, width=w)
        draw.line([cx, cy, cx, cy - s * 0.45], fill=color, width=w)
        draw.line([cx, cy, cx + s * 0.42, cy], fill=color, width=w)
    elif kind == "pause":
        draw.rectangle([cx - s * 0.55, cy - s * 0.8, cx - s * 0.15, cy + s * 0.8], fill=color)
        draw.rectangle([cx + s * 0.15, cy - s * 0.8, cx + s * 0.55, cy + s * 0.8], fill=color)
    elif kind == "calendar":
        draw.rounded_rectangle([cx - s * 0.85, cy - s * 0.7, cx + s * 0.85, cy + s * 0.85],
                               radius=dp(2), outline=color, width=w)
        draw.line([cx - s * 0.85, cy - s * 0.2, cx + s * 0.85, cy - s * 0.2], fill=color, width=w)
        draw.line([cx - s * 0.4, cy - s, cx - s * 0.4, cy - s * 0.45], fill=color, width=w)
        draw.line([cx + s * 0.4, cy - s, cx + s * 0.4, cy - s * 0.45], fill=color, width=w)
    elif kind == "skip":
        draw.line([cx - s * 0.7, cy - s * 0.7, cx + s * 0.1, cy], fill=color, width=w)
        draw.line([cx + s * 0.1, cy, cx - s * 0.7, cy + s * 0.7], fill=color, width=w)
        draw.line([cx - s * 0.7, cy + s * 0.7, cx - s * 0.7, cy - s * 0.7], fill=color, width=w)
        draw.line([cx + s * 0.55, cy - s * 0.8, cx + s * 0.55, cy + s * 0.8], fill=color, width=w)
    elif kind == "chart":
        for dx, height in ((-0.55, 0.45), (0.0, 0.8), (0.55, 0.62)):
            x = cx + s * dx
            draw.rounded_rectangle([x - s * 0.17, cy + s * 0.8 - s * height * 1.6,
                                    x + s * 0.17, cy + s * 0.8],
                                   radius=dp(1.5), fill=color)


def tile(draw, x, y, size, kind, caption, active):
    draw.rounded_rectangle([x, y, x + size, y + size], radius=dp(18),
                           fill=ACCENT if active else CARD,
                           outline=None if active else STROKE, width=0 if active else max(1, dp(1)))
    glyph(draw, x + size // 2, y + int(size * 0.38), size * 0.34, kind,
          (255, 255, 255) if active else TEXT)
    if caption:
        draw.text((x + size // 2, y + int(size * 0.74)), caption, font=font(10, True),
                  fill=(255, 255, 255) if active else SECOND, anchor="mm")


def circle(draw, x, y, size, kind, caption, active):
    draw.ellipse([x, y, x + size, y + size], fill=ACCENT if active else CARD,
                 outline=None if active else STROKE, width=0 if active else max(1, dp(1)))
    glyph(draw, x + size // 2, y + size // 2, size * 0.42, kind,
          (255, 255, 255) if active else TEXT)
    if caption:
        draw.text((x + size // 2, y + size + dp(12)), caption, font=font(10, True),
                  fill=SECOND, anchor="ma")


def bare(draw, x, y, size, kind, active):
    draw.rounded_rectangle([x, y, x + size, y + size], radius=dp(14),
                           fill=ACCENT if active else ACCENT_SOFT)
    glyph(draw, x + size // 2, y + size // 2, size * 0.44, kind,
          (255, 255, 255) if active else ACCENT)


def heading(draw, x, y, title, subtitle):
    draw.text((x, y), title, font=font(17, True), fill=TEXT, anchor="la")
    draw.text((x, y + dp(24)), subtitle, font=font(12), fill=SECOND, anchor="la")


def row_caption(draw, x, y, text):
    draw.text((x, y), text, font=font(11, True), fill=SECOND, anchor="la")


ITEMS = (("pause", "Pause"), ("skip", "Pass"), ("calendar", "Schedule"), ("chart", "Stats"))


def buttons_image():
    w, h = dp(440), dp(820)
    img = Image.new("RGB", (w, h), BG)
    draw = ImageDraw.Draw(img)
    x0 = dp(24)

    y = dp(24)
    draw.text((x0, y), "CallGuard: buttons", font=font(22, True), fill=TEXT, anchor="la")
    y += dp(44)

    # A — tiles
    heading(draw, x0, y, "A. Tiles with captions", "icon + small caption, 64 dp")
    y += dp(52)
    row_caption(draw, x0, y, "normal")
    y += dp(16)
    x = x0
    for kind, caption in ITEMS:
        tile(draw, x, y, dp(64), kind, caption, False)
        x += dp(64) + dp(10)
    y += dp(64) + dp(10)
    row_caption(draw, x0, y, "active (Pass is on)")
    y += dp(16)
    x = x0
    for kind, caption in ITEMS:
        tile(draw, x, y, dp(64), kind, caption, kind == "skip")
        x += dp(64) + dp(10)
    y += dp(64) + dp(28)

    # B — circles
    heading(draw, x0, y, "B. Circles with captions", "bigger touch target, caption below")
    y += dp(52)
    row_caption(draw, x0, y, "normal")
    y += dp(16)
    x = x0
    for kind, caption in ITEMS:
        circle(draw, x, y, dp(52), kind, caption, False)
        x += dp(52) + dp(18)
    y += dp(52) + dp(28)
    row_caption(draw, x0, y, "active (Pass is on)")
    y += dp(16)
    x = x0
    for kind, caption in ITEMS:
        circle(draw, x, y, dp(52), kind, caption, kind == "skip")
        x += dp(52) + dp(18)
    y += dp(52) + dp(28) + dp(26)

    # C — icon only
    heading(draw, x0, y, "C. Icon only", "no captions, compact")
    y += dp(52)
    row_caption(draw, x0, y, "normal")
    y += dp(16)
    x = x0
    for kind, _ in ITEMS:
        bare(draw, x, y, dp(56), kind, False)
        x += dp(56) + dp(12)
    y += dp(56) + dp(12)
    row_caption(draw, x0, y, "active (Pass is on)")
    y += dp(16)
    x = x0
    for kind, _ in ITEMS:
        bare(draw, x, y, dp(56), kind, kind == "skip")
        x += dp(56) + dp(12)

    img.save(os.path.join(OUT, "options-buttons.png"), optimize=True)
    print(os.path.join(OUT, "options-buttons.png"))


def bars_block(draw, x, y, w, h):
    draw.rounded_rectangle([x, y, x + w, y + h], radius=dp(20), fill=CARD,
                           outline=STROKE, width=max(1, dp(1)))
    seg_w = (w - dp(24)) // 4
    for i, name in enumerate(("Day", "Week", "Month", "Year")):
        sx = x + dp(12) + i * seg_w
        active = i == 0
        draw.rounded_rectangle([sx, y + dp(12), sx + seg_w - dp(3), y + dp(42)],
                               radius=dp(15), fill=ACCENT if active else MUTED_SOFT)
        draw.text((sx + (seg_w - dp(3)) // 2, y + dp(27)), name, font=font(11, True),
                  fill=(255, 255, 255) if active else TEXT, anchor="mm")

    draw.text((x + dp(14), y + dp(58)), "Blocked: 12", font=font(22, True), fill=TEXT,
              anchor="la")
    draw.text((x + dp(14), y + dp(88)), "1.7 per day on average", font=font(11), fill=SECOND,
              anchor="la")

    top = y + dp(116)
    bottom = y + h - dp(44)
    bars = (3, 5, 2, 7, 4, 1, 6, 2, 4, 5, 3, 8)
    step = (w - dp(24)) // len(bars)
    for i, value in enumerate(bars):
        bh = int((bottom - top) * value / max(bars))
        bx = x + dp(12) + i * step
        draw.rounded_rectangle([bx, bottom - bh, bx + step - dp(6), bottom],
                               radius=dp(3), fill=ACCENT if i == len(bars) - 1 else BAR_LIGHT)
    draw.line([x + dp(12), bottom, x + w - dp(12), bottom], fill=STROKE, width=max(1, dp(1)))
    draw.text((x + dp(14), bottom + dp(8)), "1 Oct", font=font(10), fill=SECOND, anchor="la")
    draw.text((x + w - dp(14), bottom + dp(8)), "12 Oct", font=font(10), fill=SECOND,
              anchor="ra")


def list_block(draw, x, y, w, h):
    draw.rounded_rectangle([x, y, x + w, y + h], radius=dp(20), fill=CARD,
                           outline=STROKE, width=max(1, dp(1)))
    draw.text((x + dp(14), y + dp(12)), "Last 7 days", font=font(12, True), fill=SECOND,
              anchor="la")
    rows = (("Today", 3), ("Yesterday", 2), ("24 Oct", 5), ("23 Oct", 1), ("22 Oct", 4))
    ry = y + dp(38)
    for name, value in rows:
        draw.text((x + dp(14), ry), name, font=font(13), fill=TEXT, anchor="la")
        draw.text((x + w - dp(14), ry), str(value), font=font(13, True), fill=ACCENT,
                  anchor="ra")
        ry += dp(28)
        draw.line([x + dp(14), ry - dp(8), x + w - dp(14), ry - dp(8)], fill=STROKE,
                  width=max(1, dp(1)))


def stats_image():
    w, h = dp(440), dp(820)
    img = Image.new("RGB", (w, h), BG)
    draw = ImageDraw.Draw(img)
    x0 = dp(24)

    y = dp(24)
    draw.text((x0, y), "CallGuard: statistics", font=font(22, True), fill=TEXT, anchor="la")
    y += dp(46)

    heading(draw, x0, y, "Layout 1 — chart", "period segments on top, bars below")
    y += dp(52)
    bars_block(draw, x0, y, dp(392), dp(300))
    y += dp(300) + dp(28)

    heading(draw, x0, y, "Layout 2 — list", "same numbers as rows, easier to read")
    y += dp(52)
    list_block(draw, x0, y, dp(392), dp(190))

    img.save(os.path.join(OUT, "options-stats.png"), optimize=True)
    print(os.path.join(OUT, "options-stats.png"))


if __name__ == "__main__":
    buttons_image()
    stats_image()
