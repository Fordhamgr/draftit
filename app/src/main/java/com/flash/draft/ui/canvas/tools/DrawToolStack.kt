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
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.Palette
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
    activeStrokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    onColorSelect: (Color) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val glassBg = if (isDark) Color(0xB31A1A1A) else Color(0xB3FFFFFF)
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)
    val iconTint = if (isDark) Color.White else Color.Black

    val liquidState = LocalLiquidState.current
    val liquidRefraction = LocalLiquidRefraction.current
    val liquidCurve = LocalLiquidCurve.current

    val dockHeight = 68.dp // Exact same scale as the parent dock

    Column(
        modifier = Modifier.pointerInput(Unit) { detectDragGestures { _, _ -> } },
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.Start
    ) {

        @Composable
        fun ExpandingToolPill(
            index: Int,
            isExpanded: Boolean,
            expandedWidth: androidx.compose.ui.unit.Dp,
            collapsedContent: @Composable () -> Unit,
            expandedContent: @Composable RowScope.() -> Unit
        ) {
            val width by animateDpAsState(
                targetValue = if (isExpanded) expandedWidth else dockHeight,
                animationSpec = tween(300)
            )

            AnimatedVisibility(
                visible = isOpen,
                enter = slideInHorizontally(
                    initialOffsetX = { -it - 50 },
                    animationSpec = tween(400, delayMillis = index * 50)
                ) + fadeIn(tween(400, delayMillis = index * 50)),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it - 50 },
                    animationSpec = tween(300)
                ) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .height(dockHeight)
                        .width(width)
                        .shadow(16.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.15f))
                        .then(
                            if (liquidState != null) Modifier.liquid(liquidState) {
                                refraction = liquidRefraction; curve = liquidCurve; shape = RoundedCornerShape(percent = 50)
                            } else Modifier
                        )
                        .background(glassBg, CircleShape)
                        .border(1.dp, glassBorder, CircleShape)
                        .clip(CircleShape)
                ) {
                    Crossfade(targetState = isExpanded, animationSpec = tween(300), label = "pillCrossfade") { expanded ->
                        if (!expanded) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                collapsedContent()
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                                expandedContent()
                            }
                        }
                    }
                }
            }
        }

        // Pill 4: Thickness
        ExpandingToolPill(
            index = 0,
            isExpanded = activeSubMenu == "Thickness" && isOpen,
            expandedWidth = 240.dp,
            collapsedContent = {
                Box(modifier = Modifier.fillMaxSize().clickable { onThicknessClick() }, contentAlignment = Alignment.Center) {
                    Icon(androidx.compose.material.icons.Icons.Default.LineWeight, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
                }
            },
            expandedContent = {
                val textColor = if (isDark) Color.LightGray else Color.DarkGray
                Text("Size", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(16.dp))
                RulerSlider(
                    value = activeStrokeWidth,
                    onValueChange = onStrokeWidthChange,
                    valueRange = 2f..40f,
                    modifier = Modifier.weight(1f).height(48.dp)
                )
            }
        )

        // Pill 3: Color Palette
        val colors = listOf(Color.Black, Color.Red, Color.Blue, Color(0xFF34C759), Color(0xFFFF9500), Color.White)
        ExpandingToolPill(
            index = 1,
            isExpanded = activeSubMenu == "Color" && isOpen,
            expandedWidth = 280.dp,
            collapsedContent = {
                Box(modifier = Modifier.fillMaxSize().clickable { onColorClick() }, contentAlignment = Alignment.Center) {
                    Icon(androidx.compose.material.icons.Icons.Default.Palette, contentDescription = null, tint = activeColor, modifier = Modifier.size(28.dp))
                }
            },
            expandedContent = {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(color, CircleShape)
                            .border(
                                width = if (activeColor == color) 3.dp else 1.dp,
                                color = if (activeColor == color) Color(0xFF2196F3) else Color.LightGray.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable { onColorSelect(color); onColorClick() /* auto close */ }
                    )
                }
            }
        )

        // Pill 2 & 1: Split Eraser / Pencil Pill
        val isDark = LocalDarkTheme.current
        val activeBg = if (isDark) Color.White else Color.Black
        ExpandingToolPill(
            index = 2,
            isExpanded = activeSubMenu == "Brush" && isOpen,
            expandedWidth = 200.dp,
            collapsedContent = {
                val isEraserActive = activeToolType == "Eraser"
                val eraserBg by animateColorAsState(if (isEraserActive) activeBg else Color.Transparent)
                val penBg by animateColorAsState(if (!isEraserActive) activeBg else Color.Transparent)
                
                val activeIconTint = if (isDark) Color.Black else Color.White
                val iconEraserTint by animateColorAsState(if (isEraserActive) activeIconTint else iconTint)
                val iconPenTint by animateColorAsState(if (!isEraserActive) activeIconTint else iconTint)

                // Circle mode with 45-degree split
                Canvas(modifier = Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { 
                            onToolChange("Pen") // Clicking anywhere on circle switches to Pen!
                        }
                    }
                ) {
                    val pathLeft = Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(0f, size.height); close() }
                    drawPath(pathLeft, eraserBg)
                    
                    val pathRight = Path().apply { moveTo(size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                    drawPath(pathRight, penBg)
                    drawLine(color = glassBorder, start = Offset(size.width, 0f), end = Offset(0f, size.height), strokeWidth = 1.dp.toPx())
                }
                
                Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onToolChange("Pen") } }) {
                    Icon(androidx.compose.material.icons.Icons.Default.CleaningServices, contentDescription = "Eraser", tint = iconEraserTint, modifier = Modifier.size(20.dp).align(Alignment.TopStart).offset(x = 16.dp, y = 16.dp))
                    Icon(androidx.compose.material.icons.Icons.Default.Brush, contentDescription = "Pen", tint = iconPenTint, modifier = Modifier.size(20.dp).align(Alignment.BottomEnd).offset(x = (-16).dp, y = (-16).dp))
                }
            },
            expandedContent = {
                // Eraser Collapser
                Box(modifier = Modifier.weight(1f).fillMaxHeight().clickable { onToolChange("Eraser") }, contentAlignment = Alignment.Center) {
                    Icon(androidx.compose.material.icons.Icons.Default.CleaningServices, contentDescription = "Eraser", tint = iconTint, modifier = Modifier.size(24.dp))
                }
                
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(glassBorder))
                
                val subTools = listOf("Pen" to androidx.compose.material.icons.Icons.Default.Create, "Brush" to androidx.compose.material.icons.Icons.Default.Brush, "Highlighter" to androidx.compose.material.icons.Icons.Default.AutoFixHigh)
                subTools.forEach { (name, icon) ->
                    val isActive = activeToolType == name
                    val toolBg by animateColorAsState(if (isActive) activeBg else Color.Transparent)
                    val activeIconTint = if (isDark) Color.Black else Color.White
                    val toolIconTint by animateColorAsState(if (isActive) activeIconTint else iconTint)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(toolBg)
                            .clickable { onToolChange(name) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = name, tint = toolIconTint, modifier = Modifier.size(24.dp))
                    }
                }
            }
        )
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