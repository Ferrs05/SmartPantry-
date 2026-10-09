from pathlib import Path
import urllib.request,zipfile
base=Path(__file__).resolve().parents[1]
(base/'downloads').mkdir(exist_ok=True)
p=base/'downloads/jdk21.zip'
if not p.exists(): urllib.request.urlretrieve('https://aka.ms/download-jdk/microsoft-jdk-21.0.8-windows-x64.zip',p)
with zipfile.ZipFile(p) as z: z.extractall(base/'.build-tools')
print('JDK extracted')

