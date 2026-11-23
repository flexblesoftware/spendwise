#!/usr/bin/env python3
import os
import sys

try:
    from PIL import Image, ImageDraw
except ImportError:
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "pillow"])
    from PIL import Image, ImageDraw

def render_icon(size: int, is_round: bool) -> Image.Image:
    # 32-bit RGBA image
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    bg_color = (0x0B, 0x4D, 0x3B, 255)
    fg_color = (0x2E, 0xCC, 0x8F, 255)

    if is_round:
        draw.ellipse([0, 0, size - 1, size - 1], fill=bg_color)
    else:
        # Full bleed, edge-to-edge, NO white border, NO transparent rim
        draw.rectangle([0, 0, size, size], fill=bg_color)

    # Proportional to 108dp canvas:
    # Top bar: w=48, h=8, r=4 (X: 30..78, Y: 36..44)
    # Mid bar: w=36, h=8, r=4 (X: 36..72, Y: 50..58)
    # Bot bar: w=24, h=8, r=4 (X: 42..66, Y: 64..72)
    # Total glyph height = 36dp (Y: 36..72) -> vertically centered at Y=54
    scale = size / 108.0
    bar_height = 8.0 * scale
    bar_radius = 4.0 * scale
    bar_gap = 6.0 * scale
    total_glyph_height = 36.0 * scale
    start_y = (size - total_glyph_height) / 2.0

    bar_widths = [48.0 * scale, 36.0 * scale, 24.0 * scale]

    for i, w in enumerate(bar_widths):
        x0 = (size - w) / 2.0
        y0 = start_y + i * (bar_height + bar_gap)
        x1 = x0 + w
        y1 = y0 + bar_height
        draw.rounded_rectangle([x0, y0, x1, y1], radius=bar_radius, fill=fg_color)

    return img

def main():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    res_dir = os.path.join(repo_root, "app", "src", "main", "res")

    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }

    for density, size in densities.items():
        density_dir = os.path.join(res_dir, f"mipmap-{density}")
        os.makedirs(density_dir, exist_ok=True)

        # 1. Square full-bleed icon
        square_path = os.path.join(density_dir, "ic_launcher.png")
        square_img = render_icon(size, is_round=False)
        square_img.save(square_path, "PNG")

        # 2. Circular icon
        round_path = os.path.join(density_dir, "ic_launcher_round.png")
        round_img = render_icon(size, is_round=True)
        round_img.save(round_path, "PNG")

        print(f"Generated {density} ({size}x{size}): {square_path} & {round_path}")

    # 3. Play Store 512x512 full bleed icon
    play_store_img = render_icon(512, is_round=False)
    play_store_paths = [
        os.path.join(repo_root, "play_store_512.png"),
        os.path.join(repo_root, "app", "src", "main", "play_store_512.png"),
    ]
    for p in play_store_paths:
        play_store_img.save(p, "PNG")
        print(f"Generated Play Store 512x512 icon: {p}")

if __name__ == "__main__":
    main()
