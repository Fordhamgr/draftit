package com.flash.draft.ui.canvas.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
fun ColorPickerWheel(
    modifier: Modifier = Modifier,
    onColorSelected: (Color) -> Unit
) {
    var touchPosition by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    touchPosition = offset
                    onColorSelected(getColorAtPoint(offset, size.width.toFloat(), size.height.toFloat()))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    touchPosition = change.position
                    onColorSelected(getColorAtPoint(change.position, size.width.toFloat(), size.height.toFloat()))
                }
            }
    ) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)
        
        // Sweep gradient for Hue
        val colors = listOf(
            Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red
        )
        drawCircle(
            brush = Brush.sweepGradient(colors, center = center),
            radius = radius,
            center = center
        )
        
        // Radial gradient for Saturation (white at center to transparent)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        
        // Draw the selector ring
        touchPosition?.let { pos ->
            // Clamp position within circle
            val dx = pos.x - center.x
            val dy = pos.y - center.y
            val dist = sqrt(dx*dx + dy*dy)
            
            val clampedPos = if (dist > radius) {
                val ratio = radius / dist
                Offset(center.x + dx * ratio, center.y + dy * ratio)
            } else {
                pos
            }
            
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = clampedPos,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = 13.dp.toPx(),
                center = clampedPos,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

private fun getColorAtPoint(point: Offset, width: Float, height: Float): Color {
    val center = Offset(width / 2, height / 2)
    val radius = minOf(width, height) / 2
    
    val dx = point.x - center.x
    val dy = point.y - center.y
    val dist = sqrt(dx * dx + dy * dy)
    
    // Calculate Hue (angle)
    var angle = atan2(dy.toDouble(), dx.toDouble()).toFloat() * (180f / PI.toFloat())
    if (angle < 0f) angle += 360f
    
    // Calculate Saturation (distance from center)
    val saturation = (dist / radius).coerceIn(0f, 1f)
    
    // Convert HSV to Color
    return Color.hsv(angle, saturation, 1f)
}
