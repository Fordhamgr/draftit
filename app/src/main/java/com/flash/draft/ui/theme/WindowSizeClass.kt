package com.flash.draft.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
enum class WindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Immutable
data class WindowSizeInfo(
    val widthClass: WindowWidthClass,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isTablet: Boolean,
    val isLargeTablet: Boolean,
    val isLandscapePhone: Boolean
)

val LocalWindowSizeInfo = staticCompositionLocalOf {
    WindowSizeInfo(
        widthClass = WindowWidthClass.COMPACT,
        screenWidthDp = 411.dp,
        screenHeightDp = 891.dp,
        isTablet = false,
        isLargeTablet = false,
        isLandscapePhone = false
    )
}

@Composable
fun rememberWindowSizeInfo(): WindowSizeInfo {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val smallestWidth = configuration.smallestScreenWidthDp

    val isTablet = smallestWidth >= 600
    val isLargeTablet = isTablet && configuration.screenWidthDp >= 840
    val isLandscapePhone = !isTablet && configuration.screenWidthDp >= 600 && configuration.screenHeightDp < 480

    val widthClass = when {
        configuration.screenWidthDp < 600 -> WindowWidthClass.COMPACT
        configuration.screenWidthDp < 840 -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.EXPANDED
    }

    return WindowSizeInfo(
        widthClass = widthClass,
        screenWidthDp = screenWidth,
        screenHeightDp = screenHeight,
        isTablet = isTablet,
        isLargeTablet = isLargeTablet,
        isLandscapePhone = isLandscapePhone
    )
}
