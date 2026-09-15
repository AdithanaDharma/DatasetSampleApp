package com.dataset.acquisition.data.model

import androidx.compose.ui.graphics.Color

/**
 * Kategori kualitas sampel cairan penelitian.
 * Setiap kategori memiliki nama folder terpisah di dalam direktori dataset.
 */
enum class QualityCategory(
    val displayName: String,
    val folderName: String,
    val badgeColor: Color
) {
    BAIK("Baik", "Baik", Color(0xFF10B981)),      // Hijau Emerald
    NORMAL("Normal", "Normal", Color(0xFF3B82F6)),  // Biru
    BURUK("Buruk", "Buruk", Color(0xFFEF4444));   // Merah Coral

    companion object {
        fun fromDisplayName(name: String): QualityCategory {
            return entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) } ?: NORMAL
        }
    }
}
