package com.techelites.attendacemarkingv1.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    onPrimary = Color.White,
    secondary = Orange500,
    onSecondary = Color.White,
    tertiary = Teal500,
    background = Color.White,
    surface = Color.White,
    error = Red500,
    onBackground = DarkGray,
    onSurface = DarkGray,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue700,
    onPrimary = Color.White,
    secondary = Orange700,
    onSecondary = Color.White,
    tertiary = Teal700,
    background = DarkBackground,
    surface = DarkSurface,
    error = Red700,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun AuraSecurityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = if (darkTheme) DarkBackground else Color.White,
            darkIcons = !darkTheme
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}