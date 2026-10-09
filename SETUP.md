# Menjalankan SmartPantry

## 1. Siapkan perangkat lunak

Pasang Git, Android Studio, dan JDK 21. Anda dapat memilih atau mengunduh JDK 21 lewat pengaturan Gradle JDK di Android Studio. JDK 17 tidak cocok dengan target Java/Kotlin 21 pada project ini.

Buka **Tools > SDK Manager** dan pasang:

- Android SDK Platform 36.
- Android SDK Build-Tools 35.0.0 (default AGP 8.9.2); versi 36.0.0 juga dapat dipasang.
- Android SDK Platform-Tools.
- Android Emulator dan system image API 36 jika ingin memakai emulator.

Internet diperlukan saat mengunduh SDK, Gradle, dan dependency pertama kali.

## 2. Clone dan buka project

```powershell
git clone https://github.com/Ferrs05/SmartPantry-.git
cd SmartPantry-
```

Di Android Studio, pilih **Open**, lalu buka folder `SmartPantry-`. Folder ini harus langsung berisi `settings.gradle.kts`, `app`, `data`, dan `domain`. Jangan membuka folder `app` sebagai project terpisah.

Izinkan Android Studio membuka project tepercaya jika muncul dialog, lalu tunggu Gradle Sync selesai.

## 3. Atur JDK dan SDK

Buka **File > Settings > Build, Execution, Deployment > Build Tools > Gradle**. Pada macOS, buka **Android Studio > Settings**. Pilih JDK 21 pada **Gradle JDK**, lalu sync ulang.

Android Studio biasanya membuat `local.properties` dengan lokasi SDK di komputer Anda. File ini tidak masuk Git. Jika perlu membuatnya sendiri, contoh Windows:

```properties
sdk.dir=C:/Users/NAMA_USER/AppData/Local/Android/Sdk
```

Ganti `NAMA_USER` dan path tersebut sesuai lokasi SDK yang tertera di SDK Manager. Gunakan garis miring `/` agar path Windows tidak memerlukan escape.

Model, SQLite, konfigurasi, font, dan foto sudah ada di project. Anda tidak perlu mengunduh dataset atau mengisi API key untuk menjalankan aplikasi.

## 4. Buat emulator

1. Buka **Tools > Device Manager**.
2. Pilih **Create Virtual Device**, lalu profil ponsel, misalnya Pixel 6.
3. Pilih system image Android API 36. Gunakan x86_64 pada Intel/AMD atau ARM64 pada host ARM yang mendukungnya.
4. Selesaikan pembuatan AVD dan jalankan sampai layar beranda Android tampil.
5. Pilih AVD tersebut pada daftar perangkat di toolbar Android Studio.

Untuk mencoba kamera, buka konfigurasi AVD dan **Show Advanced Settings**, lalu atur kamera belakang ke **Virtual Scene** atau webcam. Anda juga bisa memasukkan gambar ke emulator dan memilihnya lewat galeri aplikasi. Pilihan bahan manual dapat digunakan tanpa kamera.

Jika emulator gagal berjalan, periksa virtualisasi CPU di BIOS/UEFI dan akselerasi emulator pada komputer Anda.

## 5. Jalankan aplikasi

Pilih konfigurasi **app**, lalu tekan **Run** atau **Shift+F10**. Jika belum muncul, tunggu sync selesai, lalu pilih **Run > Edit Configurations > + > Android App**, dan pilih module `SmartPantry.app` atau `app`.

Pada beranda:

1. Pilih **Scan bahan** dan berikan izin kamera, atau pilih **Buka galeri**.
2. Ambil/pilih foto. Periksa bahan yang terdeteksi dan koreksi jika perlu.
3. Pilih **Cari resep**, gunakan pencarian/filter, lalu buka resep.

Untuk mencoba tanpa foto, pilih **Pilih bahan**, **Tambahkan bahan**, pilih bahan, tekan **Selesai**, lalu **Cari resep**. Shortcut bahan dan kartu resep beranda juga bisa langsung dibuka.

## 6. Jalankan pada ponsel

Ponsel harus menggunakan Android 8/API 26 atau lebih baru.

1. Aktifkan **Developer options** dan **USB debugging**.
2. Hubungkan ponsel lewat USB dan setujui dialog otorisasi debugging.
3. Pilih ponsel di Android Studio, lalu tekan **Run**.

Setelah instalasi, aplikasi dapat digunakan tanpa internet. Foto tidak dikirim ke server. Tautan sumber/kredit menggunakan browser jika dibuka.

## 7. Build melalui terminal

Jalankan dari root repository. Android SDK harus sudah tersedia melalui `local.properties` atau konfigurasi SDK lingkungan Anda.

Windows PowerShell:

```powershell
# Ganti dengan folder JDK 21 di komputer Anda.
$env:JAVA_HOME = 'C:/path/to/jdk-21'
.\gradlew.bat :app:assembleDebug
```

macOS/Linux:

```bash
export JAVA_HOME=/path/to/jdk-21
chmod +x gradlew
./gradlew :app:assembleDebug
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`. Anda dapat memasangnya dengan Android Studio atau Android SDK Platform-Tools:

```powershell
adb devices
# Ganti SERIAL dengan serial perangkat pada output adb devices.
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
```

APK ini memakai tanda tangan debug untuk pengembangan. Setelah dependency tersedia di cache, `--offline` dapat dipakai saat build. Jangan memakai mode offline pada sync/build pertama.

`Build.ps1` juga tersedia. Defaultnya menjalankan build, tes domain, dan lint. Untuk build saja:

```powershell
.\Build.ps1 -Tasks ':app:assembleDebug'
```

## 8. Pengujian opsional

Unit test dan lint:

```powershell
.\gradlew.bat :domain:test :app:lintDebug
```

Instrumentasi memerlukan ponsel atau emulator yang sudah menyala:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Gunakan emulator khusus pengujian. Tes dapat membuka kamera dan mengubah data aplikasi pada perangkat uji. Laporan Gradle ada di folder `build/reports` pada module terkait.

Audit database dan foto dengan Python:

```powershell
python -m pip install Pillow
python tools/test_recipes.py
python tools/test_photo_assets.py
```

Script pembentukan ulang data/foto di `tools` tidak diperlukan untuk menjalankan aplikasi. Perubahan database dapat memengaruhi pemetaan ID foto.

## Masalah umum

| Gejala | Langkah |
|---|---|
| SDK location not found | Periksa path SDK pada local.properties. |
| JVM target 21 atau Unsupported class file | Pilih Gradle JDK 21; terminal juga harus memakai JAVA_HOME yang sesuai. |
| Dependency tidak ditemukan saat sync | Matikan Gradle Offline dan periksa koneksi internet. |
| Perangkat tidak muncul | Jalankan AVD atau periksa USB debugging dan otorisasi ponsel. |
| Kamera emulator kosong | Atur kamera AVD, gunakan galeri, atau pilih bahan manual. |
| System UI/Pixel Launcher tidak merespons | Tutup emulator, gunakan Cold Boot, dan coba image API 36. Jalankan satu emulator jika RAM terbatas. |
| APK tidak bisa memperbarui instalasi lama | Periksa application ID dan tanda tangan. Jika berbeda, uninstall versi lama; data lokal aplikasi ikut hilang. |

Panduan ini mengikuti versi `0.3.0-v2-ui`. Hasil validasi terakhir: [COMPACT_UI_VALIDATION.md](COMPACT_UI_VALIDATION.md).
