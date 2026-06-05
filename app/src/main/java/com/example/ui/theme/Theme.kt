package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun getDarkColorScheme(colorName: String) = darkColorScheme(
    primary = when (colorName) {
        "Cyan" -> PrimaryCyan
        "Violet" -> PrimaryViolet
        "Emerald" -> PrimaryEmerald
        "Crimson" -> PrimaryCrimson
        "Amber" -> PrimaryAmber
        else -> PrimaryCyan
    },
    secondary = PrimaryViolet,
    tertiary = SuccessGreen,
    background = DarkBackground,
    surface = CardSurface,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onTertiary = DarkBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    error = KillSwitchRed
)

@Composable
fun MyApplicationTheme(
  themeColor: String = "Cyan",
  content: @Composable () -> Unit,
) {
  val colorScheme = getDarkColorScheme(themeColor)
  
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.background.toArgb()
      window.navigationBarColor = colorScheme.background.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
      WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
