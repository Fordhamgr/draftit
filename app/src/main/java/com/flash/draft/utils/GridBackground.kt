package com.flash.draft.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap

@Composable
fun GridBackground(scale: Float, offset: Offset) {
    Canvas(modifier = Modifier.fillMaxSize()) {
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

        val points = mutableListOf<Offset>()
        var x = startX
        while (x < size.width + scaledSpacing) {
            var y = startY
            while (y < size.height + scaledSpacing) {
                points.add(Offset(x, y))
                y += scaledSpacing
            }
            x += scaledSpacing
        }

        drawPoints(
            points = points,
            pointMode = PointMode.Points,
            color = Color.LightGray,
            strokeWidth = 4f * scale,
            cap = StrokeCap.Round
        )
    }
}