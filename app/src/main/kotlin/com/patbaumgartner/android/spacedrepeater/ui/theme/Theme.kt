package com.patbaumgartner.android.spacedrepeater.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/** Semantic colours that Material's scheme has no slot for. */
data class SpacedRepeaterAccents(
    val due: Color,
    val warning: Color,
    val error: Color,
    val textMuted: Color,
    val brand: Color,
)

val LocalSpacedRepeaterAccents = staticCompositionLocalOf {
    SpacedRepeaterAccents(BrandCyanDeep, Amber, Coral, InkMuted, BrandCyanDeep)
}

private val LightScheme = lightColorScheme(
    primary = BrandCyanDeep,
    onPrimary = Color.White,
    primaryContainer = BrandCyan,
    onPrimaryContainer = OnBrandCyan,
    secondaryContainer = Color(0xFFD7F3FC),
    onSecondaryContainer = OnBrandCyan,
    tertiaryContainer = Color(0xFFD7F3FC),
    onTertiaryContainer = OnBrandCyan,
    outline = Color(0xFF6C797E),
    outlineVariant = Color(0xFFCEDDE2),
    background = Porcelain,
    onBackground = Ink,
    surface = PorcelainRaised,
    onSurface = Ink,
    surfaceContainerHighest = Color(0xFFE2E8E8),
    surfaceVariant = Porcelain,
    onSurfaceVariant = InkMuted,
    error = Coral,
    onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = BrandCyan,
    onPrimary = OnBrandCyan,
    primaryContainer = BrandCyanLight,
    onPrimaryContainer = OnBrandCyan,
    secondaryContainer = Color(0xFF123642),
    onSecondaryContainer = BrandCyanLight,
    tertiaryContainer = Color(0xFF123642),
    onTertiaryContainer = BrandCyanLight,
    outline = Color(0xFF8A9BA1),
    outlineVariant = Color(0xFF3B4B51),
    background = Graphite,
    onBackground = Bone,
    surface = GraphiteRaised,
    onSurface = Bone,
    surfaceContainerHighest = Color(0xFF3B4B51),
    surfaceVariant = GraphiteRaised,
    onSurfaceVariant = BoneMuted,
    error = CoralLight,
    onError = OnBrandCyan,
)

private val Trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun typography(family: androidx.compose.ui.text.font.FontFamily) = Typography(
    displayLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Light,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.8).sp,
        lineHeightStyle = Trim,
    ),
    headlineSmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.6.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun SpacedRepeaterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val accents = if (darkTheme) {
        SpacedRepeaterAccents(BrandCyanLight, AmberLight, CoralLight, BoneMuted, BrandCyanLight)
    } else {
        SpacedRepeaterAccents(BrandCyanDeep, Amber, Coral, InkMuted, BrandCyanDeep)
    }
    CompositionLocalProvider(LocalSpacedRepeaterAccents provides accents) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = typography(SpacedRepeaterFontFamily),
            content = content,
        )
    }
}

object SpacedRepeaterTheme {
    val accents: SpacedRepeaterAccents
        @Composable @ReadOnlyComposable get() = LocalSpacedRepeaterAccents.current
}
