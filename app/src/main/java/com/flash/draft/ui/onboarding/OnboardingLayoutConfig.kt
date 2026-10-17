package com.flash.draft.ui.onboarding

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flash.draft.ui.theme.LocalWindowSizeInfo
import com.flash.draft.ui.theme.WindowWidthClass

@Immutable
data class OnboardingLayoutConfig(
    val isTwoPane: Boolean,
    val isTabletPortrait: Boolean,
    val maxContainerWidth: Dp,
    val maxContentWidth: Dp,
    val maxButtonWidth: Dp,
    val horizontalPadding: Dp,
    val bottomPadding: Dp,
    val heroSpacingToText: Dp,
    val textSpacingToSubtitle: Dp
)

@Composable
fun rememberOnboardingLayoutConfig(): OnboardingLayoutConfig {
    val configuration = LocalConfiguration.current
    val windowSizeInfo = LocalWindowSizeInfo.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    return remember(configuration.screenWidthDp, configuration.screenHeightDp, isLandscape, windowSizeInfo) {
        val isTwoPane = (windowSizeInfo.widthClass == WindowWidthClass.EXPANDED) ||
                (isLandscape && screenWidth >= 600.dp && screenHeight >= 420.dp)
        val isTabletPortrait = windowSizeInfo.isTablet && !isLandscape

        OnboardingLayoutConfig(
            isTwoPane = isTwoPane,
            isTabletPortrait = isTabletPortrait,
            maxContainerWidth = if (isTwoPane) 1100.dp else if (isTabletPortrait) 560.dp else Dp.Unspecified,
            maxContentWidth = if (isTwoPane) 480.dp else 480.dp,
            maxButtonWidth = if (isTwoPane || isTabletPortrait) 400.dp else Dp.Unspecified,
            horizontalPadding = when {
                isTwoPane -> 32.dp
                isTabletPortrait -> 28.dp
                else -> 20.dp
            },
            bottomPadding = when {
                isTwoPane -> 20.dp
                isTabletPortrait -> 20.dp
                else -> 24.dp
            },
            heroSpacingToText = if (isTwoPane) 0.dp else if (isTabletPortrait) 28.dp else 24.dp,
            textSpacingToSubtitle = if (isTwoPane) 14.dp else 12.dp
        )
    }
}
