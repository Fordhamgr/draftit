    package com.flash.draft.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import io.github.fletchmckee.liquid.liquid

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalDarkTheme.current
    val liquidState = LocalLiquidState.current
    val liquidRefraction = LocalLiquidRefraction.current
    val liquidCurve = LocalLiquidCurve.current

    // Premium Frosted Glass colors
    val glassBg = if (isDark) Color(0x1AFFFFFF) else Color(0xCCFFFFFF)
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)

    val isPerformanceMode = com.flash.draft.ui.canvas.item.LocalPerformanceMode.current
    val liquidModifier = if (liquidState != null && !isPerformanceMode) {
        Modifier.liquid(liquidState) {
            refraction = liquidRefraction
            curve = liquidCurve
            this.shape = shape
        }
    } else Modifier

    Box(
        modifier = modifier
            .then(if (isPerformanceMode || isDark) Modifier else Modifier.shadow(16.dp, shape, spotColor = Color.Black.copy(alpha = 0.15f)))
            .then(liquidModifier)
            .background(glassBg, shape)
            .border(1.dp, glassBorder, shape)
            .clip(shape)
            .pointerInput(Unit) { detectTapGestures { } } // <--- Consume taps here
            .padding(contentPadding),
        content = content
    )
}
