package com.flash.draft.ui.canvas.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import com.flash.draft.ui.canvas.LocalLiquidCurve
import com.flash.draft.ui.canvas.LocalLiquidRefraction
import com.flash.draft.ui.canvas.LocalLiquidState
import io.github.fletchmckee.liquid.liquid

@Composable
fun DrawColorMenu(
    isOpen: Boolean,
    activeColor: Color,
    onColorSelect: (Color) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val glassBg = if (isDark) Color(0xCC1A1A1A) else Color(0xCCFFFFFF)
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)

    val liquidState = LocalLiquidState.current
    val liquidRefraction = LocalLiquidRefraction.current
    val liquidCurve = LocalLiquidCurve.current
    val isPerformance = com.flash.draft.ui.canvas.item.LocalPerformanceMode.current
    val pillShape = RoundedCornerShape(50)

    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color(0xFF34C759), Color(0xFFFF9500), Color.White)

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { -it / 2 }, animationSpec = tween(300)) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it / 2 }, animationSpec = tween(200)) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .height(68.dp) // Perfect match to parent
                .then(if (isPerformance || isDark) Modifier else Modifier.shadow(16.dp, pillShape, spotColor = Color.Black.copy(alpha = 0.15f)))
                .then(
                    if (liquidState != null && !isPerformance) Modifier.liquid(liquidState) {
                        refraction = liquidRefraction; curve = liquidCurve; shape = pillShape
                    } else Modifier
                )
                .background(glassBg, pillShape)
                .border(1.dp, glassBorder, pillShape)
                .clip(pillShape)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color, CircleShape)
                            .border(
                                width = if (activeColor == color) 3.dp else 1.dp,
                                color = if (activeColor == color) Color(0xFF2196F3) else Color.LightGray.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable { onColorSelect(color) }
                    )
                }
            }
        }
    }
}
