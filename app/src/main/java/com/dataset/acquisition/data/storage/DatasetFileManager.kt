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
 */
class DatasetFileManager(private val context: Context) {

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
     * Mendapatkan direktori kategori untuk sesi aktif.
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
        if (!categoryDir.exists()) categoryDir.mkdirs()
        return categoryDir
    }

    /**
     * Menghasilkan nama file berdasarkan format template preset dengan Auto-Increment ID.
     */
    fun generateFileName(
        template: String,
        category: QualityCategory,
        sampleIndex: Int,
        imageIndex: Int,
        date: Date = Date()
    ): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateStr = dateFormat.format(date)

        val sampleIdStr = String.format(Locale.US, "S%03d", sampleIndex)
        val imageIdStr = String.format(Locale.US, "IMG%03d", imageIndex)

        var fileName = template
            .replace("[kategoriKualitas]", category.displayName, ignoreCase = true)
            .replace("[tanggalAmbil]", dateStr, ignoreCase = true)
            .replace("[sampleid]", sampleIdStr, ignoreCase = true)
            .replace("[imageid]", imageIdStr, ignoreCase = true)

        if (!fileName.lowercase().endsWith(".jpg") && !fileName.lowercase().endsWith(".jpeg")) {
            fileName += ".jpg"
        }
        return fileName
    }

    /**
     * [PEMINDAHAN FILE] Pindahkan foto dari temp ke folder lokasi kategori.
     */
    suspend fun saveSamplePhoto(
        tempFile: File,
        category: QualityCategory,
        session: SessionModel,
        template: String
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (!tempFile.exists() || tempFile.length() == 0L) {
                throw IllegalStateException("File temporary tidak valid.")
            }

            val targetDir = getCategoryDir(category, session)
            val sampleIndex = session.getNextSampleIndex()
            val imageIndex = session.getNextImageIndex(category)
            val fileName = generateFileName(template, category, sampleIndex, imageIndex)
            val destinationFile = File(targetDir, fileName)

            // Pindahkan file (bukan copy)
            val moved = tempFile.renameTo(destinationFile)
            
            if (!moved) {
                // Fallback copy jika renameTo gagal (beda volume storage)
                FileInputStream(tempFile).use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                        output.getFD().sync()
                    }
                }
                if (tempFile.exists()) tempFile.delete()
            }

            if (!destinationFile.exists() || destinationFile.length() == 0L) {
                throw IllegalStateException("Gagal menyimpan file.")
            }

            destinationFile
        }
    }

    /**
     * Simpan sementara ke folder 'temp' di dalam folder sesi.
     */
    fun createTempCaptureFile(session: SessionModel?): File {
        val parentDir = if (session != null) {
            val sessionFolder = File(getDatasetBaseDir(), session.name)
            val tempDir = File(sessionFolder, "temp")
            if (!tempDir.exists()) tempDir.mkdirs()
            tempDir
        } else {
            val globalTemp = File(getDatasetBaseDir(), "temp")
            if (!globalTemp.exists()) globalTemp.mkdirs()
            globalTemp
        }
        
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US).format(Date())
        return File(parentDir, "TEMP_$timeStamp.jpg")
    }
}
