package com.flash.draft.ui.canvas.tools

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flash.draft99.R
import com.flash.draft.ui.canvas.item.LocalDarkTheme

@Composable
fun DrawToolsContent(
    activeColor: Color,
    activeToolType: String,
    activeSubMenu: String?,
    onToolChange: (String) -> Unit,
    onColorClick: () -> Unit,
    onThicknessClick: () -> Unit, // Kept for compatibility, but unused here
    onRainbowClick: () -> Unit,
    activeStrokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    onColorSelect: (Color) -> Unit,
    onClose: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val iconTint = if (isDark) Color.White else Color.Black
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)

    val isPropertiesOpen = activeSubMenu == "Color" || activeSubMenu == "CustomColor" || activeSubMenu == "Thickness"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = isPropertiesOpen,
            transitionSpec = {
                if (targetState) {
                    // Opening: Morph out from the bottom-left Color Circle (x = 10%, y = 100%)
                    (scaleIn(tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialScale = 0.6f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.1f, 1f)) + 
                     fadeIn(tween(300))) togetherWith
                    (scaleOut(tween(200), targetScale = 0.6f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.1f, 1f)) + 
                     fadeOut(tween(200)))
                } else {
                    // Closing: Morph back into the bottom-left Color Circle
                    (scaleIn(tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialScale = 0.6f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.1f, 1f)) + 
                     fadeIn(tween(300))) togetherWith
                    (scaleOut(tween(200), targetScale = 0.6f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.1f, 1f)) + 
                     fadeOut(tween(200)))
                }.using(SizeTransform(clip = true) { _, _ ->
                    spring(dampingRatio = 0.8f, stiffness = 600f)
                })
            },
            label = "dockContent"
        ) { propertiesOpen ->
            if (propertiesOpen) {
                ColorPickerView(
                    activeColor = activeColor,
                    onColorSelect = onColorSelect
                )
            } else {
                // 96.dp acts as the tall frosted glass background for the floating tools, covering ~70% of them.
                Box(modifier = Modifier.fillMaxWidth().height(96.dp))
            }
        }
        if (isPropertiesOpen) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(iconTint.copy(alpha = 0.08f)))
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        // Bottom Control Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator (clicks to toggle properties)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(activeColor, CircleShape)
                    .border(2.dp, glassBorder, CircleShape)
                    .clickable { 
                        if (isPropertiesOpen) {
                            onToolChange(activeToolType) // Close properties
                        } else {
                            onColorClick() // Open properties
                        }
                    }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Thickness slider taking remaining space
            ThicknessView(
                activeStrokeWidth = activeStrokeWidth,
                onStrokeWidthChange = onStrokeWidthChange,
                modifier = Modifier.weight(1f).height(36.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Close button (closes properties or the whole dock)
            Box(
                modifier = Modifier
                    .size(48.dp) // Larger touch target
                    .clip(CircleShape)
                    .clickable { 
                        if (isPropertiesOpen) {
                            onToolChange(activeToolType) // Back to tools
                        } else {
                            onClose() // Close the whole dock
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = iconTint, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun ColorPickerView(
    activeColor: Color,
    onColorSelect: (Color) -> Unit
) {
    val hsvState = remember { 
        val array = FloatArray(3)
        android.graphics.Color.colorToHSV(activeColor.toArgb(), array)
        mutableStateOf(array)
    }
    val hue = hsvState.value[0]
    val sat = hsvState.value[1]
    val value = hsvState.value[2]

    val updateColor = { newHue: Float, newSat: Float, newVal: Float ->
        hsvState.value = floatArrayOf(newHue, newSat, newVal)
        onColorSelect(Color.hsv(newHue, newSat, newVal))
    }

    Column(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 2D SV Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.hsv(hue, 1f, 1f))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val s = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val v = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                        updateColor(hsvState.value[0], s, v)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val s = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val v = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                        updateColor(hsvState.value[0], s, v)
                    }
                }
        ) {
            // White to transparent (horizontal)
            Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.White, Color.Transparent))))
            // Transparent to black (vertical)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))))
            
            // Thumb
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(sat * size.width, (1f - value) * size.height),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }
        
        // Hue Slider
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val h = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                        updateColor(h, hsvState.value[1], hsvState.value[2])
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val h = (offset.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                        updateColor(h, hsvState.value[1], hsvState.value[2])
                    }
                }
        ) {
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                )
            )
            // Thumb
            val indicatorX = (hue / 360f) * size.width
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = Offset(indicatorX, size.height / 2f))
            drawCircle(color = Color.Black, radius = 10.dp.toPx(), center = Offset(indicatorX, size.height / 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
private fun ThicknessView(
    activeStrokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val trackColor = if (isDark) Color(0xFF333333) else Color(0xFFE5E5E5)
    val activeTrackColor = if (isDark) Color(0xFF666666) else Color(0xFFB0B0B0)
    
    Box(
        modifier = modifier.padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onStrokeWidthChange(2f + ratio * 98f) // 2f to 100f
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val ratio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onStrokeWidthChange(2f + ratio * 98f)
                    }
                }
        ) {
            val ratio = ((activeStrokeWidth - 2f) / 98f).coerceIn(0f, 1f)
            val trackHeight = 10.dp.toPx()
            val trackRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f)
            
            // Background Track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, size.height / 2f - trackHeight / 2f),
                size = androidx.compose.ui.geometry.Size(size.width, trackHeight),
                cornerRadius = trackRadius
            )
            
            // Active Track
            clipRect(right = size.width * ratio) {
                drawRoundRect(
                    color = activeTrackColor,
                    topLeft = Offset(0f, size.height / 2f - trackHeight / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width, trackHeight),
                    cornerRadius = trackRadius
                )
            }
            
            // Thumb
            val thumbRadius = 12.dp.toPx()
            val thumbCenter = Offset(size.width * ratio, size.height / 2f)
            
            // Drop shadow for thumb
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = thumbRadius + 2.dp.toPx(),
                center = thumbCenter.copy(y = thumbCenter.y + 4.dp.toPx())
            )
            
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = thumbCenter
            )
            
            // Inner dot on thumb to make it look premium
            drawCircle(
                color = activeTrackColor,
                radius = thumbRadius * 0.3f,
                center = thumbCenter
            )
        }
    }
}

@Composable
private fun ToolIllustration(
    name: String,
    drawableRes: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val offsetY by animateFloatAsState(
        targetValue = if (isActive) with(density) { -16.dp.toPx() } else 0f, // Reduced jump to prevent disconnect
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "toolOffset"
    )

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .clickable(
                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .requiredHeight(150.dp) // SVGs stick out of container
                .graphicsLayer {
                    translationY = offsetY // Much larger jump, goes way above dock
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                // Drop shadow based on the SVG alpha mask
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxHeight()
                        .offset(y = 4.dp, x = 2.dp)
                        .alpha(if (isActive) 0.4f else 0.15f),
                    contentScale = ContentScale.Fit,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black)
                )
                // Main SVG Image
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = name,
                    modifier = Modifier.fillMaxHeight(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun FloatingDrawTools(
    isVisible: Boolean,
    activeToolType: String,
    onToolChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        val tools = listOf(
            Triple("Brush", R.drawable.ic_tool_pencil, 0),
            Triple("Pen", R.drawable.ic_tool_pen, 1),
            Triple("Highlighter", R.drawable.ic_tool_highlighter, 2),
            Triple("Eraser", R.drawable.ic_tool_eraser, 3)
        )
        
        tools.forEach { (name, resId, index) ->
            val i = index as Int
            val delayEnter = i * 40
            val delayExit = i * 30
            androidx.compose.animation.AnimatedVisibility(
                visible = isVisible,
                enter = androidx.compose.animation.fadeIn(tween(250, delayMillis = delayEnter)) + 
                        androidx.compose.animation.scaleIn(tween(250, delayMillis = delayEnter, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialScale = 0.5f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)),
                exit = androidx.compose.animation.fadeOut(tween(200, delayMillis = delayExit)) + 
                       androidx.compose.animation.scaleOut(tween(200, delayMillis = delayExit, easing = androidx.compose.animation.core.FastOutLinearInEasing), targetScale = 0.5f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f))
            ) {
                ToolIllustration(
                    name as String,
                    resId as Int,
                    activeToolType == name,
                    { onToolChange(name as String) }
                )
            }
        }
    }
}
