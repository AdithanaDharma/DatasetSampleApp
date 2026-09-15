package com.dataset.acquisition.camera

import android.annotation.SuppressLint
import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.dataset.acquisition.data.model.AlignmentState
import com.dataset.acquisition.data.model.AlignmentStatus
import com.dataset.acquisition.data.model.CameraPreset
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

/**
 * ImageAnalysis Analyzer menggunakan Google ML Kit Object Detection.
 * Mendeteksi posisi dan skala sampel gelas/wadah dan memvalidasinya terhadap Target Bounding Box.
 */
class GlassAlignmentAnalyzer(
    private val onAlignmentStateChanged: (AlignmentState) -> Unit
) : ImageAnalysis.Analyzer {

    private val detectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableClassification()
        .build()

    private val objectDetector = ObjectDetection.getClient(detectorOptions)

    @Volatile
    var activePreset: CameraPreset? = null

    @Volatile
    var isEditMode: Boolean = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        val preset = activePreset

        if (mediaImage == null || preset == null) {
            imageProxy.close()
            return
        }

        if (isEditMode) {
            onAlignmentStateChanged(
                AlignmentState(
                    status = AlignmentStatus.EDIT_MODE,
                    message = "Mode Kalibrasi: Tarik pojok/sisi untuk ubah ukuran",
                    isShutterEnabled = false
                )
            )
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        val isPortrait = rotationDegrees == 90 || rotationDegrees == 270
        val frameWidth = if (isPortrait) imageProxy.height.toFloat() else imageProxy.width.toFloat()
        val frameHeight = if (isPortrait) imageProxy.width.toFloat() else imageProxy.height.toFloat()

        objectDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                if (detectedObjects.isEmpty()) {
                    val canShutter = !preset.autoLockShutter
                    onAlignmentStateChanged(
                        AlignmentState(
                            status = AlignmentStatus.NO_OBJECT,
                            detectedBoxNormalized = null,
                            message = if (preset.autoLockShutter) "Arahkan kamera ke sampel gelas" else "",
                            isShutterEnabled = canShutter
                        )
                    )
                } else {
                    val mainObject = detectedObjects.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                    if (mainObject != null) {
                        val box = mainObject.boundingBox

                        val normLeft = (box.left.toFloat() / frameWidth).coerceIn(0f, 1f)
                        val normTop = (box.top.toFloat() / frameHeight).coerceIn(0f, 1f)
                        val normRight = (box.right.toFloat() / frameWidth).coerceIn(0f, 1f)
                        val normBottom = (box.bottom.toFloat() / frameHeight).coerceIn(0f, 1f)

                        val normWidth = normRight - normLeft
                        val normHeight = normBottom - normTop
                        val normCenterX = normLeft + (normWidth / 2f)
                        val normCenterY = normTop + (normHeight / 2f)

                        val normalizedRect = RectF(normLeft, normTop, normRight, normBottom)
                        val targetRoi = preset.targetRoi

                        val dx = kotlin.math.abs(normCenterX - targetRoi.centerX)
                        val dy = kotlin.math.abs(normCenterY - targetRoi.centerY)
                        val centerAligned = dx <= targetRoi.tolerance && dy <= targetRoi.tolerance

                        val widthRatio = normWidth / targetRoi.width
                        val heightRatio = normHeight / targetRoi.height
                        val avgScaleRatio = (widthRatio + heightRatio) / 2f

                        val (status, msg, canShutter) = when {
                            !centerAligned -> Triple(
                                AlignmentStatus.MISALIGNED,
                                "Posisi sample belum pas",
                                !preset.autoLockShutter
                            )
                            avgScaleRatio < 0.65f -> Triple(
                                AlignmentStatus.TOO_SMALL,
                                "Skala terlalu kecil/jauh, dekatkan kamera",
                                !preset.autoLockShutter
                            )
                            avgScaleRatio > 1.35f -> Triple(
                                AlignmentStatus.TOO_LARGE,
                                "Skala terlalu besar/dekat, jauhkan kamera",
                                !preset.autoLockShutter
                            )
                            else -> Triple(
                                AlignmentStatus.ALIGNED,
                                "Posisi Pas",
                                true
                            )
                        }

                        onAlignmentStateChanged(
                            AlignmentState(
                                status = status,
                                detectedBoxNormalized = normalizedRect,
                                message = if (status == AlignmentStatus.ALIGNED) "" else msg,
                                isShutterEnabled = canShutter
                            )
                        )
                    }
                }
            }
            .addOnFailureListener {
                onAlignmentStateChanged(
                    AlignmentState(
                        status = AlignmentStatus.NO_OBJECT,
                        message = if (preset.autoLockShutter) "Mendeteksi posisi objek..." else "",
                        isShutterEnabled = !preset.autoLockShutter
                    )
                )
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
