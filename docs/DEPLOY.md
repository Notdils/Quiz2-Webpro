# Deploy ke hosting gratis (Render + Neon/Supabase)

Aplikasi berjalan sebagai container Docker (Tomcat 10.1 + `ROOT.war`). Render tidak punya disk persisten, jadi semua data (termasuk foto pohon) disimpan di PostgreSQL.

## 1. Database PostgreSQL gratis

Pilih salah satu:

- **Neon** (<https://neon.tech>): buat project → salin connection string.
- **Supabase** (<https://supabase.com>): Project Settings → Database → Connection string (pakai *Session pooler* atau *Direct*).

Ubah ke format JDBC:

```
postgresql://USER:PASSWORD@HOST:5432/DBNAME?sslmode=require
→ DB_URL      = jdbc:postgresql://HOST:5432/DBNAME?sslmode=require
  DB_USERNAME = USER
  DB_PASSWORD = PASSWORD
```

Database harus **kosong** (atau sudah berisi skema JSP ini). Tabel dan data demo dibuat otomatis saat aplikasi pertama kali jalan.

## 2. Web service di Render

1. Push repo ke GitHub (publik).
2. Render → **New → Blueprint** → pilih repo ini (memakai `render.yaml`), atau **New → Web Service → Docker**.
3. Isi environment variables:
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
   - `SEED_ADMIN_PASSWORD`, `SEED_STAFF_PASSWORD`, `SEED_VIEWER_PASSWORD` (wajib di produksi; seeder menolak jalan tanpa ini)
   - opsional: `SEED_ADMIN_EMAIL`, `APP_NAME`, `SEED_DEMO_DATA=false`
4. Deploy. Build pertama ±5 menit. URL: `https://<nama>.onrender.com`.

Catatan paket gratis Render: service tidur setelah 15 menit tanpa trafik; request pertama setelahnya butuh ±30–60 detik. Session login ikut hilang saat service restart (pengguna cukup login ulang).

## 3. Cek setelah deploy

```bash
BASE=https://<nama>.onrender.com bash scripts/smoke.sh   # hanya jika password seed = "password"
```

Atau manual: login sebagai admin, buka setiap menu, unggah foto pohon, ekspor CSV di Reports.

## Alternatif

Image yang sama bisa jalan di Koyeb, Railway, atau Fly.io: semuanya membaca `$PORT`, dan Dockerfile sudah menyesuaikan port Tomcat dari variabel itu.
