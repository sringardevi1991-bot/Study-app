package com.example.ui.theme

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
    primary = StudyPrimaryDark,
    onPrimary = StudyOnPrimaryDark,
    primaryContainer = StudyPrimaryContainerDark,
    onPrimaryContainer = StudyOnPrimaryContainerDark,
    secondary = StudySecondaryDark,
    onSecondary = StudyOnSecondaryDark,
    secondaryContainer = StudySecondaryContainerDark,
    onSecondaryContainer = StudyOnSecondaryContainerDark,
    tertiary = StudyTertiaryDark,
    onTertiary = StudyOnTertiaryDark,
    tertiaryContainer = StudyTertiaryContainerDark,
    onTertiaryContainer = StudyOnTertiaryContainerDark,
    background = StudyBackgroundDark,
    onBackground = StudyOnBackgroundDark,
    surface = StudySurfaceDark,
    onSurface = StudyOnSurfaceDark,
    surfaceVariant = StudySurfaceVariantDark,
    onSurfaceVariant = StudyOnSurfaceVariantDark,
    outline = StudyOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = StudyPrimary,
    onPrimary = StudyOnPrimary,
    primaryContainer = StudyPrimaryContainer,
    onPrimaryContainer = StudyOnPrimaryContainer,
    secondary = StudySecondary,
    onSecondary = StudyOnSecondary,
    secondaryContainer = StudySecondaryContainer,
    onSecondaryContainer = StudyOnSecondaryContainer,
    tertiary = StudyTertiary,
    onTertiary = StudyOnTertiary,
    tertiaryContainer = StudyTertiaryContainer,
    onTertiaryContainer = StudyOnTertiaryContainer,
    background = StudyBackground,
    onBackground = StudyOnBackground,
    surface = StudySurface,
    onSurface = StudyOnSurface,
    surfaceVariant = StudySurfaceVariant,
    onSurfaceVariant = StudyOnSurfaceVariant,
    outline = StudyOutline
)

@Composable
fun StudyMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use intentional, cohesive palette by default
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
