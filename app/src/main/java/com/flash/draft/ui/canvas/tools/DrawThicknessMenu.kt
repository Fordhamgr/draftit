package com.flash.draft.ui.canvas.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.ui.canvas.LocalLiquidCurve
import com.flash.draft.ui.canvas.LocalLiquidRefraction
import com.flash.draft.ui.canvas.LocalLiquidState
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import io.github.fletchmckee.liquid.liquid

@Composable
fun DrawThicknessMenu(
    isOpen: Boolean,
    strokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val glassBg = if (isDark) Color(0xCC1A1A1A) else Color(0xCCFFFFFF)
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)
    val textColor = if (isDark) Color.LightGray else Color.DarkGray

    val liquidState = LocalLiquidState.current
    val liquidRefraction = LocalLiquidRefraction.current
    val liquidCurve = LocalLiquidCurve.current
    val isPerformance = com.flash.draft.ui.canvas.item.LocalPerformanceMode.current
    val pillShape = RoundedCornerShape(50)

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { -it / 2 }, animationSpec = tween(300)) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it / 2 }, animationSpec = tween(200)) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .height(68.dp) // Perfect match to parent
                .then(if (isPerformance) Modifier else Modifier.shadow(16.dp, pillShape, spotColor = Color.Black.copy(alpha = 0.15f)))
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Size", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Slider(
                    value = strokeWidth,
                    onValueChange = onStrokeWidthChange,
                    valueRange = 2f..40f,
                    modifier = Modifier.width(140.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF2196F3), activeTrackColor = Color(0xFF2196F3))
                )
            }
        }
    }
}