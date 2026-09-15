package com.dataset.acquisition.data.storage

import android.content.Context
import android.os.Environment
import com.dataset.acquisition.data.model.QualityCategory
import com.dataset.acquisition.data.model.SessionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pengelola penyimpanan dataset foto sampel penelitian.
 * Bertanggung jawab membuat folder per kategori, penamaan file terstruktur,
 * serta memastikan penulisan file bersifat asynchronous dan benar-benar resolved.
 */
class DatasetFileManager(private val context: Context) {

    /**
     * Mendapatkan direktori dasar dataset.
     */
    fun getDatasetBaseDir(): File {
        val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val baseDir = File(publicDocs, "ResearchDataset")

        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED && canWriteToDir(baseDir)) {
            baseDir
        } else {
            val fallbackDir = File(context.getExternalFilesDir(null), "Dataset")
            if (!fallbackDir.exists()) fallbackDir.mkdirs()
            fallbackDir
        }
    }

    private fun canWriteToDir(dir: File): Boolean {
        return try {
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created) return false
            }
            val testFile = File(dir, ".test_write")
            val writable = testFile.createNewFile()
            if (writable) testFile.delete()
            writable
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Mendapatkan direktori kategori untuk sesi aktif (atau direktori dasar jika tanpa sesi).
     * Contoh: .../ResearchDataset/Sesi_01/Baik/
     */
    fun getCategoryDir(category: QualityCategory, session: SessionModel? = null): File {
        val parentDir = if (session != null) {
            val sessionFolder = File(getDatasetBaseDir(), session.name)
            if (!sessionFolder.exists()) sessionFolder.mkdirs()
            sessionFolder
        } else {
            getDatasetBaseDir()
        }

        val categoryDir = File(parentDir, category.folderName)
        if (!categoryDir.exists()) {
            categoryDir.mkdirs()
        }
        return categoryDir
    }

    /**
     * Menghasilkan nama file berdasarkan format template preset.
     *
     * Format Default: [kategoriKualitas]_[tanggalAmbil]_[sampleid]_[imageid].jpg
     * Contoh Output: Baik_20261024_S001_IMG001.jpg
     */
    fun generateFileName(
        template: String,
        category: QualityCategory,
        sampleId: String,
        imageIndex: Int,
        date: Date = Date()
    ): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateStr = dateFormat.format(date)

        val cleanSampleId = if (sampleId.isBlank()) "S001" else sampleId.trim()
        val imageIdStr = String.format(Locale.US, "IMG%03d", imageIndex)

        var fileName = template
            .replace("[kategoriKualitas]", category.displayName, ignoreCase = true)
            .replace("[tanggalAmbil]", dateStr, ignoreCase = true)
            .replace("[sampleid]", cleanSampleId, ignoreCase = true)
            .replace("[imageid]", imageIdStr, ignoreCase = true)

        if (!fileName.lowercase(Locale.getDefault()).endsWith(".jpg") &&
            !fileName.lowercase(Locale.getDefault()).endsWith(".jpeg")
        ) {
            fileName += ".jpg"
        }

        return fileName
    }

    /**
     * Mendapatkan indeks nomor urut foto berikutnya untuk sampel dan kategori tertentu.
     */
    fun getNextImageIndex(category: QualityCategory, sampleId: String, session: SessionModel? = null): Int {
        val folder = getCategoryDir(category, session)
        val cleanSampleId = if (sampleId.isBlank()) "S001" else sampleId.trim()

        val files = folder.listFiles { file ->
            val name = file.name
            name.contains("_${cleanSampleId}_", ignoreCase = true) ||
                    name.startsWith("${cleanSampleId}_", ignoreCase = true) ||
                    name.contains("_${cleanSampleId}.", ignoreCase = true)
        } ?: return 1

        return files.size + 1
    }

    /**
     * [BUG FIX] Async File Handling:
     * Menyimpan file foto dari cache sementara ke direktori permanen kategori dataset.
     * Memastikan penulisan stream completely flushed, closed, & verified sebelum resolved.
     */
    suspend fun saveSamplePhoto(
        tempFile: File,
        category: QualityCategory,
        sampleId: String,
        template: String,
        session: SessionModel? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(tempFile.exists() && tempFile.length() > 0) { "File foto temporary tidak valid atau kosong" }

            val targetDir = getCategoryDir(category, session)
            val nextIndex = getNextImageIndex(category, sampleId, session)
            val fileName = generateFileName(template, category, sampleId, nextIndex)
            val destinationFile = File(targetDir, fileName)

            FileInputStream(tempFile).use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }

            // Verifikasi fisik file terpasang dan tidak 0 bytes sebelum resolved
            if (!destinationFile.exists() || destinationFile.length() == 0L) {
                throw IllegalStateException("Gagal memverifikasi keberadaan file hasil simpan: ${destinationFile.absolutePath}")
            }

            // Hapus file temp setelah berhasil dipindah
            if (tempFile.exists()) {
                tempFile.delete()
            }

            destinationFile
        }
    }

    /**
     * Mendapatkan file cache sementara untuk penyimpanan shutter capture.
     */
    fun createTempCaptureFile(): File {
        val cacheDir = context.cacheDir
        return File.createTempFile("capture_preview_", ".jpg", cacheDir)
    }
}
