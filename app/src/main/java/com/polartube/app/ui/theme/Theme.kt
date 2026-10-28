package com.polartube.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MintGreen = Color(0xFF00E676)
val TrueBlack = Color(0xFF000000)
val DarkSurface = Color(0xFF121212)
val LightMint = Color(0xFF69F0AE)
val ElegantGray = Color(0xFF1A1A1A)

private val ExecutiveDarkColorScheme = darkColorScheme(
    primary = MintGreen,
    secondary = LightMint,
    background = TrueBlack,
    surface = DarkSurface,
    onPrimary = TrueBlack,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = ElegantGray,
    onSurfaceVariant = Color(0xFFAAAAAA)
)

@Composable
fun PolarTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ExecutiveDarkColorScheme,
        content = content
    )
}
