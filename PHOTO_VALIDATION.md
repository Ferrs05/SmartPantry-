# Validasi revisi foto asli — 9 Oktober 2026

## Isi APK

- APK debug: 62,804,480 byte (62.80 MB), di bawah batas 300 MB.
- 65 foto asli, 6,321,506 byte WebP; semuanya dibundel offline.
- 2,477 dari 9,272 resep mendapat foto contoh hidangan sesuai aturan pencocokan judul/cara masak. Foto dapat digunakan oleh beberapa resep dalam keluarga hidangan yang sama.
- 6,795 resep belum dipetakan. Cakupan seluruh masakan sehari-hari **belum selesai**; jumlah ini tidak dapat dianggap sebagai jumlah masakan langka. Audit per ID ada di `test-evidence/photo-recipe-audit.csv`.
- Foto berlisensi CC BY, CC BY-SA, CC0 atau public domain dari Wikimedia Commons; penulis, URL sumber, lisensi, perubahan dan hash tercatat di `photo_sources.json` serta `PHOTO_SOURCES.md`. Kredit tersedia di aplikasi.
- Tidak ada gambar AI/atlas lama di APK. Foto bukan foto asli penulis resep Cookpad. Pencocokan tidak memakai foto kategori untuk resep tanpa foto.
- Tampilan: foto nyata di beranda, border pada hero/kartu, heading PantryEditorial turunan Lora dan teks Manrope, tema krem/hijau, judul kartu tiga baris, filter bottom sheet.

## Pengujian yang dijalankan

- Gradle `:app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --offline`: berhasil. Lint 0 error, 14 warning. Log `test-evidence/build-photo-final.log`.
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

- `best.tflite`: SHA256 `89f56ff5ed3ca6c80501612d0ef968dec2d53b62c2b3b23393be8bf130aa48c1`.
- `resep.db`: SHA256 `e0a302762e704ae28d825784a0f68b77fb52c26ee6e1e7b43e9e5cf0cab61462`, 9,272 resep.

Pengujian emulator tidak membuktikan akurasi deteksi foto nyata, performa ponsel fisik atau kompatibilitas Android 8. Revisi ini tidak melatih ulang model.
