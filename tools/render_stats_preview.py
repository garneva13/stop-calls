"""Preview of the statistics screen drawn with the same geometry as BarChartView.

    python tools/render_stats_preview.py
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
BAR = (240, 84, 84)
GOOD = (109, 213, 140)
SEG = (37, 99, 235)

VALUES = [5, 8, 12, 7, 9, 4, 2]
LABELS = ["Пн\n20 окт", "Вт\n21 окт", "Ср\n22 окт", "Чт\n23 окт",
          "Пт\n24 окт", "Сб\n25 окт", "Вс\n26 окт"]


def nice_step(max_value):
    raw = -(-max_value // 4)
    if raw <= 1:
        return 1
    if raw <= 2:
        return 2
    if raw <= 5:
        return 5
    step = 10
    while step < raw:
        step *= 10
    return step


def chart(draw, x, y, w, h):
    left = x + dp(30)
    right = x + w - dp(4)
    top = y + dp(16)
    bottom = y + h - dp(26)

    step = nice_step(max(VALUES))
    axis_max = step * 4
    for index in range(5):
        gy = bottom - (bottom - top) * index / 4
        draw.line([left, gy, right, gy], fill=STROKE, width=dp(1))
        draw.text((left - dp(6), gy), str(step * index), font=font(9), fill=SECOND,
                  anchor="rm")

    slot = (right - left) / len(VALUES)
    bar_w = min(dp(26), slot * 0.56)
    for index, value in enumerate(VALUES):
        cx = left + slot * index + slot / 2
        bar_h = (bottom - top) * value / axis_max
        draw.rounded_rectangle([cx - bar_w / 2, bottom - bar_h, cx + bar_w / 2, bottom],
                               radius=dp(5), fill=BAR)
        draw.text((cx, bottom - bar_h - dp(5)), str(value), font=font(10), fill=SECOND,
                  anchor="mb")
        for line_index, part in enumerate(LABELS[index].split("\n")):
            draw.text((cx, bottom + dp(12) + line_index * dp(11)), part, font=font(9),
                      fill=SECOND, anchor="ma")


def main():
    w, h = dp(360), dp(430)
    img = Image.new("RGB", (w, h), BG)
    draw = ImageDraw.Draw(img)

    margin = dp(20)
    card_x, card_y = margin, dp(56)
    card_w = w - margin * 2
    card_h = dp(360)
    draw.rounded_rectangle([card_x, card_y, card_x + card_w, card_y + card_h],
                           radius=dp(20), fill=CARD, outline=STROKE, width=dp(1))

    draw.text((margin, dp(16)), "Статистика", font=font(26, True), fill=TEXT, anchor="la")
    draw.text((margin, dp(44)), "Сброшенные звонки от неизвестных номеров", font=font(12),
              fill=SECOND, anchor="la")

    draw.text((card_x + dp(18), card_y + dp(16)), "Сброшенные звонки", font=font(13),
              fill=SECOND, anchor="la")
    draw.text((card_x + dp(18), card_y + dp(34)), "47", font=font(40, True), fill=TEXT,
              anchor="la")
    draw.text((card_x + dp(18) + dp(60), card_y + dp(58)), "+23%", font=font(14, True),
              fill=GOOD, anchor="la")
    draw.text((card_x + dp(18), card_y + dp(86)), "чем на прошлой неделе", font=font(11),
              fill=SECOND, anchor="la")

    chart(draw, card_x + dp(18), card_y + dp(108), card_w - dp(36), dp(210))

    img.save(os.path.join(DOCS, "stats-preview.png"), optimize=True)
    print(os.path.join(DOCS, "stats-preview.png"))


if __name__ == "__main__":
    main()
