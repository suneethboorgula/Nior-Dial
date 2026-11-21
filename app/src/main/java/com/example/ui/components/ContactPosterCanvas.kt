package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ContactPosterConfig
import com.example.data.PosterAlignment
import com.example.data.PosterFontStyle
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElevatedCardBorder
import com.example.ui.theme.InterFamily
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OswaldFamily
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.PureWhite
import kotlin.math.abs

fun posterFontFamily(style: PosterFontStyle): FontFamily = when (style) {
  PosterFontStyle.EDITORIAL_SERIF -> PlayfairDisplayFamily
  PosterFontStyle.CLEAN_SANS -> InterFamily
  PosterFontStyle.CONDENSED_BOLD -> OswaldFamily
}

fun formatStackedDisplayName(name: String): String {
  val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
  return when {
    parts.size >= 2 -> "${parts.first()}\n${parts.drop(1).joinToString(" ")}"
    else -> name.trim()
  }
}

fun computeMonogram(name: String): String {
  val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
  return when {
    parts.size >= 2 -> "${parts.first().first().uppercaseChar()}${parts.last().first().uppercaseChar()}"
    parts.size == 1 && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
    else -> "?"
  }
}

private fun fallbackGradientColors(seed: String): Pair<Color, Color> {
  val palettes = listOf(
    Color(0xFF1D222B) to Color(0xFF07080A),
    Color(0xFF281D22) to Color(0xFF0A0608),
    Color(0xFF1C2623) to Color(0xFF060908),
    Color(0xFF252130) to Color(0xFF08070C),
    Color(0xFF2B231A) to Color(0xFF0A0806),
  )
  val index = abs(seed.hashCode()) % palettes.size
  return palettes[index]
}

@Composable
fun ContactPosterBackdrop(
  displayName: String,
  posterConfig: ContactPosterConfig,
  fallbackPhotoUri: String? = null,
  modifier: Modifier = Modifier,
) {
  val effectiveUri = posterConfig.posterUri ?: fallbackPhotoUri
  val (topTint, bottomTint) = fallbackGradientColors(displayName)

  Box(modifier = modifier.background(OledBlack)) {
    if (!effectiveUri.isNullOrBlank()) {
      AsyncImage(
        model = effectiveUri,
        contentDescription = "$displayName contact poster",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      // Clean atmospheric dark gradient with no monogram clutter when no poster is set
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(topTint, Color(0xFF0E131B), bottomTint),
            ),
          ),
      )
    }

    // Top scrim for crisp status bar & top-aligned typography
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(260.dp)
        .align(Alignment.TopCenter)
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color.Black.copy(alpha = 0.72f),
              Color.Black.copy(alpha = 0.32f),
              Color.Transparent,
            ),
          ),
        ),
    )

    // Bottom scrim for bottom-aligned names & frosted call controls
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(340.dp)
        .align(Alignment.BottomCenter)
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color.Transparent,
              Color.Black.copy(alpha = 0.45f),
              Color.Black.copy(alpha = 0.88f),
            ),
          ),
        ),
    )
  }
}

@Composable
fun ContactPosterNameBlock(
  displayName: String,
  posterConfig: ContactPosterConfig,
  subtitle: String? = null,
  overline: String? = null,
  hasPosterPhoto: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val fontFamily = if (hasPosterPhoto) posterFontFamily(posterConfig.fontStyle) else InterFamily
  val textColor = if (hasPosterPhoto) Color(posterConfig.textColorHex) else PureWhite
  val letterSpacing = if (hasPosterPhoto) {
    when (posterConfig.fontStyle) {
      PosterFontStyle.EDITORIAL_SERIF -> (-0.5).sp
      PosterFontStyle.CLEAN_SANS -> (-1.0).sp
      PosterFontStyle.CONDENSED_BOLD -> 0.5.sp
    }
  } else {
    (-0.25).sp
  }
  val fontSize = if (hasPosterPhoto) {
    when (posterConfig.fontStyle) {
      PosterFontStyle.EDITORIAL_SERIF -> 48.sp
      PosterFontStyle.CLEAN_SANS -> 46.sp
      PosterFontStyle.CONDENSED_BOLD -> 52.sp
    }
  } else {
    34.sp
  }
  val lineHeight = if (hasPosterPhoto) {
    when (posterConfig.fontStyle) {
      PosterFontStyle.EDITORIAL_SERIF -> 52.sp
      PosterFontStyle.CLEAN_SANS -> 50.sp
      PosterFontStyle.CONDENSED_BOLD -> 54.sp
    }
  } else {
    40.sp
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    if (!overline.isNullOrBlank()) {
      Text(
        text = overline,
        style = TextStyle(
          fontFamily = InterFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 14.sp,
          letterSpacing = 0.6.sp,
          fontFeatureSettings = "tnum",
        ),
        color = PureWhite.copy(alpha = 0.78f),
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
    }

    Text(
      text = if (hasPosterPhoto) formatStackedDisplayName(displayName) else displayName.trim(),
      style = TextStyle(
        fontFamily = fontFamily,
        fontWeight = if (hasPosterPhoto) FontWeight.Bold else FontWeight.SemiBold,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
      ),
      color = textColor,
      textAlign = TextAlign.Center,
      maxLines = 3,
      overflow = TextOverflow.Ellipsis,
    )

    if (!subtitle.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = subtitle,
        style = TextStyle(
          fontFamily = InterFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 15.sp,
          fontFeatureSettings = "tnum",
        ),
        color = PureWhite.copy(alpha = 0.78f),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ContactAvatarBadge(
  displayName: String,
  imageUri: String?,
  size: Dp = 44.dp,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(CharcoalSurface)
      .border(1.dp, ElevatedCardBorder, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    if (!imageUri.isNullOrBlank()) {
      AsyncImage(
        model = imageUri,
        contentDescription = displayName,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Text(
        text = computeMonogram(displayName),
        style = TextStyle(
          fontFamily = PlayfairDisplayFamily,
          fontWeight = FontWeight.Bold,
          fontSize = (size.value * 0.36f).sp,
        ),
        color = PureWhite.copy(alpha = 0.9f),
        maxLines = 1,
      )
    }
  }
}
