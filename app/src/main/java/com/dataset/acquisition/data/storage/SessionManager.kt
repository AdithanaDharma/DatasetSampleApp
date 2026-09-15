package com.dataset.acquisition.data.storage

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.FileProvider
import com.dataset.acquisition.data.model.QualityCategory
import com.dataset.acquisition.data.model.SessionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pengelola Sesi Foto Penelitian, Session Persistence, Auto-Zip, dan Share WhatsApp.
 */
class SessionManager(
    private val context: Context,
    private val fileManager: DatasetFileManager
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("session_data_prefs", Context.MODE_PRIVATE)

    private val _activeSession = MutableStateFlow<SessionModel?>(loadActiveSessionFromPrefs())
    val activeSession: StateFlow<SessionModel?> = _activeSession.asStateFlow()

    /**
     * Memulai sesi foto baru.
     */
    fun startNewSession(customName: String = ""): SessionModel {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val nameStr = if (customName.isBlank()) "Sesi_$dateStr" else customName.trim()
        val sessionId = "Sesi_$dateStr"

        val newSession = SessionModel(
            id = sessionId,
            name = nameStr,
            startTime = System.currentTimeMillis(),
            isFinished = false,
            photoCount = 0,
            lastSampleId = "S001",
            lastImageIndex = 0
        )

        _activeSession.value = newSession
        saveSessionToPrefs(newSession)
        return newSession
    }

    /**
     * Mendapatkan folder khusus untuk sesi aktif.
     * Contoh: .../ResearchDataset/Sesi_20261024_101500/
     */
    fun getSessionDir(session: SessionModel): File {
        val baseDir = fileManager.getDatasetBaseDir()
        val sessionDir = File(baseDir, session.name)
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        return sessionDir
    }

    /**
     * Mendapatkan sub-folder kategori di dalam folder sesi.
     * Contoh: .../ResearchDataset/Sesi_20261024_101500/Baik/
     */
    fun getSessionCategoryDir(session: SessionModel, category: QualityCategory): File {
        val sessionDir = getSessionDir(session)
        val categoryDir = File(sessionDir, category.folderName)
        if (!categoryDir.exists()) {
            categoryDir.mkdirs()
        }
        return categoryDir
    }

    /**
     * Mencatat progres foto baru dan menyimpannya secara instan (Session Persistence).
     */
    fun recordPhotoSaved(sampleId: String) {
        val current = _activeSession.value ?: startNewSession()
        val updated = current.copy(
            photoCount = current.photoCount + 1,
            lastSampleId = sampleId,
            lastImageIndex = current.lastImageIndex + 1
        )
        _activeSession.value = updated
        saveSessionToPrefs(updated)
    }

    /**
     * Memilih untuk melanjutkan sesi sebelumnya (Resume Session).
     */
    fun resumeSession(session: SessionModel) {
        _activeSession.value = session.copy(isFinished = false)
        saveSessionToPrefs(_activeSession.value!!)
    }

    /**
     * Mengakhiri sesi foto aktif.
     */
    fun finishActiveSession() {
        _activeSession.value = null
        clearActiveSessionPrefs()
    }

    /**
     * Mengompresi seluruh isi folder sesi menjadi satu file .zip (Background Task).
     */
    suspend fun zipSessionFolder(session: SessionModel): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val sessionDir = getSessionDir(session)
            val baseDir = fileManager.getDatasetBaseDir()
            val zipFile = File(baseDir, "${session.name}.zip")

            if (zipFile.exists()) {
                zipFile.delete()
            }

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
                zipFolderRecursive(sessionDir, sessionDir.name, zipOut)
            }

            zipFile
        }
    }

    private fun zipFolderRecursive(fileToZip: File, parentName: String, zipOut: ZipOutputStream) {
        if (fileToZip.isDirectory) {
            val children = fileToZip.listFiles() ?: return
            for (child in children) {
                zipFolderRecursive(child, "$parentName/${child.name}", zipOut)
            }
        } else {
            val buffer = ByteArray(2048)
            FileInputStream(fileToZip).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    val entry = ZipEntry(parentName)
                    zipOut.putNextEntry(entry)
                    var bytesRead: Int
                    while (bis.read(buffer).also { bytesRead = it } != -1) {
                        zipOut.write(buffer, 0, bytesRead)
                    }
                    zipOut.closeEntry()
                }
            }
        }
    }

    /**
     * Memicu Share Intent untuk membagikan file .zip langsung ke WhatsApp / Chooser.
     */
    fun shareZipFile(context: Context, zipFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, zipFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Dataset Penelitian - ${zipFile.nameWithoutExtension}")
                putExtra(Intent.EXTRA_TEXT, "File zip dataset foto penelitian: ${zipFile.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                
                // Prioritaskan WhatsApp
                setPackage("com.whatsapp")
            }

            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // Fallback ke Android Chooser jika WhatsApp tidak terpasang
            try {
                val authority = "${context.packageName}.fileprovider"
                val contentUri: Uri = FileProvider.getUriForFile(context, authority, zipFile)

                val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Dataset Penelitian - ${zipFile.nameWithoutExtension}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(chooserIntent, "Bagikan Dataset Zip"))
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun saveSessionToPrefs(session: SessionModel) {
        try {
            val obj = JSONObject().apply {
                put("id", session.id)
                put("name", session.name)
                put("startTime", session.startTime)
                put("isFinished", session.isFinished)
                put("photoCount", session.photoCount)
                put("lastSampleId", session.lastSampleId)
                put("lastImageIndex", session.lastImageIndex)
            }
            prefs.edit().putString("active_session_json", obj.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadActiveSessionFromPrefs(): SessionModel? {
        val jsonStr = prefs.getString("active_session_json", null) ?: return null
        return try {
            val obj = JSONObject(jsonStr)
            val isFinished = obj.optBoolean("isFinished", false)
            if (isFinished) return null

            SessionModel(
                id = obj.getString("id"),
                name = obj.getString("name"),
                startTime = obj.optLong("startTime", System.currentTimeMillis()),
                isFinished = false,
                photoCount = obj.optInt("photoCount", 0),
                lastSampleId = obj.optString("lastSampleId", "S001"),
                lastImageIndex = obj.optInt("lastImageIndex", 0)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun clearActiveSessionPrefs() {
        prefs.edit().remove("active_session_json").apply()
    }
}
