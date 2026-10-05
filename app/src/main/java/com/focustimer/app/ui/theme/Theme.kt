package com.focustimer.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// A monochrome identity. Black carries actions and selection, greys carry surfaces, and one soft
// blue is kept for progress and for whatever is live — the entry in the timer, the rest phase.
// The phase colours are therefore roles of the scheme (primary for work, tertiary for rest), not
// fixed values: a fixed black ring would vanish on the dark theme.
private val LightColors = lightColorScheme(
    primary = Color(0xFF111114),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9EBF0),
    onPrimaryContainer = Color(0xFF111114),
    secondary = Color(0xFF5F6470),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECEEF2),
    onSecondaryContainer = Color(0xFF1D1F24),
    tertiary = Color(0xFF3B6FD8),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE6EEFC),
    onTertiaryContainer = Color(0xFF0F2A5C),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111114),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFECEDF0),
    onSurfaceVariant = Color(0xFF5C6069),
    // Elevated surfaces are tinted with this colour; with a black accent the default would turn
    // bars and dialogs a muddy grey, so the tint is the surface itself.
    surfaceTint = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F9FA),
    surfaceContainer = Color(0xFFF4F5F7),
    surfaceContainerHigh = Color(0xFFEDEEF1),
    surfaceContainerHighest = Color(0xFFE4E6EA),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCE4E4),
    onErrorContainer = Color(0xFF5A0F0F),
    outline = Color(0xFF9498A1),
    outlineVariant = Color(0xFFE1E3E8),
    scrim = Color(0xFF000000)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2F3F5),
    onPrimary = Color(0xFF111114),
    primaryContainer = Color(0xFF2B2D33),
    onPrimaryContainer = Color(0xFFF2F3F5),
    secondary = Color(0xFFB9BDC7),
    onSecondary = Color(0xFF1D1F24),
    secondaryContainer = Color(0xFF2B2D33),
    onSecondaryContainer = Color(0xFFE6E8EC),
    tertiary = Color(0xFF8FB2F5),
    onTertiary = Color(0xFF0F2A5C),
    tertiaryContainer = Color(0xFF1F3563),
    onTertiaryContainer = Color(0xFFDCE7FD),
    background = Color(0xFF0F1012),
    onBackground = Color(0xFFECEDEF),
    surface = Color(0xFF0F1012),
    onSurface = Color(0xFFECEDEF),
    surfaceVariant = Color(0xFF2B2D33),
    onSurfaceVariant = Color(0xFFA9ADB6),
    surfaceTint = Color(0xFF0F1012),
    surfaceContainerLowest = Color(0xFF0B0C0D),
    surfaceContainerLow = Color(0xFF141517),
    surfaceContainer = Color(0xFF17181B),
    surfaceContainerHigh = Color(0xFF1F2024),
    surfaceContainerHighest = Color(0xFF2A2C31),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5A0F0F),
    onErrorContainer = Color(0xFFFCE4E4),
    outline = Color(0xFF6B6F78),
    outlineVariant = Color(0xFF2F3238),
    scrim = Color(0xFF000000)
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
 * Softer corners than the Material defaults. The smallest step is what text fields and menus take
 * their shape from, so raising it rounds every field in the app at once.
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
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
        shapes = AppShapes,
        content = content
    )
}
