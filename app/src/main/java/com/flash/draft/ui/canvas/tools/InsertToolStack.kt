package com.flash.draft.ui.canvas.tools

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Category
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.flash.draft.ui.canvas.item.LocalDarkTheme

@Composable
fun InsertToolStack(
    onPdfClick: () -> Unit,
    onMediaClick: () -> Unit,
    onCodeClick: () -> Unit,
    onAudioClick: () -> Unit,
    onTaskClick: () -> Unit,
    onNoteClick: () -> Unit,
    onShapeClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val textColor = if (isDark) Color.White else Color.Black
    val subtitleColor = if (isDark) Color.Gray else Color.DarkGray

    val animatable1 = remember { Animatable(0f) }
    val animatable2 = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // Bottom section (Media) animates first
        launch {
            animatable2.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 400f))
        }
        // Top section (Text) animates shortly after
        launch {
            delay(60)
            animatable1.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 400f))
        }
    }

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section 1: Text & Blocks
        Column(
            modifier = Modifier.graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
                rotationX = 90f * (1f - animatable1.value)
                alpha = animatable1.value.coerceIn(0f, 1f)
                cameraDistance = 8f * density
            },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Text & Blocks", fontSize = 12.sp, color = subtitleColor, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                GridItem(Icons.Default.StickyNote2, "Note", onNoteClick, textColor)
                GridItem(Icons.AutoMirrored.Filled.List, "Task List", onTaskClick, textColor)
                GridItem(Icons.Default.Code, "Code", onCodeClick, textColor)
                GridItem(Icons.Default.Category, "Shape", onShapeClick, textColor)
            }
        }
        
        // Section 2: Media & Files
        Column(
            modifier = Modifier.graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
                rotationX = 90f * (1f - animatable2.value)
                alpha = animatable2.value.coerceIn(0f, 1f)
                cameraDistance = 8f * density
            },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Media & Files", fontSize = 12.sp, color = subtitleColor, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                GridItem(Icons.Default.Image, "Image", onMediaClick, textColor)
                GridItem(Icons.Default.PictureAsPdf, "PDF", onPdfClick, textColor)
                GridItem(Icons.Default.Mic, "Audio", onAudioClick, textColor)
            }
        }
    }
}

@Composable
private fun GridItem(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color) {
    val isDark = LocalDarkTheme.current
    val bgTint = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(bgTint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}
