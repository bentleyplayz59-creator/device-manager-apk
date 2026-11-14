"""Generates the Device Manager logo and Android launcher icons with Pillow."""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
S = 1024


def gradient(size, top, bottom):
    img = Image.new("RGB", (size, size))
    d = ImageDraw.Draw(img)
    for y in range(size):
        t = y / (size - 1)
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        d.line([(0, y), (size, y)], fill=c)
    return img


def make_logo():
    bg = gradient(S, (0, 168, 150), (16, 52, 120))
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, S - 1, S - 1), radius=230, fill=255)
    logo = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    logo.paste(bg, (0, 0), mask)
    d = ImageDraw.Draw(logo)
    # phone body
    d.rounded_rectangle((300, 150, 724, 874), radius=70, outline="white", width=30)
    d.rounded_rectangle((452, 190, 572, 208), radius=9, fill="white")
    d.ellipse((492, 800, 532, 840), outline="white", width=10)
    # shield
    d.polygon([(512, 330), (650, 385), (650, 535), (512, 700), (374, 535), (374, 385)], fill="white")
    # check mark
    pts = [(448, 518), (500, 576), (584, 456)]
    d.line(pts, fill=(16, 52, 120), width=38, joint="curve")
    for p in (pts[0], pts[-1]):
        d.ellipse((p[0] - 19, p[1] - 19, p[0] + 19, p[1] + 19), fill=(16, 52, 120))
    return logo


def main():
    logo = make_logo()
    os.makedirs(os.path.join(ROOT, "assets"), exist_ok=True)
    logo.save(os.path.join(ROOT, "assets", "logo.png"))
    sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for name, px in sizes.items():
        out = os.path.join(ROOT, "app", "src", "main", "res", f"mipmap-{name}")
        os.makedirs(out, exist_ok=True)
        icon = logo.resize((px, px), Image.LANCZOS)
        icon.save(os.path.join(out, "ic_launcher.png"))
        icon.save(os.path.join(out, "ic_launcher_round.png"))
    print("Logo and launcher icons generated")


if __name__ == "__main__":
    main()
