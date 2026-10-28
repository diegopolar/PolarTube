package com.polartube.app.ui.theme

@Composable
fun PolarTubeTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF7CC4FF), background = Color(0xFF0E1116), surface = Color(0xFF151A21)), content = content)
