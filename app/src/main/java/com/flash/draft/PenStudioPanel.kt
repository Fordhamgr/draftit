package com.flash.draft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.ui.canvas.item.LocalDarkTheme

@Composable
fun PenStudioPanel(
    activeDrawColor: Color, activeStrokeWidth: Float, activeToolType: String, activeAlpha: Float,
    onColorChange: (Color) -> Unit, onStrokeWidthChange: (Float) -> Unit, onToolChange: (String) -> Unit, onAlphaChange: (Float) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val bgColor = if (isDark) Color(0xE61C1C1E) else Color(0xF2F2F7)
    val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)
    val textColor = if (isDark) Color.Gray else Color.DarkGray
    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color(0xFF34C759), Color(0xFFFF9500), Color.White)

    Column(
        modifier = Modifier
            .shadow(32.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.2f))
            .background(bgColor, RoundedCornerShape(24.dp))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Tools Segment
        PremiumSegmentedControl(listOf("Pen", "Highlighter", "Eraser"), activeToolType, onToolChange, isDark)

        // Colors
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier.size(32.dp).background(color, CircleShape)
                        .border(if (activeDrawColor == color) 3.dp else 1.dp, if (activeDrawColor == color) Color(0xFF007AFF) else Color.LightGray.copy(alpha=0.5f), CircleShape)
                        .clickable { onColorChange(color) }
                )
            }
        }

        // Thickness Slider
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Size", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Slider(
                value = activeStrokeWidth, onValueChange = onStrokeWidthChange, valueRange = 2f..40f,
                modifier = Modifier.width(180.dp),
                colors = SliderDefaults.colors(thumbColor = Color(0xFF007AFF), activeTrackColor = Color(0xFF007AFF))
            )
        }
    }
}