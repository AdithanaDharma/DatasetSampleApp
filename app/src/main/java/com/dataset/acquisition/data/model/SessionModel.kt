package com.dataset.acquisition.data.model

/**
 * Model data Sesi Foto Penelitian.
 * Mencatat progres sesi untuk mendukung fitur Resume Sesi dan penomoran otomatis.
 */
data class SessionModel(
    val id: String,
    val name: String,
    val startTime: Long = System.currentTimeMillis(),
    val isFinished: Boolean = false,
    val totalSampleCount: Int = 0, // Penomoran Sample ID global dalam sesi ini
    val countBaik: Int = 0,        // Penomoran Image ID per kategori Baik
    val countNormal: Int = 0,      // Penomoran Image ID per kategori Normal
    val countBuruk: Int = 0        // Penomoran Image ID per kategori Buruk
) {
    /**
     * Mendapatkan nomor urut berikutnya untuk kategori tertentu (Image ID).
     */
    fun getNextImageIndex(category: QualityCategory): Int {
        return when (category) {
            QualityCategory.BAIK -> countBaik + 1
            QualityCategory.NORMAL -> countNormal + 1
            QualityCategory.BURUK -> countBuruk + 1
        }
    }

    /**
     * Mendapatkan nomor urut sampel berikutnya secara keseluruhan (Sample ID).
     */
    fun getNextSampleIndex(): Int {
        return totalSampleCount + 1
    }
}
