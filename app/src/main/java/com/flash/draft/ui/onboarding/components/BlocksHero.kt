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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft99.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Timing constants for BlocksHero animations.
 */
object BlocksHeroTokens {
    const val POP_DURATION = 600
    val POP_EASING = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.2f)

    const val POP_BASE_DELAY = 100
    const val POP_STEP_DELAY = 120

    const val RING_LOOP_DURATION = 7200
    const val TOTAL_TILES = 6

    val SlateIcon = Color(0xFF475569)
    val TextPrimary = Color(0xFF111827)
    val CardHairlineBorder = Color(0x0D000000)

    val TileOrigins = listOf(
        // Row 1 (y = 44dp)
        Offset(17f, 44f),
        Offset(129f, 44f),
        Offset(241f, 44f),
        // Row 2 (y = 176dp)
        Offset(17f, 176f),
        Offset(129f, 176f),
        Offset(241f, 176f)
    )
}

private enum class ContentTileType {
    NOTE, TASKS, CODE, IMAGE, PDF, AUDIO
}

/**
 * Page 2: BlocksHero
 * Renders 6 modular content tiles in a 3x2 grid with line icons and labels,
 * staggered overshoot pop-ins, and a cycling selection ring that loops through each tile.
 */
@Composable
fun BlocksHero(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false,
    isLandscape: Boolean = false,
    previewProgress: Float? = if (LocalInspectionMode.current) 1f else null
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Animatable progress for each of the 6 tiles
    val tileAnimatables = remember {
        List(BlocksHeroTokens.TOTAL_TILES) { Animatable(0f) }
    }

    LaunchedEffect(isActive, isReducedMotion, previewProgress) {
        if (previewProgress != null || isReducedMotion) {
            tileAnimatables.forEach { it.snapTo(1f) }
            return@LaunchedEffect
        }

        if (isActive) {
            tileAnimatables.forEachIndexed { index, animatable ->
                launch {
                    val delayMs = BlocksHeroTokens.POP_BASE_DELAY + index * BlocksHeroTokens.POP_STEP_DELAY
                    delay(delayMs.toLong())
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = BlocksHeroTokens.POP_DURATION,
                            easing = BlocksHeroTokens.POP_EASING
                        )
                    )
                }
            }
        }
    }

    // Infinite transition for cycling selection ring (7.2s cycle)
    val infiniteTransition = rememberInfiniteTransition(label = "blocks_ring_transition")
    val rawRingLoop by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(BlocksHeroTokens.RING_LOOP_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_progress"
    )

    val isLoopingActive = isActive && !isReducedMotion && previewProgress == null
    val effectiveLoop = if (isLoopingActive) rawRingLoop else 0f

    val tileTitles = listOf(
        stringResource(R.string.onboarding_tile_note),
        stringResource(R.string.onboarding_tile_tasks),
        stringResource(R.string.onboarding_tile_code),
        stringResource(R.string.onboarding_tile_image),
        stringResource(R.string.onboarding_tile_pdf),
        stringResource(R.string.onboarding_tile_audio)
    )

    val tileTypes = listOf(
        ContentTileType.NOTE,
        ContentTileType.TASKS,
        ContentTileType.CODE,
        ContentTileType.IMAGE,
        ContentTileType.PDF,
        ContentTileType.AUDIO
    )

    OnboardingHeroContainer(
        contentDescriptionText = stringResource(R.string.onboarding_blocks_cd),
        isLandscape = isLandscape,
        modifier = modifier
    ) {
        // Render each of the 6 tiles
        BlocksHeroTokens.TileOrigins.forEachIndexed { index, origin ->
            val animatable = tileAnimatables[index]
            val progress = if (previewProgress != null || isReducedMotion) 1f else animatable.value

            val sliceStart = index / 6f
            val sliceEnd = (index + 1) / 6f
            val isInSlice = effectiveLoop in sliceStart..sliceEnd
            val sliceT = if (isInSlice) (effectiveLoop - sliceStart) / (1f / 6f) else 0f

            val ringAlpha = if (isLoopingActive && isInSlice) {
                when {
                    sliceT < 0.167f -> sliceT / 0.167f
                    sliceT <= 0.917f -> 1f
                    else -> 1f - (sliceT - 0.917f) / 0.083f
                }
            } else 0f

            Box(
                modifier = Modifier
                    .offset(origin.x.dp, origin.y.dp)
                    .size(100.dp, 120.dp)
                    .graphicsLayer {
                        alpha = progress
                        scaleX = 0.85f + 0.15f * progress
                        scaleY = 0.85f + 0.15f * progress
                        translationY = (8.dp * (1f - progress)).toPx()
                    }
            ) {
                // Outer Selection Ring (offset -2dp, width 104dp, height 124dp, 20dp radius, 2dp border)
                if (ringAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .offset((-2).dp, (-2).dp)
                            .size(104.dp, 124.dp)
                            .graphicsLayer { alpha = ringAlpha }
                            .border(2.dp, primaryColor, RoundedCornerShape(20.dp))
                    )
                }

                // Tile Card
                Box(
                    modifier = Modifier
                        .size(100.dp, 120.dp)
                        .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = Color(0x1F000000))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, BlocksHeroTokens.CardHairlineBorder, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 32dp Line Icon
                        TileLineIcon(
                            type = tileTypes[index],
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 13sp Semibold Label
                        Text(
                            text = tileTitles[index],
                            color = BlocksHeroTokens.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 32dp Custom Line Icon for each content type with 1.6dp stroke in slate (#475569).
 */
@Composable
private fun TileLineIcon(
    type: ContentTileType,
    modifier: Modifier = Modifier
) {
    val strokeColor = BlocksHeroTokens.SlateIcon

    Canvas(modifier = modifier) {
        val strokeWidth = 1.6.dp.toPx()
        val w = size.width
        val h = size.height

        when (type) {
            ContentTileType.NOTE -> {
                // Sticky Note outline with folded bottom-right corner
                val notePath = Path().apply {
                    moveTo(w * 0.18f, h * 0.15f)
                    lineTo(w * 0.82f, h * 0.15f)
                    lineTo(w * 0.82f, h * 0.65f)
                    lineTo(w * 0.62f, h * 0.85f)
                    lineTo(w * 0.18f, h * 0.85f)
                    close()
                }
                drawPath(notePath, strokeColor, style = Stroke(strokeWidth, join = StrokeJoin.Round))

                // Corner fold
                val foldPath = Path().apply {
                    moveTo(w * 0.62f, h * 0.65f)
                    lineTo(w * 0.82f, h * 0.65f)
                    moveTo(w * 0.62f, h * 0.65f)
                    lineTo(w * 0.62f, h * 0.85f)
                }
                drawPath(foldPath, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round))

                // Note text skeleton lines
                drawLine(
                    strokeColor,
                    start = Offset(w * 0.30f, h * 0.35f),
                    end = Offset(w * 0.70f, h * 0.35f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    strokeColor,
                    start = Offset(w * 0.30f, h * 0.50f),
                    end = Offset(w * 0.55f, h * 0.50f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            ContentTileType.TASKS -> {
                // Checklist with checkbox and lines
                // Row 1
                drawRoundRect(
                    strokeColor,
                    topLeft = Offset(w * 0.18f, h * 0.22f),
                    size = Size(w * 0.22f, h * 0.22f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(strokeWidth)
                )
                val checkPath = Path().apply {
                    moveTo(w * 0.22f, h * 0.33f)
                    lineTo(w * 0.27f, h * 0.38f)
                    lineTo(w * 0.36f, h * 0.27f)
                }
                drawPath(checkPath, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(
                    strokeColor,
                    start = Offset(w * 0.50f, h * 0.33f),
                    end = Offset(w * 0.82f, h * 0.33f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Row 2
                drawRoundRect(
                    strokeColor,
                    topLeft = Offset(w * 0.18f, h * 0.58f),
                    size = Size(w * 0.22f, h * 0.22f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(strokeWidth)
                )
                drawLine(
                    strokeColor,
                    start = Offset(w * 0.50f, h * 0.69f),
                    end = Offset(w * 0.75f, h * 0.69f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            ContentTileType.CODE -> {
                // < / > Code symbol
                // Left bracket <
                val leftBracket = Path().apply {
                    moveTo(w * 0.36f, h * 0.30f)
                    lineTo(w * 0.18f, h * 0.50f)
                    lineTo(w * 0.36f, h * 0.70f)
                }
                drawPath(leftBracket, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Slash /
                drawLine(
                    strokeColor,
                    start = Offset(w * 0.56f, h * 0.25f),
                    end = Offset(w * 0.44f, h * 0.75f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )

                // Right bracket >
                val rightBracket = Path().apply {
                    moveTo(w * 0.64f, h * 0.30f)
                    lineTo(w * 0.82f, h * 0.50f)
                    lineTo(w * 0.64f, h * 0.70f)
                }
                drawPath(rightBracket, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            ContentTileType.IMAGE -> {
                // Frame rectangle with rounded corners
                drawRoundRect(
                    strokeColor,
                    topLeft = Offset(w * 0.15f, h * 0.20f),
                    size = Size(w * 0.70f, h * 0.60f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(strokeWidth)
                )
                // Sun
                drawCircle(
                    strokeColor,
                    radius = 2.5.dp.toPx(),
                    center = Offset(w * 0.36f, h * 0.38f),
                    style = Stroke(strokeWidth)
                )
                // Mountain
                val mtnPath = Path().apply {
                    moveTo(w * 0.20f, h * 0.72f)
                    lineTo(w * 0.45f, h * 0.48f)
                    lineTo(w * 0.60f, h * 0.60f)
                    lineTo(w * 0.70f, h * 0.52f)
                    lineTo(w * 0.80f, h * 0.68f)
                }
                drawPath(mtnPath, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            ContentTileType.PDF -> {
                // PDF Document outline
                val docPath = Path().apply {
                    moveTo(w * 0.22f, h * 0.15f)
                    lineTo(w * 0.60f, h * 0.15f)
                    lineTo(w * 0.78f, h * 0.33f)
                    lineTo(w * 0.78f, h * 0.85f)
                    lineTo(w * 0.22f, h * 0.85f)
                    close()
                }
                drawPath(docPath, strokeColor, style = Stroke(strokeWidth, join = StrokeJoin.Round))
                // Document Fold
                val fold = Path().apply {
                    moveTo(w * 0.60f, h * 0.15f)
                    lineTo(w * 0.60f, h * 0.33f)
                    lineTo(w * 0.78f, h * 0.33f)
                }
                drawPath(fold, strokeColor, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                // Inner lines
                drawLine(
                    strokeColor,
                    start = Offset(w * 0.35f, h * 0.52f),
                    end = Offset(w * 0.65f, h * 0.52f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    strokeColor,
                    start = Offset(w * 0.35f, h * 0.66f),
                    end = Offset(w * 0.55f, h * 0.66f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            ContentTileType.AUDIO -> {
                // Sound wave bars
                val barHeights = listOf(0.35f, 0.65f, 0.90f, 0.50f, 0.75f, 0.30f)
                val barSpacing = w * 0.10f
                val startX = w * 0.25f

                barHeights.forEachIndexed { i, barH ->
                    val x = startX + i * barSpacing
                    val lineLen = h * 0.50f * barH
                    val yMid = h * 0.50f
                    drawLine(
                        strokeColor,
                        start = Offset(x, yMid - lineLen / 2f),
                        end = Offset(x, yMid + lineLen / 2f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlocksHeroPreview() {
    MaterialTheme {
        Surface {
            BlocksHero(isActive = true, previewProgress = 1f)
        }
    }
}
