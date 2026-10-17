package com.flash.draft.ui.canvas.tools

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft99.R
import com.flash.draft.ui.canvas.item.LocalDarkTheme

@Composable
fun DrawToolsContent(
    activeColor: Color,
    activeToolType: String,
    activeSubMenu: String?,
    onToolChange: (String) -> Unit,
    onColorClick: () -> Unit,
    onThicknessClick: () -> Unit,
    onRainbowClick: () -> Unit,
    activeStrokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    onColorSelect: (Color) -> Unit,
    onClose: () -> Unit,
    recentColors: List<Color> = emptyList(),
    onCommitRecentColor: (Color) -> Unit = {}
) {
    val isDark = LocalDarkTheme.current
    val iconTint = if (isDark) Color.White else Color.Black
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)

    val isPropertiesOpen = activeSubMenu == "Color" || activeSubMenu == "CustomColor" || activeSubMenu == "Thickness"

    // The entire content uses animateContentSize on a wrapper Column.
    // This is the SAME pattern as the '+' insert pill in BottomNavBar:
    // a single container that swaps its children and smoothly resizes.
    Column(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp, top = 8.dp)
            .animateContentSize(animationSpec = spring(dampingRatio = 1f, stiffness = 600f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isPropertiesOpen) {
            // ── COLOR PICKER MODE ──
            // Only the gradient picker + hue slider, no size slider, no SVG tools
            ColorPickerView(
                activeColor = activeColor,
                onColorSelect = onColorSelect,
                recentColors = recentColors,
                onCommitRecentColor = onCommitRecentColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Done button to close the picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))
                    .clickable { 
                        onCommitRecentColor(activeColor)
                        onToolChange(activeToolType) 
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text(
                    text = "Done",
                    color = iconTint,
                    fontSize = 15.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            }
        } else {
            // 96.dp spacer for the floating tools rendered externally in DraftCanvas
            Box(modifier = Modifier.fillMaxWidth().height(96.dp))
            Spacer(modifier = Modifier.height(4.dp))

            // Bottom row: [color circle] [PremiumSlider] [X]
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color circle – tapping it opens the picker
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(activeColor, CircleShape)
                        .border(2.dp, glassBorder, CircleShape)
                        .clip(CircleShape)
                        .clickable { onColorClick() }
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Thickness slider using PremiumSlider
                com.flash.draft.ui.components.PremiumSlider(
                    value = activeStrokeWidth,
                    onValueChange = onStrokeWidthChange,
                    valueRange = 2f..40f,
                    modifier = Modifier.weight(1f).height(36.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Close button (closes the whole dock)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = iconTint, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ColorPickerView(
    activeColor: Color,
    onColorSelect: (Color) -> Unit,
    recentColors: List<Color>,
    onCommitRecentColor: (Color) -> Unit
) {
    val hsvState = remember { 
        val array = FloatArray(3)
        android.graphics.Color.colorToHSV(activeColor.toArgb(), array)
        mutableStateOf(array)
    }

    LaunchedEffect(activeColor) {
        val array = FloatArray(3)
        android.graphics.Color.colorToHSV(activeColor.toArgb(), array)
        hsvState.value = array
    }

    val hue = hsvState.value[0]
    val sat = hsvState.value[1]
    val value = hsvState.value[2]

    val updateColor = { newHue: Float, newSat: Float, newVal: Float ->
        hsvState.value = floatArrayOf(newHue, newSat, newVal)
        onColorSelect(Color.hsv(newHue, newSat, newVal))
    }

    Column(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 2D SV Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.hsv(hue, 1f, 1f))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            val s = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val v = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                            updateColor(hsvState.value[0], s, v)
                        },
                        onDragEnd = {
                            val committedColor = Color.hsv(hsvState.value[0], hsvState.value[1], hsvState.value[2])
                            onCommitRecentColor(committedColor)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val s = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val v = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                        val newColor = Color.hsv(hsvState.value[0], s, v)
                        updateColor(hsvState.value[0], s, v)
                        onCommitRecentColor(newColor)
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
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            val h = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                            updateColor(h, hsvState.value[1], hsvState.value[2])
                        },
                        onDragEnd = {
                            val committedColor = Color.hsv(hsvState.value[0], hsvState.value[1], hsvState.value[2])
                            onCommitRecentColor(committedColor)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val h = (offset.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                        val newColor = Color.hsv(h, hsvState.value[1], hsvState.value[2])
                        updateColor(h, hsvState.value[1], hsvState.value[2])
                        onCommitRecentColor(newColor)
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

        // RECENT COLORS row below Hue slider
        if (recentColors.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "RECENT COLORS",
                    fontSize = 11.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    recentColors.take(8).forEach { color ->
                        val isSelected = color == activeColor
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    val array = FloatArray(3)
                                    android.graphics.Color.colorToHSV(color.toArgb(), array)
                                    hsvState.value = array
                                    onColorSelect(color)
                                    onCommitRecentColor(color)
                                }
                        )
                    }
                }
            }
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
        targetValue = if (isActive) with(density) { -16.dp.toPx() } else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "toolOffset"
    )

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .width(84.dp)
            .fillMaxHeight()
            .clickable(
                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .requiredHeight(150.dp)
                .graphicsLayer {
                    translationY = offsetY
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            if (isActive) {
                // Drop shadow layers tracing the exact SVG shape
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Black),
                    modifier = Modifier
                        .fillMaxHeight()
                        .offset(x = 2.dp, y = 4.dp)
                        .alpha(0.18f),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Black),
                    modifier = Modifier
                        .fillMaxHeight()
                        .offset(x = 3.dp, y = 7.dp)
                        .alpha(0.12f),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Black),
                    modifier = Modifier
                        .fillMaxHeight()
                        .offset(x = 4.dp, y = 10.dp)
                        .alpha(0.07f),
                    contentScale = ContentScale.Fit
                )
            }

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

@Composable
fun FloatingDrawTools(
    isVisible: Boolean,
    activeToolType: String,
    onToolChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .widthIn(max = 480.dp)
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
