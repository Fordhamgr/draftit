package com.flash.draft.ui.onboarding.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft99.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Timing constants for ScenesHero animations.
 */
object ScenesHeroTokens {
    const val POP_DURATION = 600
    val POP_EASING = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.2f)

    const val CARD_POP_DELAY = 100
    const val THUMBNAILS_BASE_DELAY = 500
    const val THUMBNAILS_STEP_DELAY = 120

    const val LOOP_DURATION = 6000
    const val TOTAL_SCENES = 3

    val AmberUnderline = Color(0xFFF59E0B)
    val TextSecondary = Color(0xFF6B7280)
    val SkeletonLight = Color(0xFFE5E7EB)
    val SkeletonMuted = Color(0xFFD1D5DB)
    val CardHairlineBorder = Color(0x0D000000)

    val ThumbnailOrigins = listOf(
        Offset(24f, 240f),
        Offset(132f, 240f),
        Offset(240f, 240f)
    )
}

/**
 * Page 3: ScenesHero
 * Features an active sketch drawing card with an animated flowing curve and amber underline,
 * and 3 scene thumbnails with skeleton layouts and a cycling selection ring.
 */
@Composable
fun ScenesHero(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false,
    previewProgress: Float? = if (LocalInspectionMode.current) 1f else null
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Pop-in animatables
    val cardProgress = remember { Animatable(0f) }
    val thumbAnimatables = remember {
        List(ScenesHeroTokens.TOTAL_SCENES) { Animatable(0f) }
    }

    LaunchedEffect(isActive, isReducedMotion, previewProgress) {
        if (previewProgress != null || isReducedMotion) {
            cardProgress.snapTo(1f)
            thumbAnimatables.forEach { it.snapTo(1f) }
            return@LaunchedEffect
        }

        if (isActive) {
            launch {
                delay(ScenesHeroTokens.CARD_POP_DELAY.toLong())
                cardProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = ScenesHeroTokens.POP_DURATION,
                        easing = ScenesHeroTokens.POP_EASING
                    )
                )
            }

            thumbAnimatables.forEachIndexed { index, animatable ->
                launch {
                    val delayMs = ScenesHeroTokens.THUMBNAILS_BASE_DELAY + index * ScenesHeroTokens.THUMBNAILS_STEP_DELAY
                    delay(delayMs.toLong())
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = ScenesHeroTokens.POP_DURATION,
                            easing = ScenesHeroTokens.POP_EASING
                        )
                    )
                }
            }
        }
    }

    // 6s infinite loop for drawing strokes & thumbnail selection ring
    val infiniteTransition = rememberInfiniteTransition(label = "scenes_loop")
    val rawLoopProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(ScenesHeroTokens.LOOP_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scenes_6s_loop"
    )

    val isLoopingActive = isActive && !isReducedMotion && previewProgress == null
    val effectiveLoop = if (isLoopingActive) rawLoopProgress else 0.5f

    // 1. Curve drawing progress (0% to 40% of 6s loop)
    val curveDrawProgress = when {
        previewProgress != null || isReducedMotion -> 1f
        effectiveLoop < 0.40f -> (effectiveLoop / 0.40f).coerceIn(0f, 1f)
        else -> 1f
    }

    // 2. Underline drawing progress (35% to 55% of 6s loop)
    val underlineDrawProgress = when {
        previewProgress != null || isReducedMotion -> 1f
        effectiveLoop < 0.35f -> 0f
        effectiveLoop < 0.55f -> ((effectiveLoop - 0.35f) / 0.20f).coerceIn(0f, 1f)
        else -> 1f
    }

    // 3. Stroke fade out from 85% to 95%
    val strokesAlpha = when {
        previewProgress != null || isReducedMotion -> 1f
        effectiveLoop < 0.85f -> 1f
        effectiveLoop < 0.95f -> 1f - (effectiveLoop - 0.85f) / 0.10f
        else -> 0f
    }

    // Zero-allocation Path and PathMeasure for strokes
    val fullCurvePath = remember { Path() }
    val trimmedCurvePath = remember { Path() }
    val fullUnderlinePath = remember { Path() }
    val trimmedUnderlinePath = remember { Path() }
    val pathMeasure = remember { PathMeasure() }

    val sceneLabels = listOf(
        stringResource(R.string.onboarding_scene_1),
        stringResource(R.string.onboarding_scene_2),
        stringResource(R.string.onboarding_scene_3)
    )

    OnboardingHeroContainer(
        contentDescriptionText = stringResource(R.string.onboarding_scenes_cd),
        modifier = modifier
    ) {
        // --- 1. DRAWING CARD (24, 24, 310x196) ---
        val cProgress = if (previewProgress != null || isReducedMotion) 1f else cardProgress.value
        Box(
            modifier = Modifier
                .offset(24.dp, 24.dp)
                .size(310.dp, 196.dp)
                .graphicsLayer {
                    alpha = cProgress
                    scaleX = 0.85f + 0.15f * cProgress
                    scaleY = 0.85f + 0.15f * cProgress
                    translationY = (8.dp * (1f - cProgress)).toPx()
                }
                .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = Color(0x1F000000))
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .border(1.dp, ScenesHeroTokens.CardHairlineBorder, RoundedCornerShape(22.dp))
        ) {
            // Inside: Two animated 4dp round-cap strokes
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = strokesAlpha }
            ) {
                val strokeWidthPx = 4.dp.toPx()

                // Flowing primary curve
                if (curveDrawProgress > 0f) {
                    fullCurvePath.rewind()
                    fullCurvePath.moveTo(42.dp.toPx(), 115.dp.toPx())
                    fullCurvePath.cubicTo(
                        95.dp.toPx(), 45.dp.toPx(),
                        185.dp.toPx(), 165.dp.toPx(),
                        268.dp.toPx(), 75.dp.toPx()
                    )

                    trimmedCurvePath.rewind()
                    pathMeasure.setPath(fullCurvePath, false)
                    pathMeasure.getSegment(0f, pathMeasure.length * curveDrawProgress, trimmedCurvePath, true)
                    drawPath(
                        path = trimmedCurvePath,
                        color = primaryColor,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Short amber underline below it
                if (underlineDrawProgress > 0f) {
                    fullUnderlinePath.rewind()
                    fullUnderlinePath.moveTo(85.dp.toPx(), 148.dp.toPx())
                    fullUnderlinePath.quadraticTo(
                        140.dp.toPx(), 152.dp.toPx(),
                        195.dp.toPx(), 146.dp.toPx()
                    )

                    trimmedUnderlinePath.rewind()
                    pathMeasure.setPath(fullUnderlinePath, false)
                    pathMeasure.getSegment(0f, pathMeasure.length * underlineDrawProgress, trimmedUnderlinePath, true)
                    drawPath(
                        path = trimmedUnderlinePath,
                        color = ScenesHeroTokens.AmberUnderline,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // --- 2. THREE SCENE THUMBNAILS (94x64 at y = 240dp) ---
        ScenesHeroTokens.ThumbnailOrigins.forEachIndexed { index, origin ->
            val animatable = thumbAnimatables[index]
            val progress = if (previewProgress != null || isReducedMotion) 1f else animatable.value

            val thumbStart = index / 3f
            val thumbEnd = (index + 1) / 3f
            val isInWindow = effectiveLoop in thumbStart..thumbEnd
            val ringAlpha = if (isLoopingActive && isInWindow) 1f else if (previewProgress != null && index == 0) 1f else 0f

            Box(
                modifier = Modifier
                    .offset(origin.x.dp, origin.y.dp)
                    .graphicsLayer {
                        alpha = progress
                        scaleX = 0.85f + 0.15f * progress
                        scaleY = 0.85f + 0.15f * progress
                        translationY = (8.dp * (1f - progress)).toPx()
                    }
            ) {
                // Selection Ring (offset -2dp, 16dp radius, 2dp theme border)
                if (ringAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .offset((-2).dp, (-2).dp)
                            .size(98.dp, 68.dp)
                            .graphicsLayer { alpha = ringAlpha }
                            .border(2.dp, primaryColor, RoundedCornerShape(16.dp))
                    )
                }

                // Thumbnail Card (94x64, 14dp radius)
                Box(
                    modifier = Modifier
                        .size(94.dp, 64.dp)
                        .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = Color(0x1F000000))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, ScenesHeroTokens.CardHairlineBorder, RoundedCornerShape(14.dp))
                        .padding(8.dp)
                ) {
                    when (index) {
                        0 -> Scene1Skeleton()
                        1 -> Scene2Skeleton(primaryColor)
                        2 -> Scene3Skeleton()
                    }
                }

                // Label 8dp below thumbnail ("Scene 1/2/3", 11sp semibold #6B7280)
                Text(
                    text = sceneLabels[index],
                    color = ScenesHeroTokens.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .offset(y = 72.dp)
                        .width(94.dp)
                )
            }
        }
    }
}

/** Scene 1 Thumbnail: Note skeleton + Mini Image */
@Composable
private fun Scene1Skeleton() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp, 18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFEF3C7))
            )
            Box(
                modifier = Modifier
                    .size(36.dp, 18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ScenesHeroTokens.SkeletonLight)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.70f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ScenesHeroTokens.SkeletonMuted)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.50f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ScenesHeroTokens.SkeletonLight)
        )
    }
}

/** Scene 2 Thumbnail: Tasks checklist skeleton */
@Composable
private fun Scene2Skeleton(primaryColor: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(primaryColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ScenesHeroTokens.SkeletonLight)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(primaryColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.60f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ScenesHeroTokens.SkeletonLight)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .border(1.dp, ScenesHeroTokens.SkeletonMuted, RoundedCornerShape(2.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.70f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ScenesHeroTokens.SkeletonLight)
            )
        }
    }
}

/** Scene 3 Thumbnail: Dark code snippet skeleton + curve */
@Composable
private fun Scene3Skeleton() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF111827))
                .padding(4.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.60f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0xFF67E8F9))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.40f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0xFFFBBF24))
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ScenesHeroTokens.SkeletonMuted)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScenesHeroPreview() {
    MaterialTheme {
        Surface {
            ScenesHero(isActive = true, previewProgress = 1f)
        }
    }
}
