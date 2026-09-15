package com.dataset.acquisition.data.model

import android.graphics.RectF

/**
 * Status keselarasan posisi dan skala objek sampel terhadap Target Bounding Box.
 */
enum class AlignmentStatus {
    ALIGNED,        // Objek presisi di tengah dan skala pas
    MISALIGNED,     // Objek terdeteksi namun posisi belum di tengah
    TOO_SMALL,      // Skala objek terlalu kecil / terlalu jauh
    TOO_LARGE,      // Skala objek terlalu besar / terlalu dekat
    NO_OBJECT,      // Tidak ada objek yang terdeteksi
    EDIT_MODE       // Mode kalibrasi free-form
}

/**
 * State realtime yang dikirimkan oleh GlassAlignmentAnalyzer ke UI.
 */
data class AlignmentState(
    val status: AlignmentStatus = AlignmentStatus.NO_OBJECT,
    val detectedBoxNormalized: RectF? = null,
    val message: String = "Arahkan kamera ke sampel gelas",
    val isShutterEnabled: Boolean = false
)
