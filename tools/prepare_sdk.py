from pathlib import Path
import urllib.request,zipfile,shutil,os
base=Path(__file__).resolve().parents[1]; target=base/'.android-sdk'
(base/'downloads').mkdir(exist_ok=True)
(target/'platforms').mkdir(parents=True,exist_ok=True)
archive=base/'downloads/platform-36.zip'
if not archive.exists(): urllib.request.urlretrieve('https://dl.google.com/android/repository/platform-36_r02.zip',archive)
with zipfile.ZipFile(archive) as z: z.extractall(target/'platforms')
source_sdk=Path(os.environ.get('ANDROID_HOME',str(Path(os.environ['LOCALAPPDATA'])/'Android/Sdk')))
for name in ['build-tools','licenses']:
 if not (target/name).exists(): shutil.copytree(source_sdk/name,target/name)
print('SDK',list((target/'platforms').iterdir()))
