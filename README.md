# Aplikasi Android Akuisisi Dataset Sampel Penelitian Cairan

Aplikasi mobile Android berbasis **Kotlin**, **Jetpack Compose**, **CameraX**, dan **Google ML Kit** yang dirancang untuk pengumpulan dataset citra sampel cairan dalam gelas. Aplikasi ini memastikan **konsistensi visual** dan mengotomasi pengorganisasian dataset dengan fitur **Auto-ID** dan **Session Management**.

---

## Fitur Utama

### 1. Kontrol Kamera Pro & Zoom Lock
- **Pro Camera Settings**: Mengunci ISO (min 50), Shutter Speed (1/12000s s/d 32s), dan White Balance (Manual Kelvin 2000K-12000K).
- **Zoom Lock**: Memastikan perbesaran gambar tetap konstan antar sampel.
- **Grid Viewfinder**: Garis bantu 3x3 untuk komposisi yang presisi.

### 2. Manajemen Sesi & Auto-Increment ID
- **Sistem Sesi**: Pengelompokkan foto per sesi penelitian (misal: Sesi_Uji_A).
- **Auto-Increment ID**: 
  - **Sample ID**: Nomor urut sampel global dalam satu sesi.
  - **Image ID**: Nomor urut gambar di dalam kategori spesifiknya.
- **Session Persistence**: Mendeteksi sesi yang belum selesai saat aplikasi dibuka kembali (Resume Session).

### 3. Penyimpanan & Export Otomatis
- **Folder Temp**: Foto disimpan sementara di folder `temp` di dalam direktori sesi.
- **Auto-Move**: File dipindahkan secara otomatis ke folder kategori (**Baik**, **Normal**, **Buruk**) setelah dikonfirmasi.
- **Zip & Share WhatsApp**: Mengompresi seluruh folder sesi menjadi file `.zip` dan membagikannya langsung ke WhatsApp.

### 4. Sistem Alignment & Bounding Box
- **Free-form Calibration**: Atur ukuran dan posisi bounding box secara bebas dengan 8 handle interaktif.
- **Deteksi Skala & Posisi (ML Kit)**: Peringatan realtime jika posisi meleset atau skala objek terlalu besar/kecil.
- **Safety Shutter**: Opsi kunci shutter otomatis hingga posisi sampel tepat.

---

## Cara Menjalankan

1. Buka folder di **Android Studio**.
2. Sambungkan HP Android (USB Debugging aktif).
3. Klik **Run 'app'**.
4. Izin kamera dan storage diperlukan.

---

## Tips Pengambilan Dataset
1. **Tripod**: Gunakan stand agar jarak konstan.
2. **Pencahayaan**: Gunakan mode **Manual Kelvin** untuk warna yang konsisten.
3. **Background**: Gunakan warna kontras di belakang gelas untuk akurasi ML Kit.
