package com.dataset.acquisition.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dataset.acquisition.data.model.AlignmentState
import com.dataset.acquisition.data.model.AlignmentStatus
import com.dataset.acquisition.data.model.TargetRoi
import com.dataset.acquisition.ui.theme.OverlayAlignedLine
import com.dataset.acquisition.ui.theme.OverlayGuideLine
import com.dataset.acquisition.ui.theme.OverlayMisalignedLine
import com.dataset.acquisition.ui.theme.StatusBlue
import kotlin.math.hypot

private enum class DragHandle {
    NONE, CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP, BOTTOM, LEFT, RIGHT
}

/**
 * Overlay interaktif Bounding Box berbentuk kotak sederhana.
 * Mendukung mode deteksi realtime dan mode kalibrasi manual (bebas / free-form).
 */
@Composable
fun InteractiveBoundingBoxOverlay(
    targetRoi: TargetRoi,
    alignmentState: AlignmentState,
    isEditMode: Boolean,
    onRoiChanged: (TargetRoi) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = this.constraints.maxWidth.toFloat()
        val screenHeight = this.constraints.maxHeight.toFloat()

        var activeHandle by remember { mutableStateOf(DragHandle.NONE) }

        // Handler gestur Drag untuk pengaturan bebas (free-form width/height/position)
        val gestureModifier = if (isEditMode) {
            Modifier.pointerInput(targetRoi, screenWidth, screenHeight) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val boxWidth = screenWidth * targetRoi.width
                        val boxHeight = screenHeight * targetRoi.height
                        val boxLeft = (screenWidth * targetRoi.centerX) - (boxWidth / 2f)
                        val boxTop = (screenHeight * targetRoi.centerY) - (boxHeight / 2f)
                        val boxRight = boxLeft + boxWidth
                        val boxBottom = boxTop + boxHeight

                        val touchRadius = 48.dp.toPx()

                        // Hit test handle mana yang disentuh
                        activeHandle = when {
                            hypot(offset.x - boxLeft, offset.y - boxTop) <= touchRadius -> DragHandle.TOP_LEFT
                            hypot(offset.x - boxRight, offset.y - boxTop) <= touchRadius -> DragHandle.TOP_RIGHT
                            hypot(offset.x - boxLeft, offset.y - boxBottom) <= touchRadius -> DragHandle.BOTTOM_LEFT
                            hypot(offset.x - boxRight, offset.y - boxBottom) <= touchRadius -> DragHandle.BOTTOM_RIGHT
                            kotlin.math.abs(offset.y - boxTop) <= touchRadius && offset.x in boxLeft..boxRight -> DragHandle.TOP
                            kotlin.math.abs(offset.y - boxBottom) <= touchRadius && offset.x in boxLeft..boxRight -> DragHandle.BOTTOM
                            kotlin.math.abs(offset.x - boxLeft) <= touchRadius && offset.y in boxTop..boxBottom -> DragHandle.LEFT
                            kotlin.math.abs(offset.x - boxRight) <= touchRadius && offset.y in boxTop..boxBottom -> DragHandle.RIGHT
                            offset.x in boxLeft..boxRight && offset.y in boxTop..boxBottom -> DragHandle.CENTER
                            else -> DragHandle.NONE
                        }
                    },
                    onDragEnd = { activeHandle = DragHandle.NONE },
                    onDragCancel = { activeHandle = DragHandle.NONE },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val dx = dragAmount.x / screenWidth
                        val dy = dragAmount.y / screenHeight

                        var left = targetRoi.left
                        var right = targetRoi.right
                        var top = targetRoi.top
                        var bottom = targetRoi.bottom

                        val minSize = 0.10f

                        when (activeHandle) {
                            DragHandle.CENTER -> {
                                val newCx = (targetRoi.centerX + dx).coerceIn(targetRoi.width / 2f, 1f - targetRoi.width / 2f)
                                val newCy = (targetRoi.centerY + dy).coerceIn(targetRoi.height / 2f, 1f - targetRoi.height / 2f)
                                onRoiChanged(targetRoi.copy(centerX = newCx, centerY = newCy))
                                return@detectDragGestures
                            }
                            DragHandle.TOP_LEFT -> {
                                left = (left + dx).coerceIn(0f, right - minSize)
                                top = (top + dy).coerceIn(0f, bottom - minSize)
                            }
                            DragHandle.TOP_RIGHT -> {
                                right = (right + dx).coerceIn(left + minSize, 1f)
                                top = (top + dy).coerceIn(0f, bottom - minSize)
                            }
                            DragHandle.BOTTOM_LEFT -> {
                                left = (left + dx).coerceIn(0f, right - minSize)
                                bottom = (bottom + dy).coerceIn(top + minSize, 1f)
                            }
                            DragHandle.BOTTOM_RIGHT -> {
                                right = (right + dx).coerceIn(left + minSize, 1f)
                                bottom = (bottom + dy).coerceIn(top + minSize, 1f)
                            }
                            DragHandle.TOP -> {
                                top = (top + dy).coerceIn(0f, bottom - minSize)
                            }
                            DragHandle.BOTTOM -> {
                                bottom = (bottom + dy).coerceIn(top + minSize, 1f)
                            }
                            DragHandle.LEFT -> {
                                left = (left + dx).coerceIn(0f, right - minSize)
                            }
                            DragHandle.RIGHT -> {
                                right = (right + dx).coerceIn(left + minSize, 1f)
                            }
                            DragHandle.NONE -> return@detectDragGestures
                        }

                        val newWidth = right - left
                        val newHeight = bottom - top
                        val newCx = left + (newWidth / 2f)
                        val newCy = top + (newHeight / 2f)

                        onRoiChanged(
                            targetRoi.copy(
                                centerX = newCx,
                                centerY = newCy,
                                width = newWidth,
                                height = newHeight
                            )
                        )
                    }
                )
            }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier)
        ) {
            // Canvas untuk menggambar Bounding Box kotak sederhana
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxWidth = screenWidth * targetRoi.width
                val boxHeight = screenHeight * targetRoi.height
                val boxLeft = (screenWidth * targetRoi.centerX) - (boxWidth / 2f)
                val boxTop = (screenHeight * targetRoi.centerY) - (boxHeight / 2f)

                val boxColor = when {
                    isEditMode -> StatusBlue
                    alignmentState.status == AlignmentStatus.ALIGNED -> OverlayAlignedLine
                    alignmentState.status == AlignmentStatus.MISALIGNED -> OverlayMisalignedLine
                    else -> OverlayGuideLine
                }

                // Semi-transparent shading di luar target box
                drawRect(
                    color = Color(0x44000000),
                    size = size
                )

                // Cutout highlight (transparan di dalam area box)
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxWidth, boxHeight)
                )

                // Gambar border bounding box kotak utama
                val strokeWidth = if (isEditMode) 3.5.dp.toPx() else 2.5.dp.toPx()
                val pathEffect = if (alignmentState.status == AlignmentStatus.ALIGNED) null else PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)

                drawRoundRect(
                    color = boxColor,
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxWidth, boxHeight),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = strokeWidth, pathEffect = pathEffect)
                )

                // Crosshair sederhana di pusat kotak
                val crosshairLength = 14.dp.toPx()
                val centerX = screenWidth * targetRoi.centerX
                val centerY = screenHeight * targetRoi.centerY
                drawLine(
                    color = boxColor.copy(alpha = 0.7f),
                    start = Offset(centerX - crosshairLength, centerY),
                    end = Offset(centerX + crosshairLength, centerY),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = boxColor.copy(alpha = 0.7f),
                    start = Offset(centerX, centerY - crosshairLength),
                    end = Offset(centerX, centerY + crosshairLength),
                    strokeWidth = 2.dp.toPx()
                )

                // Mode Kalibrasi: Gambar handle kontrol di 4 pojok & 4 sisi
                if (isEditMode) {
                    val handleRadius = 9.dp.toPx()
                    val handles = listOf(
                        Offset(boxLeft, boxTop),
                        Offset(boxLeft + boxWidth, boxTop),
                        Offset(boxLeft, boxTop + boxHeight),
                        Offset(boxLeft + boxWidth, boxTop + boxHeight),
                        Offset(boxLeft + (boxWidth / 2f), boxTop),
                        Offset(boxLeft + (boxWidth / 2f), boxTop + boxHeight),
                        Offset(boxLeft, boxTop + (boxHeight / 2f)),
                        Offset(boxLeft + boxWidth, boxTop + (boxHeight / 2f))
                    )
                    handles.forEach { h ->
                        drawCircle(
                            color = StatusBlue,
                            radius = handleRadius,
                            center = h
                        )
                        drawCircle(
                            color = Color.White,
                            radius = handleRadius * 0.45f,
                            center = h
                        )
                    }
                }
            }

            // Status Banner Notifikasi Posisi
            BannerStatusOverlay(
                alignmentState = alignmentState,
                isEditMode = isEditMode,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            )
        }
    }
}

/**
 * Banner notifikasi status posisi realtime
 */
@Composable
private fun BannerStatusOverlay(
    alignmentState: AlignmentState,
    isEditMode: Boolean,
    modifier: Modifier = Modifier
) {
    val isVisible = isEditMode || alignmentState.message.isNotBlank()

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val (bgColor, icon, text) = when {
            isEditMode -> Triple(
                StatusBlue.copy(alpha = 0.9f),
                Icons.Default.Warning,
                "Mode Kalibrasi: Tarik pojok/sisi untuk ubah ukuran kotak bebas (free-form)"
            )
            alignmentState.status == AlignmentStatus.MISALIGNED -> Triple(
                OverlayMisalignedLine.copy(alpha = 0.92f),
                Icons.Default.Warning,
                alignmentState.message
            )
            alignmentState.status == AlignmentStatus.ALIGNED -> Triple(
                OverlayAlignedLine.copy(alpha = 0.92f),
                Icons.Default.CheckCircle,
                "Posisi Pas"
            )
            else -> Triple(
                Color(0xCC1E293B),
                Icons.Default.Warning,
                alignmentState.message
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(bgColor, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}
