package com.aflyingdino.rccarcontroller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = AppPrimary,
    secondary = AppSecondary,
    surface = AppSurface,
    background = AppSurface
)

@Composable
fun RCCarControllerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
