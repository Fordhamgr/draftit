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
    primary = RedPrimaryDark,
    background = RedBackgroundDark,
    surface = RedSurfaceDark,
    onBackground = RedOnBackgroundDark,
    onSurface = RedOnSurfaceDark,
    surfaceVariant = RedSurfaceVariantDark,
    surfaceContainer = RedSurfaceContainerDark,
    onSurfaceVariant = RedOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = RedPrimaryLight,
    background = RedBackgroundLight,
    surface = RedSurfaceLight,
    onBackground = RedOnBackgroundLight,
    onSurface = RedOnSurfaceLight,
    surfaceVariant = RedSurfaceVariantLight,
    surfaceContainer = RedSurfaceContainerLight,
    onSurfaceVariant = RedOnSurfaceVariantLight
)

@Composable
fun DraftitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disabled dynamic color by default so the red theme applies consistently
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