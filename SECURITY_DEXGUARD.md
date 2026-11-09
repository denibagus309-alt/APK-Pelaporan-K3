# BAS SMART REPORTING — Rencana Integrasi Guardsquare DexGuard

## Status

**Disiapkan — menunggu paket/plugin DexGuard dan lisensi resmi Guardsquare.**

DexGuard adalah produk komersial Android app protection. Plugin Gradle DexGuard untuk pelanggan diperoleh melalui secure Maven repository dan instruksi teknis terbarunya tersedia di DexGuard Customer Portal. Karena artefak plugin, versi, repository URL, dan format lisensi bersifat khusus akun/pelanggan, file build proyek ini sengaja tidak menebak koordinat plugin atau menanam kredensial palsu.

Jangan menggunakan contoh blog DexGuard lama sebagai konfigurasi produksi; Guardsquare menandai tutorial lama tersebut sebagai outdated.

## Perlindungan yang ditargetkan pada varian release

- Obfuscation dan code hardening untuk mempersulit reverse engineering.
- Perlindungan/enkripsi kode dan resource sesuai profil DexGuard yang dilisensikan.
- Runtime Application Self-Protection (RASP), termasuk pemeriksaan integritas dan deteksi upaya manipulasi/inspeksi sesuai konfigurasi produk.
- Build release yang konsisten dan penyimpanan mapping/protection report secara privat untuk keperluan diagnosis.

## Informasi yang diperlukan untuk mengaktifkan integrasi

1. Akses/lisensi DexGuard yang sah dari Guardsquare.
2. Paket atau petunjuk integrasi Gradle terbaru dari DexGuard Customer Portal.
3. Versi plugin/artefak DexGuard dan URL secure Maven repository yang diberikan Guardsquare.
4. Cara autentikasi repository (gunakan environment variable atau `~/.gradle/gradle.properties`; jangan commit username, password, token, private key, atau file lisensi ke Git/ZIP).
5. File konfigurasi/protection profile yang disediakan untuk aplikasi BAS SMART REPORTING.

## Langkah integrasi setelah paket tersedia

1. Tambahkan secure Maven repository dan plugin sesuai dokumentasi Customer Portal.
2. Simpan kredensial hanya di environment lokal/CI secrets.
3. Terapkan plugin dan konfigurasi DexGuard hanya pada varian release yang ditetapkan.
4. Tambahkan keep rules yang benar-benar diperlukan oleh Android/Jetpack Compose, SQLite, dan komponen ekspor setelah analisis build; hindari aturan `-keep` terlalu luas.
5. Jalankan build instrumented/profiling dan buat protection configuration melalui alur resmi DexGuard.
6. Build APK release terlindungi.
7. Uji login Admin, input wajib departemen, SQLite, kamera, video, backup, restore, PDF/JPG/XLSX, serta pemasangan dan pembaruan aplikasi.
8. Simpan mapping file dan protection report secara aman; jangan distribusikan bersama APK.

## Status build saat ini

Belum ada APK DexGuard-protected. Source project awal belum dikompilasi atau diuji pada perangkat fisik. Lingkungan kerja saat ini juga belum menyediakan Gradle executable dan Android SDK, sehingga build Android tidak dapat diverifikasi di sini.

## Sumber resmi

- https://www.guardsquare.com/dexguard
- https://www.guardsquare.com/technical-partners
- https://www.guardsquare.com/blog/setting-up-dexguard-android-studio (halaman ini menyatakan instruksinya sudah usang; gunakan Customer Portal)
