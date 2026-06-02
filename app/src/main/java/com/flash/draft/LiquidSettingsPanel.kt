package com.flash.draft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
fun LiquidSettingsPanel(
    alpha: Float,
    curve: Float,
    onAlphaChange: (Float) -> Unit,
    onCurveChange: (Float) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val bgColor = if (isDark) Color(0xE61C1C1E) else Color(0xF2F2F7)
    val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x4DFFFFFF)
    val textColor = if (isDark) Color.Gray else Color.DarkGray

    Column(
        modifier = Modifier
            .width(280.dp)
            .shadow(32.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.2f))
            .background(bgColor, RoundedCornerShape(24.dp))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Liquid Physics", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) Color.White else Color.Black)

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Gray.copy(alpha = 0.2f)))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Alpha", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Slider(
                value = alpha, onValueChange = onAlphaChange, valueRange = 0f..1f,
                modifier = Modifier.width(140.dp),
                colors = SliderDefaults.colors(thumbColor = Color(0xFF673AB7), activeTrackColor = Color(0xFF673AB7))
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Curve", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Slider(
                value = curve, onValueChange = onCurveChange, valueRange = 0f..1f,
                modifier = Modifier.width(140.dp),
                colors = SliderDefaults.colors(thumbColor = Color(0xFF9C27B0), activeTrackColor = Color(0xFF9C27B0))
            )
        }
    }
}