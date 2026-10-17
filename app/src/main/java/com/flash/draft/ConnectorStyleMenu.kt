package com.flash.draft

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.R as LucideR
import com.flash.draft.model.CanvasElement
import kotlin.math.roundToInt

// Legacy segmented control preserved for PenStudioPanel
@Composable
fun PremiumSegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    isDark: Boolean
) {
    val containerBg = if (isDark) Color(0x33000000) else Color(0x0D000000)
    val activeBg = if (isDark) Color(0xFF3A3A3C) else Color(0xFFFFFFFF)
    val activeText = if (isDark) Color.White else Color.Black
    val inactiveText = if (isDark) Color.Gray else Color.DarkGray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerBg, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        options.forEach { option ->
            val isSelected = selectedOption == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) activeBg else Color.Transparent)
                    .then(if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(8.dp), spotColor = Color.Black.copy(alpha = 0.1f)) else Modifier)
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) activeText else inactiveText
                )
            }
        }
    }
}

private val ConnectorPresetSwatches = listOf(
    Color(0xFF000000) to "Black",
    Color(0xFF5B667A) to "Graphite",
    Color(0xFFFFFFFF) to "White",
    Color(0xFF2196F3) to "Blue",
    Color(0xFF4CAF50) to "Green",
    Color(0xFFFFA000) to "Orange",
    Color(0xFFF44336) to "Red"
)

private fun getConnectorColorName(color: Color): String {
    return when (color) {
        Color(0xFF000000), Color.Black -> "Black"
        Color(0xFF5B667A) -> "Graphite"
        Color(0xFFFFFFFF), Color.White -> "White"
        Color(0xFF2196F3), Color.Blue -> "Blue"
        Color(0xFF4CAF50) -> "Green"
        Color(0xFFFFA000) -> "Orange"
        Color(0xFFF44336), Color.Red -> "Red"
        else -> colorToHex(color)
    }
}

/**
 * Stateful entry composable for the redesigned bottom-docked connector formatting card.
 */
@Composable
fun ConnectorStyleMenu(
    connector: CanvasElement.Connector,
    onUpdate: (CanvasElement.Connector) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit = {}
) {
    var showColorPicker by remember { mutableStateOf(false) }

    ConnectorStyleMenuContent(
        connector = connector,
        onUpdate = onUpdate,
        onDelete = onDelete,
        onOpenColorPicker = { showColorPicker = true }
    )

    if (showColorPicker) {
        UniversalColorPickerDialog(
            title = "Connector Color",
            initialColor = connector.color,
            onColorChange = { newColor ->
                onUpdate(connector.copy(color = newColor))
            },
            onDismiss = { originalColor ->
                onUpdate(connector.copy(color = originalColor))
                showColorPicker = false
            },
            onConfirm = { confirmedColor ->
                onUpdate(connector.copy(color = confirmedColor))
                showColorPicker = false
            }
        )
    }
}

/**
 * Stateless content composable representing the 4-row bottom card.
 */
@Composable
fun ConnectorStyleMenuContent(
    connector: CanvasElement.Connector,
    onUpdate: (CanvasElement.Connector) -> Unit,
    onDelete: () -> Unit,
    onOpenColorPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            // ROW 1: Header (Preview tile, Title/Subtitle, Delete button)
            ConnectorHeaderRow(
                connector = connector,
                onDelete = onDelete
            )

            // ROW 2: 8-cell Palette (7 swatches + universal picker)
            ConnectorPaletteRow(
                selectedColor = connector.color,
                onColorSelected = { onUpdate(connector.copy(color = it)) },
                onCustomClick = onOpenColorPicker
            )

            // ROW 3: Segmented Controls (Line Style + Arrow Shape)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Line Style Segmented Control
                ConnectorSegmentedTrack(modifier = Modifier.weight(1f)) {
                    LineStyleSegment(
                        label = "Solid line",
                        selected = connector.lineStyle == "Solid",
                        onClick = { onUpdate(connector.copy(lineStyle = "Solid")) }
                    ) { tint ->
                        Canvas(modifier = Modifier.size(width = 24.dp, height = 16.dp)) {
                            drawLine(
                                color = tint,
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    LineStyleSegment(
                        label = "Dashed line",
                        selected = connector.lineStyle == "Dashed",
                        onClick = { onUpdate(connector.copy(lineStyle = "Dashed")) }
                    ) { tint ->
                        Canvas(modifier = Modifier.size(width = 24.dp, height = 16.dp)) {
                            drawLine(
                                color = tint,
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                                    0f
                                )
                            )
                        }
                    }

                    LineStyleSegment(
                        label = "Dotted line",
                        selected = connector.lineStyle == "Dotted",
                        onClick = { onUpdate(connector.copy(lineStyle = "Dotted")) }
                    ) { tint ->
                        Canvas(modifier = Modifier.size(width = 24.dp, height = 16.dp)) {
                            drawLine(
                                color = tint,
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(2.dp.toPx(), 4.5.dp.toPx()),
                                    0f
                                )
                            )
                        }
                    }
                }

                // Arrow Shape Segmented Control
                val currentArrowShape = if (connector.arrowDirection == "None") "None" else connector.arrowShape
                ConnectorSegmentedTrack(modifier = Modifier.weight(1f)) {
                    LineStyleSegment(
                        label = "No arrow",
                        selected = currentArrowShape == "None",
                        onClick = { onUpdate(connector.copy(arrowDirection = "None")) }
                    ) { tint ->
                        Canvas(modifier = Modifier.size(width = 24.dp, height = 16.dp)) {
                            drawLine(
                                color = tint,
                                start = Offset(2.dp.toPx(), size.height / 2f),
                                end = Offset(size.width - 2.dp.toPx(), size.height / 2f),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    LineStyleSegment(
                        label = "Open arrow",
                        selected = currentArrowShape == "Open",
                        onClick = { onUpdate(connector.copy(arrowShape = "Open", arrowDirection = "Both")) }
                    ) { tint ->
                        Canvas(modifier = Modifier.size(width = 24.dp, height = 16.dp)) {
                            val stroke = 2.dp.toPx()
                            val midY = size.height / 2f
                            drawLine(
                                color = tint,
                                start = Offset(2.dp.toPx(), midY),
                                end = Offset(size.width - 2.dp.toPx(), midY),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                            val headX = size.width - 2.dp.toPx()
                            val armLen = 6.dp.toPx()
                            drawLine(
                                color = tint,
                                start = Offset(headX - armLen, midY - armLen * 0.8f),
                                end = Offset(headX, midY),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = tint,
                                start = Offset(headX - armLen, midY + armLen * 0.8f),
                                end = Offset(headX, midY),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    LineStyleSegment(
                        label = "Filled arrow",
                        selected = currentArrowShape == "Filled",
                        onClick = { onUpdate(connector.copy(arrowShape = "Filled", arrowDirection = "Both")) }
                    ) { tint ->
                        Canvas(modifier = Modifier.size(width = 24.dp, height = 16.dp)) {
                            val stroke = 2.dp.toPx()
                            val midY = size.height / 2f
                            val headLen = 7.dp.toPx()
                            val headHalfH = 4.5.dp.toPx()
                            val headX = size.width - 2.dp.toPx()
                            drawLine(
                                color = tint,
                                start = Offset(2.dp.toPx(), midY),
                                end = Offset(headX - headLen * 0.9f, midY),
                                strokeWidth = stroke,
                                cap = StrokeCap.Round
                            )
                            val path = Path().apply {
                                moveTo(headX, midY)
                                lineTo(headX - headLen, midY - headHalfH)
                                lineTo(headX - headLen, midY + headHalfH)
                                close()
                            }
                            drawPath(path, color = tint)
                        }
                    }
                }
            }

            // ROW 4: Size Slider Row
            ConnectorSizeRow(
                strokeWidth = connector.strokeWidth,
                onValueChange = { onUpdate(connector.copy(strokeWidth = it)) }
            )
        }
    }
}

@Composable
private fun ConnectorHeaderRow(
    connector: CanvasElement.Connector,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorName = getConnectorColorName(connector.color)
    val subtitleText = "$colorName • ${connector.lineStyle}"
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val deleteBg = if (isDark) Color(0x33FF3B30) else Color(0xFFFFEBEE)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Preview Tile: 56x36dp, rounded 10dp, board canvas green background
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF233E32))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val midY = size.height / 2f
                    val startX = 10.dp.toPx()
                    val endX = size.width - 10.dp.toPx()
                    val stroke = 2.5.dp.toPx()
                    val color = connector.color

                    val pathEffect = when (connector.lineStyle) {
                        "Dashed" -> PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f)
                        "Dotted" -> PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.5.dp.toPx()), 0f)
                        else -> null
                    }

                    val isNoneArrow = connector.arrowDirection == "None"
                    val arrowShape = connector.arrowShape

                    if (isNoneArrow) {
                        drawLine(
                            color = color,
                            start = Offset(startX, midY),
                            end = Offset(endX, midY),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                            pathEffect = pathEffect
                        )
                    } else if (arrowShape == "Filled") {
                        val arrowLen = 9.dp.toPx()
                        val arrowHalfH = 5.dp.toPx()
                        drawLine(
                            color = color,
                            start = Offset(startX, midY),
                            end = Offset(endX - arrowLen * 0.9f, midY),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                            pathEffect = pathEffect
                        )
                        val arrowPath = Path().apply {
                            moveTo(endX, midY)
                            lineTo(endX - arrowLen, midY - arrowHalfH)
                            lineTo(endX - arrowLen, midY + arrowHalfH)
                            close()
                        }
                        drawPath(arrowPath, color = color)
                    } else { // "Open"
                        val armLen = 7.dp.toPx()
                        drawLine(
                            color = color,
                            start = Offset(startX, midY),
                            end = Offset(endX, midY),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                            pathEffect = pathEffect
                        )
                        drawLine(
                            color = color,
                            start = Offset(endX - armLen, midY - armLen * 0.8f),
                            end = Offset(endX, midY),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = color,
                            start = Offset(endX - armLen, midY + armLen * 0.8f),
                            end = Offset(endX, midY),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Title & Subtitle
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Connector",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitleText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Delete Button: 38dp circular
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(deleteBg)
                .clickable(
                    role = Role.Button,
                    onClick = onDelete
                )
                .semantics {
                    contentDescription = "Delete connector"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = LucideR.drawable.lucide_ic_trash),
                contentDescription = null,
                tint = Color(0xFFFF3B30),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ConnectorPaletteRow(
    selectedColor: Color,
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
        ConnectorPresetSwatches.forEach { (color, name) ->
            val isSelected = selectedColor == color

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

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color, CircleShape)
                        .border(1.dp, outlineVariant.copy(alpha = 0.5f), CircleShape)
                )

                if (isSelected) {
                    val lum = color.luminance()
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

        // 8th cell: Custom / Universal Color Picker
        val isCustom = !ConnectorPresetSwatches.any { it.first == selectedColor }
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

@Composable
private fun ConnectorSegmentedTrack(
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
private fun RowScope.LineStyleSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (Color) -> Unit
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
                this.contentDescription = label
            },
        contentAlignment = Alignment.Center
    ) {
        icon(if (selected) primaryColor else onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConnectorSizeRow(
    strokeWidth: Float,
    onValueChange: (Float) -> Unit,
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
        val iconColor = MaterialTheme.colorScheme.onSurfaceVariant

        // Thin indicator
        Canvas(modifier = Modifier.size(16.dp)) {
            drawLine(
                color = iconColor,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Slider
        Slider(
            value = strokeWidth,
            onValueChange = onValueChange,
            valueRange = 2f..20f,
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

        // Thick indicator
        Canvas(modifier = Modifier.size(16.dp)) {
            drawLine(
                color = iconColor,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Value chip
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
                text = strokeWidth.roundToInt().toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
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
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(10.dp)
                            )
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
                            Color(0xFF5B667A), // Graphite
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