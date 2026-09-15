package com.dataset.acquisition.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dataset.acquisition.data.model.CameraPreset
import com.dataset.acquisition.data.model.WhiteBalanceMode
import com.dataset.acquisition.ui.theme.DarkSurface
import com.dataset.acquisition.ui.theme.DarkSurfaceVariant
import com.dataset.acquisition.ui.theme.PrimaryAccent
import com.dataset.acquisition.ui.theme.TextMuted
import com.dataset.acquisition.ui.theme.TextPrimary
import com.dataset.acquisition.ui.theme.TextSecondary

private data class ShutterOption(val num: Long, val denom: Long, val label: String)

/**
 * Dialog Konfigurasi Preset Pro Mode Kamera:
 * - ISO (min 50)
 * - Shutter Speed (termasuk 1/50s & 1/12000s s/d 32s)
 * - White Balance Manual Kelvin (e.g. 2000K - 12000K)
 * - Zoom Lock
 * - Format Penamaan & Auto-Lock Shutter
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetConfigDialog(
    preset: CameraPreset,
    onDismiss: () -> Unit,
    onSavePreset: (CameraPreset) -> Unit
) {
    var name by remember { mutableStateOf(preset.name) }
    var selectedIso by remember { mutableIntStateOf(preset.iso) }
    var shutterNum by remember { mutableLongStateOf(preset.shutterSpeedNumerator) }
    var shutterDenom by remember { mutableLongStateOf(preset.shutterSpeedDenominator) }
    var selectedWbMode by remember { mutableStateOf(preset.wbMode) }
    var wbKelvin by remember { mutableIntStateOf(preset.wbKelvin) }
    var zoomRatio by remember { mutableFloatStateOf(preset.zoomRatio) }
    var namingTemplate by remember { mutableStateOf(preset.namingTemplate) }
    var autoLockShutter by remember { mutableStateOf(preset.autoLockShutter) }

    val isoOptions = listOf(50, 64, 100, 200, 400, 800, 1600, 3200, 6400, 12800)

    val shutterPresetOptions = listOf(
        ShutterOption(1, 12000, "1/12000s"),
        ShutterOption(1, 8000, "1/8000s"),
        ShutterOption(1, 4000, "1/4000s"),
        ShutterOption(1, 2000, "1/2000s"),
        ShutterOption(1, 1000, "1/1000s"),
        ShutterOption(1, 500, "1/500s"),
        ShutterOption(1, 250, "1/250s"),
        ShutterOption(1, 125, "1/125s"),
        ShutterOption(1, 60, "1/60s"),
        ShutterOption(1, 50, "1/50s"),
        ShutterOption(1, 30, "1/30s"),
        ShutterOption(1, 15, "1/15s"),
        ShutterOption(1, 8, "1/8s"),
        ShutterOption(1, 4, "1/4s"),
        ShutterOption(1, 2, "1/2s"),
        ShutterOption(1, 1, "1s"),
        ShutterOption(2, 1, "2s"),
        ShutterOption(4, 1, "4s"),
        ShutterOption(8, 1, "8s"),
        ShutterOption(15, 1, "15s"),
        ShutterOption(30, 1, "30s"),
        ShutterOption(32, 1, "32s")
    )

    var isoExpanded by remember { mutableStateOf(false) }
    var shutterExpanded by remember { mutableStateOf(false) }
    var wbExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Konfigurasi Kamera Pro Mode",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ISO (min 50), Shutter Speed (1/50s & 1/12000s - 32s), WB Kelvin & Zoom Lock",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                // Nama Preset
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Preset") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Baris ISO & Shutter Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ISO Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isoExpanded,
                        onExpandedChange = { isoExpanded = !isoExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = "ISO $selectedIso",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ISO (min 50)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isoExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = isoExpanded,
                            onDismissRequest = { isoExpanded = false }
                        ) {
                            isoOptions.forEach { iso ->
                                DropdownMenuItem(
                                    text = { Text("ISO $iso") },
                                    onClick = {
                                        selectedIso = iso
                                        isoExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Shutter Speed Dropdown
                    val currentShutterLabel = if (shutterDenom <= 1L) "${shutterNum}s" else "$shutterNum/${shutterDenom}s"

                    ExposedDropdownMenuBox(
                        expanded = shutterExpanded,
                        onExpandedChange = { shutterExpanded = !shutterExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = currentShutterLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Shutter Speed") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shutterExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = DarkSurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = shutterExpanded,
                            onDismissRequest = { shutterExpanded = false }
                        ) {
                            shutterPresetOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label) },
                                    onClick = {
                                        shutterNum = opt.num
                                        shutterDenom = opt.denom
                                        shutterExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // White Balance Dropdown
                ExposedDropdownMenuBox(
                    expanded = wbExpanded,
                    onExpandedChange = { wbExpanded = !wbExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (selectedWbMode == WhiteBalanceMode.MANUAL) "Manual (${wbKelvin}K)" else selectedWbMode.title,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("White Balance Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wbExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = wbExpanded,
                        onDismissRequest = { wbExpanded = false }
                    ) {
                        WhiteBalanceMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.title) },
                                onClick = {
                                    selectedWbMode = mode
                                    wbExpanded = false
                                }
                            )
                        }
                    }
                }

                // Input Manual Kelvin jika White Balance = MANUAL
                if (selectedWbMode == WhiteBalanceMode.MANUAL) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Suhu Warna Kelvin Manual:",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${wbKelvin}K",
                                color = PrimaryAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = wbKelvin.toFloat(),
                            onValueChange = { wbKelvin = (it / 100).toInt() * 100 },
                            valueRange = 2000f..10000f,
                            steps = 79,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryAccent,
                                activeTrackColor = PrimaryAccent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Zoom Lock Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Zoom Lock (Perbesaran Konsisten):",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format("%.1fx", zoomRatio),
                            color = PrimaryAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = zoomRatio,
                        onValueChange = { zoomRatio = (kotlin.math.round(it * 10f) / 10f) },
                        valueRange = 1.0f..3.0f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryAccent,
                            activeTrackColor = PrimaryAccent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Template Format Penamaan File
                OutlinedTextField(
                    value = namingTemplate,
                    onValueChange = { namingTemplate = it },
                    label = { Text("Format Penamaan File") },
                    supportingText = {
                        Text(
                            text = "Token: [kategoriKualitas], [tanggalAmbil], [sampleid], [imageid]",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle Auto-Lock Shutter Gating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kunci Shutter Otomatis",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Hanya izinkan foto jika sampel pas di kotak",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = autoLockShutter,
                        onCheckedChange = { autoLockShutter = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryAccent,
                            checkedTrackColor = PrimaryAccent.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            val updated = preset.copy(
                                name = name,
                                iso = selectedIso.coerceAtLeast(50),
                                shutterSpeedNumerator = shutterNum,
                                shutterSpeedDenominator = shutterDenom.coerceAtLeast(1L),
                                wbMode = selectedWbMode,
                                wbKelvin = wbKelvin.coerceIn(2000, 12000),
                                zoomRatio = zoomRatio.coerceIn(1.0f, 5.0f),
                                namingTemplate = namingTemplate,
                                autoLockShutter = autoLockShutter
                            )
                            onSavePreset(updated)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Text("Simpan Preset Pro", color = DarkSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
