"""Render the statistics screen (light theme) for the project page.

    python tools/render_stats_screen.py
"""

import os

from PIL import Image, ImageDraw

from render_mockup import (
    ACCENT, BG, CARD, GREEN, SECOND, STROKE, TEXT, W,
    dp, font, status_bar,
)

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "docs")
H = 2600
BAR = (217, 58, 58)
SEG_BG = (238, 240, 243)

WEEK = [5, 8, 12, 7, 9, 4, 2]
WEEK_LABELS = ["Пн\n20 окт", "Вт\n21 окт", "Ср\n22 окт", "Чт\n23 окт",
               "Пт\n24 окт", "Сб\n25 окт", "Вс\n26 окт"]
HOURS = [0, 0, 1, 0, 0, 0, 1, 2, 3, 5, 8, 10, 13, 11, 6, 5, 6, 7, 4, 3, 2, 1, 0, 0]


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


def chart(draw, x, y, w, h, values, labels, show_numbers):
    left = x + dp(34)
    right = x + w
    top = y + (dp(16) if show_numbers else dp(6))
    bottom = y + h - (dp(30) if labels else dp(6))

    step = nice_step(max(values))
    axis_max = step * 4
    for index in range(5):
        gy = bottom - (bottom - top) * index / 4
        draw.line([left, gy, right, gy], fill=STROKE, width=dp(1))
        draw.text((left - dp(8), gy), str(step * index), font=font(9), fill=SECOND,
                  anchor="rm")

    slot = (right - left) / len(values)
    bar_w = min(dp(26), slot * 0.56)
    for index, value in enumerate(values):
        cx = left + slot * index + slot / 2
        bar_h = (bottom - top) * value / axis_max
        if bar_h > 0:
            draw.rounded_rectangle([cx - bar_w / 2, bottom - bar_h, cx + bar_w / 2, bottom],
                                   radius=dp(5), fill=BAR)
        if show_numbers and value > 0:
            draw.text((cx, bottom - bar_h - dp(5)), str(value), font=font(10), fill=SECOND,
                      anchor="mb")
        label = labels[index] if labels and index < len(labels) else None
        if label:
            for line_index, part in enumerate(label.split("\n")):
                draw.text((cx, bottom + dp(14) + line_index * dp(11)), part, font=font(9),
                          fill=SECOND, anchor="ma")


def main():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    status_bar(draw)

    left = dp(20)
    width = W - dp(40)
    inner = width - dp(36)
    pad = dp(18)
    y = dp(52)

    draw.text((left, y), "Статистика", font=font(26, True), fill=TEXT, anchor="la")
    draw.text((left, y + dp(40)), "Сброшенные звонки от неизвестных номеров",
              font=font(13), fill=SECOND, anchor="la")
    y += dp(76)

    # Period segments
    seg_h = dp(46)
    draw.rounded_rectangle([left, y, left + width, y + seg_h], radius=dp(24),
                           fill=SEG_BG, outline=STROKE, width=dp(1))
    names = ("День", "Неделя", "Месяц", "Год")
    seg_w = (width - dp(8)) // 4
    for index, name in enumerate(names):
        sx = left + dp(4) + index * seg_w
        active = index == 1
        if active:
            draw.rounded_rectangle([sx, y + dp(4), sx + seg_w - dp(2), y + seg_h - dp(4)],
                                   radius=dp(20), fill=ACCENT)
        draw.text((sx + (seg_w - dp(2)) // 2, y + seg_h // 2), name, font=font(13, True),
                  fill=(255, 255, 255) if active else SECOND, anchor="mm")
    y += seg_h + dp(16)

    # Blocked card
    card_h = dp(430)
    draw.rounded_rectangle([left, y, left + width, y + card_h], radius=dp(22), fill=CARD,
                           outline=STROKE, width=dp(1))
    draw.text((left + pad, y + pad), "Сброшенные звонки", font=font(13), fill=SECOND,
              anchor="la")
    draw.text((left + pad, y + pad + dp(20)), "47", font=font(40, True), fill=TEXT,
              anchor="la")
    draw.text((left + pad + dp(64), y + pad + dp(46)), "+23%", font=font(14, True),
              fill=BAR, anchor="la")
    draw.text((left + pad, y + pad + dp(74)), "чем на прошлой неделе", font=font(11),
              fill=SECOND, anchor="la")
    chart(draw, left + pad, y + dp(112), inner, dp(280), WEEK, WEEK_LABELS, True)
    y += card_h + dp(16)

    # Hours card
    hours_h = dp(236)
    draw.rounded_rectangle([left, y, left + width, y + hours_h], radius=dp(22), fill=CARD,
                           outline=STROKE, width=dp(1))
    draw.text((left + pad, y + pad), "Время звонков", font=font(16, True), fill=TEXT,
              anchor="la")
    draw.text((left + pad, y + pad + dp(24)), "Когда чаще всего звонят", font=font(11),
              fill=SECOND, anchor="la")
    hour_labels = [f"{hour:02d}:00" if hour % 6 == 0 else None for hour in range(24)]
    chart(draw, left + pad, y + dp(64), inner, dp(150), HOURS, hour_labels, False)

    img.save(os.path.join(OUT, "stats-screen.png"), optimize=True)
    print(os.path.join(OUT, "stats-screen.png"), img.size)


if __name__ == "__main__":
    main()
