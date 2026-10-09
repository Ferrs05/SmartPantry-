from pathlib import Path
import urllib.request
ROOT=Path(__file__).resolve().parents[1]
for family,filename in [('lora','Lora[wght].ttf'),('manrope','Manrope[wght].ttf')]:
    directory=ROOT/'app/src/main/res/font';directory.mkdir(parents=True,exist_ok=True)
    for remote,local in [(filename,directory/(family+'.ttf')),('OFL.txt',ROOT/'app/src/main/assets/licenses'/(family+'_OFL.txt'))]:
        local.parent.mkdir(parents=True,exist_ok=True)
        url='https://raw.githubusercontent.com/google/fonts/main/ofl/'+family+'/'+urllib.parse.quote(remote)
        with urllib.request.urlopen(url,timeout=30) as r: local.write_bytes(r.read())
        print(local.name,local.stat().st_size)
