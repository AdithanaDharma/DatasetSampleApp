package com.dataset.acquisition.data.model

import android.hardware.camera2.CameraMetadata

/**
 * Pilihan mode White Balance kamera (Auto, Manual Kelvin, atau Presets).
 */
enum class WhiteBalanceMode(val title: String, val camera2Mode: Int) {
    AUTO("Auto WB", CameraMetadata.CONTROL_AWB_MODE_AUTO),
    MANUAL("Manual Kelvin (K)", CameraMetadata.CONTROL_AWB_MODE_OFF),
    DAYLIGHT("Siang Hari (Daylight)", CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT),
    FLUORESCENT("Lampu Fluorescent (Lab)", CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT),
    INCANDESCENT("Lampu Pijar (Tungsten)", CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT),
    CLOUDY("Mendung (Cloudy)", CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT)
}

/**
 * Model data Preset Kamera Pro Mode.
 * ISO min: 50
 * Shutter Speed: 1/12000s s/d 32s (termasuk 1/50s)
 * White Balance: Preset atau Manual Kelvin (misal 5500K)
 * Zoom Lock: Nilai perbesaran (misal 1.0x, 1.5x, 2.0x) agar skala konsisten
 */
data class CameraPreset(
    val id: String,
    val name: String,
    val description: String = "",

    // Parameter Manual Kamera Pro
    val iso: Int = 100,                          // Min ISO 50 (50, 100, 200, 400, 800, 1600, 3200, 6400)
    val shutterSpeedNumerator: Long = 1L,        // 1s
    val shutterSpeedDenominator: Long = 50L,     // 1/50s (termasuk 1/12000s s/d 32s)
    val wbMode: WhiteBalanceMode = WhiteBalanceMode.FLUORESCENT,
    val wbKelvin: Int = 5500,                    // Nilai Manual Kelvin WB (2000K - 12000K)
    val exposureCompensationIndex: Int = 0,      // EV step (0 = normal)
    val zoomRatio: Float = 1.0f,                 // Zoom Lock untuk perbesaran konsisten (1.0f - 5.0f)

    // Pengaturan Target Bounding Box
    val targetRoi: TargetRoi = TargetRoi(),

    // Format Penamaan File
    val namingTemplate: String = "[kategoriKualitas]_[tanggalAmbil]_[sampleid]_[imageid].jpg",

    // Safety Gating
    val autoLockShutter: Boolean = true
) {
    /**
     * White balance title untuk display singkat
     */
    val whiteBalanceTitle: String
        get() = if (wbMode == WhiteBalanceMode.MANUAL) "${wbKelvin}K" else wbMode.title

    /**
     * Menghitung Shutter Speed dalam nanodetik untuk Camera2 SENSOR_EXPOSURE_TIME.
     * Contoh 1/50s = 20_000_000L nanodetik
     */
    val shutterSpeedNanoseconds: Long
        get() = (1_000_000_000L * shutterSpeedNumerator) / shutterSpeedDenominator.coerceAtLeast(1L)

    /**
     * Label representasi shutter speed (misal: "1/50s", "1/12000s" atau "32s")
     */
    val shutterSpeedLabel: String
        get() = if (shutterSpeedDenominator <= 1L) {
            "${shutterSpeedNumerator}s"
        } else {
            "$shutterSpeedNumerator/${shutterSpeedDenominator}s"
        }
}
