package com.flash.draft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.DeleteOutline
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
fun ConnectorStyleMenu(
    connector: CanvasElement.Connector,
    onUpdate: (CanvasElement.Connector) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val bgColor = if (isDark) Color(0xE61C1C1E) else Color(0xF2F2F7) // Apple-style thick frosted glass
    val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)
    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color(0xFF34C759), Color(0xFFFF9500), Color.Gray)

    Column(
        modifier = Modifier
            .padding(bottom = 16.dp)
            .width(320.dp) // Fixed premium width prevents squishing
            .shadow(32.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.2f))
            .background(bgColor, RoundedCornerShape(24.dp))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Path Style", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color.Black)
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "Close", tint = Color.Gray) }
        }

        // Color Picker (Perfectly spaced)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color, CircleShape)
                        .border(if (connector.color == color) 3.dp else 1.dp, if (connector.color == color) Color(0xFF007AFF) else Color.LightGray.copy(alpha = 0.5f), CircleShape)
                        .clickable { onUpdate(connector.copy(color = color)) }
                )
            }
        }

        // Segments
        PremiumSegmentedControl(listOf("Solid", "Dashed", "Dotted"), connector.lineStyle, { onUpdate(connector.copy(lineStyle = it)) }, isDark)
        PremiumSegmentedControl(listOf("None", "End", "Both"), connector.arrowDirection, { onUpdate(connector.copy(arrowDirection = it)) }, isDark)

        // Destructive Action Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x1AFF3B30), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .clickable { onDelete() }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete Path", color = Color(0xFFFF3B30), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}