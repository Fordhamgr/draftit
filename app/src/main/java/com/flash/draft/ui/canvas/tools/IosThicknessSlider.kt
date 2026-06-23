package com.flash.draft.ui.canvas.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun IosThicknessSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.Gray,
    thumbColor: Color = Color.White
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                    onValueChange(newValue)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                    onValueChange(newValue)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val minThickness = 2.dp.toPx()
        val maxThickness = 16.dp.toPx()
        
        val path = Path().apply {
            moveTo(0f, height / 2f - minThickness / 2f)
            lineTo(width, height / 2f - maxThickness / 2f)
            lineTo(width, height / 2f + maxThickness / 2f)
            lineTo(0f, height / 2f + minThickness / 2f)
            close()
        }
        
        // Draw inactive track
        drawPath(
            path = path,
            color = trackColor.copy(alpha = 0.2f)
        )
        
        val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        val activeWidth = width * fraction
        
        // Draw active track
        clipRect(left = 0f, top = 0f, right = activeWidth, bottom = height) {
            drawPath(
                path = path,
                color = trackColor
            )
        }
        
        // Draw Thumb
        val currentThickness = minThickness + fraction * (maxThickness - minThickness)
        val thumbRadius = (currentThickness / 2f) + 6.dp.toPx()
        drawCircle(
            color = thumbColor,
            radius = thumbRadius,
            center = Offset(activeWidth, height / 2f)
        )
        // Draw subtle border around thumb for definition
        drawCircle(
            color = Color.Black.copy(alpha = 0.15f),
            radius = thumbRadius,
            center = Offset(activeWidth, height / 2f),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
