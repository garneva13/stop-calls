"""Render preview images of the CallGuard UI from the real layout values.

This is a mock-up renderer, not a screenshot: it draws the same strings, colours,
sizes and paddings that are declared in res/layout and res/values, so the result
approximates what the app looks like on a Pixel-class device (448x997 dp).

    python tools/render_mockup.py
"""

import os
from PIL import Image, ImageDraw, ImageFont

SCALE = 3.0
W, H = 1344, 2992
OUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "docs")

BG = (242, 243, 245)
CARD = (255, 255, 255)
STROKE = (228, 231, 235)
TEXT = (20, 24, 29)
SECOND = (95, 107, 118)
GREEN = (30, 142, 62)
ACCENT = (11, 87, 208)
DISABLED_BG = (233, 234, 236)
DISABLED_FG = (154, 160, 166)

REG = r"C:\Windows\Fonts\segoeui.ttf"
BOLD = r"C:\Windows\Fonts\segoeuib.ttf"

_fonts = {}


def dp(value):
    return int(round(value * SCALE))


def font(size_sp, bold=False):
    key = (size_sp, bold)
    if key not in _fonts:
        _fonts[key] = ImageFont.truetype(BOLD if bold else REG, dp(size_sp))
    return _fonts[key]


def line_height(size_sp, factor=1.34):
    return int(round(dp(size_sp) * factor))


def wrap(draw, text, fnt, max_w):
    lines = []
    for paragraph in text.split("\n"):
        words = paragraph.split(" ")
        current = ""
        for word in words:
            trial = (current + " " + word).strip()
            if not current or draw.textlength(trial, font=fnt) <= max_w:
                current = trial
            else:
                lines.append(current)
                current = word
        lines.append(current)
    return lines


def paragraph_height(draw, text, size_sp, max_w, bold=False, factor=1.34):
    fnt = font(size_sp, bold)
    return len(wrap(draw, text, fnt, max_w)) * line_height(size_sp, factor)


def paragraph(draw, x, y, text, size_sp, color, max_w, bold=False, factor=1.34):
    fnt = font(size_sp, bold)
    lh = line_height(size_sp, factor)
    for line in wrap(draw, text, fnt, max_w):
        draw.text((x, y), line, font=fnt, fill=color, anchor="la")
        y += lh
    return y


def card(draw, x, y, w, h, radius=18):
    draw.rounded_rectangle([x, y, x + w, y + h], radius=dp(radius),
                           fill=CARD, outline=STROKE, width=max(1, int(round(SCALE))))


def button(draw, x, y, w, h, label, enabled=True, text_sp=13):
    radius = h // 2
    fill = ACCENT if enabled else DISABLED_BG
    fg = (255, 255, 255) if enabled else DISABLED_FG
    draw.rounded_rectangle([x, y, x + w, y + h], radius=radius, fill=fill)
    draw.text((x + w // 2, y + h // 2 - dp(1)), label, font=font(text_sp, True),
              fill=fg, anchor="mm")


def button_width(draw, label, text_sp=13, padding=12):
    return int(draw.textlength(label, font=font(text_sp, True))) + dp(padding) * 2


def switch(draw, x, y, on=True):
    tw, th = dp(52), dp(32)
    draw.rounded_rectangle([x, y, x + tw, y + th], radius=th // 2,
                           fill=ACCENT if on else (196, 201, 205))
    r = dp(12)
    cx = x + tw - dp(4) - r if on else x + dp(4) + r
    cy = y + th // 2
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(255, 255, 255))


def checkbox(draw, x, y):
    s = dp(20)
    draw.rounded_rectangle([x, y + dp(2), x + s, y + dp(2) + s], radius=dp(2),
                           outline=(110, 121, 132), width=dp(2))


def status_bar(draw):
    height = dp(32)
    draw.text((dp(20), dp(8)), "11:25", font=font(13, True), fill=TEXT, anchor="la")

    # signal bars
    x = W - dp(88)
    for i, bar in enumerate((5, 8, 11, 14)):
        draw.rounded_rectangle([x + dp(i * 5), dp(20) - dp(bar), x + dp(i * 5) + dp(3), dp(20)],
                               radius=dp(1), fill=TEXT)
    # wifi
    cx, cy = W - dp(49), dp(20)
    for radius in (4, 7.5, 11):
        box = [cx - dp(radius), cy - dp(radius), cx + dp(radius), cy + dp(radius)]
        draw.arc(box, start=225, end=315, fill=TEXT, width=dp(2))
    # battery
    bx, by = W - dp(34), dp(13)
    draw.rounded_rectangle([bx, by, bx + dp(19), by + dp(10)], radius=dp(3),
                           outline=TEXT, width=dp(1.5))
    draw.rectangle([bx + dp(2), by + dp(3), bx + dp(13), by + dp(7)], fill=TEXT)
    draw.rounded_rectangle([bx + dp(20), by + dp(3), bx + dp(22), by + dp(7)],
                           radius=dp(1), fill=TEXT)


def nav_pill(draw):
    # The window stops above the navigation bar; clip the scrolling content there.
    draw.rectangle([0, H - dp(24), W, H], fill=BG)
    draw.rounded_rectangle([W // 2 - dp(54), H - dp(14), W // 2 + dp(54), H - dp(10)],
                           radius=dp(2), fill=(60, 63, 68))


def main_screen():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    status_bar(draw)

    left = dp(20)
    width = W - dp(40)
    inner = width - dp(36)
    pad = dp(18)
    y = dp(32) + dp(24)

    y = paragraph(draw, left, y, "Стоп-звонки", 26, TEXT, width, bold=True)
    y += dp(4)
    y = paragraph(draw, left, y, "Сброс входящих с номеров, которых нет в контактах", 14, SECOND, width)

    # --- status card -----------------------------------------------------
    detail = "Звонки с номеров не из контактов сбрасываются."
    y += dp(16)
    height = (pad + line_height(21) + dp(2)
              + paragraph_height(draw, detail, 14, inner) + dp(14) + dp(48) + pad)
    card(draw, left, y, width, height)
    cy = y + pad
    cy = paragraph(draw, left + pad, cy, "Фильтр включён", 21, GREEN, inner, bold=True)
    cy += dp(2)
    cy = paragraph(draw, left + pad, cy, detail, 14, SECOND, inner)
    cy += dp(14)
    row_center = cy + dp(24)
    switch(draw, left + width - pad - dp(52), row_center - dp(16))
    draw.text((left + pad, row_center), "Фильтр входящих звонков",
              font=font(16), fill=TEXT, anchor="lm")
    y += height

    # --- requirements card ----------------------------------------------
    rows = ["Доступ к контактам: есть, номеров — 624",
            "Роль «Фильтрация вызовов»: назначена"]
    bw = button_width(draw, "Готово")
    row_heights = [max(dp(40), paragraph_height(draw, r, 14, inner - bw - dp(12))) for r in rows]
    y += dp(16)
    height = (pad + line_height(16) + dp(12) + row_heights[0]
              + dp(10) + row_heights[1] + pad)
    card(draw, left, y, width, height)
    cy = y + pad
    cy = paragraph(draw, left + pad, cy, "Что нужно для работы", 16, TEXT, inner, bold=True)
    for index, label in enumerate(rows):
        cy += dp(12) if index == 0 else dp(10)
        text_h = paragraph_height(draw, label, 14, inner - bw - dp(12))
        paragraph(draw, left + pad, cy + (row_heights[index] - text_h) // 2,
                  label, 14, TEXT, inner - bw - dp(12))
        button(draw, left + width - pad - bw,
               cy + (row_heights[index] - dp(40)) // 2, bw, dp(40), "Готово", enabled=False)
        cy += row_heights[index]
    y += height

    # --- pause card ------------------------------------------------------
    hint = ("Включите паузу или пропустите следующий звонок, если ждёте курьера, "
            "врача или другой незнакомый номер.")
    state = ("Пауза и пропуск звонка отключают фильтр на время — например, когда "
             "ждёте курьера.")
    y += dp(16)
    height = (pad + line_height(16) + dp(6) + paragraph_height(draw, hint, 14, inner)
              + dp(12) + dp(40) + dp(6) + paragraph_height(draw, state, 14, inner)
              + dp(10) + dp(40) + pad)
    card(draw, left, y, width, height)
    cy = y + pad
    cy = paragraph(draw, left + pad, cy, "Временное отключение", 16, TEXT, inner, bold=True)
    cy += dp(6)
    cy = paragraph(draw, left + pad, cy, hint, 14, SECOND, inner)
    cy += dp(12)
    half = (inner - dp(8)) // 2
    button(draw, left + pad, cy, half, dp(40), "Пауза на 1 час")
    button(draw, left + pad + half + dp(8), cy, half, dp(40), "Пропустить следующий")
    cy += dp(40) + dp(6)
    cy = paragraph(draw, left + pad, cy, state, 14, SECOND, inner)
    cy += dp(10)
    draw.text((left + pad, cy + dp(20)), "Расписание: будни, 09:00–18:00",
              font=font(14), fill=TEXT, anchor="lm")
    bw2 = button_width(draw, "Настроить")
    button(draw, left + width - pad - bw2, cy, bw2, dp(40), "Настроить")
    y += height

    # --- counters card ---------------------------------------------------
    y += dp(16)
    height = pad + line_height(16) + dp(12) + dp(40) + dp(8) + dp(40) + pad
    card(draw, left, y, width, height)
    cy = y + pad
    cy = paragraph(draw, left + pad, cy, "Заблокировано звонков: 4", 16, TEXT, inner, bold=True)
    cy += dp(12)
    button(draw, left + pad, cy, half, dp(40), "Журнал (4)")
    button(draw, left + pad + half + dp(8), cy, half, dp(40), "Разрешённые (1)")
    cy += dp(40) + dp(8)
    button(draw, left + pad, cy, inner, dp(40), "Диагностика")
    y += height

    # --- rules card ------------------------------------------------------
    check_label = "Блокировать звонки без маркера проверки номера"
    rules_hint = ("Маркер (STIR/SHAKEN) подтверждает номер на стороне оператора. "
                  "В России он почти не передаётся, поэтому включать не рекомендуется: "
                  "под блокировку попадут и ваши контакты.")
    y += dp(16)
    height = (pad + line_height(16) + dp(10)
              + paragraph_height(draw, check_label, 15, inner - dp(32))
              + dp(6) + paragraph_height(draw, rules_hint, 14, inner) + pad)
    card(draw, left, y, width, height)
    cy = y + pad
    cy = paragraph(draw, left + pad, cy, "Правила", 16, TEXT, inner, bold=True)
    cy += dp(10)
    checkbox(draw, left + pad, cy)
    cy = paragraph(draw, left + pad + dp(32), cy, check_label, 15, TEXT, inner - dp(32))
    cy += dp(6)
    paragraph(draw, left + pad, cy, rules_hint, 14, SECOND, inner)
    y += height

    nav_pill(draw)
    return img


def log_screen():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    status_bar(draw)

    left = dp(20)
    width = W - dp(40)

    top = dp(32) + dp(20)
    draw.text((left, top), "Журнал блокировок", font=font(22, True), fill=TEXT, anchor="la")
    bw = button_width(draw, "Очистить")
    button(draw, W - dp(20) - bw, top + dp(4), bw, dp(40), "Очистить")

    y = top + dp(40) + dp(6)
    draw.text((left, y), "Записей: 4", font=font(13), fill=SECOND, anchor="la")
    y += dp(8) + dp(24)

    card(draw, left, y, width, H - y - dp(24), radius=18)

    rows = [
        ("+7 900 000-00-01", "24.10.2026, 11:02 · маркер: нет", "Нет в контактах"),
        ("+7 800 000-00-02", "24.10.2026, 10:47 · маркер: нет", "Нет в контактах"),
        ("+1 (415) 555-0100", "23.10.2026, 21:15 · маркер: не пройден",
         "Нет в контактах, номер подтверждён оператором"),
        ("+7 495 000-00-03", "23.10.2026, 18:30 · маркер: нет", "Нет в контактах"),
    ]

    row_y = y
    for index, (number, meta, reason) in enumerate(rows):
        row_y += dp(14)
        draw.text((left + dp(18), row_y), number, font=font(17, True), fill=TEXT, anchor="la")
        row_y += line_height(17)
        row_y += dp(2)
        draw.text((left + dp(18), row_y), meta, font=font(13), fill=SECOND, anchor="la")
        row_y += line_height(13)
        row_y += dp(2)
        row_y = paragraph(draw, left + dp(18), row_y, reason, 13, SECOND, width - dp(36))
        row_y += dp(14)
        if index < len(rows) - 1:
            draw.line([left, row_y, left + width, row_y], fill=STROKE, width=1)

    nav_pill(draw)
    return img


def diagnostics_screen():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    status_bar(draw)

    left = dp(20)
    width = W - dp(40)
    inner = width - dp(36)
    pad = dp(18)
    y = dp(32) + dp(20)

    y = paragraph(draw, left, y, "Диагностика", 22, TEXT, width, bold=True)
    y += dp(4)
    y = paragraph(draw, left, y,
                  "Проверьте, доходят ли звонки до приложения и что оно с ними делает.",
                  13, SECOND, width)

    # --- state -----------------------------------------------------------
    state = ("Роль фильтрации: назначена\n"
             "Доступ к контактам: есть, номеров — 624\n"
             "Фильтр: включён\n"
             "Пауза: нет\n"
             "Расписание: будни, 09:00–18:00 — не действует\n"
             "Событий: 4, из них сброшено: 3")
    y += dp(16)
    height = (pad + line_height(16) + dp(8)
              + paragraph_height(draw, state, 14, inner, factor=1.55) + pad)
    card(draw, left, y, width, height)
    cy = y + pad
    cy = paragraph(draw, left + pad, cy, "Состояние", 16, TEXT, inner, bold=True)
    cy += dp(8)
    paragraph(draw, left + pad, cy, state, 14, SECOND, inner, factor=1.55)
    y += height

    # --- number check ----------------------------------------------------
    check = ("Номер: +7 900 000-00-01 (79000000001)\n"
             "В контактах: нет\n"
             "В белом списке: нет\n"
             "Фильтр сбросил бы этот звонок")
    y += dp(16)
    height = (pad + line_height(16) + dp(10) + dp(26) + dp(16) + dp(40) + dp(8)
              + paragraph_height(draw, check, 13, inner, factor=1.5) + pad)
    card(draw, left, y, width, height)
    cy = y + pad
    cy = paragraph(draw, left + pad, cy, "Проверка номера", 16, TEXT, inner, bold=True)
    cy += dp(10)
    draw.text((left + pad, cy), "Номер телефона", font=font(16), fill=(150, 155, 160), anchor="la")
    draw.line([left + pad, cy + dp(26), left + width - pad, cy + dp(26)], fill=STROKE, width=dp(1))
    cy += dp(26) + dp(16)
    button(draw, left + pad, cy, button_width(draw, "Проверить"), dp(40), "Проверить")
    cy += dp(40) + dp(8)
    paragraph(draw, left + pad, cy, check, 13, SECOND, inner, factor=1.5)
    y += height

    # --- events ----------------------------------------------------------
    events = ("24.10 11:02 · +7 900 000-00-01 — сброшен: нет в контактах\n"
              "24.10 10:47 · +7 800 000-00-02 — сброшен: нет в контактах\n"
              "23.10 21:15 · +1 (415) 555-0100 — сброшен: нет в контактах\n"
              "23.10 18:30 · +7 916 123-45-67 — пропущен: номер в контактах")
    y += dp(16)
    height = (pad + line_height(16) + dp(8)
              + paragraph_height(draw, events, 13, inner, factor=1.6) + dp(12) + dp(40) + pad)
    card(draw, left, y, width, height)
    cy = y + pad
    cy = paragraph(draw, left + pad, cy, "Последние звонки", 16, TEXT, inner, bold=True)
    cy += dp(8)
    cy = paragraph(draw, left + pad, cy, events, 13, SECOND, inner, factor=1.6)
    cy += dp(12)
    button(draw, left + pad, cy, button_width(draw, "Очистить историю"), dp(40), "Очистить историю")
    y += height

    # --- role help -------------------------------------------------------
    role_help = ("В системном окне «Caller ID & spam app» выберите «Стоп-звонки». "
                 "Если выбрать «Телефон», роль останется у Google Phone и фильтр "
                 "не заработает.\n\nВручную: Настройки → Приложения → Приложения по "
                 "умолчанию → Caller ID & spam app → «Стоп-звонки».")
    y += dp(16)
    height = (pad + line_height(16) + dp(6)
              + paragraph_height(draw, role_help, 14, inner) + dp(12) + dp(40) + pad)
    card(draw, left, y, width, height)
    cy = y + pad
    cy = paragraph(draw, left + pad, cy, "Если роль не назначается", 16, TEXT, inner, bold=True)
    cy += dp(6)
    cy = paragraph(draw, left + pad, cy, role_help, 14, SECOND, inner)
    cy += dp(12)
    button(draw, left + pad, cy, button_width(draw, "Открыть настройки"), dp(40), "Открыть настройки")
    y += height

    nav_pill(draw)
    return img


def schedule_screen():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    status_bar(draw)

    left = dp(20)
    width = W - dp(40)
    inner = width - dp(36)
    pad = dp(18)
    y = dp(32) + dp(20)

    y = paragraph(draw, left, y, "Пауза по расписанию", 22, TEXT, width, bold=True)
    y += dp(4)
    y = paragraph(draw, left, y,
                  "В выбранные дни и часы фильтр не блокирует звонки с незнакомых номеров — "
                  "удобно, если ждёте доставку или звонок из больницы в рабочее время.",
                  13, SECOND, width)

    # --- switch ----------------------------------------------------------
    y += dp(16)
    height = pad + dp(48) + pad
    card(draw, left, y, width, height)
    draw.text((left + pad, y + pad + dp(24)), "Включить расписание",
              font=font(16), fill=TEXT, anchor="lm")
    switch(draw, left + width - pad - dp(52), y + pad + dp(8))
    y += height

    # --- days and time ---------------------------------------------------
    y += dp(16)
    chips_h = dp(40)
    height = (pad + line_height(16) + dp(10) + chips_h + dp(18) + line_height(16)
              + dp(10) + dp(40) + dp(8) + dp(40) + pad)
    card(draw, left, y, width, height)
    cy = y + pad
    cy = paragraph(draw, left + pad, cy, "Дни недели", 16, TEXT, inner, bold=True)
    cy += dp(10)

    gap = dp(4)
    chip_w = (inner - gap * 7) // 7
    selected = [True, True, True, True, True, False, False]
    for index, name in enumerate(("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")):
        cx = left + pad + index * (chip_w + gap)
        fill = ACCENT if selected[index] else (238, 240, 243)
        draw.rounded_rectangle([cx, cy, cx + chip_w, cy + chips_h], radius=dp(12), fill=fill)
        draw.text((cx + chip_w // 2, cy + chips_h // 2), name,
                  font=font(13, True), fill=(255, 255, 255) if selected[index] else SECOND,
                  anchor="mm")
    cy += chips_h + dp(18)

    cy = paragraph(draw, left + pad, cy, "Время", 16, TEXT, inner, bold=True)
    cy += dp(10)
    for label, value in (("Начало", "09:00"), ("Конец", "18:00")):
        draw.text((left + pad, cy + dp(20)), label, font=font(15), fill=TEXT, anchor="lm")
        bw = button_width(draw, value)
        button(draw, left + width - pad - bw, cy, bw, dp(40), value)
        cy += dp(40) + dp(8)
    y += height

    # --- summary ---------------------------------------------------------
    y += dp(16)
    note = ("Если время окончания меньше времени начала, пауза переходит через полночь — "
            "например, 22:00–08:00.")
    height = (pad + line_height(15) + dp(4) + line_height(14) + dp(6)
              + paragraph_height(draw, note, 14, inner) + pad)
    card(draw, left, y, width, height)
    cy = y + pad
    cy = paragraph(draw, left + pad, cy, "Пауза: будни, 09:00–18:00", 15, TEXT, inner)
    cy += dp(4)
    cy = paragraph(draw, left + pad, cy, "Сейчас: пауза не действует", 14, SECOND, inner)
    cy += dp(6)
    paragraph(draw, left + pad, cy, note, 14, SECOND, inner)
    y += height

    nav_pill(draw)
    return img


def main():
    if not os.path.isdir(OUT_DIR):
        os.makedirs(OUT_DIR)
    screens = (("mockup-main.png", main_screen),
               ("mockup-log.png", log_screen),
               ("mockup-diagnostics.png", diagnostics_screen),
               ("mockup-schedule.png", schedule_screen))
    for name, render in screens:
        path = os.path.join(OUT_DIR, name)
        render().save(path)
        print(path)


if __name__ == "__main__":
    main()
