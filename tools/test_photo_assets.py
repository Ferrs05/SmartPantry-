"""Verify offline photo integrity, provenance, mappings and no category fallback."""
from pathlib import Path
import json,hashlib,sqlite3
from PIL import Image
root=Path(__file__).resolve().parents[1];assets=root/'app/src/main/assets'
photos=json.loads((assets/'photo_sources.json').read_text(encoding='utf-8'))
index=json.loads((assets/'photo_index.json').read_text(encoding='utf-8'))
ids={str(r[0]) for r in sqlite3.connect(assets/'resep.db').execute('select id from resep')}
assert set(index['recipes'])<=ids
for p in photos.values():
    path=assets/p['asset']
    assert hashlib.sha256(path.read_bytes()).hexdigest()==p['sha256']
    with Image.open(path) as im: assert max(im.size)<=960 and im.format=='WEBP'
    assert p['sourceUrl'].startswith('https://commons.wikimedia.org/wiki/File:')
    assert p['author'] and p['license']
for key in index['recipes'].values(): assert key in photos
assert index['recipes']['1824']=='ayam_mentega'
assert index['recipes']['9000']=='tempe_orek'
assert '1986' not in index['recipes']
assert not list((root/'app/src/main').rglob('food_atlas*'))
assert sum(p['bytes'] for p in photos.values())<20_000_000
print(f"Photo audit passed: {len(photos)} real photographs, {len(index['recipes'])} mapped recipes; no generated atlas.")
