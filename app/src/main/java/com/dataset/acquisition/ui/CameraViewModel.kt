package com.dataset.acquisition.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dataset.acquisition.data.model.AlignmentState
import com.dataset.acquisition.data.model.AlignmentStatus
import com.dataset.acquisition.data.model.CameraPreset
import com.dataset.acquisition.data.model.QualityCategory
import com.dataset.acquisition.data.model.SessionModel
import com.dataset.acquisition.data.model.TargetRoi
import com.dataset.acquisition.data.repository.PresetRepository
import com.dataset.acquisition.data.storage.DatasetFileManager
import com.dataset.acquisition.data.storage.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel utama untuk mengelola state kamera Pro Mode, Sesi Foto,
 * Auto-Increment ID, serta alur penyimpanan dataset.
 */
class CameraViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val presetRepository = PresetRepository(application.applicationContext)
    private val fileManager = DatasetFileManager(application.applicationContext)
    val sessionManager = SessionManager(application.applicationContext, fileManager)

    val presets: StateFlow<List<CameraPreset>> = presetRepository.presets
    val activePreset: StateFlow<CameraPreset> = presetRepository.activePreset
    val activeSession: StateFlow<SessionModel?> = sessionManager.activeSession

    private val _showResumeSessionDialog = MutableStateFlow(sessionManager.activeSession.value != null)
    val showResumeSessionDialog: StateFlow<Boolean> = _showResumeSessionDialog.asStateFlow()

    private val _alignmentState = MutableStateFlow(AlignmentState())
    val alignmentState: StateFlow<AlignmentState> = _alignmentState.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _capturedPhotoFile = MutableStateFlow<File?>(null)
    val capturedPhotoFile: StateFlow<File?> = _capturedPhotoFile.asStateFlow()

    private val _showQualityDialog = MutableStateFlow(false)
    val showQualityDialog: StateFlow<Boolean> = _showQualityDialog.asStateFlow()

    private val _showPresetConfigDialog = MutableStateFlow(false)
    val showPresetConfigDialog: StateFlow<Boolean> = _showPresetConfigDialog.asStateFlow()

    private val _showNewSessionDialog = MutableStateFlow(false)
    val showNewSessionDialog: StateFlow<Boolean> = _showNewSessionDialog.asStateFlow()

    private val _isZipping = MutableStateFlow(false)
    val isZipping: StateFlow<Boolean> = _isZipping.asStateFlow()

    private val _savedStatusMessage = MutableStateFlow<String?>(null)
    val savedStatusMessage: StateFlow<String?> = _savedStatusMessage.asStateFlow()

    fun updateAlignmentState(state: AlignmentState) {
        if (!_isEditMode.value) {
            _alignmentState.value = state
        }
    }

    fun toggleEditMode() {
        val newEditMode = !_isEditMode.value
        _isEditMode.value = newEditMode
        if (newEditMode) {
            _alignmentState.value = AlignmentState(
                status = AlignmentStatus.EDIT_MODE,
                message = "Mode Kalibrasi: Atur Kotak",
                isShutterEnabled = false
            )
        }
    }

    fun updateTargetRoi(newRoi: TargetRoi) {
        presetRepository.updateActiveTargetRoi(newRoi)
    }

    fun selectPreset(presetId: String) {
        presetRepository.selectPreset(presetId)
    }

    fun savePreset(preset: CameraPreset) {
        presetRepository.savePreset(preset)
        _showPresetConfigDialog.value = false
    }

    fun openPresetConfigDialog() {
        _showPresetConfigDialog.value = true
    }

    fun closePresetConfigDialog() {
        _showPresetConfigDialog.value = false
    }

    fun openNewSessionDialog() {
        _showNewSessionDialog.value = true
    }

    fun closeNewSessionDialog() {
        _showNewSessionDialog.value = false
    }

    fun startNewSession(customName: String) {
        val session = sessionManager.startNewSession(customName)
        _showNewSessionDialog.value = false
        _showResumeSessionDialog.value = false
        _savedStatusMessage.value = "Sesi baru dimulai: ${session.name}"
    }

    fun resumeSession() {
        val session = sessionManager.activeSession.value
        if (session != null) {
            sessionManager.resumeSession(session)
            _savedStatusMessage.value = "Melanjutkan sesi: ${session.name}"
        }
        _showResumeSessionDialog.value = false
    }

    fun finishAndExportSession(context: Context) {
        val session = activeSession.value ?: return
        _isZipping.value = true

        viewModelScope.launch {
            val zipResult = sessionManager.zipSessionFolder(session)
            _isZipping.value = false

            zipResult.onSuccess { zipFile ->
                sessionManager.finishActiveSession()
                _showResumeSessionDialog.value = false
                _savedStatusMessage.value = "Sesi Selesai. Mengekspor Zip..."
                sessionManager.shareZipFile(context, zipFile)
            }.onFailure { err ->
                _savedStatusMessage.value = "Gagal membuat Zip: ${err.message}"
            }
        }
    }

    /**
     * Simpan sementara ke folder 'temp' di dalam folder sesi.
     */
    fun createTempFile(): File {
        return fileManager.createTempCaptureFile(sessionManager.activeSession.value)
    }

    fun onPhotoCaptured(file: File) {
        _capturedPhotoFile.value = file
    }

    fun onRetakePhoto() {
        _capturedPhotoFile.value?.let { temp ->
            if (temp.exists()) temp.delete()
        }
        _capturedPhotoFile.value = null
        _showQualityDialog.value = false
    }

    fun onUsePhoto() {
        _showQualityDialog.value = true
    }

    fun dismissQualityDialog() {
        _showQualityDialog.value = false
    }

    fun getEstimatedFileName(category: QualityCategory): String {
        val preset = activePreset.value
        val session = activeSession.value ?: return "Preview.jpg"
        val nextSampleIdx = session.getNextSampleIndex()
        val nextImageIdx = session.getNextImageIndex(category)
        return fileManager.generateFileName(preset.namingTemplate, category, nextSampleIdx, nextImageIdx)
    }

    /**
     * [AUTO-INCREMENT ID] & [PEMINDAHAN FILE]
     */
    fun saveLabeledPhoto(
        category: QualityCategory,
        onSuccess: (File) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val tempFile = _capturedPhotoFile.value
        if (tempFile == null || !tempFile.exists()) {
            onError(IllegalStateException("File temporary tidak ditemukan"))
            return
        }

        viewModelScope.launch {
            val preset = activePreset.value
            val session = activeSession.value ?: sessionManager.startNewSession()

            val result = fileManager.saveSamplePhoto(
                tempFile = tempFile,
                category = category,
                session = session,
                template = preset.namingTemplate
            )

            result.onSuccess { savedFile ->
                // Update counter di database/prefs secara instan
                sessionManager.recordPhotoSaved(category)
                
                _capturedPhotoFile.value = null
                _showQualityDialog.value = false

                _savedStatusMessage.value = "Berhasil disimpan: ${savedFile.name}"
                onSuccess(savedFile)
            }.onFailure { error ->
                onError(error)
            }
        }
    }

    fun clearSavedMessage() {
        _savedStatusMessage.value = null
    }
}
