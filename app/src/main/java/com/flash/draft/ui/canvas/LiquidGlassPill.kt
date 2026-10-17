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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.pointer.pointerInput
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import com.flash.draft.ui.canvas.item.LocalLiquidDarkTheme
import com.flash.draft.ui.canvas.item.LocalPerformanceMode
import io.github.fletchmckee.liquid.liquid

@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalDarkTheme.current
    val isLiquidDarkTheme = LocalLiquidDarkTheme.current
    val liquidState = LocalLiquidState.current
    val liquidRefraction = LocalLiquidRefraction.current
    val liquidCurve = LocalLiquidCurve.current
    val liquidFrost = LocalLiquidFrost.current
    val isPerformanceMode = LocalPerformanceMode.current
    val colorScheme = MaterialTheme.colorScheme

    val isLiquidActive = !isDark || isLiquidDarkTheme

    // Surface colors:
    // In dark mode: if isLiquidDarkTheme is true, active liquid glass is used; if false, sleek opaque surfaceContainerHigh
    val glassBg = when {
        !isDark -> Color(0xCCFFFFFF)
        isLiquidDarkTheme -> Color(0x1AFFFFFF)
        else -> colorScheme.surfaceContainerHigh
    }
    val glassBorder = when {
        !isDark -> Color(0x4DFFFFFF)
        isLiquidDarkTheme -> Color(0x33FFFFFF)
        else -> colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    val liquidModifier = if (liquidState != null && !isPerformanceMode && isLiquidActive) {
        Modifier.liquid(liquidState) {
            refraction = liquidRefraction
            curve = liquidCurve
            if (liquidFrost > 0f) frost = (liquidFrost * 24f).dp
            this.shape = shape
        }
    } else Modifier

    val shadowModifier = if (isPerformanceMode || (isDark && isLiquidDarkTheme)) {
        Modifier
    } else {
        Modifier.shadow(16.dp, shape, spotColor = Color.Black.copy(alpha = 0.2f))
    }

    Box(
        modifier = modifier
            .then(shadowModifier)
            .then(liquidModifier)
            .background(glassBg, shape)
            .border(1.dp, glassBorder, shape)
            .clip(shape)
            .pointerInput(Unit) { detectTapGestures { } } // <--- Consume taps here
            .padding(contentPadding),
        content = content
    )
}
