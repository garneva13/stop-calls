"""Render the 1200x630 cover image for the project post.

    python tools/make_cover.py

Output: docs/cover-1200x630.png
"""

import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H = 1200, 630
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "docs")
PHONE = os.path.join(OUT, "variant-a-control.png")
BOLD = r"C:\Windows\Fonts\segoeuib.ttf"
SEMI = r"C:\Windows\Fonts\seguisb.ttf"
REG = r"C:\Windows\Fonts\segoeui.ttf"

BG_TOP = (11, 14, 19)
BG_BOTTOM = (19, 26, 38)
WHITE = (255, 255, 255)
MUTED = (174, 184, 198)
DIM = (122, 135, 152)
ACCENT = (168, 199, 250)
ACCENT_SOFT = (18, 48, 79)


def font(path, size):
    return ImageFont.truetype(path, size)


def gradient(size):
    image = Image.new("RGB", size, BG_TOP)
    draw = ImageDraw.Draw(image)
    for y in range(size[1]):
        t = y / max(1, size[1] - 1)
        color = tuple(int(BG_TOP[i] + (BG_BOTTOM[i] - BG_TOP[i]) * t) for i in range(3))
        draw.line([(0, y), (size[0], y)], fill=color)
    return image


def glow(image, center, radius, color, alpha):
    layer = Image.new("RGBA", image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    draw.ellipse([center[0] - radius, center[1] - radius,
                  center[0] + radius, center[1] + radius],
                 fill=color + (alpha,))
    layer = layer.filter(ImageFilter.GaussianBlur(radius * 0.55))
    image.alpha_composite(layer)


def rounded(image, radius):
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, image.size[0] - 1, image.size[1] - 1],
                                           radius=radius, fill=255)
    result = image.convert("RGBA")
    result.putalpha(mask)
    return result


def pill(draw, x, y, text, size, fg, bg, border, font_path=SEMI, pad_x=18, height=40):
    fnt = font(font_path, size)
    width = int(draw.textlength(text, font=fnt)) + pad_x * 2
    draw.rounded_rectangle([x, y, x + width, y + height], radius=height // 2,
                           fill=bg, outline=border, width=1)
    draw.text((x + width / 2, y + height / 2 - 1), text, font=fnt, fill=fg, anchor="mm")
    return width


def render(height, path):
    dy = (height - H) // 2
    base = gradient((W, height)).convert("RGBA")
    glow(base, (950, 120 + dy), 320, (11, 87, 208), 70)
    glow(base, (120, 560 + dy), 300, (30, 142, 62), 45)
    glow(base, (1050, 560 + dy), 240, (120, 60, 200), 35)

    draw = ImageDraw.Draw(base)

    # --- phone on the right ---------------------------------------------
    phone = Image.open(PHONE).convert("RGB")
    phone_h = 590
    phone_w = int(phone.width * phone_h / phone.height)
    phone = phone.resize((phone_w, phone_h), Image.LANCZOS)
    phone = rounded(phone, 34)

    px = W - 64 - phone_w
    py = (height - phone_h) // 2

    shadow = Image.new("RGBA", (W, height), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)
    sdraw.rounded_rectangle([px - 10, py + 14, px + phone_w + 10, py + phone_h + 18],
                            radius=40, fill=(0, 0, 0, 190))
    shadow = shadow.filter(ImageFilter.GaussianBlur(26))
    base.alpha_composite(shadow)

    base.paste(phone, (px, py), phone)
    draw.rounded_rectangle([px, py, px + phone_w, py + phone_h], radius=34,
                           outline=(58, 66, 78), width=2)

    # --- text block ------------------------------------------------------
    left = 72

    badge = "Бесплатно · Android 11+ · открытый код"
    pill(draw, left, 92 + dy, badge, 20, ACCENT, ACCENT_SOFT, (32, 72, 116))

    title_font = font(BOLD, 78)
    draw.text((left - 4, 158 + dy), "Стоп-звонки", font=title_font, fill=WHITE)

    sub_font = font(REG, 30)
    draw.text((left, 268 + dy), "Сбрасывает звонки с номеров,", font=sub_font, fill=MUTED)
    draw.text((left, 310 + dy), "которых нет в ваших контактах", font=sub_font, fill=MUTED)

    chip_font = font(SEMI, 19)
    x = left
    for label in ("до первого гудка", "журнал", "белый список"):
        width = int(draw.textlength(label, font=chip_font)) + 44
        draw.rounded_rectangle([x, 396 + dy, x + width, 436 + dy], radius=20,
                               fill=(24, 31, 42), outline=(48, 58, 72), width=1)
        draw.ellipse([x + 16, 412 + dy, x + 24, 420 + dy], fill=(109, 213, 140))
        draw.text((x + 32, 415 + dy), label, font=chip_font, fill=(214, 221, 230),
                  anchor="lm")
        x += width + 12

    url_font = font(REG, 22)
    draw.text((left, 492 + dy), "github.com/garneva13/stop-calls", font=url_font, fill=DIM)
    draw.text((left, 524 + dy), "без рекламы · без подписок · 90 КБ", font=url_font, fill=DIM)

    base.convert("RGB").save(path, optimize=True)
    print(path)


def main():
    render(630, os.path.join(OUT, "cover-1200x630.png"))
    render(675, os.path.join(OUT, "cover-1200x675.png"))


if __name__ == "__main__":
    main()
