package com.flash.draft

import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R as LucideR
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.model.CanvasElement
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import com.flash.draft.ui.canvas.LocalLiquidState
import com.flash.draft.ui.canvas.LocalLiquidRefraction
import com.flash.draft.ui.canvas.LocalLiquidCurve
import io.github.fletchmckee.liquid.liquid
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.flash.draft.ui.components.PremiumSlider

@Composable
fun PremiumSegmentedControl(options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit, isDark: Boolean) {
    // Inset background for the pill track
    val containerBg = if (isDark) Color(0x33000000) else Color(0x0D000000)
    // Elevated background for the active chip
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
                Text(text = option, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, color = if (isSelected) activeText else inactiveText)
            }
        }
    }
}

@Composable
fun DockSegmentedIconControl(
    options: List<Pair<String, @Composable (Color) -> Unit>>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    isDark: Boolean
) {
    val containerBg = if (isDark) Color(0x33000000) else Color(0xFFF0F0F0)
    val activeBg = if (isDark) Color(0xFF3A3A3C) else Color.White
    val activeTint = if (isDark) Color.White else Color.Black
    val inactiveTint = if (isDark) Color.Gray else Color.Gray

    Row(
        modifier = Modifier
            .height(32.dp)
            .background(containerBg, RoundedCornerShape(percent = 50))
            .padding(2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (option, icon) ->
            val isSelected = selectedOption == option
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) activeBg else Color.Transparent)
                    .then(if (isSelected) Modifier.shadow(1.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.1f)) else Modifier)
                    .clickable { onOptionSelected(option) },
                contentAlignment = Alignment.Center
            ) {
                icon(if (isSelected) activeTint else inactiveTint)
            }
        }
    }
}

@Composable
fun ConnectorStyleMenu(
    connector: CanvasElement.Connector,
    onUpdate: (CanvasElement.Connector) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit = {}
) {
    val isDark = LocalDarkTheme.current
    val glassBg = if (isDark) Color(0xB31A1A1A) else Color(0xFFF9F9F9)
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0x1A000000)
    val dividerColor = if (isDark) Color(0x33FFFFFF) else Color(0x1A000000)
    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color(0xFF34C759), Color(0xFFFF9500), Color.Gray)

    val liquidState = LocalLiquidState.current
    val liquidRefraction = LocalLiquidRefraction.current
    val liquidCurve = LocalLiquidCurve.current

    val configuration = LocalConfiguration.current
    val dockHeight = if (configuration.screenWidthDp >= 600) 56.dp else 104.dp
    val pillShape = RoundedCornerShape(24.dp)
    
    var isColorPickerExpanded by remember { mutableStateOf(false) }

    val maxDockWidth = configuration.screenWidthDp.dp - 32.dp
    val isTablet = configuration.screenWidthDp >= 600

    Box(
        modifier = Modifier
            .widthIn(max = maxDockWidth)
            .height(dockHeight)
            .shadow(16.dp, pillShape, spotColor = Color.Black.copy(alpha = 0.15f))
            .then(
                if (liquidState != null) Modifier.liquid(liquidState) {
                    refraction = liquidRefraction
                    curve = liquidCurve
                    shape = pillShape
                } else Modifier
            )
            .background(glassBg, pillShape)
            .border(1.dp, glassBorder, pillShape)
            .clip(pillShape)
    ) {
        val colorPicker = @Composable {
            Row(
                modifier = Modifier
                    .background(if (isDark) Color(0x33000000) else Color(0xFFF0F0F0), RoundedCornerShape(percent = 50))
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .animateContentSize(animationSpec = tween(300)),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isColorPickerExpanded) {
                    colors.forEach { color ->
                        val isSelected = connector.color == color
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(color, CircleShape)
                                .border(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFF007AFF) else Color.LightGray.copy(alpha = 0.5f), CircleShape)
                                .clickable { 
                                    onUpdate(connector.copy(color = color))
                                    isColorPickerExpanded = false
                                }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(connector.color, CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            .clickable { isColorPickerExpanded = true }
                    )
                }
            }
        }

        val dashedEffect = remember { androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f) }
        val dottedEffect = remember { androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 10f), 0f) }

        val lineStyleSelector = @Composable {
            val lineStyleOptions: List<Pair<String, @Composable (Color) -> Unit>> = listOf(
                "Solid" to { color ->
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                        val stroke = 2.dp.toPx()
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(2.dp.toPx(), size.height/2), end = androidx.compose.ui.geometry.Offset(size.width - 2.dp.toPx(), size.height/2), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                },
                "Dashed" to { color ->
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                        val stroke = 2.dp.toPx()
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(2.dp.toPx(), size.height/2), end = androidx.compose.ui.geometry.Offset(size.width - 2.dp.toPx(), size.height/2), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round, pathEffect = dashedEffect)
                    }
                },
                "Dotted" to { color ->
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                        val stroke = 2.dp.toPx()
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(2.dp.toPx(), size.height/2), end = androidx.compose.ui.geometry.Offset(size.width - 2.dp.toPx(), size.height/2), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round, pathEffect = dottedEffect)
                    }
                }
            )
            DockSegmentedIconControl(lineStyleOptions, connector.lineStyle, { onUpdate(connector.copy(lineStyle = it)) }, isDark)
        }

        val arrowShapeSelector = @Composable {
            val arrowShapeOptions: List<Pair<String, @Composable (Color) -> Unit>> = listOf(
                "None" to { color ->
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height/2), end = androidx.compose.ui.geometry.Offset(size.width, size.height/2), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                },
                "Open" to { color ->
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height/2), end = androidx.compose.ui.geometry.Offset(size.width, size.height/2), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width/2f, 0f), end = androidx.compose.ui.geometry.Offset(size.width, size.height/2), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width/2f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height/2), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                },
                "Filled" to { color ->
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(0f, size.height/2), end = androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), size.height/2), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width, size.height/2)
                            lineTo(size.width/2f, 0f)
                            lineTo(size.width/2f, size.height)
                            close()
                        }
                        drawPath(path, color)
                    }
                }
            )
            val currentShapeStr = if (connector.arrowDirection == "None") "None" else connector.arrowShape
            DockSegmentedIconControl(arrowShapeOptions, currentShapeStr, { shape ->
                if (shape == "None") {
                    onUpdate(connector.copy(arrowDirection = "None"))
                } else {
                    onUpdate(connector.copy(arrowShape = shape, arrowDirection = "Both"))
                }
            }, isDark)
        }

        val sizeSlider = @Composable {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 4.dp).widthIn(min = 100.dp).fillMaxWidth()
            ) {
                Text("Size", color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                PremiumSlider(
                    value = connector.strokeWidth,
                    onValueChange = { onUpdate(connector.copy(strokeWidth = it)) },
                    valueRange = 2f..20f,
                    modifier = Modifier.fillMaxWidth(),
                    activeColor = if (isDark) Color.White else Color.Black,
                    inactiveColor = if (isDark) Color(0x33FFFFFF) else Color(0xFFE0E0E0)
                )
            }
        }

        val deleteBtn = @Composable {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0x1AFF3B30), CircleShape)
                    .clip(CircleShape)
            ) {
                Icon(painterResource(id = LucideR.drawable.lucide_ic_trash), "Delete", tint = Color(0xFFFF3B30), modifier = Modifier.size(16.dp))
            }
        }

        val divider = @Composable {
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(dividerColor))
        }

        if (isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .animateContentSize(animationSpec = tween(300)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                colorPicker()
                divider()
                lineStyleSelector()
                divider()
                arrowShapeSelector()
                divider()
                Box(modifier = Modifier.width(120.dp)) { sizeSlider() }
                divider()
                deleteBtn()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .animateContentSize(animationSpec = tween(300)),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorPicker()
                    divider()
                    lineStyleSelector()
                    divider()
                    deleteBtn()
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    arrowShapeSelector()
                    divider()
                    Box(modifier = Modifier.weight(1f)) { sizeSlider() }
                }
            }
        }
    }
}