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
 * Contains ZERO ViewModel references. Hoists state and event lambdas.
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. SHARED HEADER (Height 44dp) ---
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

            Spacer(modifier = Modifier.height(16.dp))

            // --- 2. HORIZONTAL PAGER (Hero + Copy per page) ---
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingPageBody(
                    page = page,
                    isActive = pagerState.currentPage == page,
                    isReducedMotion = uiState.isReducedMotion
                )
            }

            // --- 3. PAGE INDICATOR (3 dots: 8dp inactive, 24dp active theme pill) ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
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

                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    )
                }
            }

            // --- 4. PRIMARY BOTTOM CTA BUTTON (56dp tall, 18dp corner radius) ---
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
                    .fillMaxWidth()
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

/**
 * Individual Page layout containing the Hero and animated typography.
 * Supports smooth scroll on compact/short devices so content is never clipped.
 */
@Composable
private fun OnboardingPageBody(
    page: Int,
    isActive: Boolean,
    isReducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Upward fade-in animation: 700ms ease-out, 14dp translation
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
                delay(200L) // 200ms delay for headline
                headlineAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(350L) // 350ms delay for supporting text
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
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Graphic
        when (page) {
            0 -> WelcomeHero(isActive = isActive, isReducedMotion = isReducedMotion)
            1 -> BlocksHero(isActive = isActive, isReducedMotion = isReducedMotion)
            else -> ScenesHero(isActive = isActive, isReducedMotion = isReducedMotion)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Headline: 34sp, line height 1.1, bold, letter spacing -0.025em
        Text(
            text = headlineText,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 34.sp,
                lineHeight = 37.4.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .graphicsLayer {
                    alpha = headlineAnim.value
                    translationY = (14.dp * (1f - headlineAnim.value)).toPx()
                }
                .semantics { heading() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Supporting text: 16sp, line height 1.5, #6B7280
        Text(
            text = supportingText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .graphicsLayer {
                    alpha = subtitleAnim.value
                    translationY = (14.dp * (1f - subtitleAnim.value)).toPx()
                }
        )

        Spacer(modifier = Modifier.height(16.dp))
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
