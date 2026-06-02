package com.flash.draft.ui.canvas

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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

import com.flash.draft.ui.canvas.LocalLiquidRefraction
import com.flash.draft.ui.canvas.LocalLiquidCurve

// FIX: We need the 'liquid' lens modifier here, NOT liquefiable!
import io.github.fletchmckee.liquid.liquid
import kotlin.math.roundToInt

@Composable
fun CanvasItemWrapper(
    position: Offset, width: Float, height: Float, scale: Float, canvasOffset: Offset,
    isSelected: Boolean, isConnecting: Boolean, isFullyDraggable: Boolean = true,
    hasDefaultBackground: Boolean = true,
    onSelect: () -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onDelete: () -> Unit, onSaveRequest: () -> Unit,
    customMenuContent: @Composable () -> Unit = {}, content: @Composable () -> Unit
) {
    val screenPos = ViewportMath.worldToScreen(position, canvasOffset, scale)
    var isMultiTouch by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }

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
                                        onPositionDelta(dragAmount / scale)
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

                // IMPORTANT: The alpha is 0.65f here so you can actually see the liquid distortion through the card's background color!
                .then(if (hasDefaultBackground) Modifier.background(if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.65f), RoundedCornerShape(12.dp)) else Modifier)

                .then(if (hasDefaultBackground) Modifier.clip(RoundedCornerShape(12.dp)) else Modifier)
                .then(if (isConnecting) Modifier.border(4.dp, Color.Blue, RoundedCornerShape(12.dp)) else Modifier)

        ) {
            content()
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

            Box(modifier = Modifier.align(Alignment.TopCenter).offset(y = (-16).dp)) {
                Box(modifier = Modifier.graphicsLayer { translationY = -size.height }) {
                    Column(
                        modifier = Modifier
                            .wrapContentWidth()
                            .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.15f))
                            .background(if (isDark) Color(0xCC1A1A1A) else Color(0xD9FFFFFF), RoundedCornerShape(24.dp))
                            .border(1.dp, if (isDark) Color(0x22FFFFFF) else Color(0x44FFFFFF), RoundedCornerShape(24.dp))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        customMenuContent()

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Gray.copy(alpha = 0.2f)))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = "Lock", tint = if (isLocked) Color.Red else Color.Gray, modifier = Modifier.size(24.dp).clickable { isLocked = !isLocked })
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(24.dp).clickable { onDelete() })
                        }
                    }
                }
            }
        }
    }
}