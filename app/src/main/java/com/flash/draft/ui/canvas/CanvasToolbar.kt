package com.flash.draft.ui.canvas

import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R as LucideR
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.unit.sp

import io.github.fletchmckee.liquid.liquid

@Composable
fun CanvasToolbar(
    isDrawingMode: Boolean, isConnectingMode: Boolean, isLassoMode: Boolean, isDarkTheme: Boolean,
    onPanClick: () -> Unit, onDrawClick: () -> Unit, onLassoClick: () -> Unit, onConnectClick: () -> Unit,
    onStickyClick: () -> Unit, onImageClick: () -> Unit, onPdfClick: () -> Unit, onCodeClick: () -> Unit,
    onMicClick: () -> Unit, onShapeClick: () -> Unit, onTextClick: () -> Unit, onFrameClick: () -> Unit,
    onTaskListClick: () -> Unit, onAutoLayoutClick: () -> Unit, onUndoClick: () -> Unit, onRedoClick: () -> Unit,
    onThemeToggle: () -> Unit, onShareClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(if (isExpanded) 1f else 0.8f, tween(250, easing = FastOutSlowInEasing), label = "scale")
    val alpha by animateFloatAsState(if (isExpanded) 1f else 0f, tween(200), label = "alpha")
    val offsetY by animateDpAsState(if (isExpanded) 0.dp else 16.dp, tween(250, easing = FastOutSlowInEasing), label = "offset")
    val fabRotation by animateFloatAsState(if (isExpanded) 45f else 0f, tween(250, easing = FastOutSlowInEasing), label = "rotation")

    val glassBg = if (isDarkTheme) Color(0x1AFFFFFF) else Color(0xCCFFFFFF)
    val glassBorder = if (isDarkTheme) Color(0x33FFFFFF) else Color(0x4DFFFFFF)
    val iconTint = if (isDarkTheme) Color.White else Color(0xFF1D1D1F)
    val activeTint = if (isDarkTheme) Color.White else Color.Black

    // --- GRAB DYNAMIC LIQUID STATE ---
    val liquidState = LocalLiquidState.current
    val liquidRefraction = LocalLiquidRefraction.current
    val liquidCurve = LocalLiquidCurve.current
    val liquidFrost = LocalLiquidFrost.current

    val menuLiquidModifier = if (liquidState != null) {
        Modifier.liquid(liquidState) {
            refraction = liquidRefraction
            curve = liquidCurve
            if (liquidFrost > 0f) frost = (liquidFrost * 24f).dp
            shape = RoundedCornerShape(24.dp)
        }
    } else Modifier

    val fabLiquidModifier = if (liquidState != null) {
        Modifier.liquid(liquidState) {
            refraction = liquidRefraction
            curve = liquidCurve
            if (liquidFrost > 0f) frost = (liquidFrost * 24f).dp
            shape = CircleShape
        }
    } else Modifier

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (alpha > 0f) {
            Column(
                modifier = Modifier
                    .width(180.dp)
                    .graphicsLayer {
                        scaleX = scale; scaleY = scale; this.alpha = alpha
                        translationY = offsetY.toPx()
                        transformOrigin = TransformOrigin(0.9f, 1f)
                    }
                    .then(if (isDarkTheme) Modifier.graphicsLayer() else Modifier.shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.15f)))
                    .then(menuLiquidModifier)
                    .background(glassBg, RoundedCornerShape(24.dp))
                    .border(1.dp, glassBorder, RoundedCornerShape(24.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                @Composable
                fun MenuRow(icon: androidx.compose.ui.graphics.painter.Painter, label: String, isActive: Boolean = false, onClick: () -> Unit) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick(); isExpanded = false }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = label, color = if (isActive) activeTint else iconTint, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isActive) activeTint.copy(alpha = 0.15f) else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(painter = icon, contentDescription = label, tint = if (isActive) activeTint else iconTint, modifier = Modifier.size(20.dp)) }
                    }
                }

                MenuRow(painterResource(id = LucideR.drawable.lucide_ic_hand), "Pan", !isDrawingMode && !isConnectingMode && !isLassoMode) { onPanClick() }
                MenuRow(painterResource(id = LucideR.drawable.lucide_ic_sticky_note), "Note", false) { onStickyClick() }
                MenuRow(painterResource(id = LucideR.drawable.lucide_ic_type), "Text", false) { onTextClick() }
                MenuRow(painterResource(id = LucideR.drawable.lucide_ic_pen), "Draw", isDrawingMode) { onDrawClick() }
                MenuRow(painterResource(id = LucideR.drawable.lucide_ic_arrow_right), "Connect", isConnectingMode) { onConnectClick() }
                MenuRow(painterResource(id = LucideR.drawable.lucide_ic_image), "Media", false) { onImageClick() }
                MenuRow(painterResource(id = LucideR.drawable.lucide_ic_mic), "Audio", false) { onMicClick() }
                MenuRow(painterResource(id = LucideR.drawable.lucide_ic_code), "Code", false) { onCodeClick() }
                MenuRow(painterResource(id = LucideR.drawable.lucide_ic_list_todo), "Task", false) { onTaskListClick() }

                Box(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().height(1.dp).background(iconTint.copy(alpha = 0.1f)))

                MenuRow(painterResource(id = LucideR.drawable.lucide_ic_share), "Export", false) { onShareClick() }
            }
        }

        Box(
            modifier = Modifier
                .size(60.dp)
                .then(if (isDarkTheme) Modifier.graphicsLayer() else Modifier.shadow(16.dp, CircleShape, spotColor = if (isExpanded) activeTint else Color.Black))
                .then(fabLiquidModifier)
                .background(if (isExpanded) activeTint else glassBg, CircleShape)
                .border(1.dp, if (isExpanded) Color.Transparent else glassBorder, CircleShape)
                .clip(CircleShape)
                .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); isExpanded = !isExpanded },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = LucideR.drawable.lucide_ic_plus),
                contentDescription = "Menu",
                tint = if (isExpanded) Color.White else iconTint,
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer { rotationZ = fabRotation }
            )
        }
    }
}
