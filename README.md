# Aplikasi Android Akuisisi Dataset Sampel Penelitian Cairan

Aplikasi mobile Android berbasis **Kotlin**, **Jetpack Compose**, **CameraX (Camera2Interop)**, dan **Google ML Kit** yang dirancang untuk pengumpulan dataset citra sampel cairan dalam gelas. Aplikasi ini memastikan **konsistensi visual antar waktu/hari pengambilan** serta mengotomasi pengorganisasian dataset untuk riset/Tugas Akhir.

---

## Fitur Utama

### 1. Kontrol Kamera Manual & Sistem Preset
- **Pro Camera Locking**: Mengunci parameter kamera secara konsisten:
  - **ISO**: Pilihan sensitivitas sensor (100, 200, 400, 800, 1600).
  - **Shutter Speed (Exposure Time)**: 1/30s, 1/60s, 1/125s, 1/250s, 1/500s.
  - **White Balance**: Pilihan mode Fluorescent (Lab), Daylight, Incandescent, atau Auto.
  - **Exposure Value (EV)**: Exposure compensation index.
- **Preset Manager**: Menyimpan profil pencahayaan studio/lab yang dapat diakses melalui chip selector sekali klik.

### 2. Sistem Alignment & Bounding Box Manual
- **Target Bounding Box Fleksibel per Preset**: Posisi (`centerX`, `centerY`) dan dimensi (`width`, `height`) disimpan per preset untuk mengakomodasi berbagai jenis wadah (beaker glass, tabung reaksi, erlenmeyer).
- **Mode Kalibrasi Interaktif**: Tekan tombol ikon crop di kanan atas layar untuk menggeser (*drag*) dan mencubit (*pinch/resize*) kotak target langsung di atas layar.
- **Deteksi Posisi Realtime (Google ML Kit On-Device)**:
  - Berjalan offline tanpa memerlukan koneksi internet dan tanpa perlu training model terlebih dahulu.
  - Jika posisi meleset: Muncul banner peringatan *"Posisi sample belum pas"*.
  - Jika posisi tepat: Peringatan menghilang, indikator berubah hijau *"Posisi Pas"*, dan shutter terbuka.
- **Safety Shutter Gating & Bypass**: Shutter terkunci otomatis saat posisi belum pas untuk mencegah foto blur/meleset. Tersedia toggle override di menu pengaturan jika ingin mematikan kunci otomatis.

### 3. Alur Pengambilan & Konfirmasi (User Flow)
1. **Viewfinder & Guide**: Posisikan gelas ke dalam kotak panduan hingga indikator hijau.
2. **Shutter Capture**: Mengambil gambar beresolusi tinggi.
3. **Halaman Preview**:
   - Tombol **"Foto Ulang"**: Menghapus buffer foto dan kembali ke kamera.
   - Tombol **"Gunakan Foto"**: Membuka dialog pelabelan.
4. **Dialog Kategorisasi Kualitas**:
   - Pilihan: **"Baik"**, **"Normal"**, dan **"Buruk"**.
   - Input **Sample ID** (misal: `S001`).

### 4. Penyimpanan Terstruktur & Format Penamaan Otomatis
- Foto secara otomatis dipisahkan ke subfolder:
  - `Documents/ResearchDataset/Baik/`
  - `Documents/ResearchDataset/Normal/`
  - `Documents/ResearchDataset/Buruk/`
- Format penamaan berbasis template dinamis (dapat dikonfigurasi di preset):
  - **Format Default**: `[kategoriKualitas]_[tanggalAmbil]_[sampleid]_[imageid].jpg`
  - **Contoh Output**: `Baik_20261024_S001_IMG001.jpg`
  - Auto-incrementing nomor urut gambar (`IMG001`, `IMG002`, dst.) per sample.

---

## Struktur Direktori Proyek

```
app/src/main/
├── AndroidManifest.xml
├── java/com/dataset/acquisition/
│   ├── MainActivity.kt                      # Navigasi & Permission Lifecycle
│   ├── camera/
│   │   ├── ManualCameraController.kt        # Camera2Interop Manual ISO/Shutter/WB
│   │   └── GlassAlignmentAnalyzer.kt        # ML Kit Realtime Object Detection
│   ├── data/
│   │   ├── model/
│   │   │   ├── AlignmentState.kt            # State keselarasan & status peringatan
│   │   │   ├── CameraPreset.kt              # Model preset kamera & exposure
│   │   │   ├── QualityCategory.kt           # Enum Baik, Normal, Buruk & folder
│   │   │   └── TargetRoi.kt                 # Model koordinat & ukuran Bounding Box
│   │   ├── repository/
│   │   │   └── PresetRepository.kt          # Koleksi preset & state aktif
│   │   └── storage/
│   │       └── DatasetFileManager.kt        # Auto-partition folder & file naming engine
│   └── ui/
│       ├── CameraViewModel.kt               # State management terpadu
│       ├── camera/
│       │   └── CameraScreen.kt              # Viewfinder, preset selector & shutter
│       ├── components/
│       │   └── InteractiveBoundingBoxOverlay.kt # Canvas pemandu gelas & kalibrasi drag
│       ├── dialog/
│       │   ├── PresetConfigDialog.kt        # Dialog edit ISO, Shutter, Format Nama
│       │   └── QualityLabelDialog.kt        # Dialog pilih Baik/Normal/Buruk & Sample ID
│       ├── preview/
│       │   └── PreviewScreen.kt             # Halaman pratinjau foto hasil capture
│       └── theme/
│           ├── Color.kt
│           └── Theme.kt
└── res/
    ├── values/
    │   ├── strings.xml
    │   └── themes.xml
    └── xml/
```

---

## Cara Menjalankan di Android Studio

1. Buka folder ini di **Android Studio** (Koala / Ladybug / Meerkat atau yang lebih baru).
2. Tunggu proses **Gradle Sync** selesai.
3. Sambungkan perangkat smartphone Android fisik menggunakan kabel USB (pastikan opsi *USB Debugging* aktif).
4. Klik **Run 'app'** (`Shift + F10`).
5. Izinkan akses kamera saat dialog permission pertama kali muncul.

---

## Tips Pengambilan Dataset yang Optimal
1. **Gunakan Tripod/Stand HP**: Jarak dan sudut pandang kamera ke meja sampel harus tetap konstan.
2. **Pencahayaan Konsisten**: Gunakan lampu meja LED atau studio mini box. Kunci White Balance pada mode `Fluorescent` agar warna sampel tidak berubah-ubah.
3. **Background Kontras**: Gunakan latar belakang kertas karton putih atau hitam di belakang gelas agar kontur gelas dan cairan terdeteksi dengan akurasi 95%+ oleh ML Kit.
