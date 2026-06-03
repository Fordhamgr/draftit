package com.flash.draft.ui.canvas

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
    import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import com.flash.draft.utils.ViewportMath
import io.github.fletchmckee.liquid.liquid
import kotlin.math.roundToInt

@Composable
fun CanvasItemWrapper(
    position: Offset, width: Float, height: Float, scale: Float, canvasOffset: Offset,
    isSelected: Boolean, isConnecting: Boolean, isLocked: Boolean = false, onLockedChange: (Boolean) -> Unit = {}, isFullyDraggable: Boolean = true,
    isConnectionTarget: Boolean = false, onConnectRequest: () -> Unit = {},
    hasDefaultBackground: Boolean = true, backgroundAlpha: Float = 0.65f,
    onSelect: () -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onDelete: () -> Unit, onSaveRequest: () -> Unit,
    customMenuContent: @Composable () -> Unit = {}, content: @Composable (isLocked: Boolean) -> Unit
) {
    val screenPos = ViewportMath.worldToScreen(position, canvasOffset, scale)
    var isMultiTouch by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedShadow by animateDpAsState(targetValue = if (isDragging) 24.dp else 4.dp, animationSpec = tween(150), label = "shadow")
    val animatedScale by animateFloatAsState(targetValue = if (isDragging) 1.02f else 1f, animationSpec = tween(150), label = "scale")
    val isDark = LocalDarkTheme.current
    


    Box(
        modifier = Modifier
            .offset { IntOffset(screenPos.x.roundToInt(), screenPos.y.roundToInt()) }
            .graphicsLayer { scaleX = scale * animatedScale; scaleY = scale * animatedScale; transformOrigin = TransformOrigin(0f, 0f) }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.size > 1) isMultiTouch = true
                        if (event.changes.none { it.pressed }) isMultiTouch = false
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .requiredSize(width.dp, height.dp)
                .background(Color.Transparent)
                .pointerInput(Unit) { detectTapGestures(onTap = { if (!isMultiTouch) onSelect() }) }
                .then(
                    if (isFullyDraggable) {
                        Modifier.pointerInput(isLocked) {
                            detectDragGestures(
                                onDragStart = { if (!isMultiTouch && !isLocked) { isDragging = true; onSelect() } },
                                onDragEnd = { isDragging = false; onSaveRequest() },
                                onDragCancel = { isDragging = false; onSaveRequest() },
                                onDrag = { change, dragAmount ->
                                    if (!isLocked && !isMultiTouch) {
                                        change.consume()
                                        onPositionDelta(dragAmount)
                                    }
                                }
                            )
                        }
                    } else Modifier
                )

                // ---- THE CORRECT LENS MODIFIER ----
                .then(
                    if (LocalLiquidState.current != null) {
                        val currentRefraction = LocalLiquidRefraction.current
                        val currentCurve = LocalLiquidCurve.current
                        // This turns the card into a glass lens!
                        Modifier.liquid(LocalLiquidState.current!!) {
                            refraction = currentRefraction
                            curve = currentCurve
                            shape = RoundedCornerShape(12.dp)
                        }
                    } else Modifier
                )
                // -----------------------------------

                .then(if (hasDefaultBackground) Modifier.shadow(animatedShadow, RoundedCornerShape(12.dp)) else Modifier)

                // IMPORTANT: The alpha is backgroundAlpha here so you can optionally see the liquid distortion through the card's background color!
                .then(if (hasDefaultBackground) Modifier.background(if (isDark) Color(0xFF1E1E1E).copy(alpha = backgroundAlpha) else Color.White.copy(alpha = backgroundAlpha), RoundedCornerShape(12.dp)) else Modifier)

                .then(if (hasDefaultBackground) Modifier.clip(RoundedCornerShape(12.dp)) else Modifier)
                .then(if (isConnecting) Modifier.border(4.dp, Color.Blue, RoundedCornerShape(12.dp)) else Modifier)
                .then(
                    if (isConnectionTarget) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f, targetValue = 0.9f,
                            animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                            label = "pulseAlpha"
                        )
                        Modifier.border(4.dp, Color(0xFF00E676).copy(alpha = pulseAlpha), RoundedCornerShape(12.dp))
                    } else Modifier
                )

        ) {
            content(isLocked)
        }

        if (isSelected) {
            Box(modifier = Modifier.requiredSize(width.dp, height.dp)) {
                Box(modifier = Modifier.fillMaxSize().border(2.dp, Color(0xFF2196F3), RoundedCornerShape(12.dp)))

                if (!isLocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 32.dp, y = 32.dp)
                            .size(64.dp)
                            .pointerInput(isLocked) {
                                detectDragGestures(
                                    onDragEnd = { onSaveRequest() },
                                    onDrag = { change, dragAmount ->
                                        if (!isLocked && !isMultiTouch) {
                                            change.consume()
                                            onSizeDelta(dragAmount / scale)
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) { detectTapGestures(onTap = { onSelect() }) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(Color.DarkGray, CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.TopCenter).offset(y = (-16).dp).wrapContentWidth(unbounded = true)) {
                Box(modifier = Modifier.graphicsLayer { translationY = -size.height }) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Main Customization Panel
                        Box(
                            modifier = Modifier
                                .wrapContentSize()
                                .shadow(32.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.3f))
                                .then(
                                    if (LocalLiquidState.current != null) {
                                        val currentRefraction = LocalLiquidRefraction.current
                                        val currentCurve = LocalLiquidCurve.current
                                        Modifier.liquid(LocalLiquidState.current!!) {
                                            refraction = currentRefraction
                                            curve = currentCurve
                                            shape = RoundedCornerShape(24.dp)
                                        }
                                    } else Modifier
                                )
                                .background(if (isDark) Color(0xCC1A1A1A) else Color(0xCCFFFFFF), RoundedCornerShape(24.dp))
                                .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x66FFFFFF), RoundedCornerShape(24.dp))
                                .padding(16.dp)
                        ) {
                            customMenuContent()
                        }

                        // 2. Utility Pill
                        Column(
                            modifier = Modifier
                                .shadow(16.dp, androidx.compose.foundation.shape.RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                                .then(
                                    if (LocalLiquidState.current != null) {
                                        val currentRefraction = LocalLiquidRefraction.current
                                        val currentCurve = LocalLiquidCurve.current
                                        Modifier.liquid(LocalLiquidState.current!!) {
                                            refraction = currentRefraction
                                            curve = currentCurve
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                                        }
                                    } else Modifier
                                )
                                .background(if (isDark) Color(0xCC1A1A1A) else Color(0xCCFFFFFF), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                                .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x66FFFFFF), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                                .padding(horizontal = 8.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = "Connect", tint = Color(0xFF2196F3), modifier = Modifier.size(22.dp).clickable { onConnectRequest() })
                            Box(modifier = Modifier.width(16.dp).height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                            Icon(if (isLocked) androidx.compose.material.icons.Icons.Default.Lock else androidx.compose.material.icons.Icons.Default.LockOpen, contentDescription = "Lock", tint = if (isLocked) Color.Red else Color.Gray, modifier = Modifier.size(22.dp).clickable { onLockedChange(!isLocked) })
                            Box(modifier = Modifier.width(16.dp).height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                            Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(22.dp).clickable { onDelete() })
                        }
                    }
                }
            }
        }
    }
}