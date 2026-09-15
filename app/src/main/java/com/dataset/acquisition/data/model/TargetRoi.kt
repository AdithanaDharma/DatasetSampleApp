package com.dataset.acquisition.data.model

/**
 * Representasi Region of Interest (ROI) / Bounding Box target (bentuk kotak sederhana)
 * yang dapat diatur posisinya (centerX, centerY) dan ukurannya (width, height)
 * secara bebas (free-form).
 *
 * Menggunakan koordinat ternormalisasi (0.0f s/d 1.0f).
 */
data class TargetRoi(
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val width: Float = 0.45f,
    val height: Float = 0.45f,
    val tolerance: Float = 0.12f
) {
    val left: Float get() = (centerX - width / 2f).coerceIn(0f, 1f)
    val top: Float get() = (centerY - height / 2f).coerceIn(0f, 1f)
    val right: Float get() = (centerX + width / 2f).coerceIn(0f, 1f)
    val bottom: Float get() = (centerY + height / 2f).coerceIn(0f, 1f)

    /**
     * Memeriksa keselarasan objek terdeteksi dengan ROI target.
     */
    fun isAligned(
        objCenterX: Float,
        objCenterY: Float,
        objWidth: Float,
        objHeight: Float
    ): Boolean {
        val dx = kotlin.math.abs(objCenterX - centerX)
        val dy = kotlin.math.abs(objCenterY - centerY)
        val centerAligned = dx <= tolerance && dy <= tolerance

        val widthRatio = objWidth / width
        val heightRatio = objHeight / height
        val sizeAligned = widthRatio in 0.45f..1.45f && heightRatio in 0.45f..1.45f

        return centerAligned && sizeAligned
    }
}
