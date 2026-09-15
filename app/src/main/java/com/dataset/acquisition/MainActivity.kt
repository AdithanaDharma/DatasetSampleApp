package com.dataset.acquisition

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dataset.acquisition.ui.CameraViewModel
import com.dataset.acquisition.ui.camera.CameraScreen
import com.dataset.acquisition.ui.dialog.QualityLabelDialog
import com.dataset.acquisition.ui.preview.PreviewScreen
import com.dataset.acquisition.ui.theme.DarkBackground
import com.dataset.acquisition.ui.theme.DatasetAcquisitionTheme
import com.dataset.acquisition.ui.theme.PrimaryAccent
import com.dataset.acquisition.ui.theme.TextPrimary
import com.dataset.acquisition.ui.theme.TextSecondary

/**
 * Activity Utama aplikasi akuisisi dataset.
 * Mengelola runtime permissions kamera, navigasi Camera -> Preview,
 * dan dialog pelabelan dataset.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DatasetAcquisitionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: CameraViewModel) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Izin kamera ditolak. Kamera diperlukan untuk aplikasi ini.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        // Layar Permintaan Izin Kamera
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Izin Kamera Diperlukan",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aplikasi ini memerlukan akses kamera untuk mengambil citra dataset sampel cairan secara konsisten.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Berikan Akses Kamera", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // State Alur Navigasi
    val capturedFile by viewModel.capturedPhotoFile.collectAsState()
    val showQualityDialog by viewModel.showQualityDialog.collectAsState()
    val savedMessage by viewModel.savedStatusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(savedMessage) {
        savedMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSavedMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val fileToPreview = capturedFile

        if (fileToPreview == null) {
            // Halaman 1: Camera Viewfinder dengan Manual Preset & Alignment Guide
            CameraScreen(viewModel = viewModel)
        } else {
            // Halaman 2: Preview Hasil Foto (Foto Ulang vs Gunakan Foto)
            PreviewScreen(
                capturedImageFile = fileToPreview,
                onRetakePhoto = { viewModel.onRetakePhoto() },
                onUsePhoto = { viewModel.onUsePhoto() }
            )
        }

        // Pop-up / Dialog Kategorisasi Kualitas ("Baik", "Normal", "Buruk")
        if (showQualityDialog) {
            QualityLabelDialog(
                estimatedFileName = { category ->
                    viewModel.getEstimatedFileName(category)
                },
                onDismiss = { viewModel.dismissQualityDialog() },
                onConfirmSave = { category ->
                    viewModel.saveLabeledPhoto(
                        category = category,
                        onSuccess = { savedFile ->
                            Toast.makeText(
                                context,
                                "Tersimpan: ${savedFile.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onError = { err ->
                            Toast.makeText(
                                context,
                                "Gagal menyimpan: ${err.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
