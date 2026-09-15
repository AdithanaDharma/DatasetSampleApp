package com.dataset.acquisition.data.model

/**
 * Model data Sesi Foto Penelitian.
 * Mencatat progres sesi (jumlah foto, Sample ID terakhir, indeks gambar)
 * agar mendukung fitur Resume Sesi (Session Persistence).
 */
data class SessionModel(
    val id: String,
    val name: String,
    val startTime: Long = System.currentTimeMillis(),
    val isFinished: Boolean = false,
    val photoCount: Int = 0,
    val lastSampleId: String = "S001",
    val lastImageIndex: Int = 0
)
