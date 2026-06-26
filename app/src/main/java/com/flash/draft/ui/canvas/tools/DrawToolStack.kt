package com.flash.draft.ui.canvas.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import com.flash.draft.ui.canvas.item.LocalDarkTheme
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
    activeToolType: String,
    activeSubMenu: String?,
    onToolChange: (String) -> Unit,
    onColorClick: () -> Unit,
    onThicknessClick: () -> Unit,
    onRainbowClick: () -> Unit,
    activeStrokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    onColorSelect: (Color) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val glassBg = if (isDark) Color(0xCC1A1A1A) else Color(0xCCFFFFFF)
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)
    val iconTint = if (isDark) Color.White else Color.Black
    val activeBg = if (isDark) Color.White else Color.Black
    val activeIconTint = if (isDark) Color.Black else Color.White

    val liquidState = LocalLiquidState.current
    val liquidRefraction = LocalLiquidRefraction.current
    val liquidCurve = LocalLiquidCurve.current
    val isPerformance = com.flash.draft.ui.canvas.item.LocalPerformanceMode.current

    val dockHeight = 56.dp

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { -it - 50 }) + fadeIn(tween(300)),
        exit = slideOutHorizontally(targetOffsetX = { -it - 50 }) + fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .height(dockHeight)
                .then(if (isPerformance || isDark) Modifier else Modifier.shadow(16.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.15f)))
                .then(
                    if (liquidState != null && !isPerformance) Modifier.liquid(liquidState) {
                        refraction = liquidRefraction; curve = liquidCurve; shape = RoundedCornerShape(percent = 50)
                    } else Modifier
                )
                .background(glassBg, CircleShape)
                .border(1.dp, glassBorder, CircleShape)
                .clip(CircleShape)
                .animateContentSize(animationSpec = androidx.compose.animation.core.spring(stiffness = 800f, dampingRatio = 0.8f))
                .pointerInput(Unit) { detectDragGestures { _, _ -> } }
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = activeSubMenu,
                transitionSpec = {
                    (fadeIn(tween(200)) togetherWith fadeOut(tween(200)))
                },
                label = "dockContent"
            ) { subMenu ->
                Row(
                    modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when (subMenu) {
                        "Color" -> {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onColorClick() }, contentAlignment = Alignment.Center) {
                                Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Back", tint = iconTint, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(glassBorder))
                            Spacer(modifier = Modifier.width(4.dp))
                            val colors = listOf(Color.Black, Color.Red, Color(0xFF34C759))
                            colors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(32.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (activeColor == color) 3.dp else 1.dp,
                                            color = if (activeColor == color) Color(0xFF2196F3) else Color.LightGray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable { onColorSelect(color); onColorClick() }
                                )
                            }
                            // Rainbow Button
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(32.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                            listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(1.dp, glassBorder, CircleShape)
                                    .clickable { onRainbowClick() }
                            )
                        }
                        "CustomColor" -> {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onColorClick() }, contentAlignment = Alignment.Center) {
                                Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Back", tint = iconTint, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(glassBorder))
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Active Color Indicator
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(activeColor, CircleShape)
                                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            val angle = ratio * 360f
                                            onColorSelect(Color.hsv(angle, 1f, 1f))
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, _ ->
                                            change.consume()
                                            val ratio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            val angle = ratio * 360f
                                            onColorSelect(Color.hsv(angle, 1f, 1f))
                                        }
                                    }
                            ) {
                                drawRect(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)
                                    )
                                )
                            }
                        }
                        "Thickness" -> {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onThicknessClick() }, contentAlignment = Alignment.Center) {
                                Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Back", tint = iconTint, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(glassBorder))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Size", color = iconTint, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp))
                            ThicknessSlider(
                                value = activeStrokeWidth,
                                onValueChange = onStrokeWidthChange,
                                valueRange = 2f..100f,
                                modifier = Modifier.width(180.dp).height(40.dp).padding(horizontal = 8.dp),
                                trackColor = if (isDark) Color.White else Color.Black,
                                thumbColor = if (isDark) Color.White else Color.Black
                            )
                        }
                        else -> {
                            // Main Dock
                            val tools = listOf(
                                "Eraser" to androidx.compose.material.icons.Icons.Default.CleaningServices,
                                "Pen" to androidx.compose.material.icons.Icons.Default.Create,
                                "Brush" to androidx.compose.material.icons.Icons.Default.Brush,
                                "Highlighter" to androidx.compose.material.icons.Icons.Default.AutoFixHigh
                            )
                            tools.forEach { (name, icon) ->
                                val isActive = activeToolType == name
                                val toolBg by animateColorAsState(if (isActive) activeBg else Color.Transparent)
                                val toolIconTint by animateColorAsState(if (isActive) activeIconTint else iconTint)
                                
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(toolBg, CircleShape)
                                        .clip(CircleShape)
                                        .clickable { onToolChange(name) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = name, tint = toolIconTint, modifier = Modifier.size(20.dp))
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(glassBorder))
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // Color Indicator
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(activeColor, CircleShape)
                                    .border(1.dp, glassBorder, CircleShape)
                                    .clickable { onColorClick() }
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // Size Indicator
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { onThicknessClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Default.Tune, contentDescription = "Thickness", tint = iconTint, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RulerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val rangeMin = valueRange.start
    val rangeMax = valueRange.endInclusive
    val sensitivity = 0.2f // 1 pixel drag = 0.2f value change
    val currentValue by rememberUpdatedState(value)
    
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                var dragStartValue = 0f
                var accumulatedDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { 
                        dragStartValue = currentValue
                        accumulatedDrag = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount
                        val newValue = (dragStartValue - accumulatedDrag * sensitivity).coerceIn(rangeMin, rangeMax)
                        onValueChange(newValue)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val centerX = width / 2f
        
        val tickInterval = 2
        val pixelsPerUnit = 1f / sensitivity
        
        val firstTick = kotlin.math.floor((value - (centerX / pixelsPerUnit)) / tickInterval).toInt() * tickInterval
        val lastTick = kotlin.math.ceil((value + (centerX / pixelsPerUnit)) / tickInterval).toInt() * tickInterval
        
        for (i in firstTick..lastTick step tickInterval) {
            val tickValue = i.toFloat()
            if (tickValue < rangeMin || tickValue > rangeMax) continue
            
            val offsetX = (tickValue - value) * pixelsPerUnit
            val x = centerX + offsetX
            
            val dist = kotlin.math.abs(offsetX)
            val maxDist = centerX
            val ratio = 1f - (dist / maxDist).coerceIn(0f, 1f)
            val easedRatio = kotlin.math.sin(ratio * Math.PI / 2).toFloat()
            
            val isMajor = i % (tickInterval * 5) == 0
            val baseHeight = if (isMajor) height * 0.6f else height * 0.3f
            val tickHeight = baseHeight * (0.4f + 0.6f * easedRatio)
            val tickAlpha = (easedRatio * 1.5f).coerceIn(0f, 1f)
            
            drawLine(
                color = Color.Gray.copy(alpha = tickAlpha),
                start = Offset(x, centerY - tickHeight / 2f),
                end = Offset(x, centerY + tickHeight / 2f),
                strokeWidth = if (isMajor) 4f else 2f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
        
        // Center indicator
        drawLine(
            color = Color(0xFF2196F3),
            start = Offset(centerX, centerY - height * 0.4f),
            end = Offset(centerX, centerY + height * 0.4f),
            strokeWidth = 6f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
