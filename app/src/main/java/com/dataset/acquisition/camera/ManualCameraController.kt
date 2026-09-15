package com.dataset.acquisition.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.RggbChannelVector
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import com.dataset.acquisition.data.model.CameraPreset
import com.dataset.acquisition.data.model.WhiteBalanceMode
import kotlin.math.ln
import kotlin.math.pow

/**
 * Pengontrol pengaturan manual kamera (Pro Mode) berbasis Camera2Interop.
 * Mengunci ISO (termasuk ISO 50), Shutter Speed (termasuk 1/50s & 1/12000s - 32s),
 * White Balance Manual Kelvin, dan Zoom Lock (perbesaran konsisten).
 */
@Suppress("UnsafeOptInUsageError")
object ManualCameraController {

    private const val TAG = "ManualCameraController"

    /**
     * Konversi suhu warna Kelvin (2000K - 12000K) ke RggbChannelVector untuk Camera2.
     */
    fun kelvinToRggbChannelVector(kelvin: Int): RggbChannelVector {
        val temp = kelvin.coerceIn(2000, 12000) / 100.0

        val r = if (temp <= 66) 255.0 else 329.698727446 * (temp - 60).pow(-0.1332047592)
        val g = if (temp <= 66) 99.4708025861 * ln(temp) - 161.1195681661 else 288.1221695283 * (temp - 60).pow(-0.0755148492)
        val b = if (temp >= 66) 255.0 else if (temp <= 19) 0.0 else 138.5177312231 * ln(temp - 10) - 305.0447927307

        val rNorm = r.coerceIn(1.0, 255.0)
        val gNorm = g.coerceIn(1.0, 255.0)
        val bNorm = b.coerceIn(1.0, 255.0)

        val rGain = (gNorm / rNorm * 2.0).toFloat().coerceIn(1.0f, 4.0f)
        val gGain = 1.0f
        val bGain = (gNorm / bNorm * 2.0).toFloat().coerceIn(1.0f, 4.0f)

        return RggbChannelVector(rGain, gGain, gGain, bGain)
    }

    /**
     * Menerapkan konfigurasi preset ke CameraX Preview builder sebelum bindToLifecycle.
     */
    fun applyPresetToPreviewBuilder(
        builder: Preview.Builder,
        preset: CameraPreset
    ) {
        val extender = Camera2Interop.Extender(builder)
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        extender.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, preset.iso)
        extender.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, preset.shutterSpeedNanoseconds)

        if (preset.wbMode == WhiteBalanceMode.MANUAL) {
            extender.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            extender.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_FAST)
            extender.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_GAINS, kelvinToRggbChannelVector(preset.wbKelvin))
        } else {
            extender.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, preset.wbMode.camera2Mode)
        }
    }

    /**
     * Menerapkan konfigurasi preset ke ImageCapture builder agar hasil foto konsisten dengan preview.
     */
    fun applyPresetToCaptureBuilder(
        builder: ImageCapture.Builder,
        preset: CameraPreset
    ) {
        val extender = Camera2Interop.Extender(builder)
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        extender.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, preset.iso)
        extender.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, preset.shutterSpeedNanoseconds)

        if (preset.wbMode == WhiteBalanceMode.MANUAL) {
            extender.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
            extender.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_FAST)
            extender.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_GAINS, kelvinToRggbChannelVector(preset.wbKelvin))
        } else {
            extender.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, preset.wbMode.camera2Mode)
        }
    }

    /**
     * Menerapkan konfigurasi preset (ISO, Shutter, WB Kelvin, EV, Zoom Lock) secara realtime ke kamera aktif.
     */
    fun applyPresetToActiveCamera(
        camera: Camera?,
        preset: CameraPreset
    ) {
        if (camera == null) return

        try {
            val camera2Control = Camera2CameraControl.from(camera.cameraControl)
            val builder = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, preset.iso)
                .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, preset.shutterSpeedNanoseconds)

            if (preset.wbMode == WhiteBalanceMode.MANUAL) {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
                builder.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_FAST)
                builder.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_GAINS, kelvinToRggbChannelVector(preset.wbKelvin))
            } else {
                builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, preset.wbMode.camera2Mode)
            }

            camera2Control.setCaptureRequestOptions(builder.build())

            // Menerapkan Exposure Compensation
            camera.cameraControl.setExposureCompensationIndex(preset.exposureCompensationIndex)

            // Menerapkan Zoom Lock
            camera.cameraControl.setZoomRatio(preset.zoomRatio.coerceIn(1.0f, 5.0f))

            Log.d(TAG, "Menerapkan preset Pro: ISO=${preset.iso}, Shutter=${preset.shutterSpeedLabel}, WB=${preset.whiteBalanceTitle}, Zoom=${preset.zoomRatio}x")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menerapkan manual control Camera2: ${e.message}", e)
        }
    }

    fun isManualControlSupported(camera: Camera?): Boolean {
        if (camera == null) return false
        return try {
            val cameraInfo = Camera2CameraInfo.from(camera.cameraInfo)
            val level = cameraInfo.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL ||
                    level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 ||
                    level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        } catch (e: Exception) {
            false
        }
    }
}
