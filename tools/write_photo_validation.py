"""Record observed delivery checks; fail instead of reporting unfinished tests as passed."""
from pathlib import Path
import hashlib,json,sqlite3,zipfile,xml.etree.ElementTree as ET
from PIL import Image,ImageOps,ImageDraw
root=Path(__file__).resolve().parents[1]
apk=root/'app/build/outputs/apk/debug/app-debug.apk'
build=(root/'build-photo-final.log').read_text(encoding='utf-8-sig',errors='replace')
tests=(root/'instrumentation-photo-delivery.log').read_text(encoding='utf-8-sig',errors='replace')
assert 'BUILD SUCCESSFUL' in build
assert 'OK (6 tests)' in tests
coverage=json.loads((root/'test-evidence/photo-coverage.json').read_text())
issues=ET.parse(root/'app/build/reports/lint-results-debug.xml').getroot().findall('issue')
errors=sum(i.get('severity') in ('Error','Fatal') for i in issues)
warnings=sum(i.get('severity')=='Warning' for i in issues)
assert errors==0
original=root.parent/'outputs/SmartPantry_v2_debug.apk'
checks={}
with zipfile.ZipFile(original) as old,zipfile.ZipFile(apk) as new:
    for asset in ['assets/best.tflite','assets/resep.db']:
        oldhash=hashlib.sha256(old.read(asset)).hexdigest()
        newhash=hashlib.sha256(new.read(asset)).hexdigest()
        assert oldhash==newhash,asset
        checks[asset]=newhash
    assert not any('food_atlas' in n for n in new.namelist())
    assert sum(n.startswith('assets/photos/') for n in new.namelist())==coverage['uniquePhotographs']
count=sqlite3.connect(root/'app/src/main/assets/resep.db').execute('select count(*) from resep').fetchone()[0]
assert count==9272 and apk.stat().st_size<300_000_000
report=f'''# Validasi revisi foto asli — 9 Oktober 2026

## Isi APK

- APK debug: {apk.stat().st_size:,} byte ({apk.stat().st_size/1_000_000:.2f} MB), di bawah batas 300 MB.
- {coverage['uniquePhotographs']} foto asli, {coverage['photoBytes']:,} byte WebP; semuanya dibundel offline.
- {coverage['withPhoto']:,} dari {count:,} resep mendapat foto contoh hidangan sesuai aturan pencocokan judul/cara masak. Foto dapat digunakan oleh beberapa resep dalam keluarga hidangan yang sama.
- {coverage['withoutPhoto']:,} resep belum dipetakan. Cakupan seluruh masakan sehari-hari **belum selesai**; jumlah ini tidak dapat dianggap sebagai jumlah masakan langka. Audit per ID ada di `test-evidence/photo-recipe-audit.csv`.
- Foto berlisensi CC BY, CC BY-SA, CC0 atau public domain dari Wikimedia Commons; penulis, URL sumber, lisensi, perubahan dan hash tercatat di `photo_sources.json` serta `PHOTO_SOURCES.md`. Kredit tersedia di aplikasi.
- Tidak ada gambar AI/atlas lama di APK. Foto bukan foto asli penulis resep Cookpad. Pencocokan tidak memakai foto kategori untuk resep tanpa foto.
- Tampilan: foto nyata di beranda, border pada hero/kartu, heading PantryEditorial turunan Lora dan teks Manrope, tema krem/hijau, judul kartu tiga baris, filter bottom sheet.

## Pengujian yang dijalankan

- Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --offline`: berhasil. Lint {errors} error, {warnings} warning. Log `test-evidence/build-photo-final.log`.
- Android instrumentation: 6 tes lulus pada emulator SmartPantryTest dengan Wi-Fi dan data seluler dinonaktifkan. Log `test-evidence/instrumentation-photo-delivery.log` mencatat durasi dan hasil.
- Tes mencakup CameraX/capture, inferensi CPU model v2, query/ranking Room, ketiadaan izin INTERNET, alur bahan/manual/filter/detail, decoding semua foto dan batas cache bitmap.
- Python `tools/test_recipes.py`: 3 tes lulus. `tools/test_photo_assets.py`: audit hash, format, ukuran, sumber, ID resep dan tidak adanya atlas AI lulus.
- Tangkapan layar aktual: `test-evidence/real-photo-home.png`, `real-photo-recipes.png`, `real-photo-filter.png`, `real-photo-detail.png`. Foto tambahan diperiksa lewat contact sheet.
- Tes domain JVM 17 tes merupakan hasil validasi sebelumnya dan berstatus UP-TO-DATE pada build terdahulu; tidak dinyatakan sebagai eksekusi baru pada revisi ini.
- Percobaan UI awal terganggu oleh System UI emulator yang tidak merespons; suite diulang setelah emulator stabil. Screenshot dengan dialog tersebut tidak dipakai sebagai bukti akhir.

## Pengelolaan memori

Foto maksimum 960 piksel dan disimpan sebagai WebP. Kartu memakai target decode 320 piksel, detail/hero 960. Decode berjalan di thread IO dengan maksimal dua pekerjaan bersamaan. LruCache bitmap dibatasi 8 MB; semua foto berhasil dibaca dalam tes tanpa melewati batas cache. Cache dibersihkan saat UI masuk latar belakang atau menerima permintaan pelepasan memori. Bitmap yang masih tampil dan model mempunyai penggunaan RAM sendiri; 8 MB bukan batas seluruh RAM aplikasi. Belum ada pengukuran puncak RAM pada ponsel fisik.

## Model dan database

Model v2 dan SQLite identik byte per byte dengan APK v2 awal:

- `best.tflite`: SHA256 `{checks['assets/best.tflite']}`.
- `resep.db`: SHA256 `{checks['assets/resep.db']}`, {count:,} resep.

Pengujian emulator tidak membuktikan akurasi deteksi foto nyata, performa ponsel fisik atau kompatibilitas Android 8. Revisi ini tidak melatih ulang model.
'''
(root/'PHOTO_VALIDATION.md').write_text(report,encoding='utf-8')
sheet=Image.new('RGB',(1200,900),'#FAF7EF')
draw=ImageDraw.Draw(sheet)
for n,name in enumerate(['home','recipes','detail']):
    path=root/'test-evidence'/f'real-photo-{name}.png'
    with Image.open(path) as im:
        im.thumbnail((380,845),Image.Resampling.LANCZOS)
        x=n*400+(400-im.width)//2
        sheet.paste(im,(x,30))
    draw.text((n*400+20,10),{'home':'Beranda','recipes':'Daftar resep','detail':'Detail resep'}[name],fill='#254D38')
sheet.save(root/'test-evidence/real-photo-preview.jpg',quality=90)
print(json.dumps({'apkBytes':apk.stat().st_size,'lintErrors':errors,'lintWarnings':warnings,'unchangedAssets':checks,'coverage':coverage},indent=2))
