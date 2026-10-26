"""Render the current home screen (light theme) for the project page.

    python tools/render_home.py
"""

import os

from PIL import Image, ImageDraw

from render_mockup import (
    ACCENT, BG, CARD, GREEN, SECOND, STROKE, TEXT, W,
    card, dp, font, paragraph, status_bar, switch,
)

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "docs")
H = 3060

SOFT_BLUE = (227, 236, 251)
SOFT_WARN = (255, 243, 224)
SOFT_RED = (253, 232, 231)
RED = (179, 38, 30)


def rounded(draw, box, radius, fill, outline=None, width=0):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def icon(draw, image, x, y, size):
    art = Image.open(image).convert("RGBA").resize((size, size), Image.LANCZOS)
    draw._image.paste(art, (x, y), art)


def tile(img, draw, x, y, w, h, icon_path, title, subtitle, active=False):
    rounded(draw, [x, y, x + w, y + h], dp(22), CARD,
            ACCENT if active else STROKE, dp(2) if active else dp(1))
    icon(draw, icon_path, x + (w - dp(56)) // 2, y + dp(14), dp(56))
    title_font = font(13, True)
    ty = y + dp(14) + dp(56) + dp(10)
    for line in title.split("\n"):
        draw.text((x + w // 2, ty), line, font=title_font,
                  fill=ACCENT if active else TEXT, anchor="ma")
        ty += dp(18)
    for line in subtitle.split("\n"):
        draw.text((x + w // 2, ty), line, font=font(11), fill=SECOND, anchor="ma")
        ty += dp(15)


def glyph(draw, kind, cx, cy, size):
    color = ACCENT
    half = size / 2
    if kind == "stats":
        for offset, height in ((-0.55, 0.45), (0.0, 0.85), (0.55, 0.62)):
            x = cx + half * offset
            draw.rounded_rectangle([x - half * 0.16, cy + half * 0.8 - half * height * 1.5,
                                    x + half * 0.16, cy + half * 0.8],
                                   radius=dp(1.2), fill=color)
    elif kind == "person":
        draw.ellipse([cx - half * 0.42, cy - half * 0.8, cx + half * 0.42, cy - half * 0.02],
                     fill=color)
        draw.arc([cx - half * 0.72, cy - half * 0.1, cx + half * 0.72, cy + half * 1.1],
                 200, 340, fill=color, width=dp(2))
    elif kind == "shield":
        draw.polygon([(cx, cy - half * 0.85), (cx + half * 0.72, cy - half * 0.55),
                      (cx + half * 0.72, cy + half * 0.15), (cx, cy + half * 0.9),
                      (cx - half * 0.72, cy + half * 0.15), (cx - half * 0.72, cy - half * 0.55)],
                     outline=color, width=dp(2))


def main():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    draw._image = img

    status_bar(draw)
    left = dp(20)
    width = W - dp(40)
    inner = width - dp(36)
    pad = dp(18)
    y = dp(52)

    draw.text((left, y), "Стоп-звонки", font=font(26, True), fill=TEXT, anchor="la")
    draw.text((left, y + dp(40)), "Сброс входящих с номеров, которых нет в контактах",
              font=font(14), fill=SECOND, anchor="la")
    y += dp(76)

    # Hero card
    hero_h = dp(196)
    rounded(draw, [left, y, left + width, y + hero_h], dp(24), (233, 244, 236))
    shield_size = dp(44)
    draw.polygon([(left + dp(38), y + dp(24)),
                  (left + dp(38) + shield_size // 2, y + dp(38)),
                  (left + dp(38) + shield_size // 2, y + dp(64)),
                  (left + dp(38), y + dp(24) + shield_size),
                  (left + dp(38) - shield_size // 2, y + dp(64)),
                  (left + dp(38) - shield_size // 2, y + dp(38))],
                 outline=GREEN, width=dp(3))
    draw.line([left + dp(38) - dp(8), y + dp(52), left + dp(38) + dp(8), y + dp(52)],
              fill=GREEN, width=dp(3))
    draw.text((left + dp(88), y + dp(22)), "Фильтр включён", font=font(21, True),
              fill=GREEN, anchor="la")
    draw.text((left + dp(88), y + dp(56)), "Звонки не из контактов", font=font(13),
              fill=(58, 90, 66), anchor="la")
    draw.text((left + dp(88), y + dp(76)), "сбрасываются", font=font(13),
              fill=(58, 90, 66), anchor="la")
    switch(draw, left + width - dp(18) - dp(52), y + dp(22) + dp(8))
    draw.line([left + dp(18), y + dp(118), left + width - dp(18), y + dp(118)],
              fill=(200, 220, 205), width=dp(1))
    draw.text((left + dp(18), y + dp(140)), "Сегодня сброшено:", font=font(14),
              fill=(58, 90, 66), anchor="la")
    draw.text((left + dp(150), y + dp(140)), "8", font=font(14, True), fill=GREEN,
              anchor="la")
    draw.line([left + width // 2, y + dp(132), left + width // 2, y + dp(168)],
              fill=(200, 220, 205), width=dp(1))
    draw.text((left + width // 2 + dp(16), y + dp(140)), "Последний в 12:30",
              font=font(14), fill=(58, 90, 66), anchor="la")
    y += hero_h + dp(12)

    # Tiles
    gap = dp(8)
    tile_w = (width - gap * 2) // 3
    tile_h = dp(172)
    tile(img, draw, left, y, tile_w, tile_h,
         os.path.join(OUT, "Pause.png"), "Пауза", "Временно\nотключить")
    tile(img, draw, left + tile_w + gap, y, tile_w, tile_h,
         os.path.join(OUT, "Skip.png"), "Пропустить\nследующий", "Пропустить один\nзвонок")
    tile(img, draw, left + 2 * (tile_w + gap), y, tile_w, tile_h,
         os.path.join(OUT, "Schedule.png"), "Расписание", "Настроить время\nработы")
    y += tile_h + dp(24)

    # Recent blocks
    draw.text((left, y), "Последние блокировки", font=font(17, True), fill=TEXT, anchor="la")
    draw.text((left + width, y + dp(2)), "Весь журнал  ›", font=font(13, True),
              fill=ACCENT, anchor="ra")
    y += dp(32)

    rows = [("+7 962 467-93-64", "12:30"), ("+7 968 043-12-42", "12:18"),
            ("+7 912 600-72-40", "11:09")]
    row_h = dp(64)
    card_h = row_h * len(rows) + dp(12)
    rounded(draw, [left, y, left + width, y + card_h], dp(22), CARD, STROKE, dp(1))
    ry = y + dp(6)
    for index, (number, time) in enumerate(rows):
        cx = left + dp(22)
        draw.ellipse([cx, ry + dp(14), cx + dp(36), ry + dp(14) + dp(36)], fill=SOFT_RED)
        draw.line([cx + dp(11), ry + dp(25), cx + dp(25), ry + dp(39)], fill=RED, width=dp(2))
        draw.text((cx + dp(48), ry + dp(12)), number, font=font(16, True), fill=TEXT,
                  anchor="la")
        draw.text((cx + dp(48), ry + dp(34)), "Нет в контактах", font=font(12), fill=SECOND,
                  anchor="la")
        draw.text((left + width - dp(22), ry + dp(20)), time, font=font(13), fill=SECOND,
                  anchor="ra")
        ry += row_h
        if index < len(rows) - 1:
            draw.line([cx + dp(48), ry - dp(4), left + width - dp(22), ry - dp(4)],
                      fill=STROKE, width=dp(1))
    y += card_h + dp(12)

    # Week counters
    week_h = dp(96)
    rounded(draw, [left, y, left + width, y + week_h], dp(22), CARD, STROKE, dp(1))
    mid = left + width // 2
    draw.text((left + dp(22), y + dp(18)), "13", font=font(26, True), fill=TEXT, anchor="la")
    draw.text((left + dp(22), y + dp(56)), "сброшено за неделю", font=font(12), fill=SECOND,
              anchor="la")
    draw.text((mid + dp(22), y + dp(18)), "1", font=font(26, True), fill=TEXT, anchor="la")
    draw.text((mid + dp(22), y + dp(56)), "пропущено за неделю", font=font(12), fill=SECOND,
              anchor="la")
    draw.line([mid, y + dp(16), mid, y + week_h - dp(16)], fill=STROKE, width=dp(1))
    y += week_h + dp(16)

    # More block
    more_rows = [("stats", "Статистика", "сброшенные звонки по дням"),
                 ("person", "Белый список", "Номеров: 0"),
                 ("shield", "Правила", "Маркер номера, проверка контактов")]
    more_h = dp(64) * len(more_rows) + dp(8)
    rounded(draw, [left, y, left + width, y + more_h], dp(22), CARD, STROKE, dp(1))
    ry = y + dp(4)
    for index, (kind, title, subtitle) in enumerate(more_rows):
        cx = left + dp(22)
        draw.ellipse([cx, ry + dp(14), cx + dp(32), ry + dp(14) + dp(32)], fill=SOFT_BLUE)
        glyph(draw, kind, cx + dp(16), ry + dp(30), dp(18))
        draw.text((cx + dp(46), ry + dp(10)), title, font=font(15, True), fill=TEXT,
                  anchor="la")
        draw.text((cx + dp(46), ry + dp(32)), subtitle, font=font(12), fill=SECOND,
                  anchor="la")
        draw.text((left + width - dp(22), ry + dp(20)), "›", font=font(18), fill=SECOND,
                  anchor="ra")
        ry += dp(64)
        if index < len(more_rows) - 1:
            draw.line([cx + dp(46), ry - dp(4), left + width - dp(22), ry - dp(4)],
                      fill=STROKE, width=dp(1))

    img.save(os.path.join(OUT, "home.png"), optimize=True)
    print(os.path.join(OUT, "home.png"), img.size)


if __name__ == "__main__":
    main()
