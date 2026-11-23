"""Conservative recipe filter: supported foods plus an explicit basic seasoning vocabulary.
Unknown words are excluded for review, rather than silently accepted as seasoning.
"""
from pathlib import Path
import sqlite3,json,re,csv,shutil,collections
ROOT=Path(__file__).resolve().parents[1]; A=ROOT/'app/src/main/assets'; backup=ROOT/'photo-revision-backup/recipe-before-scan18'
backup.mkdir(parents=True,exist_ok=True)
for name in ['resep.db','photo_index.json','recipe_provenance.json']:
 if not (backup/name).exists(): shutil.copy2(A/name,backup/name)
# Input always uses the original database so reruns preserve IDs and the audit population.
foods='ayam bawang merah putih bamer baput cabai cabe cab cabean rawit cengek lombok tomat telur telor tahu tempe ikan gurame gurami lele nila mujair patin tongkol tenggiri kembung bandeng kakap salmon tuna haruan gabus jagung wortel bayam daun bawang prei kacang panjang kangkung kol kubis terong terung kentang'
basics='garam gula air minyak santan kemiri ketumbar merica lada jahe kunyit kunir lengkuas laos laja salam jeruk nipis limau lemon purut serai sereh kencur kemangi seledri pandan daun asam jawa kandis sunti cuka kecap saus saos sambal terasi kaldu penyedap royco masako totole ajinomoto sasa maggi pala kayu manis cengkeh kapulaga jinten jintan pekak adas wijen margarin mentega kunyit kunyitkunir kunyitkuning kunyitbubuk kunyitputih kunyitmerah kunyitkunir kara sun kara santankara santaninstan santankental santanencer bango abc indofood saori tiram jamur sapi udang ikan ayam rasa minyakgoreng'
# Flavor names sapi/udang/jamur/tiram only allowed after a condiment prefix.
flavors={'sapi','udang','jamur','tiram'}
units='kg gr gram g ons ml liter l sdm sdt sendok makan teh cm ruas jari siung buah bh butir btr biji btg batang lembar lbr papan ekor potong gelas cup bungkus bks sachet saset sch sacet sashet kotak ikat genggam sejumput secukupnya secukup secukupny cukup sesuai selera seperlunya sedikit beberapa'
notes='bahan bumbu halus haluskan dihaluskan ulek diulek iris diiris tipis cincang dicincang cincanghalus geprek digeprek parut diparut serut diserut goreng digoreng rebus direbus kukus dikukus bakar matang mentah segar besar kecil sedang ukuran uk utuh belah dibelah buang dibuang kupas dikupas cuci dicuci bersih dicuci bersihkan ambil diambil gunakan menggunakan pakai pake saya sy aku utk untuk dan atau dengan tanpa yang yg saja aja boleh bisa optional opsional pelengkap taburan garnish marinasi tumisan tumis menumis menggoreng rendam direndam masak memasak kuah aduk campur dicampur sisihkan tambahan tambah lainnya lain utama kuning hijau ijo merah putih hitam keriting bulat kerat keratkerat dadu serong memarkan dimemarkan memar gepre haluskan secukupny sesuai selera pedas manis asin bubuk cair kental encer instan instant sangrai disangrai sangraikan halus kasar fillet filet dada paha sayap kulit daging tulang kepala bagian dadaayam pahaayam filletayam ayamfillet telurayam telorayam utuh potongan godogan godog di dri dari opsional skip jika ada suka suka suka sesuai selera bumbu lainnya haluskan ulek potongpotong diirisiris dihaluskan haluskan irisiris buangbiji pipil dipipil sisir disisir muda tua pasir merah kecapmanis kecapasin garamgula airputih panas hangat dingin es airkaldu secukupnya'
allowed=set((foods+' '+basics+' '+units+' '+notes).split())-flavors
source=sqlite3.connect(backup/'resep.db'); rows=source.execute('select id,title,ingredientLines from resep').fetchall(); audit=[]; keep=[]
for rid,title,raw in rows:
 unknown=set()
 for line in json.loads(raw):
  text=line.lower()
  text=re.sub(r'\b(?:kaldu(?: bubuk)?|saus|saos|kecap|terasi|penyedap|rasa)\s+(?:sapi|udang|jamur|tiram)\b',' bumbu ',text)
  unknown.update(set(re.findall(r'[a-z]+',text))-allowed)
 if not unknown: keep.append(rid)
 audit.append({'id':rid,'title':title,'keep':not unknown,'unrecognized_tokens':' '.join(sorted(unknown))})
source.close()
assert keep, 'Refusing to create an empty recipe database'
shutil.copy2(backup/'resep.db',A/'resep.db')
c=sqlite3.connect(A/'resep.db');c.execute('pragma foreign_keys=on')
c.execute('create temp table retained(id integer primary key)');c.executemany('insert into retained values(?)',[(i,) for i in keep]);c.execute('delete from resep where id not in (select id from retained)');c.commit()
assert c.execute('pragma integrity_check').fetchone()[0]=='ok';assert not c.execute('pragma foreign_key_check').fetchall()
counts=dict(c.execute('select category,count(*) from resep group by category'));c.close()
index=json.loads((backup/'photo_index.json').read_text(encoding='utf-8'));ids=set(map(str,keep));index['recipes']={k:v for k,v in index['recipes'].items() if k in ids};(A/'photo_index.json').write_text(json.dumps(index,ensure_ascii=False),encoding='utf-8')
provenance=json.loads((backup/'recipe_provenance.json').read_text(encoding='utf-8'));provenance['recipe_count']=len(keep);provenance['scan18_filter']={'original_count':len(rows),'retained':len(keep),'removed':len(rows)-len(keep),'policy':'18 supported foods plus explicit basic-seasoning vocabulary; unknown terms excluded for review','category_counts':counts,'photo_recipes':len(index['recipes'])};(A/'recipe_provenance.json').write_text(json.dumps(provenance,ensure_ascii=False,indent=2),encoding='utf-8')
with (ROOT/'test-evidence/recipe-scan18-audit.csv').open('w',encoding='utf-8-sig',newline='') as f:
 w=csv.DictWriter(f,fieldnames=audit[0].keys());w.writeheader();w.writerows(audit)
print(json.dumps(provenance['scan18_filter'],indent=2));print('Featured retained:',1824 in keep,9000 in keep)
