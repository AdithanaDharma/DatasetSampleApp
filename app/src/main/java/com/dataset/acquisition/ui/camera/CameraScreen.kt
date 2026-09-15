package com.dataset.acquisition.ui.camera

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dataset.acquisition.camera.GlassAlignmentAnalyzer
import com.dataset.acquisition.camera.ManualCameraController
import com.dataset.acquisition.ui.CameraViewModel
import com.dataset.acquisition.ui.components.InteractiveBoundingBoxOverlay
import com.dataset.acquisition.ui.dialog.NewSessionDialog
import com.dataset.acquisition.ui.dialog.PresetConfigDialog
import com.dataset.acquisition.ui.dialog.ResumeSessionDialog
import com.dataset.acquisition.ui.theme.DarkBackground
import com.dataset.acquisition.ui.theme.DarkSurface
import com.dataset.acquisition.ui.theme.PrimaryAccent
import com.dataset.acquisition.ui.theme.StatusBlue
import com.dataset.acquisition.ui.theme.StatusGreen
import com.dataset.acquisition.ui.theme.StatusRed
import com.dataset.acquisition.ui.theme.TextMuted
import com.dataset.acquisition.ui.theme.TextPrimary
import com.dataset.acquisition.ui.theme.TextSecondary
import java.util.concurrent.Executors

private const val TAG = "CameraScreen"

/**
 * Halaman Utama Kamera Pro Mode (Optimized untuk Posisi Landscape & Bounding Box Kotak):
 * - Viewfinder CameraX dengan kontrol manual Camera2 (ISO min 50, Shutter 1/12000s-32s, WB Kelvin, Zoom Lock)
 * - Manajemen Sesi Foto, Session Persistence, & Auto-Zip Export ke WhatsApp
 * - Pemandu Bounding Box free-form & Deteksi Posisi/Skala
 */
@SuppressLint("MissingPermission")
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val activePreset by viewModel.activePreset.collectAsState()
    val presets by viewModel.presets.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val alignmentState by viewModel.alignmentState.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()

    val showPresetDialog by viewModel.showPresetConfigDialog.collectAsState()
    val showNewSessionDialog by viewModel.showNewSessionDialog.collectAsState()
    val showResumeSessionDialog by viewModel.showResumeSessionDialog.collectAsState()
    val isZipping by viewModel.isZipping.collectAsState()

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var activeCamera: Camera? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Inisialisasi Analyzer ML Kit
    val alignmentAnalyzer = remember {
        GlassAlignmentAnalyzer { state ->
            viewModel.updateAlignmentState(state)
        }
    }

    // Sinkronisasi preset aktif & mode edit ke analyzer & kamera
    LaunchedEffect(activePreset, isEditMode, activeCamera) {
        alignmentAnalyzer.activePreset = activePreset
        alignmentAnalyzer.isEditMode = isEditMode
        activeCamera?.let { camera ->
            ManualCameraController.applyPresetToActiveCamera(camera, activePreset)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 1. Viewfinder CameraX
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .also { builder ->
                            ManualCameraController.applyPresetToPreviewBuilder(builder, activePreset)
                        }
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .also { builder ->
                            ManualCameraController.applyPresetToCaptureBuilder(builder, activePreset)
                        }
                        .build()
                    imageCapture = capture

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor, alignmentAnalyzer)
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture,
                            imageAnalysis
                        )
                        activeCamera = camera
                        ManualCameraController.applyPresetToActiveCamera(camera, activePreset)
                    } catch (exc: Exception) {
                        Log.e(TAG, "Gagal mengaitkan lifecycle CameraX: ${exc.message}", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Interactive Bounding Box Overlay Kotak Free-Form
        InteractiveBoundingBoxOverlay(
            targetRoi = activePreset.targetRoi,
            alignmentState = alignmentState,
            isEditMode = isEditMode,
            onRoiChanged = { newRoi ->
                viewModel.updateTargetRoi(newRoi)
            }
        )

        // 3. Top Bar: Quick Preset Selector & Status Parameter Pro Kamera
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info Preset Aktif & Status Parameter Manual Pro (ISO, Shutter, WB, Zoom)
                Column(
                    modifier = Modifier
                        .background(DarkSurface.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = activePreset.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "ISO ${activePreset.iso} • ${activePreset.shutterSpeedLabel} • ${activePreset.whiteBalanceTitle} • ${activePreset.zoomRatio}x",
                        color = PrimaryAccent,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Tombol Toggle Mode Kalibrasi Bounding Box
                    IconButton(
                        onClick = { viewModel.toggleEditMode() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isEditMode) StatusBlue else DarkSurface.copy(alpha = 0.85f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "Edit Bounding Box",
                            tint = if (isEditMode) Color.White else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Tombol Buka Pengaturan Preset Pro
                    IconButton(
                        onClick = { viewModel.openPresetConfigDialog() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(DarkSurface.copy(alpha = 0.85f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Pengaturan Preset Pro",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Preset Quick Selection Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = preset.id == activePreset.id
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) PrimaryAccent else DarkSurface.copy(alpha = 0.8f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.selectPreset(preset.id) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = preset.name,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 4. Baris Kontrol Sesi Foto di Pojok Kiri Bawah (Session Controls)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Info Sesi Aktif
                val session = activeSession
                val sessionLabel = if (session != null) "${session.name} • ${session.photoCount} foto" else "Belum Ada Sesi"

                Column(
                    modifier = Modifier
                        .background(DarkSurface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Sesi Foto Aktif:",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = sessionLabel,
                        color = StatusGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // Tombol "Sesi Baru"
                OutlinedButton(
                    onClick = { viewModel.openNewSessionDialog() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DarkSurface.copy(alpha = 0.85f),
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = PrimaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sesi Baru", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Tombol "Sesi Selesai (WhatsApp)" jika ada sesi aktif
                if (session != null && session.photoCount > 0) {
                    Button(
                        onClick = { viewModel.finishAndExportSession(context) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        modifier = Modifier.height(38.dp)
                    ) {
                        if (isZipping) {
                            CircularProgressIndicator(
                                color = DarkBackground,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sesi Selesai (Zip)", color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Shutter Controls Ergonomis di Kanan Layar
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            val isShutterUnlocked = alignmentState.isShutterEnabled && !isEditMode && !isCapturing

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Indikator Status Shutter Lock
                if (activePreset.autoLockShutter && !isEditMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(DarkSurface.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isShutterUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isShutterUnlocked) StatusGreen else StatusRed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isShutterUnlocked) "Siap" else "Terkunci",
                            color = if (isShutterUnlocked) StatusGreen else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Tombol Shutter Utama
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .border(
                            width = 4.dp,
                            color = when {
                                isEditMode -> TextMuted
                                isShutterUnlocked -> StatusGreen
                                else -> TextMuted
                            },
                            shape = CircleShape
                        )
                        .padding(5.dp)
                        .background(
                            color = when {
                                isEditMode -> TextMuted.copy(alpha = 0.3f)
                                isShutterUnlocked -> Color.White
                                else -> TextMuted.copy(alpha = 0.4f)
                            },
                            shape = CircleShape
                        )
                        .clickable(enabled = isShutterUnlocked) {
                            val capture = imageCapture ?: return@clickable
                            val tempFile = viewModel.createTempFile()
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

                            isCapturing = true
                            capture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        isCapturing = false
                                        viewModel.onPhotoCaptured(tempFile)
                                    }

                                    override fun onError(exc: ImageCaptureException) {
                                        isCapturing = false
                                        Log.e(TAG, "Gagal mengambil foto: ${exc.message}", exc)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = PrimaryAccent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // Dialog Konfigurasi Preset Pro
        if (showPresetDialog) {
            PresetConfigDialog(
                preset = activePreset,
                onDismiss = { viewModel.closePresetConfigDialog() },
                onSavePreset = { updatedPreset ->
                    viewModel.savePreset(updatedPreset)
                }
            )
        }

        // Dialog Memulai Sesi Baru
        if (showNewSessionDialog) {
            NewSessionDialog(
                onDismiss = { viewModel.closeNewSessionDialog() },
                onStartSession = { name ->
                    viewModel.startNewSession(name)
                }
            )
        }

        // Pop-up Resume Sesi Unfinished jika terdeteksi saat app dibuka kembali
        val currentSession = activeSession
        if (showResumeSessionDialog && currentSession != null) {
            ResumeSessionDialog(
                activeSession = currentSession,
                isZipping = isZipping,
                onResumeSession = { viewModel.resumeSession() },
                onFinishAndExportSession = { viewModel.finishAndExportSession(context) }
            )
        }
    }
}
