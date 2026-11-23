# Handoff SmartPantry

## Yang sedang dikerjakan

SmartPantry adalah aplikasi Android untuk mendeteksi bahan dari foto dan mencari resep secara offline. Fokus perubahan terbaru adalah memperketat rekomendasi dan menyaring koleksi resep agar sesuai dengan 18 kelas model YOLO11n v2.

- Resep tampil jika minimal 80% jenis bahan yang terpetakan pada resep tersedia. Empat dari lima jenis lolos, tiga dari lima tidak.
- Semua kelas ikut dihitung, termasuk bawang merah, bawang putih, cabai, dan daun bawang. Tombol filter kelengkapan mensyaratkan semua bahan terpetakan tersedia.
- Garam, air, minyak, santan, dan bumbu dasar lain dalam daftar pengecualian boleh ada di resep. Bumbu di luar 18 kelas tidak masuk hitungan.
- Penyaringan menyisakan **713 dari 9.272 resep**. Sebanyak **251 resep** memiliki foto contoh hidangan. ID resep yang lolos tetap dipakai untuk pencocokan foto.
- Room memakai nama basis data lokal baru, `resep-scan18-v1.db`, agar aplikasi mengambil aset tersaring saat dibuka setelah pembaruan.

## Titik masuk kode dan data

| Bagian | Lokasi |
|---|---|
| Ambang 80% dan urutan rekomendasi | [Models.kt](domain/src/main/kotlin/id/smartpantry/domain/Models.kt) |
| Pencarian kandidat dan pembukaan SQLite | [Recipes.kt](data/src/main/kotlin/id/smartpantry/data/Recipes.kt) |
| Pilihan bahan dari deteksi atau input manual | [PantryViewModel.kt](app/src/main/java/id/smartpantry/app/presentation/PantryViewModel.kt) |
| Filter dan keterangan kecocokan di layar | [PantryApp.kt](app/src/main/java/id/smartpantry/app/presentation/PantryApp.kt) |
| Aturan penyaringan resep | [filter_scan18_recipes.py](tools/filter_scan18_recipes.py) |
| Alasan resep keluar dari koleksi | [recipe-scan18-audit.csv](test-evidence/recipe-scan18-audit.csv) |
| Sumber dan jumlah resep | [recipe_provenance.json](app/src/main/assets/recipe_provenance.json) |
| Sumber serta lisensi foto | [PHOTO_SOURCES.md](PHOTO_SOURCES.md) |

Resep berasal dari [Indonesian Food Recipes di Kaggle](https://www.kaggle.com/datasets/canggih/indonesian-food-recipes). Model, SQLite, dan foto tersedia di aset aplikasi. Penyaringan memakai daftar kata dan menyingkirkan istilah yang tidak dikenali, termasuk kemungkinan singkatan atau catatan masak pada resep yang sebenarnya valid. Baca kolom `unrecognized_tokens` sebelum memutuskan apakah satu resep perlu dipulihkan.

## Cara melanjutkan

1. Ikuti [SETUP.md](SETUP.md). Buka root proyek di Android Studio dengan Gradle JDK 21 dan SDK Platform 36.
2. Jalankan konfigurasi `app` pada emulator. Coba **Pilih bahan** untuk memeriksa rekomendasi tanpa kamera.
3. Periksa batas 80% dengan bahan yang diketahui, lalu ulangi melalui kamera atau galeri. Periksa resep dengan empat dari lima bahan terpetakan dan bandingkan dengan tiga dari lima.
4. Sebelum menjalankan ulang skrip filter, siapkan masukan berupa database penuh beserta indeks foto dan catatan asal data. Versi database sebelum penyaringan tersedia pada commit `db6adb4`. Skrip memakai salinan di `photo-revision-backup/recipe-before-scan18/` sebagai masukan setelah salinan itu ada. Jangan jadikan database 713 resep sebagai data awal bila ingin mengaudit ulang 9.272 resep.

Build debug untuk versi tersaring sudah berhasil. Pemasangan terakhir ke emulator belum berhasil karena perangkat tidak terhubung. Pengujian otomatis versi ini belum dijalankan.
