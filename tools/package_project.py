"""Package source plus bundled model/recipes; exclude local runtimes and signing material."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT.parent/'outputs'
DEST.mkdir(exist_ok=True)
evidence=ROOT/'test-evidence'
for filename in ['build-validated.log','build-test-update.log','instrumentation-updated.log','runtime-tests.log','instrumentation-offline.log']:
    shutil.copy2(ROOT/filename,evidence/filename)
shutil.copy2(ROOT/'domain/build/test-results/test/TEST-id.smartpantry.domain.DomainTest.xml',evidence/'domain-tests.xml')
shutil.copy2(ROOT/'app/build/reports/lint-results-debug.html',evidence/'lint-results-debug.html')
apk=DEST/'SmartPantry_v2_debug.apk'
shutil.copy2(ROOT/'app/build/outputs/apk/debug/app-debug.apk',apk)
archive=DEST/'SmartPantry_Android_Studio_v2.zip'
excluded={'.gradle','.gradle-user-home','.android-sdk','.build-tools','.test-avds','downloads','build','__pycache__','.git','.idea','ui-backup-2026-10-09'}
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for path in sorted(ROOT.rglob('*')):
        if not path.is_file(): continue
        relative=path.relative_to(ROOT)
        if any(part in excluded for part in relative.parts): continue
        if relative.parts[0]=='test-evidence' and path.name.startswith('Screenshot_'): continue
        if path.name in {'local.properties','gradle-threads.txt'}: continue
        if path.suffix=='.log' and relative.parts[0]!='test-evidence': continue
        z.write(path,'SmartPantry/'+relative.as_posix())
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    assert 'SmartPantry/gradle/wrapper/gradle-wrapper.jar' in z.namelist()
    assert 'SmartPantry/app/src/main/assets/best.tflite' in z.namelist()
    assert not any(n.endswith('local.properties') or n.endswith('.keystore') for n in z.namelist())
info={p.name:{'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in [apk,archive]}
(DEST/'SmartPantry_delivery.json').write_text(json.dumps(info,indent=2))
print(json.dumps(info,indent=2))
