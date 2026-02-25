package com.herdmanager.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = ForestGreenLight.copy(alpha = 0.2f),
    onPrimaryContainer = ForestGreenDark,
    secondary = WarmAmber,
    onSecondary = Stone900,
    secondaryContainer = WarmAmber.copy(alpha = 0.3f),
    onSecondaryContainer = Stone700,
    tertiary = PregnancyCheckAccent,
    onTertiary = Color.White,
    tertiaryContainer = PregnancyCheckAccent.copy(alpha = 0.2f),
    onTertiaryContainer = PregnancyCheckAccent,
    error = CalvingAccent,
    onError = Color.White,
    errorContainer = CalvingAccent.copy(alpha = 0.2f),
    onErrorContainer = CalvingAccent,
    background = Stone50,
    onBackground = Stone900,
    surface = Color.White,
    onSurface = Stone900,
    surfaceVariant = Stone100,
    onSurfaceVariant = Stone600,
    outline = Stone300
)

private val DarkColorScheme = darkColorScheme(
    primary = ForestGreenLightTint,
    onPrimary = Stone900,
    primaryContainer = ForestGreen,
    onPrimaryContainer = Stone100,
    secondary = WarmAmber,
    onSecondary = Stone900,
    secondaryContainer = WarmAmberDark,
    onSecondaryContainer = Stone100,
    tertiary = PregnancyCheckAccent.copy(alpha = 0.8f),
    onTertiary = Color.White,
    tertiaryContainer = PregnancyCheckAccent.copy(alpha = 0.3f),
    onTertiaryContainer = Stone200,
    error = CalvingAccent,
    onError = Stone900,
    errorContainer = CalvingAccent.copy(alpha = 0.3f),
    onErrorContainer = Stone200,
    background = Stone950,
    onBackground = Stone100,
    surface = Stone900,
    onSurface = Stone100,
    surfaceVariant = Stone800,
    onSurfaceVariant = Stone300,
    outline = Stone600
)

// Rounded corners throughout (award-inspired, easy to scan)
private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

@Composable
fun HerdManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
