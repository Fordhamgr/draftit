package com.flash.draft.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CobaltPrimaryDark,
    onPrimary = CobaltOnPrimaryDark,
    background = CobaltBackgroundDark,
    surface = CobaltSurfaceDark,
    onBackground = CobaltOnBackgroundDark,
    onSurface = CobaltOnSurfaceDark,
    surfaceVariant = CobaltSurfaceVariantDark,
    surfaceContainer = CobaltSurfaceContainerDark,
    surfaceContainerHigh = CobaltSurfaceContainerHighDark,
    onSurfaceVariant = CobaltOnSurfaceVariantDark,
    outline = CobaltOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimaryLight,
    onPrimary = CobaltOnPrimaryLight,
    background = CobaltBackgroundLight,
    surface = CobaltSurfaceLight,
    onBackground = CobaltOnBackgroundLight,
    onSurface = CobaltOnSurfaceLight,
    surfaceVariant = CobaltSurfaceVariantLight,
    surfaceContainer = CobaltSurfaceContainerLight,
    surfaceContainerHigh = CobaltSurfaceContainerHighLight,
    onSurfaceVariant = CobaltOnSurfaceVariantLight,
    outline = CobaltOutlineLight
)

@Composable
fun DraftitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disabled dynamic color by default so the Cobalt & Slate theme applies consistently
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}