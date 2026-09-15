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

    fun startNewSession(customName: String = ""): SessionModel {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val nameStr = if (customName.isBlank()) "Sesi_$dateStr" else customName.trim()
        val sessionId = "Sesi_$dateStr"

        val newSession = SessionModel(
            id = sessionId,
            name = nameStr,
            startTime = System.currentTimeMillis(),
            isFinished = false,
            totalSampleCount = 0,
            countBaik = 0,
            countNormal = 0,
            countBuruk = 0
        )

        _activeSession.value = newSession
        saveSessionToPrefs(newSession)
        return newSession
    }

    fun getSessionDir(session: SessionModel): File {
        val baseDir = fileManager.getDatasetBaseDir()
        val sessionDir = File(baseDir, session.name)
        if (!sessionDir.exists()) sessionDir.mkdirs()
        return sessionDir
    }

    /**
     * Folder temp di dalam folder sesi.
     */
    fun getSessionTempDir(session: SessionModel): File {
        val tempDir = File(getSessionDir(session), "temp")
        if (!tempDir.exists()) tempDir.mkdirs()
        return tempDir
    }

    /**
     * Mencatat progres foto baru secara otomatis (Auto-increment).
     */
    fun recordPhotoSaved(category: QualityCategory) {
        val current = _activeSession.value ?: startNewSession()
        val updated = when (category) {
            QualityCategory.BAIK -> current.copy(totalSampleCount = current.totalSampleCount + 1, countBaik = current.countBaik + 1)
            QualityCategory.NORMAL -> current.copy(totalSampleCount = current.totalSampleCount + 1, countNormal = current.countNormal + 1)
            QualityCategory.BURUK -> current.copy(totalSampleCount = current.totalSampleCount + 1, countBuruk = current.countBuruk + 1)
        }
        _activeSession.value = updated
        saveSessionToPrefs(updated)
    }

    fun resumeSession(session: SessionModel) {
        _activeSession.value = session.copy(isFinished = false)
        saveSessionToPrefs(_activeSession.value!!)
    }

    fun finishActiveSession() {
        _activeSession.value = null
        clearActiveSessionPrefs()
    }

    suspend fun zipSessionFolder(session: SessionModel): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val sessionDir = getSessionDir(session)
            val baseDir = fileManager.getDatasetBaseDir()
            val zipFile = File(baseDir, "${session.name}.zip")

            if (zipFile.exists()) zipFile.delete()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
                zipFolderRecursive(sessionDir, sessionDir.name, zipOut)
            }
            zipFile
        }
    }

    private fun zipFolderRecursive(fileToZip: File, parentName: String, zipOut: ZipOutputStream) {
        if (fileToZip.isDirectory) {
            // Jangan masukkan folder temp ke dalam zip jika masih ada isinya (opsional)
            if (fileToZip.name == "temp") return 

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

    fun shareZipFile(context: Context, zipFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, zipFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp")
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            try {
                val authority = "${context.packageName}.fileprovider"
                val contentUri: Uri = FileProvider.getUriForFile(context, authority, zipFile)
                val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
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
                put("totalSampleCount", session.totalSampleCount)
                put("countBaik", session.countBaik)
                put("countNormal", session.countNormal)
                put("countBuruk", session.countBuruk)
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
            if (obj.optBoolean("isFinished", false)) return null

            SessionModel(
                id = obj.getString("id"),
                name = obj.getString("name"),
                startTime = obj.optLong("startTime", System.currentTimeMillis()),
                isFinished = false,
                totalSampleCount = obj.optInt("totalSampleCount", 0),
                countBaik = obj.optInt("countBaik", 0),
                countNormal = obj.optInt("countNormal", 0),
                countBuruk = obj.optInt("countBuruk", 0)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun clearActiveSessionPrefs() {
        prefs.edit().remove("active_session_json").apply()
    }
}
