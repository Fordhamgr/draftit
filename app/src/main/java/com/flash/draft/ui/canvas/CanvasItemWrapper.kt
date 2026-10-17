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
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
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
    isFullyTappable: Boolean = true,
    isChildDragging: Boolean = false,
    isCanvasLocked: Boolean = false,
    isConnectionTarget: Boolean = false, onConnectRequest: () -> Unit = {},
    hasDefaultBackground: Boolean = true, backgroundAlpha: Float = 0.65f,
    hyperlinkTarget: String? = null, onLinkUpdate: (String?) -> Unit = {},
    onSelect: () -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onDelete: () -> Unit, onSaveRequest: () -> Unit,
    onDuplicate: (() -> Unit)? = null,
    onSelectWithOffset: ((Offset) -> Unit)? = null,
    menuContentPadding: Dp = 16.dp,
    renderSelectionChrome: Boolean = false,
    customMenuContent: (@Composable () -> Unit)? = null, content: @Composable (isLocked: Boolean) -> Unit
) {
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnSelectWithOffset by rememberUpdatedState(onSelectWithOffset)
    val currentOnPositionDelta by rememberUpdatedState(onPositionDelta)
    val currentOnSizeDelta by rememberUpdatedState(onSizeDelta)
    val currentOnSaveRequest by rememberUpdatedState(onSaveRequest)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnDuplicate by rememberUpdatedState(onDuplicate)
    val currentOnConnectRequest by rememberUpdatedState(onConnectRequest)
    val currentOnLinkUpdate by rememberUpdatedState(onLinkUpdate)
    val currentOnLockedChange by rememberUpdatedState(onLockedChange)

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

    // ── Bubble animation for toolbars ───────────────────────────────────
    // Track previous selection state to trigger the animation only on transition
    var wasSelected by remember { mutableStateOf(false) }
    var bubbleAnimTrigger by remember { mutableStateOf(false) }

    LaunchedEffect(isSelected) {
        if (isSelected && !wasSelected) {
            // Just became selected — trigger bubble grow
            bubbleAnimTrigger = false
            // Small delay to reset the animation state before growing
            delay(16)
            bubbleAnimTrigger = true
        }
        wasSelected = isSelected
    }

    val bubbleScale by animateFloatAsState(
        targetValue = if (isSelected && bubbleAnimTrigger) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bubble_scale"
    )

    val bubbleAlpha by animateFloatAsState(
        targetValue = if (isSelected && bubbleAnimTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "bubble_alpha"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale; transformOrigin = TransformOrigin(0f, 0f) }
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
<<<<<<< HEAD
                .then(
                    if (isFullyTappable) {
                        Modifier.pointerInput(isLocked) {
                            detectTapGestures(
                                onTap = { localOffset ->
                                    if (!isMultiTouch && !isZooming && !isCanvasLocked) {
                                        if (currentOnSelectWithOffset != null) {
                                            currentOnSelectWithOffset?.invoke(localOffset)
                                        } else {
                                            currentOnSelect()
                                        }
                                    }
                                }
                            )
                        }
                    } else Modifier
                )
                .then(
                    if (isFullyDraggable) {
                        Modifier.pointerInput(isLocked) {
                            detectDragGestures(
                                onDragStart = { if (!isMultiTouch && !isZooming && !isLocked && !isCanvasLocked) { isDragging = true; currentOnSelect(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) } },
                                onDragEnd = { isDragging = false; currentOnSaveRequest(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                onDragCancel = { isDragging = false; currentOnSaveRequest(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                onDrag = { change, dragAmount ->
                                    if (!isLocked && !isCanvasLocked && !isMultiTouch && !isZooming) {
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
                                        val currentFrost = LocalLiquidFrost.current
                        // This turns the card into a glass lens!
                        Modifier.liquid(LocalLiquidState.current!!) {
                            refraction = currentRefraction
                            curve = currentCurve
                                            if (currentFrost > 0f) frost = (currentFrost * 24f).dp
                            shape = RoundedCornerShape(12.dp)
                        }
                    } else Modifier
                )
                // -----------------------------------

                .then(if (hasDefaultBackground && !isPerformance && !isDark) Modifier.shadow(animatedShadow, RoundedCornerShape(12.dp)) else Modifier)

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

        if (isSelected && renderSelectionChrome) {
            // All toolbars and overlays live INSIDE this explicitly-sized Box.
            // Alignment against requiredSize is pixel-accurate at ANY item dimension.
            Box(modifier = Modifier.requiredSize(width.dp, height.dp)) {
                // Selection border
                val bgStyle = com.flash.draft.ui.canvas.item.LocalBackgroundStyle.current
                val isBgDark = bgStyle == com.flash.draft.model.BackgroundStyle.Greenprint || bgStyle == com.flash.draft.model.BackgroundStyle.Blueprint || bgStyle == com.flash.draft.model.BackgroundStyle.BlackPaper || ((bgStyle == com.flash.draft.model.BackgroundStyle.Solid || bgStyle == com.flash.draft.model.BackgroundStyle.GraphPaper) && isDark)
                val outlineColor = if (isBgDark) Color.White.copy(alpha=0.8f) else Color(0xFF2196F3)
                Box(modifier = Modifier.fillMaxSize().border(2.dp, outlineColor, RoundedCornerShape(12.dp)))

                // Resize handle (bottom-right)
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
                                        if (!isLocked && !isCanvasLocked && !isMultiTouch && !isZooming) {
                                            change.consume()
                                            currentOnSizeDelta((dragAmount / density) / scale)
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) { detectTapGestures(onTap = { if (!isCanvasLocked) currentOnSelect() }) },
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

                // ── TOP TOOLBAR (custom content: fonts, size, colors) ────────
                // Anchored to TopCenter of the requiredSize parent — always tracks.
                if (customMenuContent != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-16).dp)
                            .wrapContentSize(unbounded = true)
                    ) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = -size.height
                                    scaleX = bubbleScale
                                    scaleY = bubbleScale
                                    alpha = bubbleAlpha
                                    transformOrigin = TransformOrigin(0.5f, 1f)
                                }
                        ) {
                            // Optional 8dp pointer centered at bottom edge behind the panel
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 4.dp)
                                    .size(10.dp)
                                    .rotate(45f)
                                    .background(if (isDark) Color(0xEB1A1A1A) else Color(0xEBFFFFFF), RoundedCornerShape(2.dp))
                                    .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x66FFFFFF), RoundedCornerShape(2.dp))
                            )

                            Row(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .then(if (!isPerformance && !isDark) Modifier.shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.2f)) else Modifier)
                                    .then(
                                        if (LocalLiquidState.current != null && !isPerformance) {
                                            val currentRefraction = LocalLiquidRefraction.current
                                            val currentCurve = LocalLiquidCurve.current
                                        val currentFrost = LocalLiquidFrost.current
                                            Modifier.liquid(LocalLiquidState.current!!) {
                                                refraction = currentRefraction
                                                curve = currentCurve
                                            if (currentFrost > 0f) frost = (currentFrost * 24f).dp
                                                shape = RoundedCornerShape(24.dp)
                                            }
                                        } else Modifier
                                    )
                                    .background(if (isDark) Color(0xEB1A1A1A) else Color(0xEBFFFFFF), RoundedCornerShape(24.dp))
                                    .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x66FFFFFF), RoundedCornerShape(24.dp))
                                    .padding(menuContentPadding),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                customMenuContent()
                            }
                        }
                    }
                }

                // ── RIGHT TOOLBAR (utility actions) ──────────────────────────
                // Anchored to TopEnd of the requiredSize parent — always tracks
                // the right edge, just like the resize handle tracks BottomEnd.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 16.dp)
                        .wrapContentSize(unbounded = true)
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = size.width
                                scaleX = bubbleScale
                                scaleY = bubbleScale
                                alpha = bubbleAlpha
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .wrapContentSize()
                                .then(if (!isPerformance && !isDark) Modifier.shadow(24.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.2f)) else Modifier)
                                .then(
                                    if (LocalLiquidState.current != null && !isPerformance) {
                                        val currentRefraction = LocalLiquidRefraction.current
                                        val currentCurve = LocalLiquidCurve.current
                                        val currentFrost = LocalLiquidFrost.current
                                        Modifier.liquid(LocalLiquidState.current!!) {
                                            refraction = currentRefraction
                                            curve = currentCurve
                                            if (currentFrost > 0f) frost = (currentFrost * 24f).dp
                                            shape = RoundedCornerShape(20.dp)
                                        }
                                    } else Modifier
                                )
                                .background(if (isDark) Color(0xEB1A1A1A) else Color(0xEBFFFFFF), RoundedCornerShape(20.dp))
                                .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x66FFFFFF), RoundedCornerShape(20.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.CallMade,
                                contentDescription = "Connect",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(22.dp).clickable { currentOnConnectRequest() }
                            )

                            val hasHyperlink = hyperlinkTarget != null
                            Icon(
                                Icons.Default.Link,
                                contentDescription = "Link",
                                tint = if (hasHyperlink) Color(0xFF4CAF50) else Color.Gray,
                                modifier = Modifier.size(22.dp).clickable { isEditingLink = !isEditingLink }
                            )

                            val isTeleportable = hasHyperlink && (
                                (hyperlinkTarget!!.startsWith("scene:") && hyperlinkTarget.length > "scene:".length) ||
                                (hyperlinkTarget.startsWith("board:") && hyperlinkTarget.length > "board:".length)
                            )
                            if (isTeleportable) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Teleport",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(22.dp).clickable { currentOnLinkUpdate("TELEPORT") }
                                )
                            }

                            Box(modifier = Modifier.width(24.dp).height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))

                            Icon(
                                if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock",
                                tint = if (isLocked) Color.Red else Color.Gray,
                                modifier = Modifier.size(22.dp).clickable { currentOnLockedChange(!isLocked) }
                            )

                            if (currentOnDuplicate != null) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(22.dp).clickable { currentOnDuplicate?.invoke() }
                                )
                            }

                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red,
                                modifier = Modifier.size(22.dp).clickable { currentOnDelete() }
                            )
                        }

                        // Link Editor Dialog
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isEditingLink,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .offset(y = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer { translationY = size.height }
                                    .then(if (!isPerformance && !isDark) Modifier.shadow(16.dp, RoundedCornerShape(12.dp)) else Modifier)
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
}



