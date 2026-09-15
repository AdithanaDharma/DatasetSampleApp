package com.dataset.acquisition.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.dataset.acquisition.data.model.CameraPreset
import com.dataset.acquisition.data.model.TargetRoi
import com.dataset.acquisition.data.model.WhiteBalanceMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Repository pengelola koleksi preset kamera Pro Mode.
 * Menggunakan SharedPreferences agar state preset (ISO, EV, WB, Shutter Speed, Zoom, TargetRoi)
 * tersimpan secara permanen (persistent state) saat aplikasi ditutup dan dibuka kembali.
 */
class PresetRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("camera_presets_prefs", Context.MODE_PRIVATE)

    private val defaultPresets = listOf(
        CameraPreset(
            id = "preset_studio_beaker_250",
            name = "Studio Mini - Gelas Beaker 250ml",
            description = "Pencahayaan LED studio untuk gelas kimia 250ml",
            iso = 100,
            shutterSpeedNumerator = 1L,
            shutterSpeedDenominator = 50L,
            wbMode = WhiteBalanceMode.FLUORESCENT,
            wbKelvin = 5500,
            exposureCompensationIndex = 0,
            zoomRatio = 1.0f,
            targetRoi = TargetRoi(centerX = 0.50f, centerY = 0.50f, width = 0.45f, height = 0.45f, tolerance = 0.12f),
            namingTemplate = "[kategoriKualitas]_[tanggalAmbil]_[sampleid]_[imageid].jpg",
            autoLockShutter = true
        ),
        CameraPreset(
            id = "preset_pro_low_iso",
            name = "Pro Studio - Low Noise (ISO 50)",
            description = "Menggunakan ISO 50 terendah untuk noise minimal",
            iso = 50,
            shutterSpeedNumerator = 1L,
            shutterSpeedDenominator = 50L,
            wbMode = WhiteBalanceMode.MANUAL,
            wbKelvin = 5000,
            exposureCompensationIndex = 0,
            zoomRatio = 1.0f,
            targetRoi = TargetRoi(centerX = 0.50f, centerY = 0.50f, width = 0.40f, height = 0.40f, tolerance = 0.10f),
            namingTemplate = "[kategoriKualitas]_[tanggalAmbil]_[sampleid]_[imageid].jpg",
            autoLockShutter = true
        ),
        CameraPreset(
            id = "preset_daylight_sample",
            name = "Cahaya Alami (Daylight)",
            description = "Kalibrasi untuk pencahayaan ruangan alami",
            iso = 50,
            shutterSpeedNumerator = 1L,
            shutterSpeedDenominator = 250L,
            wbMode = WhiteBalanceMode.DAYLIGHT,
            wbKelvin = 5500,
            exposureCompensationIndex = 0,
            zoomRatio = 1.0f,
            targetRoi = TargetRoi(centerX = 0.50f, centerY = 0.50f, width = 0.45f, height = 0.45f, tolerance = 0.14f),
            namingTemplate = "[kategoriKualitas]_[tanggalAmbil]_[sampleid]_[imageid].jpg",
            autoLockShutter = true
        )
    )

    private val _presets = MutableStateFlow<List<CameraPreset>>(loadPresets())
    val presets: StateFlow<List<CameraPreset>> = _presets.asStateFlow()

    private val activePresetId: String
        get() = prefs.getString("active_preset_id", defaultPresets.first().id) ?: defaultPresets.first().id

    private val _activePreset = MutableStateFlow<CameraPreset>(
        _presets.value.find { it.id == activePresetId } ?: _presets.value.first()
    )
    val activePreset: StateFlow<CameraPreset> = _activePreset.asStateFlow()

    fun selectPreset(presetId: String) {
        val found = _presets.value.find { it.id == presetId } ?: return
        _activePreset.value = found
        prefs.edit().putString("active_preset_id", presetId).apply()
    }

    fun updateActiveTargetRoi(newRoi: TargetRoi) {
        val current = _activePreset.value
        val updated = current.copy(targetRoi = newRoi)
        savePreset(updated)
    }

    fun savePreset(preset: CameraPreset) {
        val currentList = _presets.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == preset.id }
        if (index >= 0) {
            currentList[index] = preset
        } else {
            val newPreset = if (preset.id.isBlank()) preset.copy(id = UUID.randomUUID().toString()) else preset
            currentList.add(newPreset)
        }
        _presets.value = currentList
        if (_activePreset.value.id == preset.id) {
            _activePreset.value = preset
        }
        persistPresets(currentList)
    }

    fun toggleAutoLockShutter(enabled: Boolean) {
        val current = _activePreset.value
        val updated = current.copy(autoLockShutter = enabled)
        savePreset(updated)
    }

    private fun persistPresets(list: List<CameraPreset>) {
        try {
            val jsonArray = JSONArray()
            list.forEach { preset ->
                val obj = JSONObject().apply {
                    put("id", preset.id)
                    put("name", preset.name)
                    put("description", preset.description)
                    put("iso", preset.iso)
                    put("shutterSpeedNumerator", preset.shutterSpeedNumerator)
                    put("shutterSpeedDenominator", preset.shutterSpeedDenominator)
                    put("wbMode", preset.wbMode.name)
                    put("wbKelvin", preset.wbKelvin)
                    put("exposureCompensationIndex", preset.exposureCompensationIndex)
                    put("zoomRatio", preset.zoomRatio.toDouble())
                    put("namingTemplate", preset.namingTemplate)
                    put("autoLockShutter", preset.autoLockShutter)

                    val roiObj = JSONObject().apply {
                        put("centerX", preset.targetRoi.centerX.toDouble())
                        put("centerY", preset.targetRoi.centerY.toDouble())
                        put("width", preset.targetRoi.width.toDouble())
                        put("height", preset.targetRoi.height.toDouble())
                        put("tolerance", preset.targetRoi.tolerance.toDouble())
                    }
                    put("targetRoi", roiObj)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("saved_presets_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPresets(): List<CameraPreset> {
        val jsonStr = prefs.getString("saved_presets_json", null) ?: return defaultPresets
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<CameraPreset>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val roiObj = obj.optJSONObject("targetRoi")
                val roi = if (roiObj != null) {
                    TargetRoi(
                        centerX = roiObj.optDouble("centerX", 0.5).toFloat(),
                        centerY = roiObj.optDouble("centerY", 0.5).toFloat(),
                        width = roiObj.optDouble("width", 0.45).toFloat(),
                        height = roiObj.optDouble("height", 0.45).toFloat(),
                        tolerance = roiObj.optDouble("tolerance", 0.12).toFloat()
                    )
                } else TargetRoi()

                val wbModeStr = obj.optString("wbMode", WhiteBalanceMode.FLUORESCENT.name)
                val wbMode = try {
                    WhiteBalanceMode.valueOf(wbModeStr)
                } catch (e: Exception) {
                    WhiteBalanceMode.FLUORESCENT
                }

                list.add(
                    CameraPreset(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        iso = obj.optInt("iso", 100),
                        shutterSpeedNumerator = obj.optLong("shutterSpeedNumerator", 1L),
                        shutterSpeedDenominator = obj.optLong("shutterSpeedDenominator", 50L),
                        wbMode = wbMode,
                        wbKelvin = obj.optInt("wbKelvin", 5500),
                        exposureCompensationIndex = obj.optInt("exposureCompensationIndex", 0),
                        zoomRatio = obj.optDouble("zoomRatio", 1.0).toFloat(),
                        targetRoi = roi,
                        namingTemplate = obj.optString("namingTemplate", "[kategoriKualitas]_[tanggalAmbil]_[sampleid]_[imageid].jpg"),
                        autoLockShutter = obj.optBoolean("autoLockShutter", true)
                    )
                )
            }
            if (list.isEmpty()) defaultPresets else list
        } catch (e: Exception) {
            e.printStackTrace()
            defaultPresets
        }
    }
}
