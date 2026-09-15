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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
 * Overlay interaktif Bounding Box dengan Grid Kamera.
 * Dioptimalkan untuk responsivitas free-form dan tampilan bersih.
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

                        val touchRadius = 64.dp.toPx()

                        activeHandle = when {
                            hypot(offset.x - boxLeft, offset.y - boxTop) <= touchRadius -> DragHandle.TOP_LEFT
                            hypot(offset.x - boxRight, offset.y - boxTop) <= touchRadius -> DragHandle.TOP_RIGHT
                            hypot(offset.x - boxLeft, offset.y - boxBottom) <= touchRadius -> DragHandle.BOTTOM_LEFT
                            hypot(offset.x - boxRight, offset.y - boxBottom) <= touchRadius -> DragHandle.BOTTOM_RIGHT
                            kotlin.math.abs(offset.y - boxTop) <= touchRadius / 1.5f && offset.x in boxLeft..boxRight -> DragHandle.TOP
                            kotlin.math.abs(offset.y - boxBottom) <= touchRadius / 1.5f && offset.x in boxLeft..boxRight -> DragHandle.BOTTOM
                            kotlin.math.abs(offset.x - boxLeft) <= touchRadius / 1.5f && offset.y in boxTop..boxBottom -> DragHandle.LEFT
                            kotlin.math.abs(offset.x - boxRight) <= touchRadius / 1.5f && offset.y in boxTop..boxBottom -> DragHandle.RIGHT
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
                        val minSize = 0.05f

                        when (activeHandle) {
                            DragHandle.CENTER -> {
                                val newCx = (targetRoi.centerX + dx).coerceIn(targetRoi.width / 2f, 1f - targetRoi.width / 2f)
                                val newCy = (targetRoi.centerY + dy).coerceIn(targetRoi.height / 2f, 1f - targetRoi.height / 2f)
                                onRoiChanged(targetRoi.copy(centerX = newCx, centerY = newCy))
                                return@detectDragGestures
                            }
                            DragHandle.TOP_LEFT -> { left = (left + dx).coerceIn(0f, right - minSize); top = (top + dy).coerceIn(0f, bottom - minSize) }
                            DragHandle.TOP_RIGHT -> { right = (right + dx).coerceIn(left + minSize, 1f); top = (top + dy).coerceIn(0f, bottom - minSize) }
                            DragHandle.BOTTOM_LEFT -> { left = (left + dx).coerceIn(0f, right - minSize); bottom = (bottom + dy).coerceIn(top + minSize, 1f) }
                            DragHandle.BOTTOM_RIGHT -> { right = (right + dx).coerceIn(left + minSize, 1f); bottom = (bottom + dy).coerceIn(top + minSize, 1f) }
                            DragHandle.TOP -> top = (top + dy).coerceIn(0f, bottom - minSize)
                            DragHandle.BOTTOM -> bottom = (bottom + dy).coerceIn(top + minSize, 1f)
                            DragHandle.LEFT -> left = (left + dx).coerceIn(0f, right - minSize)
                            DragHandle.RIGHT -> right = (right + dx).coerceIn(left + minSize, 1f)
                            DragHandle.NONE -> return@detectDragGestures
                        }

                        val newWidth = right - left
                        val newHeight = bottom - top
                        onRoiChanged(targetRoi.copy(centerX = left + (newWidth / 2f), centerY = top + (newHeight / 2f), width = newWidth, height = newHeight))
                    }
                )
            }
        } else {
            Modifier
        }

        Box(modifier = Modifier.fillMaxSize().then(gestureModifier)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridColor = Color.White.copy(alpha = 0.15f)
                val gridStroke = 1.dp.toPx()
                drawLine(gridColor, Offset(size.width / 3, 0f), Offset(size.width / 3, size.height), gridStroke)
                drawLine(gridColor, Offset(size.width * 2 / 3, 0f), Offset(size.width * 2 / 3, size.height), gridStroke)
                drawLine(gridColor, Offset(0f, size.height / 3), Offset(size.width, size.height / 3), gridStroke)
                drawLine(gridColor, Offset(0f, size.height * 2 / 3), Offset(size.width, size.height * 2 / 3), gridStroke)

                val boxWidth = screenWidth * targetRoi.width
                val boxHeight = screenHeight * targetRoi.height
                val boxLeft = (screenWidth * targetRoi.centerX) - (boxWidth / 2f)
                val boxTop = (screenHeight * targetRoi.centerY) - (boxHeight / 2f)
                val boxRight = boxLeft + boxWidth
                val boxBottom = boxTop + boxHeight

                val boxColor = when {
                    isEditMode -> StatusBlue
                    alignmentState.status == AlignmentStatus.ALIGNED -> OverlayAlignedLine
                    alignmentState.status == AlignmentStatus.MISALIGNED -> OverlayMisalignedLine
                    else -> OverlayGuideLine
                }

                drawRect(color = Color(0x22000000), size = size)
                drawRect(color = Color.Transparent, topLeft = Offset(boxLeft, boxTop), size = Size(boxWidth, boxHeight))

                val strokeWidth = if (isEditMode) 3.dp.toPx() else 2.dp.toPx()
                val pathEffect = if (alignmentState.status == AlignmentStatus.ALIGNED) null else PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)

                drawRoundRect(
                    color = boxColor,
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxWidth, boxHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(width = strokeWidth, pathEffect = pathEffect)
                )

                if (isEditMode) {
                    val r = 6.dp.toPx()
                    val hPos = listOf(
                        Offset(boxLeft, boxTop), Offset(boxRight, boxTop),
                        Offset(boxLeft, boxBottom), Offset(boxRight, boxBottom),
                        Offset(boxLeft + boxWidth / 2, boxTop), Offset(boxLeft + boxWidth / 2, boxBottom),
                        Offset(boxLeft, boxTop + boxHeight / 2), Offset(boxRight, boxTop + boxHeight / 2)
                    )
                    hPos.forEach {
                        drawCircle(StatusBlue, r, it)
                        drawCircle(Color.White, r * 0.4f, it)
                    }
                }
            }

            BannerStatusOverlay(
                alignmentState = alignmentState,
                isEditMode = isEditMode,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 70.dp)
            )
        }
    }
}

@Composable
private fun BannerStatusOverlay(
    alignmentState: AlignmentState,
    isEditMode: Boolean,
    modifier: Modifier = Modifier
) {
    val isVisible = isEditMode || (alignmentState.message.isNotBlank() && alignmentState.status != AlignmentStatus.ALIGNED)
    AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        val (bgColor, icon, text) = if (isEditMode) Triple(StatusBlue.copy(0.8f), Icons.Default.Warning, "Mode Kalibrasi: Atur Kotak")
        else Triple(OverlayMisalignedLine.copy(0.8f), Icons.Default.Warning, alignmentState.message)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(bgColor, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.padding(end = 6.dp).size(16.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 11.sp)
        }
    }
}
