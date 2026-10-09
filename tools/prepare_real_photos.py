"""Download licensed dish photographs; never substitute category images for recipes."""
from pathlib import Path
import json, urllib.request, urllib.parse, re, html, time, io, hashlib
from PIL import Image, ImageOps, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
CACHE=ROOT/'downloads/photo_research'
ASSETS=ROOT/'app/src/main/assets'
PHOTOS=ASSETS/'photos'
PHOTOS.mkdir(parents=True,exist_ok=True)
SELECT={
 'home':'Indonesian vegetables.JPG',
 'ayam_kecap':'Ayam Kecap 4.jpg','ayam_goreng':'Ayam goreng kalasan.JPG',
 'ayam_bakar':'Ayam bakar.jpg','ayam_geprek':'Ayam geprek.jpg',
 'ayam_rica':'Ayam Rica-rica.JPG','ayam_woku':'Ayam Woku Manado-2.jpg',
 'opor_ayam':'Opor Ayam 3.jpg','soto_ayam':'Soto Ayam home-made.JPG',
 'bubur_ayam':'Bubur ayam chicken porridge.JPG','mie_ayam':'Mie ayam (Dumai).jpg',
 'nasi_goreng_ayam':'Nasi Goreng Ayam in Bali.jpg','chicken_katsu':'Chicken Cutlet 001.jpg',
 'sate_ayam':'Sate Ayam Panggang.jpg','ayam_balado':'Ayam Balado 101722.jpg',
 'telur_dadar':'Telur Dadar.jpg','telur_ceplok':'Telur Ceplok.jpg',
 'telur_balado':'Telur Balado.jpg','tahu_goreng':'Tahu Goreng.jpg',
 'tahu_bacem':'42. Tahu bacem 2.jpg','tahu_isi':'Tahu isi goreng plus cabe rawit.jpg',
 'tahu_crispy':'Tahu goreng crispy.jpg','tahu_walik':'Tahu walik.jpg',
 'tahu_petis':'Tahu petis.jpg','perkedel_tahu':'Frikadel Tahu.jpg',
 'tempe_goreng':'Indonesian fried tempeh.JPG','tempe_mendoan':'Sepiring mendoan.jpg',
 'tempe_orek':'Tempe Orek.jpg','tempe_kering':'Kering tempe.jpg',
 'tempe_bacem':'Tempe bacem lauk soto Pak Marto.JPG',
 'tempe_penyet':'Sambal tempe penyet kemangi.JPG',
 'tempe_teri':'Sambal goreng teri tempe.JPG',
 'ikan_goreng':'Ikan goreng kunyit.JPG','ikan_bakar':'Ikan Bakar.jpg',
 'ikan_asam_manis':'Ikan asam manis di Makassar.JPG',
 'ikan_pesmol':'Ikan pesmol.JPG','ikan_asam_padeh':'Ikan Asam Padeh Padang.jpg',
 'teri_balado':'Ikan teri balado.JPG','lele_goreng':'Lele Goreng Khas Pantura.jpg',
 'mangut_lele':'Mangut lele.jpg',
 'tahu_aci':'Tahu aci.jpg','tahu_gejrot':'Tahu Gejrot.jpg',
 'tahu_telur':'Tahu telor.JPG','mun_tahu':'Mun Tahu 1.jpg',
 'telur_pindang':'Telur pindang.JPG','telur_bali':'Telur bumbu Bali.JPG',
 'pepes_mas':'Pepes ikan emas (pais lauk mas) Sunda.jpg',
 'tempe_sambal':'Sambal goreng tempe.JPG','tahu_bakso':'Tahubakso.jpg',
 'ayam_nugget':'Chicken Nugget Fiesta.JPG','ayam_rujak':'Ayam bumbu rujak.jpg',
 'ayam_kremes':'Ayam kremes.jpg','capcay':'Cap Cai.JPG',
 'ayam_mentega':'Ayam Mentega Wisma Lamban Danau.jpg',
 'pepes_tahu':'Pèpès Tahu.jpg','sup_ayam':'Sop Ayam Klaten.jpg',
 'nasi_tim_ayam':'Nasi tim.JPG','keripik_tempe':'Keripik tempe Lombok.JPG',
 'lodeh_tahu':'Sayur Lodeh Ibu Sulastri.jpg',
 'telur_gulung':'Rolled Fried Egg 2.jpg',
 'ayam_asam_manis':'Ayam Asam Manis.jpg',
 'semur_telur':'Semur endog 3.jpg',
 'semur_tahu':'Semur tahu 4.jpg',
 'semur_tempe':'Semur tèmpè 2.jpg',
 'semur_ayam':'Ayam Semur dan Tempe.jpg',
}
def request(url):
    return urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'SmartPantryPhotoResearch/1.0 (CC attribution)'}),timeout=45)
def strip(s): return html.unescape(re.sub('<[^>]+>','',s)).strip()
pages={}
for f in CACHE.glob('*.json'):
    for p in json.loads(f.read_text(encoding='utf-8')).get('query',{}).get('pages',{}).values():
        if p.get('imageinfo'): pages[p['title']]=p
missing=['File:'+s for s in SELECT.values() if 'File:'+s not in pages]
if missing:
    args={'action':'query','format':'json','titles':'|'.join(missing),'prop':'imageinfo','iiprop':'url|extmetadata|size','iiurlwidth':960}
    with request('https://commons.wikimedia.org/w/api.php?'+urllib.parse.urlencode(args)) as r: data=json.load(r)
    (CACHE/('selected_'+hashlib.sha256('|'.join(missing).encode()).hexdigest()[:10]+'.json')).write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
    pages.update({p['title']:p for p in data.get('query',{}).get('pages',{}).values() if p.get('imageinfo')})
manifest={}
for key,title in SELECT.items():
    p=pages.get('File:'+title)
    if not p: print('MISSING',key,title,flush=True); continue
    info=p['imageinfo'][0]; meta=info['extmetadata']
    license=strip(meta.get('LicenseShortName',{}).get('value',''))
    license_url=meta.get('LicenseUrl',{}).get('value','')
    if not (license.startswith('CC BY') or license=='CC0' or license=='Public domain'):
        print('REJECT LICENSE',key,license,flush=True); continue
    original=ROOT/'downloads/real_photos'/(key+'_'+str(p['pageid']))
    previous=ROOT/'downloads/real_photos'/key
    if not original.exists() and previous.exists() and key not in {'sate_ayam','mie_ayam'}:
        original.write_bytes(previous.read_bytes())
    original.parent.mkdir(parents=True,exist_ok=True)
    target=PHOTOS/(key+'.webp')
    if not original.exists():
        # Commons recommends preset thumbnail sizes to reduce load.
        try:
            url=info.get('thumburl',info['url'])
            if '/thumb/' not in url:
                base=info['url'].split('?')[0]
                prefix,tail=base.split('/commons/')
                url=prefix+'/commons/thumb/'+tail+'/330px-'+tail.rsplit('/',1)[-1]
            with request(url) as r: original.write_bytes(r.read())
        except Exception as e: print('DOWNLOAD FAILED',key,str(e),flush=True); continue
        time.sleep(3)
    with Image.open(original) as im:
        im=ImageOps.exif_transpose(im).convert('RGB')
        im.thumbnail((960,960),Image.Resampling.LANCZOS)
        im.save(target,'WEBP',quality=82,method=6)
        size=im.size
    manifest[key]={'asset':'photos/'+target.name,'dishName':key.replace('_',' ').capitalize(),
        'author':strip(meta.get('Artist',{}).get('value','')),'license':license,
        'licenseUrl':license_url,'sourceUrl':info['descriptionurl'],'originalUrl':info['url'],
        'description':strip(meta.get('ImageDescription',{}).get('value','')),
        'changes':'Diperkecil, dikonversi ke WebP; tampilan dapat memotong tepi foto.',
        'width':size[0],'height':size[1],'bytes':target.stat().st_size,
        'sha256':hashlib.sha256(target.read_bytes()).hexdigest()}
    print('OK',key,license,target.stat().st_size,flush=True)
(ASSETS/'photo_sources.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
keys=list(manifest)
for start in range(0,len(keys),20):
    group=keys[start:start+20]; sheet=Image.new('RGB',(1000, ((len(group)+3)//4)*180),'white'); draw=ImageDraw.Draw(sheet)
    for n,key in enumerate(group):
        with Image.open(PHOTOS/(key+'.webp')) as im:
            thumb=ImageOps.fit(im,(240,145)); x=(n%4)*250;y=(n//4)*180
            sheet.paste(thumb,(x,y));draw.text((x+4,y+148),key,fill='black')
    sheet.save(ROOT/'test-evidence'/f'real-photo-contact-{start//20}.jpg')
print('TOTAL',len(manifest),sum(v['bytes'] for v in manifest.values()),flush=True)
