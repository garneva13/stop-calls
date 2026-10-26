"""Prepare RuStore assets: 512x512 icon and 1080x1920 screenshots.

    python tools/make_store_assets.py

Outputs: docs/store/icon-512.png, docs/store/screen-1.png, docs/store/screen-2.png
"""

import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(ROOT, "docs")
OUT = os.path.join(DOCS, "store")


def icon():
    """512x512, fully opaque background — RuStore rejects transparent edges."""
    source = Image.open(os.path.join(DOCS, "logo.png")).convert("RGBA")
    source = source.resize((512, 512), Image.LANCZOS)
    background = Image.new("RGB", (512, 512), (17, 34, 31))  # ic_launcher_background
    background.paste(source, (0, 0), source)
    path = os.path.join(OUT, "icon-512.png")
    background.save(path, optimize=True)
    print(path, background.size)


def screenshot(source_name, target_name, crop_top):
    source = Image.open(os.path.join(DOCS, source_name)).convert("RGB")
    # Remove the Android status bar: RuStore forbids system UI on screenshots.
    source = source.crop((0, crop_top, source.width, source.height))

    width = 1080
    height = 2160
    scale = width / source.width
    resized = source.resize((width, int(source.height * scale)), Image.LANCZOS)

    if resized.height >= height:
        top = 0
        result = resized.crop((0, top, width, top + height))
    else:
        canvas = Image.new("RGB", (width, height), (242, 243, 245))
        canvas.paste(resized, (0, 0))
        result = canvas

    path = os.path.join(OUT, target_name)
    result.save(path, optimize=True)
    print(path, result.size)


def main():
    os.makedirs(OUT, exist_ok=True)
    icon()
    # 52 dp status bar at 3x scale = 156 px.
    screenshot("home.png", "screen-1.png", 156)
    screenshot("stats-screen.png", "screen-2.png", 156)


if __name__ == "__main__":
    main()
