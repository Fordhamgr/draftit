package com.flash.draft.ui.onboarding.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flash.draft99.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Timing constants for WelcomeHero animations (in milliseconds).
 */
object WelcomeHeroTokens {
    const val POP_DURATION = 600
    val POP_EASING = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.2f)

    const val NOTE_DELAY = 100
    const val TASKS_DELAY = 300
    const val CODE_DELAY = 500
    const val IMAGE_DELAY = 700

    const val CONNECTOR_DURATION = 900
    val CONNECTOR_EASING = FastOutSlowInEasing
    const val CONNECTOR_1_DELAY = 1000
    const val CONNECTOR_2_DELAY = 1300
    const val CONNECTOR_3_DELAY = 1600

    const val CHECKBOX_LOOP_DURATION = 4000
    const val CURSOR_LOOP_DURATION = 9000

    val AmberBackground = Color(0xFFFEF3C7)
    val AmberTag = Color(0xFF92400E)
    val AmberSkeleton = Color(0xFFFDE68A)

    val TasksTag = Color(0xFF6B7280)
    val SkeletonLight = Color(0xFFE5E7EB)

    val CodeBackground = Color(0xFF111827)
    val CodeCyan = Color(0xFF67E8F9)
    val CodeAmber = Color(0xFFFBBF24)
    val CodeMuted = Color(0xFF94A3B8)

    val CardHairlineBorder = Color(0x0D000000)
}

/**
 * Page 1: WelcomeHero
 * Renders 4 content blocks (Note, Tasks, Code, Image), 3 animated connectors,
 * a looping checkbox interaction, and a drifting "You" cursor.
 */
@Composable
fun WelcomeHero(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false,
    isLandscape: Boolean = false,
    previewProgress: Float? = if (LocalInspectionMode.current) 1f else null
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Animatable progress for blocks & connectors
    val noteProgress = remember { Animatable(0f) }
    val tasksProgress = remember { Animatable(0f) }
    val codeProgress = remember { Animatable(0f) }
    val imageProgress = remember { Animatable(0f) }

    val conn1Progress = remember { Animatable(0f) }
    val conn2Progress = remember { Animatable(0f) }
    val conn3Progress = remember { Animatable(0f) }

    LaunchedEffect(isActive, isReducedMotion, previewProgress) {
        if (previewProgress != null || isReducedMotion) {
            noteProgress.snapTo(1f)
            tasksProgress.snapTo(1f)
            codeProgress.snapTo(1f)
            imageProgress.snapTo(1f)
            conn1Progress.snapTo(1f)
            conn2Progress.snapTo(1f)
            conn3Progress.snapTo(1f)
            return@LaunchedEffect
        }

        if (isActive) {
            launch {
                delay(WelcomeHeroTokens.NOTE_DELAY.toLong())
                noteProgress.animateTo(1f, tween(WelcomeHeroTokens.POP_DURATION, easing = WelcomeHeroTokens.POP_EASING))
            }
            launch {
                delay(WelcomeHeroTokens.TASKS_DELAY.toLong())
                tasksProgress.animateTo(1f, tween(WelcomeHeroTokens.POP_DURATION, easing = WelcomeHeroTokens.POP_EASING))
            }
            launch {
                delay(WelcomeHeroTokens.CODE_DELAY.toLong())
                codeProgress.animateTo(1f, tween(WelcomeHeroTokens.POP_DURATION, easing = WelcomeHeroTokens.POP_EASING))
            }
            launch {
                delay(WelcomeHeroTokens.IMAGE_DELAY.toLong())
                imageProgress.animateTo(1f, tween(WelcomeHeroTokens.POP_DURATION, easing = WelcomeHeroTokens.POP_EASING))
            }

            // Connectors
            launch {
                delay(WelcomeHeroTokens.CONNECTOR_1_DELAY.toLong())
                conn1Progress.animateTo(1f, tween(WelcomeHeroTokens.CONNECTOR_DURATION, easing = WelcomeHeroTokens.CONNECTOR_EASING))
            }
            launch {
                delay(WelcomeHeroTokens.CONNECTOR_2_DELAY.toLong())
                conn2Progress.animateTo(1f, tween(WelcomeHeroTokens.CONNECTOR_DURATION, easing = WelcomeHeroTokens.CONNECTOR_EASING))
            }
            launch {
                delay(WelcomeHeroTokens.CONNECTOR_3_DELAY.toLong())
                conn3Progress.animateTo(1f, tween(WelcomeHeroTokens.CONNECTOR_DURATION, easing = WelcomeHeroTokens.CONNECTOR_EASING))
            }
        }
    }

    // Unconditional infinite transition for loops
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_infinite")

    // Checkbox loop (4s cycle)
    val rawCheckboxLoop by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(WelcomeHeroTokens.CHECKBOX_LOOP_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "checkbox_loop"
    )

    // Cursor loop (9s cycle)
    val rawCursorLoop by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(WelcomeHeroTokens.CURSOR_LOOP_DURATION, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cursor_loop"
    )

    val isLoopingActive = isActive && !isReducedMotion && previewProgress == null

    // Pre-allocated paths for zero-allocation rendering at 120fps
    val fullPath1 = remember { Path() }
    val fullPath2 = remember { Path() }
    val fullPath3 = remember { Path() }
    val trimmedPath = remember { Path() }
    val pathMeasure = remember { PathMeasure() }

    OnboardingHeroContainer(
        contentDescriptionText = stringResource(R.string.onboarding_welcome_cd),
        isLandscape = isLandscape,
        modifier = modifier
    ) {
        // --- 1. CONNECTORS CANVAS (Under blocks) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 2.dp.toPx()

            // Path 1: Note right edge (156, 74) -> Tasks left edge (196, 104)
            val p1Progress = if (previewProgress != null || isReducedMotion) 1f else conn1Progress.value
            if (p1Progress > 0f) {
                fullPath1.rewind()
                fullPath1.moveTo(156.dp.toPx(), 74.dp.toPx())
                fullPath1.cubicTo(
                    176.dp.toPx(), 74.dp.toPx(),
                    176.dp.toPx(), 104.dp.toPx(),
                    196.dp.toPx(), 104.dp.toPx()
                )
                trimmedPath.rewind()
                pathMeasure.setPath(fullPath1, false)
                pathMeasure.getSegment(0f, pathMeasure.length * p1Progress, trimmedPath, true)
                drawPath(
                    path = trimmedPath,
                    color = primaryColor,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            // Path 2: Note bottom edge (88, 120) -> Code top edge (118, 196)
            val p2Progress = if (previewProgress != null || isReducedMotion) 1f else conn2Progress.value
            if (p2Progress > 0f) {
                fullPath2.rewind()
                fullPath2.moveTo(88.dp.toPx(), 120.dp.toPx())
                fullPath2.cubicTo(
                    88.dp.toPx(), 158.dp.toPx(),
                    118.dp.toPx(), 158.dp.toPx(),
                    118.dp.toPx(), 196.dp.toPx()
                )
                trimmedPath.rewind()
                pathMeasure.setPath(fullPath2, false)
                pathMeasure.getSegment(0f, pathMeasure.length * p2Progress, trimmedPath, true)
                drawPath(
                    path = trimmedPath,
                    color = primaryColor,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            // Path 3: Tasks bottom edge (267, 156) -> Image top edge (272, 206)
            val p3Progress = if (previewProgress != null || isReducedMotion) 1f else conn3Progress.value
            if (p3Progress > 0f) {
                fullPath3.rewind()
                fullPath3.moveTo(267.dp.toPx(), 156.dp.toPx())
                fullPath3.cubicTo(
                    267.dp.toPx(), 181.dp.toPx(),
                    272.dp.toPx(), 181.dp.toPx(),
                    272.dp.toPx(), 206.dp.toPx()
                )
                trimmedPath.rewind()
                pathMeasure.setPath(fullPath3, false)
                pathMeasure.getSegment(0f, pathMeasure.length * p3Progress, trimmedPath, true)
                drawPath(
                    path = trimmedPath,
                    color = primaryColor,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }

        // --- 2. NOTE BLOCK (20, 28, 136x92) ---
        val nProgress = if (previewProgress != null || isReducedMotion) 1f else noteProgress.value
        Box(
            modifier = Modifier
                .offset(20.dp, 28.dp)
                .size(136.dp, 92.dp)
                .graphicsLayer {
                    alpha = nProgress
                    scaleX = 0.85f + 0.15f * nProgress
                    scaleY = 0.85f + 0.15f * nProgress
                    translationY = (8.dp * (1f - nProgress)).toPx()
                }
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1F000000))
                .clip(RoundedCornerShape(16.dp))
                .background(WelcomeHeroTokens.AmberBackground)
                .border(1.dp, WelcomeHeroTokens.CardHairlineBorder, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_tag_note),
                    color = WelcomeHeroTokens.AmberTag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.05.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(WelcomeHeroTokens.AmberSkeleton)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(WelcomeHeroTokens.AmberSkeleton)
                )
            }
        }

        // --- 3. TASKS BLOCK (196, 52, 142x104) ---
        val tProgress = if (previewProgress != null || isReducedMotion) 1f else tasksProgress.value
        val checkboxCycle = if (isLoopingActive) rawCheckboxLoop else 0.5f
        val isFirstChecked = checkboxCycle in 0.15f..0.75f
        val checkboxScale = when {
            checkboxCycle in 0.10f..0.20f -> 1.0f + 0.2f * ((checkboxCycle - 0.10f) / 0.10f)
            checkboxCycle in 0.20f..0.25f -> 1.2f - 0.2f * ((checkboxCycle - 0.20f) / 0.05f)
            isFirstChecked -> 1.0f
            else -> 1.0f
        }

        Box(
            modifier = Modifier
                .offset(196.dp, 52.dp)
                .size(142.dp, 104.dp)
                .graphicsLayer {
                    alpha = tProgress
                    scaleX = 0.85f + 0.15f * tProgress
                    scaleY = 0.85f + 0.15f * tProgress
                    translationY = (8.dp * (1f - tProgress)).toPx()
                }
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1F000000))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, WelcomeHeroTokens.CardHairlineBorder, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_tag_tasks),
                    color = WelcomeHeroTokens.TasksTag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.05.sp
                )
                // Row 1 (Animated Checkbox)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .graphicsLayer {
                                scaleX = checkboxScale
                                scaleY = checkboxScale
                            }
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isFirstChecked) primaryColor else Color.Transparent)
                            .border(
                                1.5.dp,
                                if (isFirstChecked) primaryColor else WelcomeHeroTokens.SkeletonLight,
                                RoundedCornerShape(3.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFirstChecked) {
                            Text(
                                text = "✓",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(WelcomeHeroTokens.SkeletonLight)
                    )
                }

                // Row 2 (Checked)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(WelcomeHeroTokens.SkeletonLight)
                    )
                }

                // Row 3 (Unchecked)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .border(1.5.dp, WelcomeHeroTokens.SkeletonLight, RoundedCornerShape(3.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(WelcomeHeroTokens.SkeletonLight)
                    )
                }
            }
        }

        // --- 4. CODE BLOCK (40, 196, 156x96) ---
        val cProgress = if (previewProgress != null || isReducedMotion) 1f else codeProgress.value
        Box(
            modifier = Modifier
                .offset(40.dp, 196.dp)
                .size(156.dp, 96.dp)
                .graphicsLayer {
                    alpha = cProgress
                    scaleX = 0.85f + 0.15f * cProgress
                    scaleY = 0.85f + 0.15f * cProgress
                    translationY = (8.dp * (1f - cProgress)).toPx()
                }
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0x33000000))
                .clip(RoundedCornerShape(16.dp))
                .background(WelcomeHeroTokens.CodeBackground)
                .border(1.dp, WelcomeHeroTokens.CardHairlineBorder, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Line 1: canvas.add(block)
                Row {
                    Text("canvas.", color = WelcomeHeroTokens.CodeMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                    Text("add", color = WelcomeHeroTokens.CodeCyan, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                    Text("(block)", color = WelcomeHeroTokens.CodeMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                }
                // Line 2: block.connect(note)
                Row {
                    Text("block.", color = WelcomeHeroTokens.CodeMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                    Text("connect", color = WelcomeHeroTokens.CodeAmber, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                    Text("(note)", color = WelcomeHeroTokens.CodeMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                }
                // Line 3: scene.save()
                Row {
                    Text("scene.", color = WelcomeHeroTokens.CodeMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                    Text("save", color = WelcomeHeroTokens.CodeCyan, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                    Text("()", color = WelcomeHeroTokens.CodeMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // --- 5. IMAGE BLOCK (222, 206, 100x84) ---
        val iProgress = if (previewProgress != null || isReducedMotion) 1f else imageProgress.value
        Box(
            modifier = Modifier
                .offset(222.dp, 206.dp)
                .size(100.dp, 84.dp)
                .graphicsLayer {
                    alpha = iProgress
                    scaleX = 0.85f + 0.15f * iProgress
                    scaleY = 0.85f + 0.15f * iProgress
                    translationY = (8.dp * (1f - iProgress)).toPx()
                }
                .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1F000000))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.08f),
                            primaryColor.copy(alpha = 0.22f)
                        )
                    )
                )
                .border(1.dp, WelcomeHeroTokens.CardHairlineBorder, RoundedCornerShape(16.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Sun
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = 7.dp.toPx(),
                    center = Offset(24.dp.toPx(), 24.dp.toPx())
                )

                // Mountain silhouettes
                val mountainPath = Path().apply {
                    moveTo(0f, size.height)
                    lineTo(35.dp.toPx(), size.height - 38.dp.toPx())
                    lineTo(65.dp.toPx(), size.height - 18.dp.toPx())
                    lineTo(85.dp.toPx(), size.height - 30.dp.toPx())
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(
                    path = mountainPath,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }

        // --- 6. "YOU" CURSOR LOOP (Drifting across canvas) ---
        val cursorCycle = if (isLoopingActive) rawCursorLoop else 0f
        val (cursorOffsetX, cursorOffsetY) = when {
            cursorCycle < 0.25f -> {
                val t = cursorCycle / 0.25f
                val easeT = FastOutSlowInEasing.transform(t)
                (0f + (-90f) * easeT) to (0f + (-40f) * easeT)
            }
            cursorCycle < 0.50f -> {
                val t = (cursorCycle - 0.25f) / 0.25f
                val easeT = FastOutSlowInEasing.transform(t)
                (-90f + (120f) * easeT) to (-40f + (120f) * easeT)
            }
            cursorCycle < 0.75f -> {
                val t = (cursorCycle - 0.50f) / 0.25f
                val easeT = FastOutSlowInEasing.transform(t)
                (30f + (80f) * easeT) to (80f + (-30f) * easeT)
            }
            else -> {
                val t = (cursorCycle - 0.75f) / 0.25f
                val easeT = FastOutSlowInEasing.transform(t)
                (110f + (-110f) * easeT) to (50f + (-50f) * easeT)
            }
        }

        // Base anchor point: (170dp, 150dp)
        Row(
            modifier = Modifier
                .offset(
                    x = (170 + cursorOffsetX).dp,
                    y = (150 + cursorOffsetY).dp
                )
                .graphicsLayer {
                    alpha = if (previewProgress != null || isReducedMotion || nProgress > 0.5f) 1f else 0f
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme Arrow Pointer
            Canvas(modifier = Modifier.size(16.dp)) {
                val pointerPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, size.height * 0.45f)
                    lineTo(size.width * 0.45f, size.height * 0.55f)
                    lineTo(size.width * 0.25f, size.height)
                    close()
                }
                drawPath(path = pointerPath, color = primaryColor)
            }

            Spacer(modifier = Modifier.width(4.dp))

            // "You" pill label
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(primaryColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_tag_you),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeHeroPreview() {
    MaterialTheme {
        Surface {
            WelcomeHero(isActive = true, previewProgress = 1f)
        }
    }
}
