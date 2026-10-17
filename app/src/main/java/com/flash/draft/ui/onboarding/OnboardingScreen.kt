package com.flash.draft.ui.onboarding

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flash.draft99.R
import com.flash.draft.ui.onboarding.components.BlocksHero
import com.flash.draft.ui.onboarding.components.ScenesHero
import com.flash.draft.ui.onboarding.components.WelcomeHero
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TextSecondary = Color(0xFF6B7280)
private val TextPrimary = Color(0xFF111827)
private val IndicatorInactive = Color(0xFFD3D8E1)

/**
 * Stateful entry point for Onboarding.
 * Collects state from OnboardingViewModel, handles system navigation and effects.
 */
@Composable
fun OnboardingScreen(
    onNavigateToDashboard: () -> Unit,
    viewModel: OnboardingViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = uiState.currentPage) { uiState.totalPages }
    val coroutineScope = rememberCoroutineScope()
    val isReducedMotion = rememberReducedMotionEnabled()

    LaunchedEffect(isReducedMotion) {
        viewModel.onEvent(OnboardingUiEvent.ReducedMotionDetected(isReducedMotion))
    }

    // Keep ViewModel in sync with pager scroll settle
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { settledPage ->
            if (settledPage != uiState.currentPage) {
                viewModel.onEvent(OnboardingUiEvent.PageSelected(settledPage))
            }
        }
    }

    // Handle single-shot ViewModel effects
    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OnboardingEffect.NavigateToDashboard -> {
                    onNavigateToDashboard()
                }
                is OnboardingEffect.ScrollToPage -> {
                    pagerState.animateScrollToPage(
                        page = effect.page,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
        }
    }

    // Intercept back button to navigate backwards in pager
    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    OnboardingContent(
        uiState = uiState,
        pagerState = pagerState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

/**
 * Pure stateless Onboarding UI content.
 * Contains ZERO ViewModel references. Adapts across Phones, Portrait Tablets, and Landscape Two-Pane displays.
 */
@Composable
fun OnboardingContent(
    uiState: OnboardingUiState,
    pagerState: PagerState,
    onEvent: (OnboardingUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val layoutConfig = rememberOnboardingLayoutConfig()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = layoutConfig.horizontalPadding)
                .padding(bottom = layoutConfig.bottomPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (layoutConfig.maxContainerWidth != Dp.Unspecified) {
                            Modifier.widthIn(max = layoutConfig.maxContainerWidth)
                        } else {
                            Modifier
                        }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- 1. PERSISTENT HEADER (Height 44dp) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: App Icon (34dp, 10dp radius) + "Draft It" Wordmark (19sp bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.applogo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Text(
                            text = "Draft It",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    // Right: "Skip" button (Pages 0 & 1 only; invisible & non-clickable on Page 2)
                    val isSkipVisible = pagerState.currentPage < 2
                    Box(
                        modifier = Modifier.size(width = 60.dp, height = 44.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEvent(OnboardingUiEvent.SkipClicked)
                            },
                            enabled = isSkipVisible && uiState.isActionEnabled,
                            modifier = Modifier
                                .graphicsLayer { alpha = if (isSkipVisible) 1f else 0f }
                                .then(
                                    if (!isSkipVisible) Modifier.clearAndSetSemantics { } else Modifier
                                )
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_skip),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- 2. HORIZONTAL PAGER (Swiping Content Only) ---
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    if (layoutConfig.isTwoPane) {
                        TwoPaneOnboardingPage(
                            page = page,
                            isActive = pagerState.currentPage == page,
                            isReducedMotion = uiState.isReducedMotion
                        )
                    } else {
                        SingleColumnOnboardingPage(
                            page = page,
                            isActive = pagerState.currentPage == page,
                            isReducedMotion = uiState.isReducedMotion,
                            config = layoutConfig
                        )
                    }
                }

                // --- 3. PERSISTENT PAGE INDICATOR (48dp Accessible Hit Targets) ---
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    repeat(uiState.totalPages) { index ->
                        val isSelected = index == pagerState.currentPage
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "indicator_dot_width"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isSelected) primaryColor else IndicatorInactive,
                            label = "indicator_dot_color"
                        )

                        // 48dp Minimum Touch Target Container
                        Box(
                            modifier = Modifier
                                .size(width = maxOf(44.dp, dotWidth + 16.dp), height = 44.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(dotWidth)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- 4. PERSISTENT PRIMARY BOTTOM CTA BUTTON ---
                val buttonText = when (pagerState.currentPage) {
                    0 -> stringResource(R.string.onboarding_btn_get_started)
                    1 -> stringResource(R.string.onboarding_btn_continue)
                    else -> stringResource(R.string.onboarding_btn_start_creating)
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEvent(OnboardingUiEvent.NextClicked)
                    },
                    enabled = uiState.isActionEnabled,
                    modifier = Modifier
                        .then(
                            if (layoutConfig.maxButtonWidth != Dp.Unspecified) {
                                Modifier.width(layoutConfig.maxButtonWidth)
                            } else {
                                Modifier.fillMaxWidth()
                            }
                        )
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = primaryColor.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single-column layout for Phones and Portrait Tablets.
 * Proportional flexible vertical spacers center content optically and eliminate bottom void.
 */
@Composable
private fun SingleColumnOnboardingPage(
    page: Int,
    isActive: Boolean,
    isReducedMotion: Boolean,
    config: OnboardingLayoutConfig,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val headlineAnim = remember { Animatable(0f) }
    val subtitleAnim = remember { Animatable(0f) }

    LaunchedEffect(isActive, isReducedMotion) {
        if (isReducedMotion) {
            headlineAnim.snapTo(1f)
            subtitleAnim.snapTo(1f)
            return@LaunchedEffect
        }

        if (isActive) {
            headlineAnim.snapTo(0f)
            subtitleAnim.snapTo(0f)

            launch {
                delay(200L)
                headlineAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(350L)
                subtitleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    val headlineText = when (page) {
        0 -> stringResource(R.string.onboarding_welcome_title)
        1 -> stringResource(R.string.onboarding_blocks_title)
        else -> stringResource(R.string.onboarding_scenes_title)
    }

    val supportingText = when (page) {
        0 -> stringResource(R.string.onboarding_welcome_desc)
        1 -> stringResource(R.string.onboarding_blocks_desc)
        else -> stringResource(R.string.onboarding_scenes_desc)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Top flexible spacer absorbs void on tall screens
        Spacer(modifier = Modifier.weight(0.35f, fill = false))

        // Hero Graphic
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (page) {
                0 -> WelcomeHero(isActive = isActive, isReducedMotion = isReducedMotion)
                1 -> BlocksHero(isActive = isActive, isReducedMotion = isReducedMotion)
                else -> ScenesHero(isActive = isActive, isReducedMotion = isReducedMotion)
            }
        }

        Spacer(modifier = Modifier.height(config.heroSpacingToText))

        // Headline
        Text(
            text = headlineText,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = if (config.isTabletPortrait) 36.sp else 34.sp,
                lineHeight = if (config.isTabletPortrait) 40.sp else 37.4.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = config.maxContentWidth)
                .padding(horizontal = 8.dp)
                .graphicsLayer {
                    alpha = headlineAnim.value
                    translationY = (14.dp * (1f - headlineAnim.value)).toPx()
                }
                .semantics { heading() }
        )

        Spacer(modifier = Modifier.height(config.textSpacingToSubtitle))

        // Supporting text
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = if (config.isTabletPortrait) 17.sp else 16.sp,
                lineHeight = if (config.isTabletPortrait) 26.sp else 24.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = config.maxContentWidth)
                .padding(horizontal = 12.dp)
                .graphicsLayer {
                    alpha = subtitleAnim.value
                    translationY = (14.dp * (1f - subtitleAnim.value)).toPx()
                }
        )

        // Bottom flexible spacer balances layout optically
        Spacer(modifier = Modifier.weight(1f, fill = false))
    }
}

/**
 * Two-pane layout for Landscape Tablets and Foldables.
 * Left Pane: Hero illustration (height-first constraint to prevent overflow).
 * Right Pane: Vertically centered typography.
 */
@Composable
private fun TwoPaneOnboardingPage(
    page: Int,
    isActive: Boolean,
    isReducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val headlineAnim = remember { Animatable(0f) }
    val subtitleAnim = remember { Animatable(0f) }

    LaunchedEffect(isActive, isReducedMotion) {
        if (isReducedMotion) {
            headlineAnim.snapTo(1f)
            subtitleAnim.snapTo(1f)
            return@LaunchedEffect
        }

        if (isActive) {
            headlineAnim.snapTo(0f)
            subtitleAnim.snapTo(0f)

            launch {
                delay(200L)
                headlineAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(350L)
                subtitleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    val headlineText = when (page) {
        0 -> stringResource(R.string.onboarding_welcome_title)
        1 -> stringResource(R.string.onboarding_blocks_title)
        else -> stringResource(R.string.onboarding_scenes_title)
    }

    val supportingText = when (page) {
        0 -> stringResource(R.string.onboarding_welcome_desc)
        1 -> stringResource(R.string.onboarding_blocks_desc)
        else -> stringResource(R.string.onboarding_scenes_desc)
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Pane: Hero Illustration (Height-first constraint)
        Box(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            when (page) {
                0 -> WelcomeHero(
                    isActive = isActive,
                    isReducedMotion = isReducedMotion,
                    isLandscape = true,
                    modifier = Modifier.fillMaxHeight(0.88f)
                )
                1 -> BlocksHero(
                    isActive = isActive,
                    isReducedMotion = isReducedMotion,
                    isLandscape = true,
                    modifier = Modifier.fillMaxHeight(0.88f)
                )
                else -> ScenesHero(
                    isActive = isActive,
                    isReducedMotion = isReducedMotion,
                    isLandscape = true,
                    modifier = Modifier.fillMaxHeight(0.88f)
                )
            }
        }

        // Right Pane: Vertically Centered Copy
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.025).em,
                    color = TextPrimary
                ),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = headlineAnim.value
                        translationY = (14.dp * (1f - headlineAnim.value)).toPx()
                    }
                    .semantics { heading() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                ),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = subtitleAnim.value
                        translationY = (14.dp * (1f - subtitleAnim.value)).toPx()
                    }
            )
        }
    }
}

/**
 * Resilient check for system animator duration scale (0f = reduced motion).
 * Safe against OEM security exceptions and preview environments.
 */
@Composable
fun rememberReducedMotionEnabled(): Boolean {
    if (LocalInspectionMode.current) return false
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingContentPreview() {
    MaterialTheme {
        OnboardingContent(
            uiState = OnboardingUiState(currentPage = 0, totalPages = 3),
            pagerState = rememberPagerState(initialPage = 0) { 3 },
            onEvent = {}
        )
    }
}
