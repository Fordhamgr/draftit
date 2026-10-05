package com.flash.draft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp

@Composable
fun PremiumSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.Black,
    inactiveColor: Color = Color(0xFFE0E0E0)
) {
    var width by remember { mutableStateOf(1f) }
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(24.dp)
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val newFraction = (offset.x / width).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start))
                }
            }
            .pointerInput(valueRange) {
                detectDragGestures { change, _ ->
                    val newFraction = (change.position.x / width).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start))
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
        ) {
            Box(modifier = Modifier.fillMaxHeight().weight(fraction.coerceAtLeast(0.001f)).background(activeColor))
            Box(modifier = Modifier.fillMaxHeight().weight((1f - fraction).coerceAtLeast(0.001f)).background(inactiveColor))
        }

        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset((fraction * width).toInt() - 2.dp.roundToPx(), 0) }
                .size(width = 4.dp, height = 24.dp)
                .background(activeColor, RoundedCornerShape(2.dp))
        )
    }
}
