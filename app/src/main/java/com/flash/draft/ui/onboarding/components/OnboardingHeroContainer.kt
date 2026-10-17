package com.flash.draft.ui.onboarding.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Reference dimensions for hero coordinate system */
const val HERO_DESIGN_WIDTH = 358f
const val HERO_DESIGN_HEIGHT = 340f
private const val HERO_CORNER_RADIUS = 28f
private const val HERO_DOT_RADIUS = 1.2f
private const val HERO_DOT_SPACING = 20f

val HeroBackground = Color(0xFFF6F7F9)
val HeroBorder = Color(0xFFE8EAEF)
val HeroDotColor = Color(0xFFD3D8E1)

/**
 * Responsive Hero Container:
 * Maintains the exact 358:340 aspect ratio, renders the background dot grid,
 * applies a 28dp rounded corner clip and 1dp border, and scales child contents
 * proportionally in 2D to fit any screen dimensions without clipping.
 *
 * In landscape, `matchHeightConstraintsFirst = true` bounds container height
 * to prevent viewport overflow.
 *
 * NOTE: To replace Compose illustrations with Lottie in the future,
 * render the LottieAnimation composable directly inside [content].
 */
@Composable
fun OnboardingHeroContainer(
    contentDescriptionText: String,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false,
    content: @Composable BoxScope.(scale: Float) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(HERO_DESIGN_WIDTH / HERO_DESIGN_HEIGHT, matchHeightConstraintsFirst = isLandscape)
            .clip(RoundedCornerShape(HERO_CORNER_RADIUS.dp))
            .border(1.dp, HeroBorder, RoundedCornerShape(HERO_CORNER_RADIUS.dp))
            .semantics { contentDescription = contentDescriptionText },
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth.value
        val containerHeight = maxHeight.value
        val scale = minOf(
            containerWidth / HERO_DESIGN_WIDTH,
            containerHeight / HERO_DESIGN_HEIGHT
        ).coerceAtMost(1f)

        // 1. Dot Grid Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = HeroBackground)

            val spacingPx = HERO_DOT_SPACING.dp.toPx()
            val dotRadiusPx = HERO_DOT_RADIUS.dp.toPx()

            val numCols = (size.width / spacingPx).toInt() + 1
            val numRows = (size.height / spacingPx).toInt() + 1

            for (col in 0..numCols) {
                for (row in 0..numRows) {
                    drawCircle(
                        color = HeroDotColor,
                        radius = dotRadiusPx,
                        center = Offset(col * spacingPx, row * spacingPx)
                    )
                }
            }
        }

        // 2. Scaled Content Box (358x340dp coordinate space scaled to fit)
        Box(
            modifier = Modifier
                .size(HERO_DESIGN_WIDTH.dp, HERO_DESIGN_HEIGHT.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            content(scale)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingHeroContainerPreview() {
    Surface {
        OnboardingHeroContainer(
            contentDescriptionText = "Hero Container Preview"
        ) {
            // Placeholder empty hero container preview
        }
    }
}
