package com.flash.draft.ui.canvas

import androidx.compose.ui.res.painterResource
import com.composables.icons.lucide.R as LucideR
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft.ui.canvas.item.LocalDarkTheme
import io.github.fletchmckee.liquid.liquid

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import com.flash.draft.ui.canvas.tools.InsertToolStack

@Composable
fun BottomNavBar(
    activeTool: String,
    onToolSelect: (String) -> Unit,
    isInsertMenuOpen: Boolean,
    onAddClick: () -> Unit,
    showDynamicLeft: Boolean,
    onDynamicLeftClick: () -> Unit,
    dynamicLeftContent: @Composable () -> Unit,
    onPdfClick: () -> Unit,
    onMediaClick: () -> Unit,
    onCodeClick: () -> Unit,
    onAudioClick: () -> Unit,
    onTaskClick: () -> Unit,
    onNoteClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val glassBg = if (isDark) Color(0xCC1A1A1A) else Color(0xCCFFFFFF)
    val glassBorder = if (isDark) Color(0x4DFFFFFF) else Color(0x66FFFFFF)

    val liquidState = LocalLiquidState.current
    val liquidRefraction = LocalLiquidRefraction.current
    val liquidCurve = LocalLiquidCurve.current
    val isPerformance = com.flash.draft.ui.canvas.item.LocalPerformanceMode.current

    // PERFECT SCALE: Lock everything to 68dp
    val dockHeight = 68.dp
    val pillShape = RoundedCornerShape(percent = 50)

    // Smoothly pushes the main dock right when the dynamic pill appears
    val startPadding by animateDpAsState(
        targetValue = if (showDynamicLeft) 84.dp else 0.dp,
        animationSpec = tween(300),
        label = "dockSlide"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
            .pointerInput(Unit) { detectDragGestures { _, _ -> } }
    ) {

        // ----------------------------------------------------
        // 1. DYNAMIC PILL (Bottom Left - Perfect Circle)
        // ----------------------------------------------------
        AnimatedVisibility(
            visible = showDynamicLeft,
            enter = scaleIn(tween(250)) + fadeIn(tween(250)),
            exit = scaleOut(tween(200)) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Box(
                modifier = Modifier
                    .size(dockHeight)
                    .then(if (isPerformance) Modifier else Modifier.shadow(16.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.15f)))
                    .then(
                        if (liquidState != null && !isPerformance) Modifier.liquid(liquidState) {
                            refraction = liquidRefraction; curve = liquidCurve; shape = CircleShape
                        } else Modifier
                    )
                    .background(glassBg, CircleShape)
                    .border(2.dp, glassBorder.copy(alpha = 0.6f), CircleShape)
                    .clip(CircleShape)
                    .clickable { onDynamicLeftClick() },
                contentAlignment = Alignment.Center
            ) {
                dynamicLeftContent()
            }
        }

        // ----------------------------------------------------
        // 2. MAIN DOCK (Center - Dynamically Shrinking)
        // ----------------------------------------------------
        AnimatedVisibility(
            visible = !isInsertMenuOpen,
            enter = scaleIn(spring(stiffness = 400f)) + fadeIn(),
            exit = scaleOut(spring(stiffness = 400f)) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .padding(start = startPadding, end = 84.dp) // 84dp avoids the + button on the right
                    .widthIn(max = 400.dp) // Stops it from getting excessively wide on tablets
                    .fillMaxWidth() // Automatically shrinks and expands to fill available padded space
                    .height(dockHeight)
                    .then(if (isPerformance) Modifier else Modifier.shadow(16.dp, pillShape, spotColor = Color.Black.copy(alpha = 0.15f)))
                    .then(
                        if (liquidState != null && !isPerformance) Modifier.liquid(liquidState) {
                            refraction = liquidRefraction; curve = liquidCurve; shape = pillShape
                        } else Modifier
                    )
                    .background(glassBg, pillShape)
                    .border(1.dp, glassBorder, pillShape)
                    .clip(pillShape)
            ) {
                Row(
                    // Padding ensures the first and last icons don't touch the pill edge
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // weight(1f) ensures absolute perfect symmetry. They share the space equally.
                    DockItem(Modifier.weight(1f), painterResource(id = LucideR.drawable.lucide_ic_hand), "Pan", activeTool == "Pan") { onToolSelect("Pan") }
                    DockItem(Modifier.weight(1f), painterResource(id = LucideR.drawable.lucide_ic_pen), "Draw", activeTool == "Draw") { onToolSelect("Draw") }
                    DockItem(Modifier.weight(1f), painterResource(id = LucideR.drawable.lucide_ic_type), "Text", activeTool == "Text") { onToolSelect("Text") }
                }
            }
        }

        // ----------------------------------------------------
        // 3. SEPARATE '+' PILL (Bottom Right)
        // ----------------------------------------------------
        val popoverShape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .then(if (isPerformance) Modifier else Modifier.shadow(16.dp, popoverShape, spotColor = Color.Black.copy(alpha = 0.2f)))
                .then(
                    if (liquidState != null && !isPerformance) Modifier.liquid(liquidState) {
                        refraction = liquidRefraction; curve = liquidCurve; shape = popoverShape
                    } else Modifier
                )
                .background(glassBg, popoverShape)
                .border(1.dp, glassBorder, popoverShape)
                .clip(popoverShape)
                .animateContentSize(animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f)),
            contentAlignment = Alignment.Center
        ) {
            if (!isInsertMenuOpen) {
                Box(
                    modifier = Modifier
                        .size(dockHeight)
                        .clickable(
                            interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = LucideR.drawable.lucide_ic_plus),
                        contentDescription = "Insert Menu",
                        tint = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    InsertToolStack(
                        onPdfClick = onPdfClick,
                        onMediaClick = onMediaClick,
                        onCodeClick = onCodeClick,
                        onAudioClick = onAudioClick,
                        onTaskClick = onTaskClick,
                        onNoteClick = onNoteClick
                    )
                    // The collapse button at the bottom right corner (where the + button was)
                    Box(
                        modifier = Modifier
                            .size(dockHeight)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { onAddClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = LucideR.drawable.lucide_ic_x),
                            contentDescription = "Close Menu",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val activeTint = if (isDark) Color.White else Color.Black
    val inactiveTint = if (isDark) Color.LightGray else Color.DarkGray
    val currentTint = if (isActive) activeTint else inactiveTint
    val bgTint = if (isActive) activeTint.copy(alpha = 0.15f) else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(bgTint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = currentTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = currentTint
        )
    }
}