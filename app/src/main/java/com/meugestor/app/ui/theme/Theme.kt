package com.meugestor.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = EmeraldGreen,
    onPrimary = White,
    primaryContainer = Color(0xFFA8D5B5),
    onPrimaryContainer = EmeraldGreenDark,
    secondary = BluePetrol,
    onSecondary = White,
    secondaryContainer = BluePetrolLight,
    onSecondaryContainer = BluePetrolDark,
    tertiary = GoldAccent,
    onTertiary = NearBlack,
    tertiaryContainer = GoldAccentLight,
    onTertiaryContainer = DarkGray,
    background = LightGray,
    onBackground = NearBlack,
    surface = White,
    onSurface = NearBlack,
    surfaceVariant = MediumGray,
    onSurfaceVariant = DarkGray,
    error = ExpenseRed,
    onError = White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = MediumGray,
    outlineVariant = LightGray
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldGreenLight,
    onPrimary = EmeraldGreenDark,
    primaryContainer = EmeraldGreen,
    onPrimaryContainer = EmeraldGreenLight,
    secondary = BluePetrolLight,
    onSecondary = BluePetrolDark,
    secondaryContainer = BluePetrol,
    onSecondaryContainer = BluePetrolLight,
    tertiary = GoldAccentLight,
    onTertiary = NearBlack,
    tertiaryContainer = GoldAccent,
    onTertiaryContainer = NearBlack,
    background = DarkSurface,
    onBackground = White,
    surface = DarkSurfaceVariant,
    onSurface = White,
    surfaceVariant = DarkCard,
    onSurfaceVariant = MediumGray,
    error = Color(0xFFFF6B68),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = DarkGray,
    outlineVariant = DarkCard
)

@Composable
fun MeuGestorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MeuGestorTypography,
        shapes = MeuGestorShapes,
        content = content
    )
}
