package com.yft.rippleup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val scheme = lightColorScheme(
    primary = Teal,
    onPrimary = White,
    secondary = Secondary,
    onSecondary = White,
    background = BgMain,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Mint,
    onSurfaceVariant = Secondary,
    error = DangerRed,
    outline = BorderHair,
)

@Composable
fun RippleUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = RippleTypography,
        content = content,
    )
}

/** Status-bar icons should be dark on all light screens; light on the QR camera. */
val StatusBarDarkIcons = Color.Unspecified
