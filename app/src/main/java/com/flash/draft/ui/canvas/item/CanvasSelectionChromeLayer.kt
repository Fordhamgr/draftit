package com.flash.draft.ui.canvas.item

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.model.BackgroundStyle
import com.flash.draft.model.CanvasElement
import com.flash.draft.ui.canvas.*
import io.github.fletchmckee.liquid.liquid
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun CanvasSelectionChromeLayer(
    selectedElement: CanvasElement,
    scale: Float,
    isCanvasLocked: Boolean,
    context: Context,
    onUpdateElement: (CanvasElement) -> Unit,
    onDeleteElement: (String) -> Unit,
    onDuplicateElement: (CanvasElement) -> Unit,
    onStartConnection: (String) -> Unit,
    onSizeDelta: (Offset, String) -> Unit,
    onSaveRequest: () -> Unit,
    onSendToBack: (String) -> Unit,
    onBringToFront: (String) -> Unit,
    onNavigateToScene: (String) -> Unit,
    onNavigateToBoard: (String) -> Unit,
    onMakeSticker: ((CanvasElement.ImageCard) -> Unit)? = null
) {
    val pos = when (selectedElement) {
        is CanvasElement.StickyNote -> selectedElement.position
        is CanvasElement.ImageCard -> selectedElement.position
        is CanvasElement.PdfCard -> selectedElement.position
        is CanvasElement.CodeBlock -> selectedElement.position
        is CanvasElement.AudioNote -> selectedElement.position
        is CanvasElement.ShapeNode -> selectedElement.position
        is CanvasElement.LabelNode -> selectedElement.position
        is CanvasElement.FrameNode -> selectedElement.position
        is CanvasElement.TaskListNode -> selectedElement.position
        else -> return
    }
    val dim = getElementDimensions(selectedElement)
    val width = dim.width
    val height = dim.height

    val isLocked = when (selectedElement) {
        is CanvasElement.StickyNote -> selectedElement.isLocked
        is CanvasElement.ImageCard -> selectedElement.isLocked
        is CanvasElement.PdfCard -> selectedElement.isLocked
        is CanvasElement.CodeBlock -> selectedElement.isLocked
        is CanvasElement.AudioNote -> selectedElement.isLocked
        is CanvasElement.ShapeNode -> selectedElement.isLocked
        is CanvasElement.LabelNode -> selectedElement.isLocked
        is CanvasElement.FrameNode -> selectedElement.isLocked
        is CanvasElement.TaskListNode -> selectedElement.isLocked
        else -> false
    } || isCanvasLocked

    val hyperlinkTarget = selectedElement.hyperlinkTarget

    val isDark = LocalDarkTheme.current
    val isPerformance = LocalPerformanceMode.current
    val density = LocalDensity.current.density

    var isEditingLink by remember { mutableStateOf(false) }
    var linkTargetInput by remember(hyperlinkTarget) { mutableStateOf(hyperlinkTarget ?: "") }

    var bubbleAnimTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(selectedElement.id) {
        bubbleAnimTrigger = false
        delay(16)
        bubbleAnimTrigger = true
    }

    val bubbleScale by animateFloatAsState(
        targetValue = if (bubbleAnimTrigger) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bubble_scale"
    )

    val bubbleAlpha by animateFloatAsState(
        targetValue = if (bubbleAnimTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "bubble_alpha"
    )

    val menuContentPadding: Dp = when (selectedElement) {
        is CanvasElement.StickyNote -> 8.dp
        is CanvasElement.LabelNode -> 14.dp
        else -> 16.dp
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
    ) {
        Box(modifier = Modifier.requiredSize(width.dp, height.dp)) {
            // ── 1. SELECTION BORDER ──────────────────────────────────────────
            val bgStyle = LocalBackgroundStyle.current
            val isBgDark = bgStyle == BackgroundStyle.Greenprint ||
                    bgStyle == BackgroundStyle.Blueprint ||
                    bgStyle == BackgroundStyle.BlackPaper ||
                    ((bgStyle == BackgroundStyle.Solid || bgStyle == BackgroundStyle.GraphPaper) && isDark)
            val outlineColor = if (isBgDark) Color.White.copy(alpha = 0.8f) else Color(0xFF2196F3)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, outlineColor, RoundedCornerShape(12.dp))
            )

            // ── 2. RESIZE HANDLE (Bottom-Right) ─────────────────────────────
            if (!isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 32.dp, y = 32.dp)
                        .size(64.dp)
                        .pointerInput(selectedElement.id, isLocked) {
                            detectDragGestures(
                                onDragEnd = { onSaveRequest() },
                                onDrag = { change, dragAmount ->
                                    if (!isLocked && !isCanvasLocked) {
                                        change.consume()
                                        onSizeDelta((dragAmount / density) / scale, selectedElement.id)
                                    }
                                }
                            )
                        },
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

            // ── 3. TOP TOOLBAR (Customizable toolstack per element type) ────
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
                    // 8dp pointer centered at bottom edge behind panel
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
                        when (selectedElement) {
                            is CanvasElement.StickyNote -> StickyNoteToolStack(
                                note = selectedElement,
                                isSelected = true,
                                onUpdate = { onUpdateElement(it) },
                                onSaveRequest = onSaveRequest
                            )
                            is CanvasElement.LabelNode -> TextBoxToolStack(
                                labelNode = selectedElement,
                                onUpdate = { onUpdateElement(it) },
                                onSaveRequest = onSaveRequest
                            )
                            is CanvasElement.ShapeNode -> ShapeToolStack(
                                shapeNode = selectedElement,
                                onUpdate = { onUpdateElement(it) },
                                onSaveRequest = onSaveRequest,
                                onSendToBack = { onSendToBack(selectedElement.id) },
                                onBringToFront = { onBringToFront(selectedElement.id) }
                            )
                            is CanvasElement.CodeBlock -> CodeSnippetToolStack(
                                codeBlock = selectedElement
                            )
                            is CanvasElement.ImageCard -> ImageToolStack(
                                imageCard = selectedElement,
                                onUpdate = { onUpdateElement(it) },
                                onSaveRequest = onSaveRequest,
                                onDuplicate = { onDuplicateElement(selectedElement) },
                                onMakeSticker = { onMakeSticker?.invoke(selectedElement) }
                            )
                            is CanvasElement.TaskListNode -> TaskListToolStack(
                                taskNode = selectedElement,
                                onUpdate = { onUpdateElement(it) },
                                onSaveRequest = onSaveRequest
                            )
                            is CanvasElement.PdfCard -> PdfToolStack(
                                pdfCard = selectedElement,
                                context = context
                            )
                            is CanvasElement.AudioNote -> AudioToolStack(
                                audioNote = selectedElement,
                                onUpdate = { onUpdateElement(it) },
                                onSaveRequest = onSaveRequest
                            )
                            else -> {}
                        }
                    }
                }
            }

            // ── 4. RIGHT TOOLBAR (Utility actions) ──────────────────────────
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
                        // Connect action
                        Icon(
                            Icons.AutoMirrored.Filled.CallMade,
                            contentDescription = "Connect",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(22.dp).clickable { onStartConnection(selectedElement.id) }
                        )

                        // Link action
                        val hasHyperlink = hyperlinkTarget != null
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "Link",
                            tint = if (hasHyperlink) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.size(22.dp).clickable { isEditingLink = !isEditingLink }
                        )

                        val isTeleportable = hasHyperlink && (
                            (hyperlinkTarget.startsWith("scene:") && hyperlinkTarget.length > "scene:".length) ||
                            (hyperlinkTarget.startsWith("board:") && hyperlinkTarget.length > "board:".length)
                        )
                        if (isTeleportable) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Teleport",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(22.dp).clickable {
                                    val target = hyperlinkTarget
                                    if (target.startsWith("scene:")) onNavigateToScene(target.removePrefix("scene:"))
                                    else if (target.startsWith("board:")) onNavigateToBoard(target.removePrefix("board:"))
                                }
                            )
                        }

                        Box(modifier = Modifier.width(24.dp).height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))

                        // Lock action
                        val updatedLock = !isLocked
                        Icon(
                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = if (isLocked) Color.Red else Color.Gray,
                            modifier = Modifier.size(22.dp).clickable {
                                val updated = when (selectedElement) {
                                    is CanvasElement.StickyNote -> selectedElement.copy(isLocked = updatedLock)
                                    is CanvasElement.ImageCard -> selectedElement.copy(isLocked = updatedLock)
                                    is CanvasElement.PdfCard -> selectedElement.copy(isLocked = updatedLock)
                                    is CanvasElement.CodeBlock -> selectedElement.copy(isLocked = updatedLock)
                                    is CanvasElement.AudioNote -> selectedElement.copy(isLocked = updatedLock)
                                    is CanvasElement.ShapeNode -> selectedElement.copy(isLocked = updatedLock)
                                    is CanvasElement.LabelNode -> selectedElement.copy(isLocked = updatedLock)
                                    is CanvasElement.FrameNode -> selectedElement.copy(isLocked = updatedLock)
                                    is CanvasElement.TaskListNode -> selectedElement.copy(isLocked = updatedLock)
                                    else -> selectedElement
                                }
                                onUpdateElement(updated)
                                onSaveRequest()
                            }
                        )

                        // Duplicate action
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = Color.Gray,
                            modifier = Modifier.size(22.dp).clickable { onDuplicateElement(selectedElement) }
                        )

                        // Delete action
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red,
                            modifier = Modifier.size(22.dp).clickable { onDeleteElement(selectedElement.id) }
                        )
                    }

                    // Link Editor Dialog
                    AnimatedVisibility(
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
                                    val newLink = linkTargetInput.takeIf { it.isNotBlank() }
                                    val updated = when (selectedElement) {
                                        is CanvasElement.StickyNote -> selectedElement.copy(hyperlinkTarget = newLink)
                                        is CanvasElement.ImageCard -> selectedElement.copy(hyperlinkTarget = newLink)
                                        is CanvasElement.PdfCard -> selectedElement.copy(hyperlinkTarget = newLink)
                                        is CanvasElement.CodeBlock -> selectedElement.copy(hyperlinkTarget = newLink)
                                        is CanvasElement.AudioNote -> selectedElement.copy(hyperlinkTarget = newLink)
                                        is CanvasElement.ShapeNode -> selectedElement.copy(hyperlinkTarget = newLink)
                                        is CanvasElement.LabelNode -> selectedElement.copy(hyperlinkTarget = newLink)
                                        is CanvasElement.FrameNode -> selectedElement.copy(hyperlinkTarget = newLink)
                                        is CanvasElement.TaskListNode -> selectedElement.copy(hyperlinkTarget = newLink)
                                        else -> selectedElement
                                    }
                                    onUpdateElement(updated)
                                    onSaveRequest()
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
