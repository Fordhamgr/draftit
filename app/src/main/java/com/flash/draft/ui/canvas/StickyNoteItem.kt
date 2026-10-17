package com.flash.draft.ui.canvas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.flash.draft.AppFonts
import com.flash.draft.model.CanvasElement
import com.flash.draft.ui.canvas.item.LocalDarkTheme

private enum class StickyMenuType {
    None, Font, Style
}

private fun getFontFamily(fontName: String): FontFamily = when (fontName) {
    "Serif" -> AppFonts.Serif
    "Mono" -> AppFonts.Mono
    "Cursive" -> AppFonts.Handwriting
    else -> AppFonts.Sans
}

@Composable
private fun StylePictogram(
    style: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 28f
        val strokeWidth = 1.7f * s
        val strokeColor = color
        val fillColor = color.copy(alpha = 0.14f)

        when (style) {
            "Classic" -> {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(4f * s, 4f * s),
                    size = Size(20f * s, 20f * s),
                    cornerRadius = CornerRadius(3.5f * s)
                )
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(4f * s, 4f * s),
                    size = Size(20f * s, 20f * s),
                    cornerRadius = CornerRadius(3.5f * s),
                    style = Stroke(width = strokeWidth)
                )
            }
            "Pinned" -> {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(4f * s, 6.5f * s),
                    size = Size(20f * s, 18.5f * s),
                    cornerRadius = CornerRadius(3.5f * s)
                )
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(4f * s, 6.5f * s),
                    size = Size(20f * s, 18.5f * s),
                    cornerRadius = CornerRadius(3.5f * s),
                    style = Stroke(width = strokeWidth)
                )
                drawCircle(
                    color = strokeColor,
                    radius = 3f * s,
                    center = Offset(14f * s, 6.5f * s)
                )
            }
            "Taped" -> {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(4f * s, 6.5f * s),
                    size = Size(20f * s, 18.5f * s),
                    cornerRadius = CornerRadius(3.5f * s)
                )
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(4f * s, 6.5f * s),
                    size = Size(20f * s, 18.5f * s),
                    cornerRadius = CornerRadius(3.5f * s),
                    style = Stroke(width = strokeWidth)
                )
                withTransform({
                    rotate(-6f, pivot = Offset(14f * s, 6.5f * s))
                }) {
                    drawRoundRect(
                        color = strokeColor.copy(alpha = 0.35f),
                        topLeft = Offset(9f * s, 3.5f * s),
                        size = Size(10f * s, 6f * s),
                        cornerRadius = CornerRadius(1f * s)
                    )
                }
            }
            "Folded" -> {
                val bodyPath = Path().apply {
                    moveTo(4f * s, 4f * s)
                    lineTo(24f * s, 4f * s)
                    lineTo(24f * s, 18f * s)
                    lineTo(18f * s, 24f * s)
                    lineTo(4f * s, 24f * s)
                    close()
                }
                drawPath(bodyPath, color = fillColor)
                drawPath(bodyPath, color = strokeColor, style = Stroke(width = strokeWidth))

                val foldPath = Path().apply {
                    moveTo(18f * s, 24f * s)
                    lineTo(18f * s, 18f * s)
                    lineTo(24f * s, 18f * s)
                    close()
                }
                drawPath(foldPath, color = strokeColor.copy(alpha = 0.35f))
                drawPath(foldPath, color = strokeColor, style = Stroke(width = strokeWidth))
            }
        }
    }
}

private class UpwardChipPopupPositionProvider(
    private val isStyleMenu: Boolean,
    private val spacingPx: Int,
    private val panelOffsetPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val targetX = if (isStyleMenu) {
            anchorBounds.right - popupContentSize.width
        } else {
            anchorBounds.left
        }
        val x = targetX.coerceIn(16, (windowSize.width - popupContentSize.width - 16).coerceAtLeast(16))
        val panelTop = anchorBounds.top - panelOffsetPx
        val y = (panelTop - popupContentSize.height - spacingPx).coerceAtLeast(16)
        return IntOffset(x, y)
    }
}

@Composable
internal fun StickyNoteToolStack(
    note: CanvasElement.StickyNote,
    isSelected: Boolean,
    onUpdate: (CanvasElement.StickyNote) -> Unit,
    onSaveRequest: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val ink = if (isDark) Color(0xFFF2F4F8) else Color(0xFF0E1325)
    val chipBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val activeChipBg = if (isDark) Color(0xFF2196F3).copy(alpha = 0.22f) else Color(0xFF2196F3).copy(alpha = 0.12f)
    val activeTint = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0)
    val menuSurfaceBg = if (isDark) Color(0xEB1A1A1A) else Color(0xEBFFFFFF)
    val menuBorder = if (isDark) Color(0x33FFFFFF) else Color(0x66FFFFFF)

    var openMenu by remember { mutableStateOf(StickyMenuType.None) }

    LaunchedEffect(isSelected) {
        if (!isSelected) {
            openMenu = StickyMenuType.None
        }
    }

    val density = LocalDensity.current
    val spacingPx = with(density) { 8.dp.roundToPx() }
    val panelOffsetPx = with(density) { 58.dp.roundToPx() }

    val currentFont = when (note.fontFamily) {
        "Serif" -> AppFonts.Serif
        "Mono" -> AppFonts.Mono
        "Cursive" -> AppFonts.Handwriting
        else -> AppFonts.Sans
    }

    Column(
        modifier = Modifier.width(240.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Row 1: Colors (6 swatches) ───────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colors = listOf(
                0xFFFFF9C4 to "Yellow",
                0xFFFFCDD2 to "Pink",
                0xFFBBDEFB to "Blue",
                0xFFC8E6C9 to "Green",
                0xFFE1BEE7 to "Purple",
                0xFFFFFFFF to "White"
            )
            colors.forEach { (colorVal, colorName) ->
                val isSelectedColor = note.color == Color(colorVal)
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onUpdate(note.copy(color = Color(colorVal)))
                            onSaveRequest()
                        }
                        .semantics {
                            role = Role.RadioButton
                            selected = isSelectedColor
                            contentDescription = colorName
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelectedColor) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(2.dp, Color(0xFF2196F3), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color(colorVal), CircleShape)
                                    .border(0.5.dp, ink.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF161A26),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color(colorVal), CircleShape)
                                .border(0.5.dp, ink.copy(alpha = 0.15f), CircleShape)
                        )
                    }
                }
            }
        }

        // ── Row 2: Font Chip, Size Stepper, Style Chip ───────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Font Chip (76dp)
            val fontChevronRot by animateFloatAsState(
                targetValue = if (openMenu == StickyMenuType.Font) 180f else 0f,
                animationSpec = tween(200),
                label = "font_chevron"
            )
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (openMenu == StickyMenuType.Font) activeChipBg else chipBg,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        openMenu = if (openMenu == StickyMenuType.Font) StickyMenuType.None else StickyMenuType.Font
                    }
                    .semantics {
                        role = Role.Button
                        contentDescription = "Font, ${note.fontFamily}"
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Aa",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = currentFont,
                        color = if (openMenu == StickyMenuType.Font) activeTint else ink
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(fontChevronRot),
                        tint = if (openMenu == StickyMenuType.Font) activeTint else ink
                    )
                }

                if (openMenu == StickyMenuType.Font) {
                    val fontPositionProvider = remember(spacingPx, panelOffsetPx) {
                        UpwardChipPopupPositionProvider(
                            isStyleMenu = false,
                            spacingPx = spacingPx,
                            panelOffsetPx = panelOffsetPx
                        )
                    }
                    Popup(
                        popupPositionProvider = fontPositionProvider,
                        onDismissRequest = { openMenu = StickyMenuType.None },
                        properties = PopupProperties(
                            focusable = true,
                            dismissOnClickOutside = true,
                            dismissOnBackPress = true
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .width(180.dp)
                                .shadow(24.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                                .background(menuSurfaceBg, RoundedCornerShape(22.dp))
                                .border(1.dp, menuBorder, RoundedCornerShape(22.dp))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf("Sans", "Serif", "Mono", "Cursive").forEach { fontName ->
                                val isSelectedFont = note.fontFamily == fontName || (note.fontFamily == "SansSerif" && fontName == "Sans")
                                val rowBg = if (isSelectedFont) activeChipBg else Color.Transparent
                                val rowColor = if (isSelectedFont) activeTint else ink

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(rowBg, RoundedCornerShape(16.dp))
                                        .clickable {
                                            onUpdate(note.copy(fontFamily = fontName))
                                            onSaveRequest()
                                            openMenu = StickyMenuType.None
                                        }
                                        .padding(horizontal = 12.dp)
                                        .semantics {
                                            role = Role.RadioButton
                                            selected = isSelectedFont
                                            contentDescription = "Font, $fontName"
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = fontName,
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelectedFont) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = getFontFamily(fontName),
                                        color = rowColor
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (isSelectedFont) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = activeTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Size Stepper (100dp)
            Row(
                modifier = Modifier
                    .width(100.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(chipBg, RoundedCornerShape(16.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val canDecrease = note.fontSize > 10f
                val canIncrease = note.fontSize < 48f

                Box(
                    modifier = Modifier
                        .size(32.dp, 44.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        .then(
                            if (canDecrease) {
                                Modifier.clickable {
                                    onUpdate(note.copy(fontSize = note.fontSize - 2f))
                                    onSaveRequest()
                                }
                            } else Modifier
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Smaller"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ink.copy(alpha = if (canDecrease) 1f else 0.4f)
                    )
                }

                Text(
                    text = "${note.fontSize.toInt()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink
                )

                Box(
                    modifier = Modifier
                        .size(32.dp, 44.dp)
                        .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                        .then(
                            if (canIncrease) {
                                Modifier.clickable {
                                    onUpdate(note.copy(fontSize = note.fontSize + 2f))
                                    onSaveRequest()
                                }
                            } else Modifier
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Bigger"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ink.copy(alpha = if (canIncrease) 1f else 0.4f)
                    )
                }
            }

            // Style Chip (Remaining width ~ 52dp)
            val styleChevronRot by animateFloatAsState(
                targetValue = if (openMenu == StickyMenuType.Style) 180f else 0f,
                animationSpec = tween(200),
                label = "style_chevron"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (openMenu == StickyMenuType.Style) activeChipBg else chipBg,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        openMenu = if (openMenu == StickyMenuType.Style) StickyMenuType.None else StickyMenuType.Style
                    }
                    .semantics {
                        role = Role.Button
                        contentDescription = "Note style, ${note.style}"
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StylePictogram(
                        style = note.style,
                        modifier = Modifier.size(22.dp),
                        color = if (openMenu == StickyMenuType.Style) activeTint else ink
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(styleChevronRot),
                        tint = if (openMenu == StickyMenuType.Style) activeTint else ink
                    )
                }

                if (openMenu == StickyMenuType.Style) {
                    val stylePositionProvider = remember(spacingPx, panelOffsetPx) {
                        UpwardChipPopupPositionProvider(
                            isStyleMenu = true,
                            spacingPx = spacingPx,
                            panelOffsetPx = panelOffsetPx
                        )
                    }
                    Popup(
                        popupPositionProvider = stylePositionProvider,
                        onDismissRequest = { openMenu = StickyMenuType.None },
                        properties = PopupProperties(
                            focusable = true,
                            dismissOnClickOutside = true,
                            dismissOnBackPress = true
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .width(196.dp)
                                .shadow(24.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                                .background(menuSurfaceBg, RoundedCornerShape(22.dp))
                                .border(1.dp, menuBorder, RoundedCornerShape(22.dp))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf("Classic", "Pinned", "Taped", "Folded").forEach { styleName ->
                                val isSelectedStyle = note.style == styleName
                                val rowBg = if (isSelectedStyle) activeChipBg else Color.Transparent
                                val rowColor = if (isSelectedStyle) activeTint else ink

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(rowBg, RoundedCornerShape(16.dp))
                                        .clickable {
                                            onUpdate(note.copy(style = styleName))
                                            onSaveRequest()
                                            openMenu = StickyMenuType.None
                                        }
                                        .padding(horizontal = 12.dp)
                                        .semantics {
                                            role = Role.RadioButton
                                            selected = isSelectedStyle
                                            contentDescription = "Note style, $styleName"
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StylePictogram(
                                        style = styleName,
                                        modifier = Modifier.size(24.dp),
                                        color = rowColor
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = styleName,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelectedStyle) FontWeight.SemiBold else FontWeight.Medium,
                                        color = rowColor
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (isSelectedStyle) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = activeTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StickyNoteItem(
    note: CanvasElement.StickyNote, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean, isConnectionTarget: Boolean = false, isCanvasLocked: Boolean = false, onConnectRequest: () -> Unit = {},
    onUpdate: (CanvasElement.StickyNote) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit, onDuplicate: (() -> Unit)? = null
) {
    val currentFont = when (note.fontFamily) {
        "Serif" -> AppFonts.Serif
        "Mono" -> AppFonts.Mono
        "Cursive" -> AppFonts.Handwriting
        else -> AppFonts.Sans
    }
    var isDragging by remember { mutableStateOf(false) }

    CanvasItemWrapper(
        position = note.position, width = note.width, height = note.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting, isConnectionTarget = isConnectionTarget, isCanvasLocked = isCanvasLocked, onConnectRequest = onConnectRequest,
        isLocked = note.isLocked || isCanvasLocked, onLockedChange = { onUpdate(note.copy(isLocked = it)) },
        isFullyDraggable = false, isChildDragging = isDragging, backgroundAlpha = 1f, // Critical so we don't block the TextField, and 1f to block out arrows behind
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        onDelete = onDelete, onSaveRequest = onSaveRequest, onDuplicate = onDuplicate, hyperlinkTarget = note.hyperlinkTarget, onLinkUpdate = { newTarget -> onUpdate(note.copy(hyperlinkTarget = newTarget)); onSaveRequest() },
        menuContentPadding = 8.dp,
        customMenuContent = {
            StickyNoteToolStack(
                note = note,
                isSelected = isSelected,
                onUpdate = onUpdate,
                onSaveRequest = onSaveRequest
            )
        }
    ) { isLocked ->
        Box(modifier = Modifier.fillMaxSize().shadow(4.dp)) {
            Column(modifier = Modifier.fillMaxSize().background(note.color)) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(32.dp).background(Color.Transparent)
                        .pointerInput(isLocked) {
                            detectDragGestures(
                                onDragStart = { if (!isLocked) { onSelect(); isDragging = true } }, 
                                onDragEnd = { if (!isLocked) { onSaveRequest(); isDragging = false } },
                                onDragCancel = { if (!isLocked) { onSaveRequest(); isDragging = false } },
                                onDrag = { change, dragAmount -> 
                                    if (!isLocked) {
                                        change.consume()
                                        onPositionDelta(dragAmount)
                                    }
                                }
                            )
                        }
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.DragHandle, contentDescription = "Drag", tint = Color.Black.copy(alpha = 0.2f), modifier = Modifier.align(Alignment.Center))
                    }
                }
                BasicTextField(
                    value = note.text,
                    enabled = isSelected,
                    onValueChange = { onUpdate(note.copy(text = it)); onSaveRequest() },
                    modifier = Modifier.fillMaxSize().padding(8.dp).onFocusChanged { if (it.isFocused) onSelect() },
                    textStyle = LocalTextStyle.current.merge(TextStyle(fontSize = note.fontSize.sp, fontFamily = currentFont, color = Color.Black))
                )
            }
            when (note.style) {
                "Pinned" -> {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin",
                        tint = Color(0xFFE53935),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-8).dp)
                            .size(28.dp)
                            .rotate(-10f)
                            .shadow(2.dp, CircleShape)
                    )
                }
                "Taped" -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-4).dp)
                            .size(80.dp, 24.dp)
                            .rotate(-3f)
                            .shadow(1.dp)
                            .background(Color(0x88FFFFEE))
                            .border(0.5.dp, Color.Black.copy(alpha = 0.05f))
                    )
                }
                "Folded" -> {
                    androidx.compose.foundation.Canvas(modifier = Modifier.align(Alignment.BottomEnd).size(30.dp)) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, size.height)
                            lineTo(size.width, 0f)
                            lineTo(0f, 0f)
                            close()
                        }
                        drawPath(
                            path = path,
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Transparent),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            )
                        )
                        val foldPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, size.height)
                            lineTo(size.width, 0f)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(foldPath, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}


