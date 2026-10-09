# Hasil build dan pengujian

Tanggal: 8 Oktober 2026. Project: SmartPantry 0.1.0-v2, application ID `id.smartpantry.app`.

## Hasil yang dijalankan

| Pemeriksaan | Hasil |
|---|---|
| Build APK debug | Lulus; 56.495.338 byte / 56,5 MB |
| Unit test domain JVM | 17 test, 0 gagal, 0 error |
| Pengujian pemetaan resep dan SQLite | 3 test lulus |
| Android lint | 0 error, 13 warning |
| Kompilasi APK instrumentasi | Lulus |
| Instrumentasi Android | 5 test lulus; 29,449 detik untuk seluruh suite |
| Suite Android dengan jaringan dimatikan | 5 test yang sama lulus kembali; 23,068 detik |
| Verifikasi tanda tangan APK | Lulus, signature v2, debug key |
| Minimum SDK dalam APK | API 26; target API 36 |
| Izin dalam APK | Kamera dan izin receiver internal AndroidX; tidak ada INTERNET |
| Model, config dan DB di APK | Byte identik dengan aset project |

## Pengujian Android yang lulus

Emulator Android 17/API 37, x86_64, RAM 2 GB dan dua inti. Emulator test menggunakan data baru di workspace.

1. CameraX membuka kamera virtual emulator, mengambil foto ke memori dan mencapai layar hasil setelah inferensi model v2.
2. LiteRT membuka model FLOAT32 dengan input NCHW `[1,3,512,512]` dan output `[1,22,5376]`, menjalankan inferensi pada gambar sintetis, serta menghasilkan nilai dan kotak yang valid.
3. Room membuka database resep dari APK, memastikan 9.272 resep, dan menjalankan rekomendasi serta pembacaan detail.
4. Aplikasi tidak memiliki izin INTERNET.
5. Bahan manual → tombol pencarian aktif → daftar resep → detail bahan dan langkah.

Log runtime mengonfirmasi TensorFlow Lite aktif dan memakai delegate CPU XNNPACK. Pengujian CPU memakai dua thread; tidak menggunakan server inferensi.

## Artefak dan bukti

- `build-validated.log`: build APK, unit test, lint dan kompilasi test awal yang lulus.
- `build-test-update.log`: build ulang test setelah pembaruan AndroidX Test; lint lulus.
- `instrumentation-updated.log`: `OK (5 tests)`.
- `instrumentation-offline.log`: `OK (5 tests)` setelah Wi-Fi dan data emulator dimatikan.
- `runtime-tests.log`: runtime terpisah `OK (3 tests)`; ketiganya juga termasuk suite lima test.
- `domain/build/test-results/test/TEST-id.smartpantry.domain.DomainTest.xml`: 17 unit test lulus.
- `app/build/reports/lint-results-debug.html`: laporan lint.
- `test-evidence/home.png`: screenshot antarmuka APK pada emulator.
- `test-evidence/gallery-result.png` dan `gallery-result.xml`: hasil pemilihan PNG melalui photo picker sistem, pemrosesan sukses, tidak ada bahan terdeteksi pada gambar sintetis. Jaringan emulator dimatikan. Inferensi pada contoh ini 140 ms; proses input sampai hasil 1.419 ms. Ini satu pengukuran emulator, bukan benchmark ponsel.

ZIP project menyertakan ringkasan ini, screenshot dan salinan log pengujian yang lulus dalam `test-evidence`. Cache build, SDK, JDK, data emulator dan private signing key tidak disertakan.

## Perbaikan selama pengujian

- Akses Room antar modul diperbaiki dengan mengekspos dependency runtime melalui `api`.
- Input konfigurasi Drive diperbaiki dari NHWC menjadi NCHW sesuai binary model.
- Library ikon besar diganti dengan ikon core dan vektor lokal.
- Konstanta rotasi kamera memakai `Surface.ROTATION_0` agar sesuai API.
- Espresso 3.6.1 gagal pada API 37 karena refleksi `InputManager.getInstance`. Test diperbarui ke Espresso 3.7.0, runner 1.7.0, dan JUnit AndroidX 1.3.0; seluruh suite kemudian lulus. [Catatan rilis resmi](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0).
- AVD sebelumnya lambat saat boot; pengujian berhasil pada AVD baru setelah boot selesai. Penyebab pasti kelambatan AVD lama tidak disimpulkan.

## Batas hasil ini

Pengujian tersebut membuktikan pipeline aplikasi berjalan pada emulator, termasuk photo picker sistem untuk PNG. Akurasi deteksi pada foto bahan asli, waktu inferensi ponsel, kompatibilitas pada perangkat Android 8, rotasi EXIF JPEG pada perangkat fisik, dan SUS belum diuji. Kamera virtual dan gambar sintetis tidak dipakai untuk mengklaim akurasi model. Target mAP dan AP per kelas tetap mengacu hasil evaluasi model v2 sebelumnya; bawang putih dan tempe masih di bawah AP50 0,70.

Lint masih memberi saran pembaruan dependency dan penggunaan ekstensi Kotlin. Warning tidak dimatikan atau disembunyikan dengan baseline.

Model SHA256: `89f56ff5ed3ca6c80501612d0ef968dec2d53b62c2b3b23393be8bf130aa48c1`.
