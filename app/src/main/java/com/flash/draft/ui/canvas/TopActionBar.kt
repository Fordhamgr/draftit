package com.flash.draft.ui.canvas

import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R as LucideR
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flash.draft.ui.canvas.item.LocalDarkTheme

@Composable
fun TopActionBar(
    onBackClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val iconTint = if (isDark) Color.White else Color(0xFF1D1D1F)
    val dividerColor = iconTint.copy(alpha = 0.15f)

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. The Back Button Pill
        LiquidGlassPill {
            IconButton(onClick = onBackClick, modifier = Modifier.size(32.dp)) {
                Icon(painterResource(id = LucideR.drawable.lucide_ic_arrow_left), contentDescription = "Back", tint = iconTint)
            }
        }

        // 2. The Undo/Redo Dock
        LiquidGlassPill {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onUndoClick, modifier = Modifier.size(32.dp)) {
                    Icon(painterResource(id = LucideR.drawable.lucide_ic_undo), contentDescription = "Undo", tint = iconTint)
                }

                // Vertical Divider
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(dividerColor))

                IconButton(onClick = onRedoClick, modifier = Modifier.size(32.dp)) {
                    Icon(painterResource(id = LucideR.drawable.lucide_ic_redo), contentDescription = "Redo", tint = iconTint)
                }
            }
        }
    }
}