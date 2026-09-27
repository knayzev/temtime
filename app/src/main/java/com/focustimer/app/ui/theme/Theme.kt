package com.focustimer.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// A deliberate indigo/coral/teal identity instead of the Material baseline purple.
val WorkColor = Color(0xFF5B4FE9)
val RestColor = Color(0xFF00A896)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4FE9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E0FF),
    onPrimaryContainer = Color(0xFF180568),
    secondary = Color(0xFFFF7A59),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBD1),
    onSecondaryContainer = Color(0xFF3A0D00),
    tertiary = Color(0xFF00A896),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9EF2E5),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B23),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B23),
    surfaceVariant = Color(0xFFE5E0EC),
    onSurfaceVariant = Color(0xFF47454F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF79747E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC3BFFF),
    onPrimary = Color(0xFF2A1B8F),
    primaryContainer = Color(0xFF4030B0),
    onPrimaryContainer = Color(0xFFE4E0FF),
    secondary = Color(0xFFFFB4A0),
    onSecondary = Color(0xFF5F1600),
    secondaryContainer = Color(0xFF7D2A0F),
    onSecondaryContainer = Color(0xFFFFDBD1),
    tertiary = Color(0xFF82D5C7),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF005046),
    onTertiaryContainer = Color(0xFF9EF2E5),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE5E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF47454F),
    onSurfaceVariant = Color(0xFFC9C5D0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF928F99)
)

/**
 * Headlines are tightened and made heavier than the Material defaults so screen titles read as
 * titles at a glance; body text is given a little more line height because most of this app is
 * short paragraphs of advice rather than dense copy.
 */
private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontSize = 25.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.15.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
)

/**
 * [dynamicColor] lets Android 12+ recolour the app from the wallpaper. It is off by default so
 * the brand palette stays the norm, and the caller decides whether the user opted in.
 */
@Composable
fun FocusTimerTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
