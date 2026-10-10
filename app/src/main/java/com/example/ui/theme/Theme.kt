package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.telefam.company.app.domain.AppThemeOption

val LocalAppDarkTheme = compositionLocalOf { false }

@Composable
fun isTelefamDarkTheme(): Boolean = LocalAppDarkTheme.current

@Composable
fun resolveTelefamDarkTheme(themeModeWire: String?): Boolean {
    return when (AppThemeOption.fromWire(themeModeWire)) {
        AppThemeOption.LIGHT -> false
        AppThemeOption.DARK -> true
        AppThemeOption.SYSTEM -> isSystemInDarkTheme()
    }
}

private val TelefamDarkColorScheme = darkColorScheme(
    primary = TelefamRedBright,
    onPrimary = Color.White,
    primaryContainer = TelefamRedDark,
    onPrimaryContainer = Color.White,
    secondary = TelefamRed,
    onSecondary = Color.White,
    background = TelefamDarkBackground,
    onBackground = TelefamTextPrimaryDark,
    surface = TelefamCardSurfaceDark,
    onSurface = TelefamTextPrimaryDark,
    surfaceVariant = TelefamAvatarCircleDark,
    onSurfaceVariant = TelefamTextSecondaryDark,
    outline = TelefamInputBorderDark,
    outlineVariant = TelefamAuthBorderDark,
    error = TelefamError
)

private val TelefamLightColorScheme = lightColorScheme(
    primary = TelefamRed,
    onPrimary = Color.White,
    primaryContainer = TelefamAvatarCircleLight,
    onPrimaryContainer = TelefamRedDark,
    secondary = TelefamRedBright,
    onSecondary = Color.White,
    background = TelefamLightBackground,
    onBackground = TelefamTextPrimaryLight,
    surface = TelefamCardSurfaceLight,
    onSurface = TelefamTextPrimaryLight,
    surfaceVariant = TelefamAvatarCircleLight,
    onSurfaceVariant = TelefamTextSecondaryLight,
    outline = TelefamInputBorderLight,
    outlineVariant = TelefamAuthBorderLight,
    error = TelefamError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TelefamDarkColorScheme else TelefamLightColorScheme

    CompositionLocalProvider(LocalAppDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
