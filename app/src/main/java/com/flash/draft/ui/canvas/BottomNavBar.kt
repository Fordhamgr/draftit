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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
    isDrawToolsExpanded: Boolean,
    expandedToolsContent: @Composable () -> Unit,
    onPdfClick: () -> Unit,
    onMediaClick: () -> Unit,
    onCodeClick: () -> Unit,
    onAudioClick: () -> Unit,
    onTaskClick: () -> Unit,
    onNoteClick: () -> Unit,
    onShapeClick: () -> Unit,
    forceCenter: Boolean = false
) {
    val isDark = LocalDarkTheme.current
    val glassBg = if (isDark) Color(0x1AFFFFFF) else Color(0xCCFFFFFF)
    val glassBorder = if (isDark) Color(0x4DFFFFFF) else Color(0x66FFFFFF)

    val liquidState = LocalLiquidState.current
    val liquidRefraction = LocalLiquidRefraction.current
    val liquidCurve = LocalLiquidCurve.current
    val isPerformance = com.flash.draft.ui.canvas.item.LocalPerformanceMode.current

    // PERFECT SCALE: Lock everything to 68dp
    val dockHeight = 68.dp
    val pillShape = RoundedCornerShape(percent = 50)

    // Smoothly pushes the main dock right when the dynamic pill appears or if forced to center
    val startPadding by animateDpAsState(
        targetValue = if (showDynamicLeft || forceCenter) 84.dp else 0.dp,
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
        // --- MORPHING PILL: Circle → Expanded Tool Dock ---
        AnimatedVisibility(
            visible = showDynamicLeft,
            enter = scaleIn(tween(250)) + fadeIn(tween(250)),
            exit = scaleOut(tween(200)) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            val morphSpec = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.78f, stiffness = 400f)
            val cornerRadius by animateDpAsState(
                targetValue = if (isDrawToolsExpanded) 28.dp else dockHeight / 2,
                animationSpec = morphSpec,
                label = "cornerMorph"
            )
            val morphShape = RoundedCornerShape(cornerRadius)

            Box(
                modifier = Modifier
                    .then(if (isPerformance || isDark) Modifier else Modifier.shadow(16.dp, morphShape, clip = false, spotColor = Color.Black.copy(alpha = 0.15f)))
                    .then(
                        if (liquidState != null && !isPerformance) Modifier.liquid(liquidState) {
                            refraction = liquidRefraction; curve = liquidCurve; shape = morphShape
                        } else Modifier
                    )
                    .background(glassBg, morphShape)
                    .border(
                        width = if (isDrawToolsExpanded) 1.dp else 2.dp,
                        color = glassBorder.copy(alpha = if (isDrawToolsExpanded) 1f else 0.6f),
                        shape = morphShape
                    )
                    // .clip(morphShape) -> Removed to allow SVG tools to stick out (iOS style)
                    .clickable(
                        interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { if (!isDrawToolsExpanded) onDynamicLeftClick() }
            ) {
                AnimatedContent(
                    targetState = isDrawToolsExpanded,
                    transitionSpec = {
                        (fadeIn(tween(300, delayMillis = 150)) togetherWith fadeOut(tween(150)))
                            .using(SizeTransform(clip = false) { _, _ ->
                                spring(dampingRatio = 0.78f, stiffness = 400f)
                            })
                    },
                    label = "pillMorph"
                ) { expanded ->
                    if (expanded) {
                        expandedToolsContent()
                    } else {
                        Box(
                            modifier = Modifier.size(dockHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            dynamicLeftContent()
                        }
                    }
                }
            }
        }

        // ----------------------------------------------------
        // 2. MAIN DOCK (Center - Dynamically Shrinking)
        // ----------------------------------------------------
        AnimatedVisibility(
            visible = !isInsertMenuOpen && !isDrawToolsExpanded,
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
                    .then(if (isPerformance || isDark) Modifier else Modifier.shadow(16.dp, pillShape, spotColor = Color.Black.copy(alpha = 0.15f)))
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
        // --- + BUTTON: Collapses when draw tools are expanded ---
        val plusScale by animateFloatAsState(if (isDrawToolsExpanded) 0.5f else 1f, spring(stiffness = 400f), label = "plusScale")
        val plusAlpha by animateFloatAsState(if (isDrawToolsExpanded) 0f else 1f, tween(200), label = "plusAlpha")

        val popoverShape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .graphicsLayer {
                    alpha = plusAlpha
                    scaleX = plusScale
                    scaleY = plusScale
                    transformOrigin = TransformOrigin(1f, 1f)
                }
                .then(if (isPerformance || isDark) Modifier else Modifier.shadow(16.dp, popoverShape, spotColor = Color.Black.copy(alpha = 0.2f)))
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
                        ) { if (!isDrawToolsExpanded) onAddClick() },
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
                        onNoteClick = onNoteClick,
                        onShapeClick = onShapeClick
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
