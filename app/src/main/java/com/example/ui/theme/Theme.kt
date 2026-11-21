package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NoirColorScheme = darkColorScheme(
  primary = PureWhite,
  onPrimary = OledBlack,
  primaryContainer = GraphiteSurface,
  onPrimaryContainer = PureWhite,
  secondary = SubtleText,
  onSecondary = OledBlack,
  secondaryContainer = CharcoalSurface,
  onSecondaryContainer = PureWhite,
  tertiary = CallGreen,
  onTertiary = OledBlack,
  error = CallCrimson,
  onError = PureWhite,
  background = OledBlack,
  onBackground = PureWhite,
  surface = OledBlack,
  onSurface = PureWhite,
  surfaceVariant = CharcoalSurface,
  onSurfaceVariant = MutedSlate,
  surfaceContainer = CharcoalSurface,
  surfaceContainerHigh = GraphiteSurface,
  outline = ElevatedCardBorder,
)

@Composable
fun NoirDialTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = NoirColorScheme,
    typography = NoirTypography,
    content = content,
  )
}
