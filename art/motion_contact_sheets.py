"""Summarize timed in-game captures without altering the source evidence.

Run after verifyCaptures: python3 art/motion_contact_sheets.py.
Writes eight evenly spaced, timestamped frames per organ under build/art/motion.
"""
import json
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path('build/captures')
OUT = Path('build/art/motion')


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    report = json.loads((ROOT / 'report.json').read_text())
    entries = report['captures']
    for folder in sorted(ROOT.glob('motion_*')):
        shots = [e for e in entries if e['scenario'] == folder.name and e['view'].startswith('motion')]
        if not shots:
            continue
        sheet = Image.new('RGB', (1100, 1160), '#202020')
        draw = ImageDraw.Draw(sheet)
        for cell in range(8):
            shot = shots[round(cell * (len(shots) - 1) / 7)]
            img = Image.open(folder / (shot['view'] + '.png')).convert('RGB')
            crop = img.crop((365, 245, 915, 515))
            x, y = (cell % 2) * 550, (cell // 2) * 290
            sheet.paste(crop, (x, y + 20))
            elapsed = (shot['capturedAtMillis'] - shots[0]['capturedAtMillis']) / 1000
            draw.text((x + 5, y + 4), f"{folder.name} {elapsed:.2f}s", fill='white')
        sheet.save(OUT / (folder.name + '.png'))


if __name__ == '__main__':
    main()
