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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
 * Halaman Utama Kamera - Native Look & Feel:
 * - Viewfinder full screen dengan grid.
 * - Kontrol shutter di sisi kanan (landscape).
 * - Bottom Bar untuk manajemen sesi & pengaturan.
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

    val alignmentAnalyzer = remember {
        GlassAlignmentAnalyzer { state -> viewModel.updateAlignmentState(state) }
    }

    LaunchedEffect(activePreset, isEditMode, activeCamera) {
        alignmentAnalyzer.activePreset = activePreset
        alignmentAnalyzer.isEditMode = isEditMode
        activeCamera?.let { camera ->
            ManualCameraController.applyPresetToActiveCamera(camera, activePreset)
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // 1. Viewfinder
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .also { ManualCameraController.applyPresetToPreviewBuilder(it, activePreset) }
                        .build().also { it.surfaceProvider = previewView.surfaceProvider }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .also { ManualCameraController.applyPresetToCaptureBuilder(it, activePreset) }
                        .build()
                    imageCapture = capture

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().also { it.setAnalyzer(cameraExecutor, alignmentAnalyzer) }

                    try {
                        cameraProvider.unbindAll()
                        activeCamera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture, imageAnalysis)
                        ManualCameraController.applyPresetToActiveCamera(activeCamera!!, activePreset)
                    } catch (exc: Exception) {
                        Log.e(TAG, "Lifecycle Error", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Grid & Bounding Box Overlay
        InteractiveBoundingBoxOverlay(
            targetRoi = activePreset.targetRoi,
            alignmentState = alignmentState,
            isEditMode = isEditMode,
            onRoiChanged = { viewModel.updateTargetRoi(it) }
        )

        // 3. Top Status Info (Hanya Parameter Pro)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "ISO ${activePreset.iso} • ${activePreset.shutterSpeedLabel} • ${activePreset.whiteBalanceTitle} • ${activePreset.zoomRatio}x",
                    color = PrimaryAccent, fontSize = 10.sp, fontWeight = FontWeight.Medium
                )
            }
        }

        // 4. Manajemen Sesi & Pengaturan (Bottom Left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Sesi Info
            val session = activeSession
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column {
                    Text("SESI AKTIF", color = Color.White.copy(0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (session != null) "${session.name} (${session.totalSampleCount} foto)" else "Belum Ada Sesi",
                        color = if (session != null) StatusGreen else Color.White,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { viewModel.openNewSessionDialog() },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(0.5f), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = PrimaryAccent)
                    Spacer(Modifier.width(4.dp))
                    Text("Sesi Baru", fontSize = 11.sp)
                }

                // Tombol Bounding Box (Repositioned)
                IconButton(
                    onClick = { viewModel.toggleEditMode() },
                    modifier = Modifier.size(36.dp).background(if (isEditMode) StatusBlue else Color.Black.copy(0.5f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.CropFree, "Edit", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                // Tombol Settings (Repositioned)
                IconButton(
                    onClick = { viewModel.openPresetConfigDialog() },
                    modifier = Modifier.size(36.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Tune, "Config", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                if (session != null && session.totalSampleCount > 0) {
                    Button(
                        onClick = { viewModel.finishAndExportSession(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        if (isZipping) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        } else {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp), tint = Color.Black)
                            Spacer(Modifier.width(4.dp))
                            Text("Zip & WhatsApp", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Shutter Controls (Center Right)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .navigationBarsPadding()
                .padding(end = 30.dp)
        ) {
            val isUnlocked = (alignmentState.isShutterEnabled || !activePreset.autoLockShutter) && !isEditMode && !isCapturing
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (activePreset.autoLockShutter && !isEditMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.background(Color.Black.copy(0.6f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock, null, tint = if (isUnlocked) StatusGreen else StatusRed, modifier = Modifier.size(10.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isUnlocked) "Siap" else "Kunci", color = if (isUnlocked) StatusGreen else Color.White, fontSize = 9.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Native Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .border(4.dp, if (isUnlocked) Color.White else Color.Gray, CircleShape)
                        .padding(6.dp)
                        .background(if (isUnlocked) Color.White else Color.Gray.copy(0.4f), CircleShape)
                        .clickable(enabled = isUnlocked) {
                            val cap = imageCapture ?: return@clickable
                            val file = viewModel.createTempFile()
                            isCapturing = true
                            cap.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(res: ImageCapture.OutputFileResults) {
                                    isCapturing = false
                                    viewModel.onPhotoCaptured(file)
                                }
                                override fun onError(exc: ImageCaptureException) {
                                    isCapturing = false
                                    Log.e(TAG, "Capture Error", exc)
                                }
                            })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) CircularProgressIndicator(color = PrimaryAccent, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
                }
            }
        }

        // Dialogs
        if (showPresetDialog) PresetConfigDialog(activePreset, { viewModel.closePresetConfigDialog() }, { viewModel.savePreset(it) })
        if (showNewSessionDialog) NewSessionDialog({ viewModel.closeNewSessionDialog() }, { viewModel.startNewSession(it) })
        if (showResumeSessionDialog && activeSession != null) ResumeSessionDialog(activeSession!!, isZipping, { viewModel.resumeSession() }, { viewModel.finishAndExportSession(context) })
    }
}
