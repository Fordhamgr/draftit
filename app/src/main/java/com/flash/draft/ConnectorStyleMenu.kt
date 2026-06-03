package com.flash.draft

import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R as LucideR
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.graphics.vector.ImageVector

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

// A compact segmented control icon version for the dock
@Composable
fun DockSegmentedIconControl(
    options: List<Pair<String, @Composable (Color) -> Unit>>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    isDark: Boolean
) {
    val containerBg = if (isDark) Color(0x33000000) else Color(0x0D000000)
    val activeBg = if (isDark) Color(0xFF3A3A3C) else Color(0xFFFFFFFF)
    val activeTint = if (isDark) Color.White else Color.Black
    val inactiveTint = if (isDark) Color.Gray else Color.DarkGray

    Row(
        modifier = Modifier
            .height(36.dp)
            .background(containerBg, RoundedCornerShape(percent = 50))
            .padding(2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (option, icon) ->
            val isSelected = selectedOption == option
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) activeBg else Color.Transparent)
                    .then(if (isSelected) Modifier.shadow(2.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.1f)) else Modifier)
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
    onClose: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val glassBg = if (isDark) Color(0xB31A1A1A) else Color(0xB3FFFFFF)
    val glassBorder = if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)
    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color(0xFF34C759), Color(0xFFFF9500), Color.Gray)

    val liquidState = LocalLiquidState.current
    val liquidRefraction = LocalLiquidRefraction.current
    val liquidCurve = LocalLiquidCurve.current

    val dockHeight = 56.dp
    val pillShape = RoundedCornerShape(percent = 50)
    
    var isColorPickerExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
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
        Row(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp).animateContentSize(animationSpec = tween(300)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Colors (Expandable)
            Row(
                modifier = Modifier
                    .background(if (isDark) Color(0x33000000) else Color(0x0D000000), RoundedCornerShape(percent = 50))
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
            
            // Only show the rest if color picker is NOT expanded to save space, or just show them anyway
            // The prompt says "which is a single circle that expands to show more colors on click, saving space"
            // Since it's a pill, it can just grow horizontally. We can show all elements always, it will just stretch.
            
            // Vertical Divider
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f)))

            // Line Style Options
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
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(2.dp.toPx(), size.height/2), end = androidx.compose.ui.geometry.Offset(size.width - 2.dp.toPx(), size.height/2), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f))
                    }
                },
                "Dotted" to { color ->
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
                        val stroke = 2.dp.toPx()
                        drawLine(color, start = androidx.compose.ui.geometry.Offset(2.dp.toPx(), size.height/2), end = androidx.compose.ui.geometry.Offset(size.width - 2.dp.toPx(), size.height/2), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 10f), 0f))
                    }
                }
            )
            DockSegmentedIconControl(lineStyleOptions, connector.lineStyle, { onUpdate(connector.copy(lineStyle = it)) }, isDark)

            // Thickness Adjuster
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text("Size", color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                androidx.compose.material3.Slider(
                    value = connector.strokeWidth,
                    onValueChange = { onUpdate(connector.copy(strokeWidth = it)) },
                    valueRange = 2f..20f,
                    modifier = Modifier.width(100.dp),
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = if (isDark) Color.White else Color.Black,
                        activeTrackColor = if (isDark) Color.White else Color.Black,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )
            }

            // Vertical Divider
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f)))

            // Delete
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0x1AFF3B30), CircleShape)
                    .clip(CircleShape)
            ) {
                Icon(painterResource(id = LucideR.drawable.lucide_ic_trash), "Delete", tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
            }

            // Close
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .background(if (isDark) Color(0x33000000) else Color(0x0D000000), CircleShape)
                    .clip(CircleShape)
            ) {
                Icon(painterResource(id = LucideR.drawable.lucide_ic_x), "Close", tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(20.dp))
            }
        }
    }
}