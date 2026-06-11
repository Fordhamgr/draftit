package com.flash.draft.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

@Composable
fun GridBackground(scaleProvider: () -> Float, offsetProvider: () -> Offset) {
    // Track GridBackground recomposition
    androidx.compose.runtime.SideEffect {
        android.util.Log.d("DraftCanvasPerf", "GridBackground function recomposed")
    }

    class FloatArrayHolder(var array: FloatArray)
    val holder = androidx.compose.runtime.remember { FloatArrayHolder(FloatArray(4000)) }
    val paint = androidx.compose.runtime.remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeCap = android.graphics.Paint.Cap.ROUND
            isAntiAlias = true
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val drawStart = System.nanoTime()
        val scale = scaleProvider()
        val offset = offsetProvider()
        
        var currentBaseSpacing = 75f
        var scaledSpacing = currentBaseSpacing * scale

        while (scaledSpacing < 40f) {
            currentBaseSpacing *= 2f
            scaledSpacing = currentBaseSpacing * scale
        }
        while (scaledSpacing > 120f) {
            currentBaseSpacing /= 2f
            scaledSpacing = currentBaseSpacing * scale
        }

        val remainderX = offset.x % scaledSpacing
        val remainderY = offset.y % scaledSpacing

        val startX = remainderX - scaledSpacing
        val startY = remainderY - scaledSpacing

        var index = 0
        var x = startX
        while (x < size.width + scaledSpacing) {
            var y = startY
            while (y < size.height + scaledSpacing) {
                if (index >= holder.array.size) {
                    holder.array = FloatArray(holder.array.size * 2)
                }
                holder.array[index++] = x
                holder.array[index++] = y
                y += scaledSpacing
            }
            x += scaledSpacing
        }

        paint.strokeWidth = 4f * scale
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawPoints(holder.array, 0, index, paint)
        }

        val elapsed = (System.nanoTime() - drawStart) / 1_000_000f
        if (elapsed > 2f) {
            android.util.Log.d("DraftCanvasPerf", "GridBackground draw took ${elapsed}ms")
        }
    }
}