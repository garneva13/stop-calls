"""Render three UI directions for CallGuard plus a redesigned journal.

    cd tools && python render_variants.py

Reuses the primitives from render_mockup.py (same dp grid, same fonts) so the
variants stay comparable with the current screens.
"""

import os

from PIL import Image, ImageDraw

from render_mockup import (
    ACCENT, BG, CARD, H, SECOND, STROKE, TEXT, W,
    dp, font, nav_pill, status_bar,
)

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "docs")

# Material 3 blue palette (what dynamic colour looks like on a Pixel)
M3_SURFACE = (253, 251, 255)
M3_CONTAINER = (241, 243, 249)
M3_PRIMARY = (11, 87, 208)
M3_PRIMARY_CONTAINER = (211, 227, 253)
M3_ON_PRIMARY_CONTAINER = (4, 30, 73)
M3_VARIANT = (68, 71, 78)
M3_SECONDARY_CONTAINER = (224, 232, 246)

GREEN_TINT = (233, 244, 236)
GREEN_DARK = (27, 94, 32)
GREEN_SOFT = (58, 90, 66)
RED = (179, 38, 30)
RED_TINT = (253, 232, 231)
AMBER = (176, 96, 0)
AMBER_TINT = (255, 243, 224)


# ----------------------------------------------------------------- icons


def glyph(draw, cx, cy, size, kind, color):
    s = size / 2.0
    w = max(2, dp(1.7))
    if kind == "shield":
        draw.polygon([
            (cx - s, cy - s * 0.85), (cx + s, cy - s * 0.85), (cx + s, cy + s * 0.15),
            (cx + s * 0.55, cy + s * 0.62), (cx, cy + s),
            (cx - s * 0.55, cy + s * 0.62), (cx - s, cy + s * 0.15),
        ], outline=color, width=w)
    elif kind == "clock":
        draw.ellipse([cx - s, cy - s, cx + s, cy + s], outline=color, width=w)
        draw.line([cx, cy, cx, cy - s * 0.5], fill=color, width=w)
        draw.line([cx, cy, cx + s * 0.45, cy], fill=color, width=w)
    elif kind == "calendar":
        draw.rounded_rectangle([cx - s, cy - s * 0.8, cx + s, cy + s], radius=dp(2),
                               outline=color, width=w)
        draw.line([cx - s, cy - s * 0.25, cx + s, cy - s * 0.25], fill=color, width=w)
        draw.line([cx - s * 0.45, cy - s, cx - s * 0.45, cy - s * 0.55], fill=color, width=w)
        draw.line([cx + s * 0.45, cy - s, cx + s * 0.45, cy - s * 0.55], fill=color, width=w)
    elif kind == "list":
        for offset in (-0.55, 0.0, 0.55):
            draw.ellipse([cx - s, cy + s * offset - dp(1.3), cx - s + dp(2.6),
                          cy + s * offset + dp(1.3)], fill=color)
            draw.line([cx - s * 0.45, cy + s * offset, cx + s, cy + s * offset],
                      fill=color, width=w)
    elif kind == "home":
        draw.line([cx - s, cy - s * 0.05, cx, cy - s * 0.75], fill=color, width=w)
        draw.line([cx, cy - s * 0.75, cx + s, cy - s * 0.05], fill=color, width=w)
        draw.line([cx - s * 0.62, cy + s * 0.05, cx - s * 0.62, cy + s], fill=color, width=w)
        draw.line([cx + s * 0.62, cy + s * 0.05, cx + s * 0.62, cy + s], fill=color, width=w)
        draw.line([cx - s * 0.62, cy + s, cx + s * 0.62, cy + s], fill=color, width=w)
    elif kind == "tune":
        for offset, knob in ((-0.6, -0.3), (0.0, 0.35), (0.6, -0.45)):
            draw.line([cx - s, cy + s * offset, cx + s, cy + s * offset], fill=color, width=w)
            draw.ellipse([cx + s * knob - dp(2.2), cy + s * offset - dp(2.2),
                          cx + s * knob + dp(2.2), cy + s * offset + dp(2.2)], fill=color)
    elif kind == "book":
        draw.rounded_rectangle([cx - s * 0.85, cy - s * 0.85, cx + s * 0.85, cy + s * 0.85],
                               radius=dp(2), outline=color, width=w)
        draw.line([cx, cy - s * 0.75, cx, cy + s * 0.75], fill=color, width=w)
    elif kind == "ban":
        draw.ellipse([cx - s, cy - s, cx + s, cy + s], outline=color, width=w)
        draw.line([cx - s * 0.7, cy - s * 0.7, cx + s * 0.7, cy + s * 0.7],
                  fill=color, width=w)
    elif kind == "check":
        draw.line([cx - s * 0.8, cy, cx - s * 0.2, cy + s * 0.6], fill=color, width=w)
        draw.line([cx - s * 0.2, cy + s * 0.6, cx + s * 0.85, cy - s * 0.6],
                  fill=color, width=w)
    elif kind == "person":
        draw.ellipse([cx - s * 0.42, cy - s, cx + s * 0.42, cy - s * 0.1],
                     outline=color, width=w)
        draw.arc([cx - s * 0.95, cy - s * 0.1, cx + s * 0.95, cy + s * 1.3], 180, 360,
                 fill=color, width=w)
    elif kind == "info":
        draw.ellipse([cx - s, cy - s, cx + s, cy + s], outline=color, width=w)
        draw.line([cx, cy - s * 0.1, cx, cy + s * 0.55], fill=color, width=w)
        draw.ellipse([cx - dp(1.3), cy - s * 0.5, cx + dp(1.3), cy - s * 0.5 + dp(2.6)],
                     fill=color)
    elif kind == "help":
        draw.ellipse([cx - s, cy - s, cx + s, cy + s], outline=color, width=w)
        draw.arc([cx - s * 0.4, cy - s * 0.55, cx + s * 0.4, cy - s * 0.05], 180, 400,
                 fill=color, width=w)
        draw.line([cx, cy + s * 0.05, cx, cy + s * 0.2], fill=color, width=w)
        draw.ellipse([cx - dp(1.3), cy + s * 0.42, cx + dp(1.3), cy + s * 0.42 + dp(2.6)],
                     fill=color)


def icon_circle(draw, cx, cy, radius, tint, kind, color):
    draw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], fill=tint)
    glyph(draw, cx, cy, radius * 1.05, kind, color)


# ----------------------------------------------------------------- chips


def chip(draw, x, y, label, selected=False, height=44, size=14, icon=None):
    text_w = draw.textlength(label, font=font(size, True))
    width = int(text_w + dp(28) + (dp(22) if icon else 0))
    radius = height // 2
    if selected:
        draw.rounded_rectangle([x, y, x + width, y + height], radius=radius, fill=M3_PRIMARY)
        color = (255, 255, 255)
    else:
        draw.rounded_rectangle([x, y, x + width, y + height], radius=radius,
                               fill=CARD, outline=STROKE, width=max(1, int(dp(1))))
        color = TEXT
    text_x = x + (width - text_w) // 2
    if icon:
        glyph(draw, x + dp(18), y + height // 2, dp(16), icon, color)
        text_x = x + dp(30) + (width - dp(30) - text_w) // 2
    draw.text((text_x, y + height // 2), label, font=font(size, True), fill=color, anchor="lm")
    return width


def toggle_pill(draw, x, y, on=True, width=64, height=40):
    draw.rounded_rectangle([x, y, x + width, y + height], radius=height // 2,
                           fill=M3_PRIMARY if on else (203, 206, 210))
    r = height // 2 - dp(4)
    cx = x + width - dp(4) - r if on else x + dp(4) + r
    draw.ellipse([cx - r, y + height // 2 - r, cx + r, y + height // 2 + r], fill=(255, 255, 255))


def chevron(draw, x, cy, color, size=16):
    s = dp(size) / 2
    draw.line([x, cy - s * 0.55, x + s * 0.55, cy], fill=color, width=max(2, dp(1.6)))
    draw.line([x + s * 0.55, cy, x, cy + s * 0.55], fill=color, width=max(2, dp(1.6)))


def link_row(draw, x, y, width, height, kind, title, subtitle, last=False):
    icon_circle(draw, x + dp(34), y + height // 2, dp(15), M3_PRIMARY_CONTAINER, kind,
                M3_ON_PRIMARY_CONTAINER)
    draw.text((x + dp(62), y + height // 2 - (dp(18) if subtitle else dp(8))), title,
              font=font(15, True), fill=TEXT, anchor="la")
    if subtitle:
        draw.text((x + dp(62), y + height // 2 + dp(3)), subtitle, font=font(12), fill=SECOND,
                  anchor="la")
    chevron(draw, x + width - dp(20), y + height // 2, M3_VARIANT)
    if not last:
        draw.line([x + dp(62), y + height, x + width, y + height], fill=STROKE, width=1)


# ----------------------------------------------------------- variant A


def variant_a():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    status_bar(draw)

    left = dp(20)
    width = W - dp(40)
    y = dp(32) + dp(16)

    draw.text((left, y), "Стоп-звонки", font=font(20, True), fill=TEXT, anchor="la")
    glyph(draw, left + width - dp(10), y + dp(11), dp(22), "tune", SECOND)
    y += dp(22) + dp(16)

    # hero
    hero_h = dp(164)
    draw.rounded_rectangle([left, y, left + width, y + hero_h], radius=dp(24), fill=GREEN_TINT)
    draw.text((left + dp(20), y + dp(20)), "Фильтр включён", font=font(26, True), fill=GREEN_DARK,
              anchor="la")
    toggle_pill(draw, left + width - dp(20) - dp(64), y + dp(18))
    draw.text((left + dp(20), y + dp(58)), "Звонки не из контактов сбрасываются",
              font=font(14), fill=GREEN_SOFT, anchor="la")
    draw.line([left + dp(20), y + dp(92), left + width - dp(20), y + dp(92)],
              fill=(196, 222, 203), width=1)
    draw.text((left + dp(20), y + dp(112)), "Сегодня сброшено: 3", font=font(13, True),
              fill=GREEN_DARK, anchor="la")
    last = "последний в 11:02"
    lw = draw.textlength(last, font=font(12))
    draw.text((left + width - dp(20) - lw, y + dp(113)), last, font=font(12), fill=GREEN_SOFT,
              anchor="la")
    y += hero_h + dp(14)

    # quick actions
    chip_x = left
    for label, icon in (("Пауза 1 ч", "clock"), ("Пропустить", "check"),
                        ("Расписание", "calendar")):
        chip_x += chip(draw, chip_x, y, label, icon=icon) + dp(8)
    y += dp(44) + dp(26)

    # recent blocks
    draw.text((left, y + dp(10)), "Последние блокировки", font=font(16, True), fill=TEXT,
              anchor="la")
    link = "Весь журнал"
    link_w = draw.textlength(link, font=font(13, True))
    draw.text((left + width - link_w - dp(18), y + dp(12)), link, font=font(13, True), fill=ACCENT,
              anchor="la")
    chevron(draw, left + width - dp(14), y + dp(19), ACCENT, 14)
    y += dp(38)

    rows = (("+7 900 000-00-01", "11:02"), ("+7 800 000-00-02", "10:47"),
            ("+1 (415) 555-0100", "вчера"))
    row_h = dp(70)
    draw.rounded_rectangle([left, y, left + width, y + row_h * len(rows)], radius=dp(20),
                           fill=CARD, outline=STROKE, width=max(1, dp(1)))
    for index, (number, time) in enumerate(rows):
        cy = y + index * row_h
        icon_circle(draw, left + dp(36), cy + row_h // 2, dp(15), RED_TINT, "ban", RED)
        draw.text((left + dp(62), cy + dp(18)), number, font=font(15, True), fill=TEXT,
                  anchor="la")
        draw.text((left + dp(62), cy + dp(40)), "нет в контактах", font=font(12), fill=SECOND,
                  anchor="la")
        draw.text((left + width - dp(18), cy + dp(23)), time, font=font(13), fill=SECOND,
                  anchor="ra")
        if index < len(rows) - 1:
            draw.line([left + dp(62), cy + row_h, left + width, cy + row_h], fill=STROKE, width=1)
    y += row_h * len(rows) + dp(14)

    # stats
    stats_h = dp(84)
    draw.rounded_rectangle([left, y, left + width, y + stats_h], radius=dp(20), fill=CARD,
                           outline=STROKE, width=max(1, dp(1)))
    half = width // 2
    for index, (value, label) in enumerate((("12", "сброшено за неделю"),
                                            ("4", "пропущено за неделю"))):
        bx = left + index * half
        draw.text((bx + dp(20), y + dp(18)), value, font=font(24, True), fill=TEXT, anchor="la")
        draw.text((bx + dp(20), y + dp(52)), label, font=font(12), fill=SECOND, anchor="la")
        if index == 1:
            draw.line([left + half, y + dp(16), left + half, y + stats_h - dp(16)], fill=STROKE,
                      width=1)
    y += stats_h + dp(14)

    # more links
    more = (("person", "Белый список", "1 номер"), ("shield", "Правила", "маркер, контакты"),
            ("info", "Диагностика", "роль и последние звонки"))
    draw.rounded_rectangle([left, y, left + width, y + dp(64) * len(more)], radius=dp(20),
                           fill=CARD, outline=STROKE, width=max(1, dp(1)))
    for index, (kind, title, subtitle) in enumerate(more):
        link_row(draw, left, y + index * dp(64), width, dp(64), kind, title, subtitle,
                 last=index == len(more) - 1)
    nav_pill(draw)
    return img


# ----------------------------------------------------------- variant B


def variant_b():
    img = Image.new("RGB", (W, H), M3_SURFACE)
    draw = ImageDraw.Draw(img)

    left = dp(20)
    width = W - dp(40)
    card_top = dp(152)
    header_h = card_top + dp(16)

    draw.rectangle([0, 0, W, header_h], fill=M3_PRIMARY_CONTAINER)
    status_bar_colored(draw, M3_ON_PRIMARY_CONTAINER)

    draw.text((left, dp(44)), "Стоп-звонки", font=font(28, True), fill=M3_ON_PRIMARY_CONTAINER,
              anchor="la")
    draw.text((left, dp(86)), "Спам и мошенники не дозвонятся", font=font(14),
              fill=(40, 68, 110), anchor="la")

    # master card
    draw.rounded_rectangle([left, card_top, left + width, card_top + dp(112)], radius=dp(28),
                           fill=(240, 242, 248))
    draw.text((left + dp(22), card_top + dp(24)), "Фильтрация звонков", font=font(17, True),
              fill=TEXT, anchor="la")
    draw.text((left + dp(22), card_top + dp(54)), "Включено · сегодня сброшено 3",
              font=font(13), fill=SECOND, anchor="la")
    toggle_pill(draw, left + width - dp(22) - dp(64), card_top + dp(32))
    y = card_top + dp(112) + dp(26)

    # quick actions
    draw.text((left, y), "Быстрые действия", font=font(16, True), fill=TEXT, anchor="la")
    y += dp(30)
    action_w = (width - dp(12)) // 2
    for index, (label, kind, tint) in enumerate((
            ("Пауза 1 час", "clock", M3_PRIMARY_CONTAINER),
            ("Пропустить звонок", "check", M3_SECONDARY_CONTAINER))):
        bx = left + index * (action_w + dp(12))
        draw.rounded_rectangle([bx, y, bx + action_w, y + dp(84)], radius=dp(24), fill=tint)
        glyph(draw, bx + dp(24), y + dp(26), dp(20), kind, M3_ON_PRIMARY_CONTAINER)
        draw.text((bx + dp(18), y + dp(54)), label, font=font(14, True),
                  fill=M3_ON_PRIMARY_CONTAINER, anchor="la")
    y += dp(84) + dp(26)

    # schedule row
    draw.text((left, y), "Расписание", font=font(16, True), fill=TEXT, anchor="la")
    y += dp(30)
    draw.rounded_rectangle([left, y, left + width, y + dp(72)], radius=dp(24),
                           fill=M3_SECONDARY_CONTAINER)
    icon_circle(draw, left + dp(36), y + dp(36), dp(16), (255, 255, 255), "calendar",
                M3_ON_PRIMARY_CONTAINER)
    draw.text((left + dp(64), y + dp(20)), "Будни, 09:00–18:00", font=font(15, True), fill=TEXT,
              anchor="la")
    draw.text((left + dp(64), y + dp(44)), "сейчас не действует", font=font(12), fill=M3_VARIANT,
              anchor="la")
    chevron(draw, left + width - dp(22), y + dp(36), M3_VARIANT)
    y += dp(72) + dp(26)

    # recent
    draw.text((left, y), "Последние", font=font(16, True), fill=TEXT, anchor="la")
    y += dp(16)
    rows = (("+7 900 000-00-01", "сегодня, 11:02"), ("+7 800 000-00-02", "сегодня, 10:47"),
            ("+1 (415) 555-0100", "вчера, 21:15"))
    draw.rounded_rectangle([left, y, left + width, y + dp(64) * len(rows)], radius=dp(24),
                           fill=CARD, outline=STROKE, width=max(1, dp(1)))
    for index, (number, meta) in enumerate(rows):
        cy = y + index * dp(64)
        icon_circle(draw, left + dp(34), cy + dp(32), dp(15), RED_TINT, "ban", RED)
        draw.text((left + dp(60), cy + dp(14)), number, font=font(15, True), fill=TEXT,
                  anchor="la")
        draw.text((left + dp(60), cy + dp(36)), meta, font=font(12), fill=SECOND, anchor="la")
        chevron(draw, left + width - dp(20), cy + dp(32), M3_VARIANT)
        if index < len(rows) - 1:
            draw.line([left + dp(60), cy + dp(64), left + width, cy + dp(64)], fill=STROKE,
                      width=1)

    bottom_nav(draw, 0)
    return img


def bottom_nav(draw, active):
    top = H - dp(80)
    draw.rectangle([0, top, W, H], fill=M3_CONTAINER)
    items = (("Главная", "home"), ("Журнал", "list"), ("Настройки", "tune"))
    slot = W // 3
    for index, (label, kind) in enumerate(items):
        cx = slot * index + slot // 2
        color = M3_ON_PRIMARY_CONTAINER if index == active else M3_VARIANT
        if index == active:
            draw.rounded_rectangle([cx - dp(34), top + dp(10), cx + dp(34), top + dp(40)],
                                   radius=dp(16), fill=M3_PRIMARY_CONTAINER)
        glyph(draw, cx, top + dp(25), dp(20), kind, color)
        draw.text((cx, top + dp(54)), label, font=font(12, True), fill=color, anchor="mm")


def status_bar_colored(draw, color):
    draw.text((dp(20), dp(8)), "11:25", font=font(13, True), fill=color, anchor="la")
    x = W - dp(88)
    for i, bar in enumerate((5, 8, 11, 14)):
        draw.rounded_rectangle([x + dp(i * 5), dp(20) - dp(bar), x + dp(i * 5) + dp(3), dp(20)],
                               radius=dp(1), fill=color)
    cx, cy = W - dp(49), dp(20)
    for radius in (4, 7.5, 11):
        box = [cx - dp(radius), cy - dp(radius), cx + dp(radius), cy + dp(radius)]
        draw.arc(box, start=225, end=315, fill=color, width=dp(2))
    bx, by = W - dp(34), dp(13)
    draw.rounded_rectangle([bx, by, bx + dp(19), by + dp(10)], radius=dp(3), outline=color,
                           width=dp(1.5))
    draw.rectangle([bx + dp(2), by + dp(3), bx + dp(13), by + dp(7)], fill=color)
    draw.rounded_rectangle([bx + dp(20), by + dp(3), bx + dp(22), by + dp(7)], radius=dp(1),
                           fill=color)


# ----------------------------------------------------------- variant C


def settings_row(draw, x, y, width, height, kind, title, subtitle, control=None,
                 last=False, on=True):
    icon_circle(draw, x + dp(34), y + height // 2, dp(16), M3_PRIMARY_CONTAINER, kind,
                M3_ON_PRIMARY_CONTAINER)
    draw.text((x + dp(62), y + height // 2 - (dp(18) if subtitle else dp(8))), title,
              font=font(15, True), fill=TEXT, anchor="la")
    if subtitle:
        draw.text((x + dp(62), y + height // 2 + dp(3)), subtitle, font=font(12), fill=SECOND,
                  anchor="la")
    if control == "switch":
        toggle_pill(draw, x + width - dp(20) - dp(52), y + height // 2 - dp(14), on=on,
                    width=52, height=28)
    elif control:
        text_w = draw.textlength(control, font=font(13))
        draw.text((x + width - dp(34) - text_w, y + height // 2), control, font=font(13),
                  fill=SECOND, anchor="lm")
        chevron(draw, x + width - dp(22), y + height // 2, M3_VARIANT)
    else:
        chevron(draw, x + width - dp(22), y + height // 2, M3_VARIANT)
    if not last:
        draw.line([x + dp(62), y + height, x + width, y + height], fill=STROKE, width=1)


def variant_c():
    img = Image.new("RGB", (W, H), M3_SURFACE)
    draw = ImageDraw.Draw(img)
    status_bar(draw)

    left = dp(20)
    width = W - dp(40)
    y = dp(32) + dp(18)

    draw.text((left, y), "Стоп-звонки", font=font(22, True), fill=TEXT, anchor="la")
    y += dp(32)
    draw.ellipse([left, y + dp(4), left + dp(8), y + dp(12)], fill=(46, 160, 67))
    draw.text((left + dp(16), y), "Фильтр включён · 3 сброшено сегодня", font=font(13),
              fill=SECOND, anchor="la")
    y += dp(30)

    def group_label(text):
        nonlocal y
        draw.text((left + dp(4), y), text, font=font(12, True), fill=M3_VARIANT, anchor="la")
        y += dp(20)

    def group(rows, radius=24):
        nonlocal y
        height = dp(72) * len(rows)
        draw.rounded_rectangle([left, y, left + width, y + height], radius=dp(radius), fill=CARD,
                               outline=STROKE, width=max(1, dp(1)))
        for index, row in enumerate(rows):
            settings_row(draw, left, y + index * dp(72), width, dp(72), row[0], row[1], row[2],
                         row[3] if len(row) > 3 else None, last=index == len(rows) - 1,
                         on=row[4] if len(row) > 4 else True)
        y += height + dp(22)

    group_label("ФИЛЬТР")
    group([("shield", "Фильтрация звонков", "незнакомые номера сбрасываются", "switch", True),
           ("clock", "Пауза", "сейчас выключена", "1 час"),
           ("calendar", "Расписание", "будни, 09:00–18:00", None)])

    group_label("ДАННЫЕ")
    group([("list", "Журнал", "12 записей", "все"),
           ("person", "Белый список", "1 номер", "изменить"),
           ("info", "Диагностика", "роль и последние звонки")])

    group_label("ПРАВИЛА")
    group([("ban", "Блокировать без маркера", "в России маркер почти не передаётся",
            "switch", False),
           ("book", "Как это работает", "инструкция и ограничения", None)])

    note = "Все данные хранятся только на телефоне · версия 1.2"
    nw = draw.textlength(note, font=font(12))
    draw.text((left + (width - nw) / 2, y), note, font=font(12), fill=SECOND, anchor="la")

    bottom_nav(draw, 2)
    return img


# ---------------------------------------------------------------- journal


def journal():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    status_bar(draw)

    left = dp(20)
    width = W - dp(40)
    y = dp(32) + dp(16)

    draw.text((left, y), "Журнал", font=font(22, True), fill=TEXT, anchor="la")
    clear = "Очистить"
    cw = draw.textlength(clear, font=font(13, True))
    draw.text((left + width - cw, y + dp(8)), clear, font=font(13, True), fill=ACCENT, anchor="la")
    y += dp(32)
    draw.text((left, y), "12 записей · 3 сегодня", font=font(13), fill=SECOND, anchor="la")
    y += dp(30)

    x = left
    for label, selected in (("Все 12", True), ("Сброшенные 9", False), ("Пропущенные 3", False)):
        x += chip(draw, x, y, label, selected=selected, height=38, size=13) + dp(8)
    y += dp(38) + dp(20)

    def group(title, rows):
        nonlocal y
        draw.text((left + dp(4), y), title, font=font(12, True), fill=SECOND, anchor="la")
        y += dp(20)
        height = dp(68) * len(rows)
        draw.rounded_rectangle([left, y, left + width, y + height], radius=dp(20), fill=CARD,
                               outline=STROKE, width=max(1, dp(1)))
        for index, (kind, color, tint, number, meta) in enumerate(rows):
            cy = y + index * dp(68)
            icon_circle(draw, left + dp(34), cy + dp(34), dp(15), tint, kind, color)
            draw.text((left + dp(60), cy + dp(14)), number, font=font(15, True), fill=TEXT,
                      anchor="la")
            draw.text((left + dp(60), cy + dp(37)), meta, font=font(12), fill=SECOND, anchor="la")
            if index < len(rows) - 1:
                draw.line([left + dp(60), cy + dp(68), left + width, cy + dp(68)], fill=STROKE,
                          width=1)
        y += height + dp(20)

    group("СЕГОДНЯ", [
        ("ban", RED, RED_TINT, "+7 900 000-00-01", "11:02 · нет в контактах · сброшен"),
        ("ban", RED, RED_TINT, "+7 800 000-00-02", "10:47 · нет в контактах · сброшен"),
        ("check", (46, 125, 50), (232, 245, 233), "+7 916 123-45-67",
         "09:15 · номер в контактах · пропущен"),
    ])
    group("ВЧЕРА", [
        ("ban", RED, RED_TINT, "+1 (415) 555-0100", "21:15 · нет в контактах · сброшен"),
        ("clock", AMBER, AMBER_TINT, "+7 495 000-00-03", "18:30 · пауза по расписанию"),
    ])

    nav_pill(draw)
    return img


def main():
    if not os.path.isdir(OUT):
        os.makedirs(OUT)
    outputs = (("variant-a-control.png", variant_a),
               ("variant-b-material.png", variant_b),
               ("variant-c-settings.png", variant_c),
               ("variant-journal.png", journal))
    for name, render in outputs:
        path = os.path.join(OUT, name)
        render().save(path)
        print(path)


if __name__ == "__main__":
    main()
