"""Render month/year/day charts to check label collisions before a release.

    python tools/render_stats_periods.py
"""

import os

from PIL import Image, ImageDraw

from render_mockup import BG, CARD, SECOND, STROKE, TEXT, W, dp, font
from render_stats_screen import chart

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "docs")


def month_labels():
    labels = [None] * 30
    for index in range(30):
        if index % 5 == 0 or index == 29:
            labels[index] = f"{index + 1} окт"
    return labels


def year_labels():
    names = ["янв", "фев", "мар", "апр", "май", "июн",
             "июл", "авг", "сен", "окт", "ноя", "дек"]
    return list(names)


def day_labels():
    return [f"{hour:02d}:00" if hour % 6 == 0 else None for hour in range(24)]


def card(draw, x, y, w, h, title, values, labels, show_numbers):
    draw.rounded_rectangle([x, y, x + w, y + h], radius=dp(22), fill=CARD,
                           outline=STROKE, width=dp(1))
    draw.text((x + dp(18), y + dp(16)), title, font=font(15, True), fill=TEXT, anchor="la")
    chart(draw, x + dp(18), y + dp(48), w - dp(36), h - dp(60), values, labels, show_numbers)


def main():
    h = dp(900)
    img = Image.new("RGB", (W, h), BG)
    draw = ImageDraw.Draw(img)
    margin = dp(20)
    width = W - margin * 2

    y = dp(24)
    draw.text((margin, y), "Проверка подписей графиков", font=font(20, True), fill=TEXT,
              anchor="la")
    y += dp(40)

    month_values = [2, 1, 3, 0, 4, 2, 1, 0, 3, 5, 2, 4, 1, 0, 2, 3, 1, 4, 2, 0,
                    3, 2, 5, 1, 0, 2, 4, 1, 3, 2]
    card(draw, margin, y, width, dp(260), "Месяц: 30 дней", month_values, month_labels(), True)
    y += dp(276)

    year_values = [3, 5, 2, 4, 6, 1, 0, 2, 7, 4, 3, 5]
    card(draw, margin, y, width, dp(260), "Год: 12 месяцев", year_values, year_labels(), True)
    y += dp(276)

    day_values = [0, 0, 1, 0, 0, 0, 1, 2, 3, 5, 8, 10, 13, 11, 6, 5, 6, 7, 4, 3, 2, 1, 0, 0]
    card(draw, margin, y, width, dp(240), "День: 24 часа", day_values, day_labels(), False)

    img.save(os.path.join(OUT, "check-periods.png"), optimize=True)
    print(os.path.join(OUT, "check-periods.png"), img.size)


if __name__ == "__main__":
    main()
