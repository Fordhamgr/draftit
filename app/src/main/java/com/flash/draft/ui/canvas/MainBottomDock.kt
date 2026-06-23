package com.flash.draft.ui.canvas

import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R as LucideR
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.ui.canvas.item.LocalDarkTheme

@Composable
fun MainBottomDock(
    activeTool: String,
    onToolSelect: (String) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val iconTint = if (isDark) Color.White else Color(0xFF1D1D1F)
    val activeTint = if (isDark) Color.White else Color.Black

    LiquidGlassPill {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(painterResource(id = LucideR.drawable.lucide_ic_hand), "Pan", activeTool == "Pan", iconTint, activeTint) { onToolSelect("Pan") }
            DockItem(painterResource(id = LucideR.drawable.lucide_ic_pen), "Draw", activeTool == "Draw", iconTint, activeTint) { onToolSelect("Draw") }
            DockItem(painterResource(id = LucideR.drawable.lucide_ic_type), "Text", activeTool == "Text", iconTint, activeTint) { onToolSelect("Text") }
            DockItem(painterResource(id = LucideR.drawable.lucide_ic_sticky_note), "Note", activeTool == "Note", iconTint, activeTint) { onToolSelect("Note") }
        }
    }
}

@Composable
private fun DockItem(
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    isActive: Boolean,
    tint: Color,
    activeTint: Color,
    onClick: () -> Unit
) {
    val currentTint = if (isActive) activeTint else tint

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(if (isActive) activeTint.copy(alpha = 0.15f) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = currentTint, modifier = Modifier.size(26.dp))
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = currentTint
        )
    }
}
