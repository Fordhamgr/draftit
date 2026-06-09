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

    // Premium iOS-style Frosted Glass colors
    val glassBg = if (isDark) Color(0x991A1A1A) else Color(0x99FFFFFF)
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)

    // The universal liquid lens
    val liquidModifier = if (liquidState != null) {
        Modifier.liquid(liquidState) {
            refraction = liquidRefraction
            curve = liquidCurve
            this.shape = shape
        }
    } else Modifier

    Box(
        modifier = modifier
            .shadow(16.dp, shape, spotColor = Color.Black.copy(alpha = 0.15f))
            .then(liquidModifier)
            .background(glassBg, shape)
            .border(1.dp, glassBorder, shape)
            .clip(shape)
            .padding(contentPadding),
        content = content
    )
}