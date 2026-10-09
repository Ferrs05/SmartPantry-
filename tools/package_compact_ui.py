"""Deliver the real photograph revision without replacing older deliveries."""
from pathlib import Path
import os,shutil,zipfile,json,hashlib
root=Path(__file__).resolve().parents[1];dest=root.parent/'outputs';dest.mkdir(exist_ok=True)
apk=dest/'SmartPantry_v2_CompactUI_debug.apk'
source=root/'app/build/outputs/apk/debug/app-debug.apk'
assert source.stat().st_size<300_000_000
shutil.copy2(source,apk)
with zipfile.ZipFile(apk) as z:
    assert 'assets/photo_index.json' in z.namelist()
    assert 'assets/photo_sources.json' in z.namelist()
    assert 'assets/best.tflite' in z.namelist()
    assert not any('food_atlas' in n for n in z.namelist())
    assert sum(n.startswith('assets/photos/') for n in z.namelist())==len(json.loads((root/'app/src/main/assets/photo_sources.json').read_text(encoding='utf-8')))
for name in ['build-compact-ui.log','instrumentation-compact-ui.log','photo-index.log']:
    shutil.copy2(root/name,root/'test-evidence'/name)
archive=dest/'SmartPantry_Android_Studio_v2_CompactUI.zip'
excluded={'.gradle','.gradle-user-home','.android-sdk','.build-tools','.test-avds','downloads',
          'build','__pycache__','.git','.idea','ui-backup-2026-10-09','photo-revision-backup'}
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for directory,subdirs,files in os.walk(root):
        subdirs[:]=sorted(d for d in subdirs if d not in excluded)
        for name in sorted(files):
            path=Path(directory)/name;rel=path.relative_to(root)
            if name in {'local.properties','gradle-threads.txt','gradle-photo-stack.txt','revise_photo_ui.py','redesign_compact_ui.py','prepare_compact_package.py','package_ui.py','ARTWORK.md'}:continue
            if path.suffix in {'.jks','.keystore'}:continue
            if path.suffix=='.log' and rel.parts[0]!='test-evidence':continue
            if rel.parts[0]=='test-evidence' and name.startswith('polished-'):continue
            z.write(path,'SmartPantry/'+rel.as_posix())
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    assert not any('food_atlas' in n for n in z.namelist())
    assert 'SmartPantry/app/src/main/assets/photo_index.json' in z.namelist()
info={p.name:{'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in [apk,archive]}
(dest/'SmartPantry_CompactUI_delivery.json').write_text(json.dumps(info,indent=2),encoding='utf-8')
print(json.dumps(info,indent=2))
