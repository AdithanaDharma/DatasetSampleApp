package com.dataset.acquisition.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dataset.acquisition.ui.theme.DarkBackground
import com.dataset.acquisition.ui.theme.DarkSurface
import com.dataset.acquisition.ui.theme.PrimaryAccent
import com.dataset.acquisition.ui.theme.StatusRed
import com.dataset.acquisition.ui.theme.TextPrimary
import java.io.File

/**
 * Halaman Preview / Konfirmasi foto sampel (Optimized untuk Landscape).
 */
@Composable
fun PreviewScreen(
    capturedImageFile: File,
    onRetakePhoto: () -> Unit,
    onUsePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Tampilan Foto Hasil Tangkapan Kamera
        AsyncImage(
            model = capturedImageFile,
            contentDescription = "Hasil Foto Sampel",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Header Informasi
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .background(DarkSurface.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Pratinjau Hasil Sampel",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }

        // Bottom Action Bar: "Foto Ulang" vs "Gunakan Foto"
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(DarkBackground.copy(alpha = 0.85f))
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tombol "Foto Ulang"
            OutlinedButton(
                onClick = onRetakePhoto,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = StatusRed
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = StatusRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Foto Ulang",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            // Tombol "Gunakan Foto"
            Button(
                onClick = onUsePhoto,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryAccent
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Gunakan Foto",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
