package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BubbleBlitzColorScheme = darkColorScheme(
  primary = PurpleBtnLight,
  onPrimary = Color.White,
  primaryContainer = BrightViolet,
  onPrimaryContainer = GoldBright,
  secondary = GoldBright,
  onSecondary = Color(0xFF4A1A00),
  secondaryContainer = BlueBtnDark,
  onSecondaryContainer = Color.White,
  tertiary = GreenLight,
  onTertiary = Color(0xFF0B3506),
  background = DeepIndigo,
  onBackground = Color.White,
  surface = CardDarkPurple,
  onSurface = Color.White,
  surfaceVariant = RoyalPurple,
  onSurfaceVariant = IceBlue,
  outline = CardLavenderBorder,
  error = RedBtnLight,
  onError = Color.White,
)

@Composable
fun BubbleBlitzTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = BubbleBlitzColorScheme,
    typography = BubbleBlitzTypography,
    content = content,
  )
}
