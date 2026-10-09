"""Build deterministic Room-compatible recipe assets from the report's Kaggle CSVs."""
import csv
import hashlib
import json
import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NAMES = ['Ayam','Bawang Merah','Bawang Putih','Cabai','Tomat','Telur','Tahu','Tempe',
         'Ikan','Jagung','Wortel','Bayam','Daun Bawang','Kacang Panjang','Kangkung','Kol','Terong','Kentang']
PATTERNS = [r'ayam',r'bawang merah|bamer',r'bawang putih|baput',r'cabai|cabe|cabai',
            r'tomat',r'telur|telor',r'tahu',r'tempe',
            r'ikan|guram[eiy]|lele|nila|mujair|patin|tongkol|tenggiri|kembung|bandeng|kakap|salmon|tuna',
            r'jagung',r'wortel',r'bayam',r'daun bawang|bawang daun|daun prei',
            r'kacang panjang',r'kangkung',r'kol|kubis',r'terong|terung',r'kentang']
ALIASES = [re.compile(r'\b(?:'+p+r')\b') for p in PATTERNS]
EXCLUSIONS = [r'kaldu\s+(?:bubuk\s+)?ayam|telur\s+ayam|minyak\s+ayam|masako|royco', '', '',
              r'saus\s+(?:cabai|cabe)|saos\s+(?:cabai|cabe)',r'(?:saus|saos|pasta)\s+tomat', '',
              r'kulit tahu|kembang tahu', '',r'kecap ikan|saus ikan|saos ikan|minyak ikan',r'tepung jagung|maizena',
              '', '', '', '', '',r'brokoli|kembang kol|kol bunga', '',r'tepung kentang']

def map_line(line):
    text = re.sub(r'[^a-z0-9\s]', ' ', line.lower())
    text = re.sub(r'\s+', ' ', text)
    return {i for i,p in enumerate(ALIASES) if p.search(text) and
            not (EXCLUSIONS[i] and re.search(EXCLUSIONS[i], text))}

def main():
    assets = ROOT/'app/src/main/assets'
    assets.mkdir(parents=True, exist_ok=True)
    db_path = assets/'resep.db'
    db_path.unlink(missing_ok=True)
    db = sqlite3.connect(db_path)
    db.executescript('''
    PRAGMA user_version=1;
    PRAGMA foreign_keys=ON;
    CREATE TABLE bahan(id INTEGER NOT NULL PRIMARY KEY, nama TEXT NOT NULL);
    CREATE TABLE resep(id INTEGER NOT NULL PRIMARY KEY, title TEXT NOT NULL,
      ingredientLines TEXT NOT NULL, steps TEXT NOT NULL, category TEXT NOT NULL,
      loves INTEGER NOT NULL, sourceUrl TEXT NOT NULL, uncheckedLines TEXT NOT NULL);
    CREATE TABLE resep_bahan(resepId INTEGER NOT NULL, bahanId INTEGER NOT NULL,
      PRIMARY KEY(resepId,bahanId), FOREIGN KEY(resepId) REFERENCES resep(id) ON DELETE CASCADE,
      FOREIGN KEY(bahanId) REFERENCES bahan(id) ON DELETE CASCADE);
    CREATE INDEX index_resep_bahan_bahanId ON resep_bahan(bahanId);
    ''')
    db.executemany('INSERT INTO bahan VALUES (?,?)', enumerate(NAMES))
    seen_url, seen_content = set(), set()
    stats = {'source':'https://www.kaggle.com/datasets/canggih/indonesian-food-recipes',
             'categories':{},'skipped':{'empty':0,'duplicate':0,'no_supported_ingredients':0},
             'class_recipe_counts':{n:0 for n in NAMES},
             'mapping':'whole-word aliases from Ingredients only; no category/title inference',
             'raw_sha256':{}}
    ident=0
    for category in ['ayam','ikan','tahu','telur','tempe']:
        path=ROOT/f'downloads/recipes/dataset-{category}.csv'
        stats['raw_sha256'][path.name]=hashlib.sha256(path.read_bytes()).hexdigest()
        kept=raw=0
        with path.open(encoding='utf-8-sig', newline='') as f:
            for row in csv.DictReader(f):
                raw+=1
                title=row['Title'].strip()
                lines=[s.strip() for s in row['Ingredients'].split('--') if s.strip()]
                steps=[s.strip() for s in row['Steps'].split('--') if s.strip()]
                if not title or not lines or not steps:
                    stats['skipped']['empty']+=1; continue
                url=row['URL'].strip()
                content=(title.casefold(),tuple(lines),tuple(steps))
                if (url and url in seen_url) or content in seen_content:
                    stats['skipped']['duplicate']+=1; continue
                mappings=[map_line(s) for s in lines]
                ids=set().union(*mappings)
                if not ids:
                    stats['skipped']['no_supported_ingredients']+=1; continue
                seen_url.add(url); seen_content.add(content)
                ident+=1; kept+=1
                try: loves=int(row['Loves'])
                except (ValueError,TypeError): loves=0
                full_url='https://cookpad.com'+url if url.startswith('/') else url
                unchecked=[line for line,matched in zip(lines,mappings) if not matched]
                db.execute('INSERT INTO resep VALUES (?,?,?,?,?,?,?,?)',
                    (ident,title,json.dumps(lines,ensure_ascii=False),json.dumps(steps,ensure_ascii=False),
                     category,loves,full_url,json.dumps(unchecked,ensure_ascii=False)))
                for i in sorted(ids):
                    db.execute('INSERT INTO resep_bahan VALUES (?,?)',(ident,i))
                    stats['class_recipe_counts'][NAMES[i]]+=1
        stats['categories'][category]={'raw':raw,'kept':kept}
    db.commit()
    assert db.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
    assert not db.execute('PRAGMA foreign_key_check').fetchall()
    db.close()
    stats['recipe_count']=ident
    (assets/'recipe_provenance.json').write_text(json.dumps(stats,ensure_ascii=False,indent=2),encoding='utf-8')
    (ROOT/'tools/ingredient_aliases.json').write_text(json.dumps(dict(zip(NAMES,PATTERNS)),indent=2))
    print(json.dumps(stats,ensure_ascii=True,indent=2))

if __name__=='__main__': main()
