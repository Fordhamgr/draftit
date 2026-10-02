package com.flash.draft.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Standard gesture-driven motion. 
 * Used for drag-and-drop, scale, and interruptible user gestures.
 */
fun <T> springGesture() = spring<T>(
    dampingRatio = 0.8f,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * Standard transition motion.
 * Used for navigation, reveals, and UI-triggered state changes.
 */
fun <T> tweenTransition() = tween<T>(
    durationMillis = 250,
    easing = FastOutSlowInEasing
)

/**
 * Elite interaction feedback modifier.
 * Implements the 0.96 scale-down spring and haptic feedback as mandated by ui design rules.md.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.premiumClickable(
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = springGesture(),
        label = "premium_press_scale",
        finishedListener = { 
            if (isPressed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
            onLongClick = onLongClick?.let { {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                it()
            } }
        )
}
