package com.nexus.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = NexusPrimary,
    primaryContainer = NexusPrimaryContainer,
    secondary = NexusSecondary,
    secondaryContainer = NexusSecondaryContainer,
    tertiary = NexusTertiary,
    tertiaryContainer = NexusTertiaryContainer,
    error = NexusError,
    errorContainer = NexusErrorContainer,
    background = NexusBackground,
    surface = NexusSurface,
)

private val DarkColors = darkColorScheme(
    primary = NexusPrimaryDark,
    primaryContainer = NexusPrimaryContainerDark,
    secondary = NexusSecondaryDark,
    secondaryContainer = NexusSecondaryContainerDark,
    tertiary = NexusTertiaryDark,
    tertiaryContainer = NexusTertiaryContainerDark,
    error = NexusErrorDark,
    errorContainer = NexusErrorContainerDark,
    background = NexusBackgroundDark,
    surface = NexusSurfaceDark,
)

@Composable
fun NexusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexusTypography,
        content = content,
    )
}
