package com.flash.draft.ui.canvas

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import com.flash.draft.ui.canvas.item.LocalPerformanceMode

import com.flash.draft.utils.ViewportMath
import io.github.fletchmckee.liquid.liquid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CanvasItemWrapper(
    position: Offset, width: Float, height: Float, scale: Float, canvasOffset: Offset,
    isSelected: Boolean, isConnecting: Boolean, isLocked: Boolean = false, onLockedChange: (Boolean) -> Unit = {}, isFullyDraggable: Boolean = true,
    isChildDragging: Boolean = false,
    isConnectionTarget: Boolean = false, onConnectRequest: () -> Unit = {},
    hasDefaultBackground: Boolean = true, backgroundAlpha: Float = 0.65f,
    hyperlinkTarget: String? = null, onLinkUpdate: (String?) -> Unit = {},
    onSelect: () -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onDelete: () -> Unit, onSaveRequest: () -> Unit,
    onDuplicate: (() -> Unit)? = null,
    customMenuContent: (@Composable () -> Unit)? = null, content: @Composable (isLocked: Boolean) -> Unit
) {
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnPositionDelta by rememberUpdatedState(onPositionDelta)
    val currentOnSizeDelta by rememberUpdatedState(onSizeDelta)
    val currentOnSaveRequest by rememberUpdatedState(onSaveRequest)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnDuplicate by rememberUpdatedState(onDuplicate)
    val currentOnConnectRequest by rememberUpdatedState(onConnectRequest)
    val currentOnLinkUpdate by rememberUpdatedState(onLinkUpdate)
    val currentOnLockedChange by rememberUpdatedState(onLockedChange)

    val screenPos = ViewportMath.worldToScreen(position, canvasOffset, scale)
    var isMultiTouch by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    // Zoom cooldown: stays true briefly after pinch ends to prevent accidental taps/drags
    var isZooming by remember { mutableStateOf(false) }
    var zoomCooldownJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val animatedShadow by animateDpAsState(targetValue = if (isDragging || isChildDragging) 24.dp else 4.dp, animationSpec = tween(150), label = "shadow")
    val animatedScale by animateFloatAsState(targetValue = if (isDragging || isChildDragging) 1.02f else 1f, animationSpec = tween(150), label = "scale")
    val isDark = LocalDarkTheme.current
    val isPerformance = LocalPerformanceMode.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current.density
    
    var isEditingLink by remember { mutableStateOf(false) }
    var linkTargetInput by remember(hyperlinkTarget) { mutableStateOf(hyperlinkTarget ?: "") }
    


    Box(
        modifier = Modifier
            .offset { IntOffset(screenPos.x.roundToInt(), screenPos.y.roundToInt()) }
            .graphicsLayer { scaleX = scale * animatedScale; scaleY = scale * animatedScale; transformOrigin = TransformOrigin(0f, 0f) }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.size > 1) {
                            isMultiTouch = true
                            isZooming = true
                            zoomCooldownJob?.cancel()
                        }
                        if (event.changes.none { it.pressed }) {
                            if (isMultiTouch) {
                                isMultiTouch = false
                                // Keep isZooming true for a short cooldown so that the
                                // finger-lift at the end of a pinch doesn't trigger a
                                // tap or drag on the card underneath.
                                zoomCooldownJob?.cancel()
                                zoomCooldownJob = coroutineScope.launch {
                                    delay(200L)
                                    isZooming = false
                                }
                            }
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .requiredSize(width.dp, height.dp)
                .background(Color.Transparent)
                .pointerInput(isLocked) { detectTapGestures(onTap = { if (!isLocked && !isMultiTouch && !isZooming) currentOnSelect() }) }
                .then(
                    if (isFullyDraggable) {
                        Modifier.pointerInput(isLocked) {
                            detectDragGestures(
                                onDragStart = { if (!isMultiTouch && !isZooming && !isLocked) { isDragging = true; currentOnSelect(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) } },
                                onDragEnd = { isDragging = false; currentOnSaveRequest(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                onDragCancel = { isDragging = false; currentOnSaveRequest(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                onDrag = { change, dragAmount ->
                                    if (!isLocked && !isMultiTouch && !isZooming) {
                                        change.consume()
                                        currentOnPositionDelta(dragAmount)
                                    }
                                }
                            )
                        }
                    } else Modifier
                )

                // ---- THE CORRECT LENS MODIFIER ----
                .then(
                    if (LocalLiquidState.current != null && !isPerformance) {
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

                .then(if (hasDefaultBackground && !isPerformance) Modifier.shadow(animatedShadow, RoundedCornerShape(12.dp)) else Modifier)

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
                                    onDragEnd = { currentOnSaveRequest() },
                                    onDrag = { change, dragAmount ->
                                        if (!isLocked && !isMultiTouch && !isZooming) {
                                            change.consume()
                                            currentOnSizeDelta((dragAmount / density) / scale)
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) { detectTapGestures(onTap = { currentOnSelect() }) },
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
                        modifier = Modifier
                            .wrapContentSize()
                            .then(if (!isPerformance) Modifier.shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.2f)) else Modifier)
                            .then(
                                if (LocalLiquidState.current != null && !isPerformance) {
                                    val currentRefraction = LocalLiquidRefraction.current
                                    val currentCurve = LocalLiquidCurve.current
                                    Modifier.liquid(LocalLiquidState.current!!) {
                                        refraction = currentRefraction
                                        curve = currentCurve
                                        shape = RoundedCornerShape(24.dp)
                                    }
                                } else Modifier
                            )
                            .background(if (isDark) Color(0xEB1A1A1A) else Color(0xEBFFFFFF), RoundedCornerShape(24.dp))
                            .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x66FFFFFF), RoundedCornerShape(24.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (customMenuContent != null) {
                            customMenuContent()
                            Box(modifier = Modifier.width(1.dp).height(48.dp).background(Color.Gray.copy(alpha = 0.3f)))
                        }

                        // Utility Items Grid (2x3 or flow)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = "Connect", tint = Color(0xFF2196F3), modifier = Modifier.size(22.dp).clickable { currentOnConnectRequest() })
                                
                                val hasHyperlink = hyperlinkTarget != null
                                Icon(Icons.Default.Link, contentDescription = "Link", tint = if (hasHyperlink) Color(0xFF4CAF50) else Color.Gray, modifier = Modifier.size(22.dp).clickable { isEditingLink = !isEditingLink })
                                
                                val isTeleportable = hasHyperlink && (
                                    (hyperlinkTarget!!.startsWith("scene:") && hyperlinkTarget.length > "scene:".length) || 
                                    (hyperlinkTarget.startsWith("board:") && hyperlinkTarget.length > "board:".length)
                                )
                                if (isTeleportable) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Teleport", tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp).clickable { currentOnLinkUpdate("TELEPORT") })
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(if (isLocked) androidx.compose.material.icons.Icons.Default.Lock else androidx.compose.material.icons.Icons.Default.LockOpen, contentDescription = "Lock", tint = if (isLocked) Color.Red else Color.Gray, modifier = Modifier.size(22.dp).clickable { currentOnLockedChange(!isLocked) })
                                if (currentOnDuplicate != null) {
                                    Icon(androidx.compose.material.icons.Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.Gray, modifier = Modifier.size(22.dp).clickable { currentOnDuplicate?.invoke() })
                                }
                                Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(22.dp).clickable { currentOnDelete() })
                            }
                        }
                    }
                        
                        // 3. Link Editor Dialog (Inline next to pill)
                        androidx.compose.animation.AnimatedVisibility(visible = isEditingLink) {
                            Box(
                                modifier = Modifier
                                    .then(if (!isPerformance) Modifier.shadow(16.dp, RoundedCornerShape(12.dp)) else Modifier)
                                    .background(if (isDark) Color(0xCC1A1A1A) else Color(0xCCFFFFFF), RoundedCornerShape(12.dp))
                                    .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x66FFFFFF), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BasicTextField(
                                        value = linkTargetInput,
                                        onValueChange = { linkTargetInput = it },
                                        textStyle = TextStyle(color = if (isDark) Color.White else Color.Black, fontSize = 14.sp),
                                        modifier = Modifier.width(120.dp).background(if (isDark) Color(0xFF333333) else Color(0xFFEEEEEE), RoundedCornerShape(4.dp)).padding(4.dp),
                                        decorationBox = { innerTextField ->
                                            if (linkTargetInput.isEmpty()) Text("scene:X or board:Y", color = Color.Gray, fontSize = 12.sp)
                                            innerTextField()
                                        }
                                    )
                                    Icon(Icons.Default.Check, "Save Link", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp).clickable {
                                        currentOnLinkUpdate(linkTargetInput.takeIf { it.isNotBlank() })
                                        isEditingLink = false
                                    })
                                }
                            }
                        }
                    }
                }
            }
    }
}