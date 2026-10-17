package com.flash.draft.ui.canvas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.AppFonts
import com.flash.draft.model.CanvasElement
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import com.flash.draft.ui.canvas.item.getAdaptiveColor
import kotlin.math.roundToInt

@Composable
fun LabelItem(
    labelNode: CanvasElement.LabelNode, scale: Float, offset: Offset, isSelected: Boolean, isConnecting: Boolean, isConnectionTarget: Boolean = false, isCanvasLocked: Boolean = false, onConnectRequest: () -> Unit = {},
    onUpdate: (CanvasElement.LabelNode) -> Unit, onPositionDelta: (Offset) -> Unit, onSizeDelta: (Offset) -> Unit,
    onSaveRequest: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit, onDuplicate: (() -> Unit)? = null,
    isNewlySpawned: Boolean = false
) {
    val spawnHighlightAlpha = androidx.compose.runtime.remember {
        Animatable(if (isNewlySpawned) 1f else 0f)
    }

    LaunchedEffect(isNewlySpawned) {
        if (isNewlySpawned) {
            spawnHighlightAlpha.snapTo(1f)
            spawnHighlightAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 2000,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var textFieldValue by remember(labelNode.id) {
        mutableStateOf(TextFieldValue(labelNode.text, TextRange(labelNode.text.length)))
    }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Sync external changes to labelNode.text (e.g. undo/redo)
    LaunchedEffect(labelNode.text) {
        if (labelNode.text != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = labelNode.text,
                selection = TextRange(labelNode.text.length)
            )
        }
    }

    // Dismiss edit mode on system back
    BackHandler(enabled = isEditing) {
        isEditing = false
        focusManager.clearFocus()
        keyboardController?.hide()
        onSaveRequest()
    }

    // Handle deselection, canvas lock, or item lock
    LaunchedEffect(isSelected, isCanvasLocked, labelNode.isLocked) {
        if (!isSelected || isCanvasLocked || labelNode.isLocked) {
            if (isEditing) {
                isEditing = false
                focusManager.clearFocus()
                keyboardController?.hide()
                onSaveRequest()
            }
        }
    }

    // Handle isEditing focus and keyboard
    LaunchedEffect(isEditing) {
        if (isEditing) {
            textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.text.length))
            delay(50)
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    CanvasItemWrapper(
        position = labelNode.position, width = labelNode.width, height = labelNode.height,
        scale = scale, canvasOffset = offset, isSelected = isSelected, isConnecting = isConnecting, isConnectionTarget = isConnectionTarget, isCanvasLocked = isCanvasLocked, onConnectRequest = onConnectRequest,
        isLocked = labelNode.isLocked || isCanvasLocked, onLockedChange = { onUpdate(labelNode.copy(isLocked = it)) },
        hasDefaultBackground = false, // <-- Makes it transparent!
        isFullyDraggable = false,
        isFullyTappable = false,
        isChildDragging = isDragging, // Critical for text editing
        onSelect = onSelect, onPositionDelta = onPositionDelta, onSizeDelta = onSizeDelta,
        onDelete = onDelete, onSaveRequest = onSaveRequest, onDuplicate = onDuplicate, hyperlinkTarget = labelNode.hyperlinkTarget, onLinkUpdate = { newTarget -> onUpdate(labelNode.copy(hyperlinkTarget = newTarget)); onSaveRequest() },
        menuContentPadding = 14.dp,
        customMenuContent = {
            TextBoxToolStack(
                labelNode = labelNode,
                onUpdate = onUpdate,
                onSaveRequest = onSaveRequest
            )
        }
    ) { isLocked ->
        val resolvedColor = getAdaptiveColor(labelNode.color, LocalDarkTheme.current)
        val textStyle = TextStyle(
            color = resolvedColor,
            fontSize = labelNode.fontSize.sp,
            fontWeight = if (labelNode.isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (labelNode.isItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (labelNode.isUnderline) TextDecoration.Underline else TextDecoration.None,
            textAlign = when (labelNode.textAlign) {
                "Center" -> TextAlign.Center
                "Right" -> TextAlign.Right
                else -> TextAlign.Left
            },
            fontFamily = when (labelNode.fontFamily) {
                "Serif" -> AppFonts.Serif
                "Mono", "Monospace" -> AppFonts.Mono
                "Cursive", "Curs" -> AppFonts.Handwriting
                else -> AppFonts.Sans
            }
        )

        val isDark = LocalDarkTheme.current
        val highlightAlpha = spawnHighlightAlpha.value
        val highlightColor = if (isDark) Color(0xFF60A5FA) else MaterialTheme.colorScheme.primary
        val cornerShape = RoundedCornerShape(8.dp)

        // Wrap in a box to capture drag gestures when not selected
        Box(
            modifier = Modifier.fillMaxSize()
                .then(
                    if (highlightAlpha > 0f) {
                        Modifier
                            .border(
                                width = 2.dp,
                                color = highlightColor.copy(alpha = highlightAlpha),
                                shape = cornerShape
                            )
                            .background(
                                color = highlightColor.copy(alpha = highlightAlpha * 0.12f),
                                shape = cornerShape
                            )
                    } else Modifier
                )
                .background(getAdaptiveColor(labelNode.backgroundColor, isDark), cornerShape)
                .then(
                    if (!isEditing && !isLocked) {
                        Modifier
                            .pointerInput(isSelected, isLocked) {
                                detectTapGestures(
                                    onTap = {
                                        val now = System.currentTimeMillis()
                                        if (isSelected || (now - lastTapTimestamp < 350L)) {
                                            lastTapTimestamp = 0L
                                            isEditing = true
                                        } else {
                                            lastTapTimestamp = now
                                            onSelect()
                                        }
                                    }
                                )
                            }
                            .pointerInput(isLocked) {
                                detectDragGestures(
                                    onDragStart = { 
                                        lastTapTimestamp = 0L
                                        isDragging = true
                                        if (!isSelected) onSelect() 
                                    },
                                    onDragEnd = { 
                                        if (!isLocked) onSaveRequest()
                                        isDragging = false 
                                    },
                                    onDragCancel = { 
                                        if (!isLocked) onSaveRequest()
                                        isDragging = false 
                                    },
                                    onDrag = { change, dragAmount -> 
                                        if (!isLocked) {
                                            change.consume()
                                            onPositionDelta(dragAmount)
                                        }
                                    }
                                )
                            }
                    } else Modifier
                )
        ) {
            // The Text Field
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onUpdate(labelNode.copy(text = newValue.text))
                },
                enabled = isEditing,
                cursorBrush = SolidColor(resolvedColor),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .focusRequester(focusRequester),
                textStyle = textStyle,
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (labelNode.text.isEmpty()) {
                            Text(
                                text = "Type text...",
                                style = textStyle.copy(
                                    color = resolvedColor.copy(alpha = 0.4f)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Dedicated Top Drag Pill (visible when selected)
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-22).dp)
                        .size(width = 44.dp, height = 20.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                        )
                        .pointerInput(isLocked) {
                            detectDragGestures(
                                onDragStart = { 
                                    isDragging = true
                                    if (!isLocked) onSelect() 
                                }, 
                                onDragEnd = { 
                                    isDragging = false
                                    if (!isLocked) onSaveRequest() 
                                },
                                onDragCancel = { 
                                    isDragging = false
                                    if (!isLocked) onSaveRequest() 
                                },
                                onDrag = { change, dragAmount -> 
                                    if (!isLocked) {
                                        change.consume()
                                        onPositionDelta(dragAmount)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag text box",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ── Text Box Tool Stack & Formatting Panel ───────────────────────────────────

private val TextPaletteSwatches = listOf(
    Color(0xFF000000) to "Ink",
    Color(0xFF5B667A) to "Graphite",
    Color(0xFFFFFFFF) to "White",
    Color(0xFF2196F3) to "Blue",
    Color(0xFF4CAF50) to "Green",
    Color(0xFFFF9800) to "Orange",
    Color(0xFFF44336) to "Red"
)

private val FillPaletteSwatches = listOf(
    Color(0x00000000) to "None",
    Color(0xFFFFFFFF) to "White",
    Color(0xFFFFF9C4) to "Butter",
    Color(0xFFBBDEFB) to "Sky",
    Color(0xFFC8E6C9) to "Mint",
    Color(0xFFFFD8E4) to "Rose",
    Color(0xFFE5DCFF) to "Lilac"
)

private fun getTextSwatchName(color: Color): String {
    return TextPaletteSwatches.firstOrNull { it.first == color }?.second ?: "Custom"
}

private fun getFillSwatchName(color: Color): String {
    if (color.alpha == 0f) return "None"
    return FillPaletteSwatches.firstOrNull { it.first == color }?.second ?: "Custom"
}

@Composable
internal fun TextBoxToolStack(
    labelNode: CanvasElement.LabelNode,
    onUpdate: (CanvasElement.LabelNode) -> Unit,
    onSaveRequest: () -> Unit
) {
    var activeColorPicker by remember { mutableStateOf("fg") }

    Column(
        modifier = Modifier.width(352.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drag handle: 36x4dp, outlineVariant, 2dp corners
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(2.dp)
                )
        )

        // ROW 1: two groups, each weight(1f), spacedBy 12.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Group A (multi-toggle): Bold, Italic, Underline
            SegmentedTrack(modifier = Modifier.weight(1f)) {
                SegmentIcon(
                    icon = Icons.Default.FormatBold,
                    contentDescription = "Bold",
                    selected = labelNode.isBold,
                    isToggleable = true,
                    onClick = {
                        onUpdate(labelNode.copy(isBold = !labelNode.isBold))
                        onSaveRequest()
                    }
                )
                SegmentIcon(
                    icon = Icons.Default.FormatItalic,
                    contentDescription = "Italic",
                    selected = labelNode.isItalic,
                    isToggleable = true,
                    onClick = {
                        onUpdate(labelNode.copy(isItalic = !labelNode.isItalic))
                        onSaveRequest()
                    }
                )
                SegmentIcon(
                    icon = Icons.Default.FormatUnderlined,
                    contentDescription = "Underline",
                    selected = labelNode.isUnderline,
                    isToggleable = true,
                    onClick = {
                        onUpdate(labelNode.copy(isUnderline = !labelNode.isUnderline))
                        onSaveRequest()
                    }
                )
            }

            // Group B (single-select): Align left, center, right
            SegmentedTrack(modifier = Modifier.weight(1f)) {
                SegmentIcon(
                    icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                    contentDescription = "Align left",
                    selected = labelNode.textAlign == "Left",
                    onClick = {
                        onUpdate(labelNode.copy(textAlign = "Left"))
                        onSaveRequest()
                    }
                )
                SegmentIcon(
                    icon = Icons.Default.FormatAlignCenter,
                    contentDescription = "Align center",
                    selected = labelNode.textAlign == "Center",
                    onClick = {
                        onUpdate(labelNode.copy(textAlign = "Center"))
                        onSaveRequest()
                    }
                )
                SegmentIcon(
                    icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                    contentDescription = "Align right",
                    selected = labelNode.textAlign == "Right",
                    onClick = {
                        onUpdate(labelNode.copy(textAlign = "Right"))
                        onSaveRequest()
                    }
                )
            }
        }

        // ROW 2: font family (Sans, Serif, Mono, Cursive)
        val fontOptions = remember {
            listOf(
                Triple("Sans", FontFamily.SansSerif, 14.sp),
                Triple("Serif", FontFamily.Serif, 16.sp),
                Triple("Mono", FontFamily.Monospace, 13.sp),
                Triple("Curs", FontFamily.Cursive, 18.sp)
            )
        }
        SegmentedTrack(modifier = Modifier.fillMaxWidth()) {
            fontOptions.forEach { (storedKey, fontFam, fontSize) ->
                val isSelected = labelNode.fontFamily == storedKey || (storedKey == "Sans" && labelNode.fontFamily == "SansSerif")
                val displayLabel = if (storedKey == "Curs") "Cursive" else storedKey
                FontSegment(
                    label = displayLabel,
                    fontFamily = fontFam,
                    fontSize = fontSize,
                    selected = isSelected,
                    onClick = {
                        onUpdate(labelNode.copy(fontFamily = storedKey))
                        onSaveRequest()
                    }
                )
            }
        }

        // ROW 3: size
        SizeRow(
            fontSize = labelNode.fontSize,
            onValueChange = { onUpdate(labelNode.copy(fontSize = it)) },
            onValueChangeFinished = { onSaveRequest() }
        )

        // ROW 4: colour tiles (Text / Fill)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ColorTile(
                label = "TEXT",
                valueName = getTextSwatchName(labelNode.color),
                color = labelNode.color,
                isFill = false,
                isActive = activeColorPicker == "fg",
                onClick = { activeColorPicker = "fg" }
            )
            ColorTile(
                label = "FILL",
                valueName = getFillSwatchName(labelNode.backgroundColor),
                color = labelNode.backgroundColor,
                isFill = true,
                isActive = activeColorPicker == "bg",
                onClick = { activeColorPicker = "bg" }
            )
        }

        // ROW 5: palette (ALWAYS visible)
        val isFg = activeColorPicker == "fg"
        var showColorPicker by remember { mutableStateOf(false) }

        ColorPaletteRow(
            swatches = if (isFg) TextPaletteSwatches else FillPaletteSwatches,
            selectedColor = if (isFg) labelNode.color else labelNode.backgroundColor,
            isFill = !isFg,
            onColorSelected = { selectedColor ->
                if (isFg) {
                    onUpdate(labelNode.copy(color = selectedColor))
                } else {
                    onUpdate(labelNode.copy(backgroundColor = selectedColor))
                }
                onSaveRequest()
            },
            onCustomClick = {
                showColorPicker = true
            }
        )

        if (showColorPicker) {
            val initialColor = if (isFg) {
                labelNode.color
            } else {
                if (labelNode.backgroundColor.alpha == 0f) Color.White else labelNode.backgroundColor
            }
            UniversalColorPickerDialog(
                title = if (isFg) "Text Color" else "Fill Color",
                initialColor = initialColor,
                onColorChange = { newColor ->
                    if (isFg) {
                        onUpdate(labelNode.copy(color = newColor))
                    } else {
                        onUpdate(labelNode.copy(backgroundColor = newColor))
                    }
                },
                onDismiss = { originalColor ->
                    if (isFg) {
                        onUpdate(labelNode.copy(color = originalColor))
                    } else {
                        onUpdate(labelNode.copy(backgroundColor = originalColor))
                    }
                    showColorPicker = false
                },
                onConfirm = { confirmedColor ->
                    if (isFg) {
                        onUpdate(labelNode.copy(color = confirmedColor))
                    } else {
                        onUpdate(labelNode.copy(backgroundColor = confirmedColor))
                    }
                    onSaveRequest()
                    showColorPicker = false
                }
            )
        }
    }
}

@Composable
private fun SegmentedTrack(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun RowScope.SegmentIcon(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isToggleable: Boolean = false
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(11.dp)

    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(shape)
            .background(if (selected) primaryColor.copy(alpha = 0.14f) else Color.Transparent)
            .then(
                if (isToggleable) {
                    Modifier.toggleable(
                        value = selected,
                        role = Role.Checkbox,
                        onValueChange = { onClick() }
                    )
                } else {
                    Modifier.selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = onClick
                    )
                }
            )
            .semantics {
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (selected) primaryColor else onSurfaceVariant
        )
    }
}

@Composable
private fun RowScope.FontSegment(
    label: String,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(11.dp)

    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(shape)
            .background(if (selected) primaryColor.copy(alpha = 0.14f) else Color.Transparent)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics {
                this.contentDescription = "Font $label"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = fontFamily,
            fontSize = fontSize,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) primaryColor else onSurfaceVariant,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SizeRow(
    fontSize: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(start = 16.dp, end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "A",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = fontSize,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 12f..72f,
            modifier = Modifier.weight(1f),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .shadow(2.dp, CircleShape)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        )
        Text(
            text = "A",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 40.dp)
                .height(34.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fontSize.roundToInt().toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RowScope.ColorTile(
    label: String,
    valueName: String,
    color: Color,
    isFill: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(14.dp)
    val isNone = isFill && color.alpha == 0f

    Box(
        modifier = modifier
            .weight(1f)
            .height(52.dp)
            .clip(shape)
            .background(if (isActive) primaryColor.copy(alpha = 0.14f) else surfaceVariant)
            .then(
                if (isActive) Modifier.border(1.5.dp, primaryColor, shape)
                else Modifier
            )
            .selectable(
                selected = isActive,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics {
                this.contentDescription = "$label color, $valueName"
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chip (32x32dp, 8dp radius)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            ) {
                // Glyph slightly above centre
                if (isFill) {
                    Icon(
                        imageVector = Icons.Default.FormatColorFill,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.Center)
                            .offset(y = (-3).dp)
                    )
                } else {
                    Text(
                        text = "A",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-3).dp)
                    )
                }

                // Bar below glyph: 4dp high, 6dp horizontal inset, 5dp from bottom, 2dp corners
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 5.dp)
                        .padding(horizontal = 6.dp)
                        .fillMaxWidth()
                        .height(4.dp)
                ) {
                    if (isNone) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(
                                color = Color.White,
                                cornerRadius = CornerRadius(2.dp.toPx())
                            )
                            drawLine(
                                color = Color(0xFFE53935),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawRoundRect(
                                color = outlineVariant.copy(alpha = 0.5f),
                                cornerRadius = CornerRadius(2.dp.toPx()),
                                style = Stroke(width = 0.75.dp.toPx())
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color, RoundedCornerShape(2.dp))
                                .border(0.5.dp, outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            // Labels Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = valueName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ColorPaletteRow(
    swatches: List<Pair<Color, String>>,
    selectedColor: Color,
    isFill: Boolean,
    onColorSelected: (Color) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val rainbowColors = remember {
        listOf(
            Color(0xFFFF0000),
            Color(0xFFFF9900),
            Color(0xFFFFFF00),
            Color(0xFF33CC33),
            Color(0xFF0099FF),
            Color(0xFF6633FF),
            Color(0xFFFF0099),
            Color(0xFFFF0000)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        swatches.forEach { (color, name) ->
            val isNone = isFill && color.alpha == 0f
            val isSelected = if (isNone) {
                isFill && selectedColor.alpha == 0f
            } else {
                selectedColor == color
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onColorSelected(color) }
                    )
                    .semantics {
                        this.contentDescription = name
                    },
                contentAlignment = Alignment.Center
            ) {
                // If selected, 3dp surface-colored gap then a 2dp primary ring
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .border(2.dp, primaryColor, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(surfaceColor, CircleShape)
                    )
                }

                if (isNone) {
                    Canvas(modifier = Modifier.size(32.dp)) {
                        drawCircle(color = Color.White)
                        drawLine(
                            color = Color(0xFFE53935),
                            start = Offset(size.width * 0.22f, size.height * 0.22f),
                            end = Offset(size.width * 0.78f, size.height * 0.78f),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawCircle(
                            color = outlineVariant.copy(alpha = 0.5f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color, CircleShape)
                            .border(1.dp, outlineVariant.copy(alpha = 0.5f), CircleShape)
                    )
                }

                if (isSelected) {
                    val lum = if (isNone) 1f else color.luminance()
                    val checkTint = if (lum > 0.5f) Color(0xFF161A26) else Color.White
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = checkTint
                    )
                }
            }
        }

        // 8th cell: Custom colour
        val isCustom = !swatches.any { it.first == selectedColor } && (!isFill || selectedColor.alpha != 0f)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(
                    role = Role.Button,
                    onClick = onCustomClick
                )
                .semantics {
                    this.contentDescription = if (isCustom) "Custom colour, selected" else "Custom colour"
                },
            contentAlignment = Alignment.Center
        ) {
            if (isCustom) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .border(2.dp, primaryColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(surfaceColor, CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Brush.sweepGradient(rainbowColors), CircleShape)
                    .border(1.dp, outlineVariant.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isCustom) {
                    val lum = selectedColor.luminance()
                    val checkTint = if (lum > 0.5f) Color(0xFF161A26) else Color.White
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(selectedColor, CircleShape)
                            .border(0.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = checkTint,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

@Composable
private fun UniversalColorPickerDialog(
    title: String,
    initialColor: Color,
    onColorChange: (Color) -> Unit,
    onDismiss: (Color) -> Unit,
    onConfirm: (Color) -> Unit
) {
    val hsvState = remember {
        val array = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), array)
        mutableStateOf(array)
    }

    var currentColor by remember { mutableStateOf(initialColor) }

    val updateHsv = { newHue: Float, newSat: Float, newVal: Float ->
        val newH = newHue.coerceIn(0f, 360f)
        val newS = newSat.coerceIn(0f, 1f)
        val newV = newVal.coerceIn(0f, 1f)
        hsvState.value = floatArrayOf(newH, newS, newV)
        val c = Color.hsv(newH, newS, newV)
        currentColor = c
        onColorChange(c)
    }

    Dialog(
        onDismissRequest = { onDismiss(initialColor) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Title + Hex + Swatch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = colorToHex(currentColor),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Live Swatch
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    )
                }

                val hue = hsvState.value[0]
                val sat = hsvState.value[1]
                val value = hsvState.value[2]

                // 2D Saturation / Value Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.hsv(hue, 1f, 1f))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, _ ->
                                    change.consume()
                                    val s = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    val v = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                    updateHsv(hsvState.value[0], s, v)
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val s = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val v = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                                updateHsv(hsvState.value[0], s, v)
                            }
                        }
                        .semantics { contentDescription = "Color saturation and brightness picker" }
                ) {
                    // White to transparent (horizontal)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                    )
                    // Transparent to black (vertical)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    )

                    // Thumb indicator
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val posX = (sat * size.width).coerceIn(0f, size.width)
                        val posY = ((1f - value) * size.height).coerceIn(0f, size.height)
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.4f),
                            radius = 10.dp.toPx(),
                            center = Offset(posX, posY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 8.dp.toPx(),
                            center = Offset(posX, posY)
                        )
                        drawCircle(
                            color = currentColor,
                            radius = 6.dp.toPx(),
                            center = Offset(posX, posY)
                        )
                    }
                }

                // Hue Slider
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, _ ->
                                    change.consume()
                                    val h = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                                    updateHsv(h, hsvState.value[1], hsvState.value[2])
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val h = (offset.x / size.width.toFloat()).coerceIn(0f, 1f) * 360f
                                updateHsv(h, hsvState.value[1], hsvState.value[2])
                            }
                        }
                        .semantics { contentDescription = "Hue slider" }
                ) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Red, Color.Yellow, Color.Green,
                                Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                            )
                        )
                    )
                    // Thumb
                    val indicatorX = ((hue / 360f) * size.width).coerceIn(0f, size.width)
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.4f),
                        radius = 11.dp.toPx(),
                        center = Offset(indicatorX, size.height / 2f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 9.dp.toPx(),
                        center = Offset(indicatorX, size.height / 2f)
                    )
                    drawCircle(
                        color = Color.hsv(hue, 1f, 1f),
                        radius = 7.dp.toPx(),
                        center = Offset(indicatorX, size.height / 2f)
                    )
                }

                // Quick Palette Swatches Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickTones = remember {
                        listOf(
                            Color(0xFF000000), // Black
                            Color(0xFF5B667A), // Slate
                            Color(0xFFFFFFFF), // White
                            Color(0xFF2563EB), // Blue
                            Color(0xFF16A34A), // Green
                            Color(0xFFEA580C), // Orange
                            Color(0xFFDC2626)  // Red
                        )
                    }
                    quickTones.forEach { toneColor ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(toneColor)
                                .border(
                                    width = if (currentColor == toneColor) 2.dp else 1.dp,
                                    color = if (currentColor == toneColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    val array = FloatArray(3)
                                    android.graphics.Color.colorToHSV(toneColor.toArgb(), array)
                                    hsvState.value = array
                                    currentColor = toneColor
                                    onColorChange(toneColor)
                                }
                        )
                    }
                }

                // Action Buttons (Cancel / Apply)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onDismiss(initialColor) },
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(currentColor) },
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Apply",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
