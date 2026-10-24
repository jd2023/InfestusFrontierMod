"""Summarize timed in-game captures without altering the source evidence.

Run after verifyCaptures: python3 art/motion_contact_sheets.py.
Writes eight evenly spaced, timestamped frames per organ under build/art/motion.
"""
import json
import re
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path('build/captures')
OUT = Path('build/art/motion')


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    report = json.loads((ROOT / 'report.json').read_text())
    entries = report['captures']
    for folder in sorted(ROOT.glob('motion_*')):
        groups = {}
        for entry in entries:
            if entry['scenario'] == folder.name:
                groups.setdefault(re.sub(r'_\d{3}$', '', entry['view']), []).append(entry)
        for group, shots in groups.items():
            if len(shots) < 8:
                continue
            door = folder.name.endswith('_door')
            bounds = (465, 140, 815, 680) if door else (365, 245, 915, 515)
            w, h = bounds[2] - bounds[0], bounds[3] - bounds[1]
            sheet = Image.new('RGB', (w * 2, (h + 20) * 4), '#202020')
            draw = ImageDraw.Draw(sheet)
            for cell in range(8):
                index = [0, 1, 2, 3, 4, 5, 8, len(shots) - 1][cell] if 'opening' in group or 'closing' in group else round(cell * (len(shots) - 1) / 7)
                shot = shots[index]
                img = Image.open(folder / (shot['view'] + '.png')).convert('RGB')
                x, y = (cell % 2) * w, (cell // 2) * (h + 20)
                sheet.paste(img.crop(bounds), (x, y + 20))
                elapsed = (shot['capturedAtMillis'] - shots[0]['capturedAtMillis']) / 1000
                draw.text((x + 5, y + 4), f"{group} {elapsed:.2f}s", fill='white')
            suffix = '' if group == 'motion' else '_' + group
            sheet.save(OUT / (folder.name + suffix + '.png'))


if __name__ == '__main__':
    main()
