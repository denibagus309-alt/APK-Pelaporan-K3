# BAS SMART REPORTING — Android Project
Aplikasi lokal Android untuk PT. Bangunperkasa Adhitamasentra.

## Konfigurasi operasional
- Satu perangkat Android yang dioperasikan Admin.
- Seluruh laporan wajib mencatat Bagian/Departemen.
- Offline-first dengan database SQLite lokal.
- Kredit aplikasi: **Dikembangkan oleh Deni**.

## Fitur dalam source saat ini
- Dashboard dan 14 kategori laporan sesuai struktur yang disepakati.
- Subfolder Kotak P3K, Limbah B3, dan Temuan 5R.
- Login Admin lokal dan penggantian PIN. PIN awal source: `123456`; wajib diganti sebelum pemakaian.
- Input laporan dengan departemen wajib, judul, keterangan, tanggal/waktu otomatis.
- Ambil foto melalui kamera dan rekam video melalui aplikasi kamera perangkat.
- Database SQLite dan media disimpan di direktori privat aplikasi.
- Backup database + foto/video ke ZIP dan restore dari ZIP.
- Ekspor PDF, JPG, dan Excel XLSX.
- Filter laporan: semua data, hari ini, dan bulan berjalan.
- Pencarian berdasarkan judul, keterangan, kategori, subfolder, dan departemen.
- Filter departemen serta fitur edit/hapus laporan dengan konfirmasi.
- PDF multi-halaman untuk seluruh hasil filter; JPG merupakan ringkasan satu halaman.

## Build APK
1. Ekstrak ZIP proyek.
2. Buka folder proyek melalui Android Studio.
3. Gunakan JDK 17 dan Android SDK Platform 35.
4. Tunggu Gradle Sync sampai selesai.
5. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. APK debug, jika build berhasil: `app/build/outputs/apk/debug/app-debug.apk`.

## Perubahan v0.3
Lihat `CHANGELOG_v0.3.md` untuk daftar perubahan pencarian, filter departemen, edit/hapus, dan PDF multi-halaman.

## Status validasi
Source sudah diperbarui untuk menyimpan departemen sebagai kolom database (termasuk migrasi database versi sebelumnya) dan menambahkan filter periode laporan. **APK belum dibangun atau diuji** karena lingkungan kerja ini tidak menyediakan Android SDK/Gradle build environment. Fitur kamera, restore, ekspor, kompatibilitas perangkat, dan stabilitas harus diuji melalui build Android Studio dan perangkat Android sebelum digunakan untuk operasional produksi.

## Backup dan kehilangan perangkat
Database dan media berada di direktori privat aplikasi. Jika aplikasi di-uninstall atau data aplikasi dihapus, data lokal dapat hilang. Buat backup ZIP secara berkala dan simpan di lokasi lain seperti komputer, USB OTG, atau penyimpanan cloud.

## Keamanan Guardsquare DexGuard
DexGuard akan diterapkan setelah fitur inti selesai dan build dasar lulus pengujian. Aktivasi build DexGuard memerlukan lisensi/paket dan konfigurasi resmi dari Guardsquare Customer Portal. Jangan memasukkan kredensial atau file lisensi ke source control atau ZIP proyek. Lihat `SECURITY_DEXGUARD.md`.
