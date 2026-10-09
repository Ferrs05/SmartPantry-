"""Auditable, conservative dish matching. Never use protein category as a photo fallback."""
from pathlib import Path
import sqlite3,json,re,csv,collections
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
sources=json.loads((ASSETS/'photo_sources.json').read_text(encoding='utf-8'))
def normalized(text):
    text=text.lower().replace('telor','telur').replace('krispi','crispy').replace('krispy','crispy').replace('kripik','keripik')
    return re.sub(r'[^a-z0-9]+',' ',text).strip()
# First matching rule wins. Specific methods precede generic fried preparations.
# These are representative photographs of the same named dish, not the Cookpad author photograph.
RULES=[
 ('ayam_asam_manis',r'ayam.*(asam manis|asem manis|asamanis)',r'nasi|mie|mi |telur|tahu|tempe|ikan|bakar|panggang'),
 ('semur_telur',r'semur.*telur|telur.*semur',r'puyuh|dadar|ceplok|tahu|tempe|ayam|daging|kentang'),
 ('semur_tahu',r'semur.*tahu|tahu.*semur',r'telur|tempe|ayam|daging|kentang|ikan'),
 ('semur_tempe',r'semur.*tempe|tempe.*semur',r'telur|tahu|ayam|daging|kentang|ikan'),
 ('semur_ayam',r'semur.*ayam|ayam.*semur',r'telur|tahu|tempe|daging|kentang|ikan|mie|nasi|ceker|usus|ampela'),
 ('telur_gulung',r'telur gulung',r'dadar|bihun|sosis|korea|jepang|keju|nori|sayur|bayam|tahu|gulung telur'),
 ('nasi_tim_ayam',r'nasi tim.*ayam',r'jamur|mpasi|bayi'),
 ('sup_ayam',r'(sup|sop).*ayam',r'jagung|krim|cream|bakso|baso|enoki|jamur|santan|tom yam|tom yum|tomat'),
 ('ayam_mentega',r'ayam.*mentega',r'crispy|nasi|mie|mi |katsu|tepung|lada hitam|teriyaki'),
 ('pepes_tahu',r'pepes.*tahu',r'jamur|udang|ayam|ikan|teri|sosis|tempe|telur asin'),
 ('keripik_tempe',r'keripik.*tempe|tempe.*keripik',r''),
 ('lodeh_tahu',r'lodeh.*tahu',r'terong|nangka|krecek'),
 ('nasi_goreng_ayam',r'nasi goreng.*ayam',r'udang|seafood|kambing|sapi|kimchi'),
 ('bubur_ayam',r'bubur.*ayam',r'mpasi|bayi|merah'),
 ('mie_ayam',r'(mie|mi) ayam',r'goreng|bakso|pangsit|jamur'),
 ('soto_ayam',r'soto.*ayam',r'betawi|lamongan|banjar|santan|sapi'),
 ('sate_ayam',r'sate.*ayam',r'lilit|taichan|telur|kulit|usus|ampela'),
 ('chicken_katsu',r'(chicken|ayam).*katsu',r'keju|cheese|curry|kari|sandwich'),
 ('ayam_nugget',r'nugget.*ayam|ayam.*nugget|chicken nugget',r'wortel|tahu|tempe|sayur|brokoli'),
 ('ayam_geprek',r'ayam.*geprek',r'keju|mozzarella'),
 ('ayam_woku',r'ayam.*woku',r'belanga.*ikan'),
 ('ayam_rica',r'ayam.*rica',r'usus|ceker|ampela'),
 ('ayam_rujak',r'ayam.*bumbu rujak',r'bakar|panggang'),
 ('ayam_kremes',r'ayam.*kremes',r'kecap|mentega'),
 ('ayam_balado',r'ayam.*balado',r'ceker|ampela|terong|kentang|hijau'),
 ('opor_ayam',r'opor.*ayam',r'putih|tahu|telur'),
 ('ayam_kecap',r'ayam.*kecap',r'nasi|mie|mi |mentega|bakso|sate|bakar|panggang|soto|sup|sop|ceker|usus|ampela|telur'),
 ('ayam_bakar',r'ayam.*bakar',r'taliwang|betutu|woku|rica|rujak'),
 ('ayam_goreng',r'ayam.*goreng|fried chicken',r'mentega|kecap|geprek|tepung|crispy|crisp|kfc|katsu|nasi|mie|mi |telur|saus|saos|sambal|balado|pedas|madu|suwir|bawang|ala korea|serundeng|terasi|kulit|ceker|usus|ampela'),
 ('tahu_aci',r'tahu.*aci',r'kuah|sup|sop|pedas manis|asamanis|asam manis'),
 ('tahu_gejrot',r'tahu.*gejrot',r''),
 ('tahu_bakso',r'tahu.*(bakso|baso)',r'kuah|sup|sop|pepes|sayur'),
 ('tahu_telur',r'tahu telur|tahu telor',r'orak|dadar|kukus|pepes|martabak|semur|balado|sup|sop'),
 ('mun_tahu',r'mun tahu',r''),
 ('tahu_walik',r'tahu.*walik',r'kuah'),
 ('tahu_petis',r'tahu.*petis',r'kupat|ketupat|campur'),
 ('tahu_bacem',r'tahu.*bacem|bacem.*tahu',r'tempe'),
 ('perkedel_tahu',r'(perkedel|pergedel|perkdel).*tahu',r'jagung|ayam|udang|sapi|bayam|brokoli|kukus'),
 ('tahu_isi',r'tahu.*(isi|brontak|berontak)',r'bakso|baso|ayam|udang|sosis|keju|telur|aci|walik|sapi|pepes|kuah'),
 ('tahu_crispy',r'tahu.*(crispy|crisp|crispi|kriuk|tepung)',r'isi|aci|walik|bakso|baso|telur|saus|saos|asam|gejrot|petis|susu|nori|keju|kuah|pepes'),
 ('tahu_goreng',r'tahu.*goreng',r'isi|aci|walik|bakso|baso|telur|saus|saos|asam|gejrot|petis|tempe|tepung|crispy|pepes|sambal|kecap|kuah|balado|pedas|susu|bawang|bulat'),
 ('tempe_mendoan',r'(tempe.*mendoan|mendoan)',r'isi|mercon'),
 ('tempe_bacem',r'tempe.*bacem|bacem.*tempe',r'tahu'),
 ('tempe_penyet',r'tempe.*penyet|penyet.*tempe',r'tahu|ayam'),
 ('tempe_teri',r'(tempe.*teri|teri.*tempe)',r'kuah|santan|sayur|lodeh|basah'),
 ('tempe_kering',r'kering.*tempe|tempe.*kering',r'teri|kentang|kacang|tahu'),
 ('tempe_orek',r'(orek.*tempe|tempe.*orek)',r'teri|kentang|kacang|tahu|kering|buncis|kacangpanjang'),
 ('tempe_sambal',r'sambal goreng.*tempe',r'tahu|santan|kuah|teri|basah|kentang'),
 ('tempe_goreng',r'tempe.*goreng|goreng.*tempe',r'tepung|mendoan|kriuk|crispy|sambal|balado|kering|orek|tahu|penyet|bacem|isi|mercon|tepung|bumbu|kecap|kari|kunyit'),
 ('telur_bali',r'telur.*bali',r'ceplok|dadar|tahu|puyuh'),
 ('telur_pindang',r'(telur.*pindang|pindang.*telur)',r'puyuh'),
 ('telur_balado',r'telur.*balado|balado.*telur',r'ceplok|dadar|puyuh|tahu|kentang|terong|hijau'),
 ('telur_dadar',r'telur.*dadar|dadar.*telur',r'padang|tahu|tempe|gulung|keju|kornet|sosis|crispy|saus|saos|balado|ceplok|kuah|fuyung|fuyong|fu yung|jepang|korea|nori|bayam|brokoli|buncis|kentang|mie|mi '),
 ('telur_ceplok',r'telur.*(ceplok|mata sapi)|ceplok.*telur',r'kecap|balado|saus|saos|tauco|kuah|santan|asem|asam|bali|sambal|pedas|lada|tahu|nasi'),
 ('teri_balado',r'teri.*balado|balado.*teri',r'tempe|kentang|kacang|tahu'),
 ('mangut_lele',r'mangut.*lele|lele.*mangut',r'tahu|tempe'),
 ('lele_goreng',r'lele.*goreng|pecel lele',r'tepung|fillet|sambal|balado|kecap|saus|saos|mangut'),
 ('pepes_mas',r'pepes.*(ikan mas|ikan emas)',r'tahu'),
 ('ikan_pesmol',r'pesmol',r'telur|ayam|tahu|lele'),
 ('ikan_asam_padeh',r'asam padeh|asem padeh',r'ayam|tahu|telur|daging|lele'),
 ('ikan_asam_manis',r'(ikan|kakap|gurame|gurami).*asam manis',r'lele|teri|tongkol|tenggiri|bandeng'),
 ('ikan_bakar',r'(ikan|gurame|gurami|nila|mas|kakap).*bakar',r'teri|sate|pepes|lele|tuna|tongkol|tenggiri|bandeng'),
 ('ikan_goreng',r'(ikan|kakap|nila|gurame|gurami|mas).*goreng',r'lele|teri|tepung|fillet|saus|saos|balado|kecap|asam|tongkol|bandeng|bakar|pesmol|tenggiri|tuna'),
 ('capcay',r'capcay|cap cay|cap cai',r'goreng|seafood|udang|bakso|sosis|kekian'),
]
names={'home':'Sayuran segar di pasar Jakarta','ayam_rica':'Ayam rica-rica','pepes_mas':'Pepes ikan mas',
       'tempe_sambal':'Sambal goreng tempe','ayam_nugget':'Nugget ayam','ayam_rujak':'Ayam bumbu rujak'}
for k,v in sources.items(): v['dishName']=names.get(k,k.replace('_',' ').capitalize())
# Reject combinations and preparations not visible in the selected photograph.
EXTRA_EXCLUDES={
 'ayam_kecap':r'barbeque|barbecue|bbq|tempe|tahu',
 'ayam_balado':r'tempe|tahu','ayam_rujak':r'tempe|tahu',
 'sup_ayam':r'soun|makaroni|sayur|wortel|kentang|tahu|telur',
 'telur_dadar':r'toge|tauge|cah|tumis|ayam|udang|sapi|teri|tomat|bakso|baso|sayur|sarden|asin|cumi',
 'telur_ceplok':r'kornet|tumis|sarden|sosis|hijau|ijo|pete|petai|bayam|buncis|cabe|cabai',
 'tahu_telur':r'asin|tumis|opor|brokoli|jamur|sayur|santan|kecap|kari|bali|geprek|isi|fuyung|fu yung|aci|ceplok|balado|kornet',
 'tahu_petis':r'walek|walik|tek|gimbal',
 'tahu_crispy':r'rambutan|bola|bulat|fantasi|fantasy',
 'tahu_goreng':r'nasi|semur|bihun|kulit',
 'tahu_bacem':r'telur|ayam|ikan','tahu_bakso':r'kornet',
 'tempe_goreng':r'telur|pedas|manis|stick|stik|keju|sosis|sapi|ayam|udang|teri|bakso|baso|tepung|krispi',
 'tempe_sambal':r'hati|sapi|ayam|udang|telur',
 'teri_balado':r'telur|hijau|ijo|tempe|kentang',
 'nasi_goreng_ayam':r'sosis|telur|kornet','lodeh_tahu':r'ayam|telur|tempe',
 'ikan_goreng':r'sambal|ijo|hijau|cabai|cabe',
 'capcay':r'telur|jamur',
}
index={'recipes':{},'categories':{'ayam':'ayam_kecap','ikan':'ikan_bakar','telur':'telur_dadar','tahu':'tahu_goreng','tempe':'tempe_mendoan'}}
c=sqlite3.connect(ASSETS/'resep.db'); rows=c.execute('select id,title,category,loves from resep').fetchall()
audit=[];counts=collections.Counter()
for rid,title,category,loves in rows:
    t=normalized(title); key=None
    for dish,include,exclude in RULES:
        if dish in sources and re.search(include,t) and not (exclude and re.search(exclude,t)) and not re.search(EXTRA_EXCLUDES.get(dish,r'(?!)'),t):
            key=dish;break
    if key: index['recipes'][str(rid)]=key;counts[key]+=1
    audit.append({'id':rid,'title':title,'category':category,'loves':loves,'photo':key or ''})
(ASSETS/'photo_sources.json').write_text(json.dumps(sources,ensure_ascii=False,indent=2),encoding='utf-8')
(ASSETS/'photo_index.json').write_text(json.dumps(index,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
with (ROOT/'test-evidence/photo-recipe-audit.csv').open('w',newline='',encoding='utf-8-sig') as f:
    w=csv.DictWriter(f,fieldnames=list(audit[0]));w.writeheader();w.writerows(audit)
summary={'recipes':len(rows),'withPhoto':len(index['recipes']),'withoutPhoto':len(rows)-len(index['recipes']),
         'uniquePhotographs':len(sources),'photoBytes':sum(x['bytes'] for x in sources.values()),'families':dict(counts)}
(ROOT/'test-evidence/photo-coverage.json').write_text(json.dumps(summary,indent=2),encoding='utf-8')
pending=[a for a in audit if not a['photo']]
pending.sort(key=lambda a:a['loves'],reverse=True)
(ROOT/'test-evidence/photo-pending-top.json').write_text(json.dumps(pending[:150],ensure_ascii=False,indent=2),encoding='utf-8')
doc=['# Sumber foto SmartPantry','',
 'Foto asli Wikimedia Commons. Foto resep merupakan contoh hidangan dengan nama dan metode yang sesuai, bukan foto asli penulis resep Cookpad. Pemetaan tidak memakai fallback kategori.',
 '',f"{len(sources)} foto; {len(index['recipes'])} dari {len(rows)} resep dipetakan. Resep lain tetap tersedia tanpa foto. Angka ini tidak membuktikan bahwa semua resep yang umum sudah tercakup.",'',
 'Perubahan: diperkecil maksimal 960 piksel, dikonversi ke WebP kualitas 82, EXIF dilepas. Pemotongan tepi dilakukan saat tampilan. Foto turunan mempertahankan lisensi sumber masing-masing; CC BY-SA berlaku pada foto turunannya.',
 '', '| Hidangan | Fotografer | Lisensi | Sumber | Resep |','|---|---|---|---|---|']
for key,p in sources.items():
    doc.append(f"| {p['dishName']} | {p['author'].replace('|',' ')} | [{p['license']}]({p['licenseUrl']}) | [Foto]({p['sourceUrl']}) | {counts[key]} |")
(ROOT/'PHOTO_SOURCES.md').write_text('\n'.join(doc)+'\n',encoding='utf-8')
print(json.dumps(summary,indent=2))
