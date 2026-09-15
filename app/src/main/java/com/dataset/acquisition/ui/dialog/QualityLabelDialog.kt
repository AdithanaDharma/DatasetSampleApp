package com.dataset.acquisition.ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dataset.acquisition.data.model.QualityCategory
import com.dataset.acquisition.ui.theme.DarkSurface
import com.dataset.acquisition.ui.theme.DarkSurfaceVariant
import com.dataset.acquisition.ui.theme.PrimaryAccent
import com.dataset.acquisition.ui.theme.TextMuted
import com.dataset.acquisition.ui.theme.TextPrimary
import com.dataset.acquisition.ui.theme.TextSecondary

/**
 * Dialog pemilihan label kualitas sampel ("Baik", "Normal", "Buruk")
 * serta konfirmasi Sample ID (Optimized untuk Landscape).
 */
@Composable
fun QualityLabelDialog(
    initialSampleId: String,
    estimatedFileName: (QualityCategory, String) -> String,
    onDismiss: () -> Unit,
    onConfirmSave: (QualityCategory, String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(QualityCategory.BAIK) }
    var sampleId by remember { mutableStateOf(initialSampleId) }

    val previewName = estimatedFileName(selectedCategory, sampleId)

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
                    text = "Kategorisasi Kualitas Sampel",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Pilih kategori untuk menentukan folder penyimpanan dan label dataset",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Input Sample ID
                OutlinedTextField(
                    value = sampleId,
                    onValueChange = { sampleId = it.uppercase() },
                    label = { Text("Sample ID") },
                    placeholder = { Text("Contoh: S001") },
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

                Text(
                    text = "Kategori Kualitas:",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pilihan Kategori: Baik, Normal, Buruk
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QualityCategory.entries.forEach { category ->
                        val isSelected = category == selectedCategory
                        val categoryColor = category.badgeColor

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) categoryColor.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) categoryColor else Color.Transparent
                                    ),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(categoryColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = category.displayName,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Preview Folder & Nama File yang akan disimpan
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = PrimaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Lokasi: /Dataset/${selectedCategory.folderName}/",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = previewName,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                        onClick = { onConfirmSave(selectedCategory, sampleId) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = DarkSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan", color = DarkSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
