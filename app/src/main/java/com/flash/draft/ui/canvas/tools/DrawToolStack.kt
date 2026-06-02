package com.flash.draft.ui.canvas.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.flash.draft.ui.canvas.LocalLiquidCurve
import com.flash.draft.ui.canvas.LocalLiquidRefraction
import com.flash.draft.ui.canvas.LocalLiquidState
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import io.github.fletchmckee.liquid.liquid

@Composable
fun DrawToolStack(
    isOpen: Boolean,
    activeColor: Color,
    onBrushClick: () -> Unit,
    onEraserClick: () -> Unit,
    onColorClick: () -> Unit,
    onThicknessClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val glassBg = if (isDark) Color(0xB31A1A1A) else Color(0xB3FFFFFF)
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)
    val iconTint = if (isDark) Color.White else Color.Black

    val liquidState = LocalLiquidState.current
    val liquidRefraction = LocalLiquidRefraction.current
    val liquidCurve = LocalLiquidCurve.current

    val dockHeight = 68.dp // Exact same scale as the parent dock

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300)) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(200)) + fadeOut()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            @Composable
            fun ToolPill(icon: ImageVector, onClick: () -> Unit, tint: Color = iconTint) {
                Box(
                    modifier = Modifier
                        .size(dockHeight)
                        .shadow(16.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.15f))
                        .then(
                            if (liquidState != null) Modifier.liquid(liquidState) {
                                refraction = liquidRefraction; curve = liquidCurve; shape = CircleShape
                            } else Modifier
                        )
                        .background(glassBg, CircleShape)
                        .border(1.dp, glassBorder, CircleShape)
                        .clip(CircleShape)
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
                }
            }

            // Pill 4: Thickness
            ToolPill(Icons.Default.LineWeight, onThicknessClick)

            // Pill 3: Color Palette (Shows active color)
            ToolPill(Icons.Default.Palette, onColorClick, tint = activeColor)

            // Pill 2: Eraser
            ToolPill(Icons.Default.CleaningServices, onEraserClick)

            // Pill 1: Brush / Pen
            ToolPill(Icons.Default.Brush, onBrushClick)
        }
    }
}