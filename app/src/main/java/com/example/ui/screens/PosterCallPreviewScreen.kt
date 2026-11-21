package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ContactPosterConfig
import com.example.telecom.ActiveCallUiState
import com.example.telecom.AudioRouteType
import com.example.telecom.CallEntry
import com.example.telecom.CallUiStatus
import com.example.ui.theme.FrostedDarkFill
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.delay

@Composable
fun PosterCallPreviewScreen(
  displayName: String,
  phoneNumber: String,
  posterConfig: ContactPosterConfig,
  fallbackPhotoUri: String?,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var previewIncoming by remember { mutableStateOf(true) }
  var isMuted by remember { mutableStateOf(false) }
  var isSpeaker by remember { mutableStateOf(false) }
  var isOnHold by remember { mutableStateOf(false) }
  var durationSec by remember { mutableLongStateOf(14L) }

  val effectiveName = posterConfig.customCallerName?.takeIf { it.isNotBlank() } ?: displayName
  val effectiveNumber = posterConfig.customCallerNumber?.takeIf { it.isNotBlank() } ?: phoneNumber

  LaunchedEffect(previewIncoming, isOnHold) {
    if (!previewIncoming && !isOnHold) {
      while (true) {
        delay(1000L)
        durationSec += 1
      }
    }
  }

  val previewCall = CallEntry(
    callId = "preview_call",
    callerName = effectiveName,
    callerNumber = effectiveNumber,
    status = when {
      previewIncoming -> CallUiStatus.RINGING
      isOnHold -> CallUiStatus.HOLDING
      else -> CallUiStatus.ACTIVE
    },
    isIncoming = previewIncoming,
    connectTimeMillis = System.currentTimeMillis(),
    durationSeconds = durationSec,
    canHold = true,
    canSeparate = false,
    isConference = false,
    posterConfig = posterConfig,
    fallbackPhotoUri = fallbackPhotoUri,
  )

  val previewActiveState = ActiveCallUiState(
    primaryCall = previewCall,
    secondaryHeldCall = null,
    allCalls = listOf(previewCall),
    canAddCall = true,
    canMergeCalls = false,
    canSwapCalls = false,
    isConference = false,
    isMuted = isMuted,
    audioRoute = if (isSpeaker) AudioRouteType.SPEAKER else AudioRouteType.EARPIECE,
  )

  Box(modifier = modifier.fillMaxSize()) {
    if (previewIncoming) {
      IncomingCallScreen(
        call = previewCall,
        onAccept = { previewIncoming = false },
        onDecline = onClose,
        onRejectWithMessage = { onClose() },
        onScheduleReminder = { onClose() },
        topOverlayPadding = 44.dp,
      )
    } else {
      ActiveInCallScreen(
        uiState = previewActiveState,
        onToggleMute = { isMuted = !isMuted },
        onToggleSpeaker = { isSpeaker = !isSpeaker },
        onToggleHold = { isOnHold = !isOnHold },
        onSwapCalls = {},
        onMergeCalls = {},
        onAddCall = {},
        onSplitParticipant = {},
        onDisconnectParticipant = {},
        onPlayDtmf = {},
        onStopDtmf = {},
        onEndCall = onClose,
        topOverlayPadding = 44.dp,
      )
    }

    // Floating Preview Mode Pill at Top
    Row(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .statusBarsPadding()
        .padding(top = 6.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(FrostedDarkFill)
        .border(1.dp, FrostedGlassBorder, RoundedCornerShape(24.dp))
        .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      PreviewTabPill(
        label = "Incoming",
        selected = previewIncoming,
        tag = "preview_tab_incoming",
        onClick = { previewIncoming = true },
      )
      PreviewTabPill(
        label = "Active Call",
        selected = !previewIncoming,
        tag = "preview_tab_active",
        onClick = { previewIncoming = false },
      )
      Spacer(modifier = Modifier.width(2.dp))
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(18.dp))
          .clickable(onClick = onClose)
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("preview_close_button"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = "Close Preview",
          tint = PureWhite,
        )
      }
    }
  }
}

@Composable
private fun PreviewTabPill(
  label: String,
  selected: Boolean,
  tag: String,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(18.dp))
      .background(if (selected) PureWhite else FrostedDarkFill)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 6.dp)
      .testTag(tag),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold,
      color = if (selected) OledBlack else MutedSlate,
    )
  }
}
