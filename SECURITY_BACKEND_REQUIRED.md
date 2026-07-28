# Perbaikan keamanan backend SiTeki

Perbaikan sisi Android dan web sudah mengurangi paparan data, tetapi endpoint
Google Apps Script yang dipublikasikan untuk akses anonim tetap harus diperbaiki
di project Apps Script pemilik Spreadsheet. Source Apps Script tidak ada di
repository ini, sehingga perubahan berikut harus dilakukan pada project tersebut.

## Wajib sebelum modul pengguna dan lembur dibuka kembali

1. Ubah login menjadi `POST`; jangan kirim username atau password lewat URL.
2. Simpan password menggunakan hash adaptif dengan salt. Jangan simpan atau
   mengembalikan password plaintext.
3. Setelah login berhasil, keluarkan token sesi acak/bertanda tangan dengan masa
   berlaku singkat. Simpan hanya hash token pada backend.
4. Semua operasi baca dan tulis harus memverifikasi token, masa berlaku, user
   aktif, serta role di server. Pengecekan role di UI bukan kontrol keamanan.
5. Endpoint daftar pengguna tidak boleh mengembalikan password, gaji, alamat,
   nomor telepon, atau data pribadi lain kecuali benar-benar diperlukan dan role
   pemanggil sudah diverifikasi.
6. Endpoint lembur hanya boleh membaca data pemilik token. Rekap seluruh pegawai
   hanya untuk role Admin yang diverifikasi server.
7. Tambahkan rate limit untuk login, audit log tanpa password/token, dan respons
   error generik.
8. Ganti deployment publik lama setelah versi aman aktif. Jangan biarkan URL
   deployment lama tetap menerima request anonim.
9. Karena password pernah dapat dibaca dari endpoint publik, paksa reset seluruh
   password pengguna dan cabut sesi lama.

## Firestore

`firestore.rules` di root repository adalah target aturan aman: pembacaan perlu
Firebase Authentication dan penulisan perlu custom claim `role = Admin`.

Jangan deploy aturan ini sebelum login Android dan web dipindahkan ke Firebase
Authentication, karena aplikasi saat ini belum mengirim identitas Firebase dan
akan kehilangan akses. Setelah migrasi:

```powershell
firebase deploy --only firestore:rules
```

Aktifkan App Check untuk Android dan web, lalu batasi API key pada Google Cloud
Console ke aplikasi/package/domain yang digunakan. API key Firebase bukan
password, tetapi pembatasan key, Authentication, App Check, dan Security Rules
harus dipakai bersama.

## Deployment web

Meta Content Security Policy pada `Siteki_Web/index.html` adalah perlindungan
tambahan. Untuk produksi, kirim juga header HTTP berikut dari hosting:

- `Content-Security-Policy` (samakan dengan kebijakan di HTML)
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: no-referrer`
- `Permissions-Policy: camera=(), microphone=(), geolocation=()`

