"""Gather Commons file descriptions and licenses for manual photo selection."""
from pathlib import Path
import concurrent.futures, json, time, urllib.parse, urllib.request
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'downloads/photo_research'
OUT.mkdir(parents=True,exist_ok=True)
QUERIES=['Indonesian vegetables','ayam kecap','ayam goreng','ayam bakar','ayam mentega',
 'ayam geprek','ayam rica rica','ayam woku','opor ayam','soto ayam','sup ayam','bubur ayam',
 'mie ayam','nasi goreng ayam','ayam teriyaki','chicken katsu','sate ayam','ayam balado',
 'telur dadar','telur ceplok','telur balado','telur pindang','semur telur','telur gulung',
 'tahu goreng','tahu isi','tahu crispy','tahu bacem','tahu aci','perkedel tahu','tahu telur',
 'pepes tahu','tahu gejrot','tahu walik','tempe goreng','tempe mendoan','tempe orek',
 'kering tempe','tempe bacem','tempe penyet','perkedel tempe','sayur lodeh',
 'ikan goreng','ikan bakar','pepes ikan','ikan asam manis','pindang ikan','ikan balado',
 'ikan kuah kuning','tongkol suwir','sarden','sambal teri','bakso ikan','sup ikan',
 'telur orak arik','tumis tahu','tumis tempe']
def fetch(query):
    target=OUT/(query.replace(' ','_')+'.json')
    if target.exists(): return query,json.loads(target.read_text(encoding='utf-8'))
    args={'action':'query','format':'json','generator':'search','gsrsearch':query,
          'gsrnamespace':6,'gsrlimit':7,'prop':'imageinfo','iiprop':'url|extmetadata|size','iiurlwidth':720}
    url='https://commons.wikimedia.org/w/api.php?'+urllib.parse.urlencode(args)
    for attempt in range(3):
        try:
            request=urllib.request.Request(url,headers={'User-Agent':'SmartPantryPhotoResearch/1.0 (educational project; CC attribution)'})
            with urllib.request.urlopen(request,timeout=30) as response: data=json.load(response)
            target.write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
            return query,data
        except Exception as error:
            if attempt==2: return query,{'error':str(error)}
            time.sleep(2+attempt)
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
    for query,data in pool.map(fetch,QUERIES):
        pages=sorted(data.get('query',{}).get('pages',{}).values(),key=lambda p:p.get('index',0))
        print(json.dumps({'query':query,'files':[{'title':p['title'],'license':p.get('imageinfo',[{}])[0].get('extmetadata',{}).get('LicenseShortName',{}).get('value','')} for p in pages[:5]],'error':data.get('error')},ensure_ascii=True),flush=True)
