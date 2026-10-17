package com.flash.draft.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OnboardingScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: OnboardingViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OnboardingEffect.NavigateToDashboard -> onNavigateToDashboard()
            }
        }
    }

    BackHandler(enabled = uiState.currentPage > 0) {
        viewModel.onEvent(OnboardingUiEvent.BackClicked)
    }

    OnboardingContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
fun OnboardingContent(
    uiState: OnboardingUiState,
    onEvent: (OnboardingUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it / 6 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Navigation Bar (Back + Skip)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (uiState.currentPage > 0) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEvent(OnboardingUiEvent.BackClicked)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEvent(OnboardingUiEvent.SkipClicked)
                        },
                        enabled = uiState.isActionEnabled
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Middle Content Area: Animated Card Stack
                AnimatedContent(
                    targetState = uiState.currentPage,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(
                                initialOffsetX = { fullWidth -> (fullWidth * 0.45f).toInt() },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeIn(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> -(fullWidth * 0.45f).toInt() },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) + fadeOut(
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            )
                        } else {
                            (slideInHorizontally(
                                initialOffsetX = { fullWidth -> -(fullWidth * 0.45f).toInt() },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeIn(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> (fullWidth * 0.45f).toInt() },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) + fadeOut(
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            )
                        }.using(SizeTransform(clip = false))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "onboarding_card_transition"
                ) { targetPage ->
                    val card = uiState.cards.getOrNull(targetPage)
                    if (card != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Demo Vector Illustration Card
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (card.illustrationType) {
                                        DemoIllustrationType.INFINITE_CANVAS -> {
                                            InfiniteCanvasIllustration()
                                        }
                                        DemoIllustrationType.FLUID_STUDIO -> {
                                            FluidStudioIllustration()
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = card.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = card.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 20.dp)
                ) {
                    repeat(uiState.totalPages) { index ->
                        val isSelected = index == uiState.currentPage
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "indicator_width"
                        )
                        val color by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            label = "indicator_color"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onEvent(OnboardingUiEvent.PageSelected(index))
                                }
                        )
                    }
                }

                // Pinned 56dp Action Button
                val isLastPage = uiState.currentPage >= uiState.totalPages - 1
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEvent(OnboardingUiEvent.NextClicked)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    enabled = uiState.isActionEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isLastPage) "Get Started" else "Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun InfiniteCanvasIllustration(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val dotColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val cardBg = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        val gridSpacing = 22.dp.toPx()
        val numCols = (size.width / gridSpacing).toInt()
        val numRows = (size.height / gridSpacing).toInt()

        // Background canvas grid dots
        for (col in 0..numCols) {
            for (row in 0..numRows) {
                drawCircle(
                    color = dotColor,
                    radius = 1.25.dp.toPx(),
                    center = Offset(col * gridSpacing, row * gridSpacing)
                )
            }
        }

        // Floating Note / Canvas item
        val noteWidth = 140.dp.toPx()
        val noteHeight = 90.dp.toPx()
        val noteLeft = size.width * 0.12f
        val noteTop = size.height * 0.18f

        drawRoundRect(
            color = cardBg,
            topLeft = Offset(noteLeft, noteTop),
            size = Size(noteWidth, noteHeight),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
        )
        drawRoundRect(
            color = cardBorder,
            topLeft = Offset(noteLeft, noteTop),
            size = Size(noteWidth, noteHeight),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Mock lines in the floating note
        drawLine(
            color = primaryColor.copy(alpha = 0.7f),
            start = Offset(noteLeft + 16.dp.toPx(), noteTop + 24.dp.toPx()),
            end = Offset(noteLeft + noteWidth - 28.dp.toPx(), noteTop + 24.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = tertiaryColor.copy(alpha = 0.5f),
            start = Offset(noteLeft + 16.dp.toPx(), noteTop + 42.dp.toPx()),
            end = Offset(noteLeft + noteWidth - 48.dp.toPx(), noteTop + 42.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Expressive Bezier Stroke crossing the canvas
        val strokePath = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.78f)
            cubicTo(
                size.width * 0.35f, size.height * 0.45f,
                size.width * 0.55f, size.height * 0.95f,
                size.width * 0.88f, size.height * 0.35f
            )
        }

        drawPath(
            path = strokePath,
            brush = Brush.horizontalGradient(
                listOf(primaryColor, secondaryColor, tertiaryColor)
            ),
            style = Stroke(
                width = 5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Stylus touch indicator at end of stroke
        drawCircle(
            color = tertiaryColor,
            radius = 6.dp.toPx(),
            center = Offset(size.width * 0.88f, size.height * 0.35f)
        )
        drawCircle(
            color = cardBg,
            radius = 3.dp.toPx(),
            center = Offset(size.width * 0.88f, size.height * 0.35f)
        )
    }
}

@Composable
private fun FluidStudioIllustration(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val dockBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    val inactiveToolColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        val centerX = size.width / 2f

        // Floating dynamic color droplets / swatches
        val swatches = listOf(
            Triple(centerX - 80.dp.toPx(), size.height * 0.30f, primaryColor),
            Triple(centerX - 25.dp.toPx(), size.height * 0.22f, secondaryColor),
            Triple(centerX + 35.dp.toPx(), size.height * 0.26f, tertiaryColor),
            Triple(centerX + 85.dp.toPx(), size.height * 0.34f, primaryColor.copy(alpha = 0.85f))
        )

        swatches.forEach { (x, y, color) ->
            drawCircle(
                color = color.copy(alpha = 0.25f),
                radius = 20.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = color,
                radius = 14.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = surfaceColor.copy(alpha = 0.85f),
                radius = 4.dp.toPx(),
                center = Offset(x - 3.dp.toPx(), y - 3.dp.toPx())
            )
        }

        // Connecting fluid curve
        val fluidPath = Path().apply {
            moveTo(centerX - 95.dp.toPx(), size.height * 0.36f)
            quadraticTo(
                centerX, size.height * 0.12f,
                centerX + 95.dp.toPx(), size.height * 0.38f
            )
        }
        drawPath(
            path = fluidPath,
            brush = Brush.horizontalGradient(listOf(primaryColor, tertiaryColor)),
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // Liquid Floating Dock
        val dockWidth = size.width * 0.82f
        val dockHeight = 64.dp.toPx()
        val dockLeft = (size.width - dockWidth) / 2f
        val dockTop = size.height * 0.62f

        // Dock background
        drawRoundRect(
            color = surfaceColor,
            topLeft = Offset(dockLeft, dockTop),
            size = Size(dockWidth, dockHeight),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
        )
        drawRoundRect(
            color = dockBorderColor,
            topLeft = Offset(dockLeft, dockTop),
            size = Size(dockWidth, dockHeight),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Mock Tool Items inside the Dock
        val toolCount = 4
        val slotWidth = dockWidth / toolCount
        val toolCenterY = dockTop + dockHeight / 2f

        for (i in 0 until toolCount) {
            val toolCenterX = dockLeft + slotWidth * i + slotWidth / 2f

            if (i == 1) {
                // Highlighted active tool slot
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.16f),
                    topLeft = Offset(toolCenterX - 18.dp.toPx(), toolCenterY - 18.dp.toPx()),
                    size = Size(36.dp.toPx(), 36.dp.toPx()),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                drawCircle(
                    color = primaryColor,
                    radius = 6.dp.toPx(),
                    center = Offset(toolCenterX, toolCenterY)
                )
            } else {
                drawCircle(
                    color = inactiveToolColor,
                    radius = 5.dp.toPx(),
                    center = Offset(toolCenterX, toolCenterY)
                )
            }
        }
    }
}
