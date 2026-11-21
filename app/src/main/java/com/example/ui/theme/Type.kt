package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlayfairDisplayFamily = FontFamily(
  Font(R.font.playfair_display, FontWeight.Normal),
  Font(R.font.playfair_display, FontWeight.Bold),
)

val InterFamily = FontFamily(
  Font(R.font.inter, FontWeight.Normal),
  Font(R.font.inter, FontWeight.Medium),
  Font(R.font.inter, FontWeight.SemiBold),
  Font(R.font.inter, FontWeight.Bold),
)

val OswaldFamily = FontFamily(
  Font(R.font.oswald, FontWeight.Normal),
  Font(R.font.oswald, FontWeight.Bold),
)

val NoirTypography = Typography(
  displayLarge = TextStyle(
    fontFamily = PlayfairDisplayFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 40.sp,
    lineHeight = 46.sp,
    letterSpacing = (-0.5).sp,
  ),
  headlineLarge = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.25).sp,
  ),
  headlineMedium = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
  ),
  titleLarge = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.8.sp,
  ),
)
