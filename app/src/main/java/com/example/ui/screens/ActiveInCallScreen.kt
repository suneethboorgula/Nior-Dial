package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddIcCall
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telecom.ActiveCallUiState
import com.example.telecom.AudioRouteType
import com.example.telecom.CallUiStatus
import com.example.ui.components.ContactPosterBackdrop
import com.example.ui.components.ContactPosterNameBlock
import com.example.ui.theme.CallCrimson
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.FrostedDarkFill
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassFill
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.InterFamily
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PureWhite

private val DTMF_ROWS = listOf(
  listOf('1' to "", '2' to "ABC", '3' to "DEF"),
  listOf('4' to "GHI", '5' to "JKL", '6' to "MNO"),
  listOf('7' to "PQRS", '8' to "TUV", '9' to "WXYZ"),
  listOf('*' to "", '0' to "+", '#' to ""),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveInCallScreen(
  uiState: ActiveCallUiState,
  onToggleMute: () -> Unit,
  onToggleSpeaker: () -> Unit,
  onToggleHold: () -> Unit,
  onSwapCalls: () -> Unit,
  onMergeCalls: () -> Unit,
  onAddCall: () -> Unit,
  onSplitParticipant: (String) -> Unit,
  onDisconnectParticipant: (String) -> Unit,
  onPlayDtmf: (Char) -> Unit,
  onStopDtmf: () -> Unit,
  onEndCall: () -> Unit,
  modifier: Modifier = Modifier,
  topOverlayPadding: Dp = 0.dp,
) {
  val primary = uiState.primaryCall ?: return
  var showDtmfOverlay by remember { mutableStateOf(false) }
  var dtmfHistory by remember { mutableStateOf("") }
  var showManageConferenceSheet by remember { mutableStateOf(false) }

  val statusText = when (primary.status) {
    CallUiStatus.DIALING, CallUiStatus.CONNECTING -> "Calling…"
    CallUiStatus.ACTIVE -> {
      val mins = primary.durationSeconds / 60
      val secs = primary.durationSeconds % 60
      String.format("%02d:%02d • Connected", mins, secs)
    }
    CallUiStatus.HOLDING -> "On Hold"
    CallUiStatus.RINGING -> "Ringing…"
    CallUiStatus.DISCONNECTED -> "Call Ended"
  }

  val subtitleLine = if (primary.isConference && primary.childNames.isNotEmpty()) {
    "$statusText • ${primary.childNames.joinToString(", ")}"
  } else {
    statusText
  }

  Box(modifier = modifier.fillMaxSize()) {
    ContactPosterBackdrop(
      displayName = primary.callerName,
      posterConfig = primary.posterConfig,
      fallbackPhotoUri = primary.fallbackPhotoUri,
      modifier = Modifier.fillMaxSize(),
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top section: Optional multi-call held pill / conference manage pill + caller poster header
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp + topOverlayPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (uiState.secondaryHeldCall != null) {
          val held = uiState.secondaryHeldCall
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(FrostedDarkFill)
              .border(1.dp, FrostedGlassBorder, RoundedCornerShape(18.dp))
              .clickable(enabled = uiState.canSwapCalls, onClick = onSwapCalls)
              .padding(horizontal = 14.dp, vertical = 10.dp)
              .testTag("held_call_banner"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = held.callerName,
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "On Hold",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlate,
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Rounded.SwapCalls,
                contentDescription = "Swap Calls",
                tint = PureWhite,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Swap",
                style = MaterialTheme.typography.labelLarge,
                color = PureWhite,
              )
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
        } else if (uiState.isConference && uiState.conferenceParticipants.isNotEmpty()) {
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(FrostedDarkFill)
              .border(1.dp, FrostedGlassBorder, RoundedCornerShape(20.dp))
              .clickable { showManageConferenceSheet = true }
              .padding(horizontal = 16.dp, vertical = 8.dp)
              .testTag("manage_conference_pill"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Group,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Manage ${uiState.conferenceParticipants.size} Participants",
              style = MaterialTheme.typography.labelLarge,
              color = PureWhite,
            )
          }
          Spacer(modifier = Modifier.height(14.dp))
        } else {
          Spacer(modifier = Modifier.height(24.dp))
        }

        val hasPosterPhoto = !primary.posterConfig.posterUri.isNullOrBlank() ||
          !primary.fallbackPhotoUri.isNullOrBlank()
        ContactPosterNameBlock(
          displayName = primary.callerName,
          posterConfig = primary.posterConfig,
          overline = if (!hasPosterPhoto) subtitleLine else null,
          subtitle = if (hasPosterPhoto) subtitleLine else primary.callerNumber.takeIf {
            !primary.isConference && it != primary.callerName
          },
          hasPosterPhoto = hasPosterPhoto,
        )
      }

      // Bottom section: Either In-Call Controls Grid OR DTMF Keypad Overlay + End Call Button
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        AnimatedVisibility(
          visible = !showDtmfOverlay,
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              InCallControlButton(
                label = "Mute",
                icon = Icons.Rounded.MicOff,
                isActive = uiState.isMuted,
                tag = "incall_mute_button",
                onClick = onToggleMute,
              )
              InCallControlButton(
                label = "Keypad",
                icon = Icons.Rounded.Dialpad,
                isActive = false,
                tag = "incall_keypad_button",
                onClick = { showDtmfOverlay = true },
              )
              InCallControlButton(
                label = "Speaker",
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                isActive = uiState.audioRoute == AudioRouteType.SPEAKER,
                tag = "incall_speaker_button",
                onClick = onToggleSpeaker,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              InCallControlButton(
                label = if (primary.status == CallUiStatus.HOLDING) "Resume" else "Hold",
                icon = Icons.Rounded.Pause,
                isActive = primary.status == CallUiStatus.HOLDING,
                enabled = primary.canHold || primary.status == CallUiStatus.ACTIVE || primary.status == CallUiStatus.HOLDING,
                tag = "incall_hold_button",
                onClick = onToggleHold,
              )
              InCallControlButton(
                label = "Add Call",
                icon = Icons.Rounded.AddIcCall,
                isActive = false,
                enabled = uiState.canAddCall,
                tag = "incall_add_call_button",
                onClick = onAddCall,
              )
              InCallControlButton(
                label = "Merge",
                icon = Icons.Rounded.CallMerge,
                isActive = uiState.isConference,
                enabled = uiState.canMergeCalls,
                tag = "incall_merge_calls_button",
                onClick = onMergeCalls,
              )
            }
          }
        }

        AnimatedVisibility(
          visible = showDtmfOverlay,
          enter = slideInVertically { it / 2 } + fadeIn(),
          exit = slideOutVertically { it / 2 } + fadeOut(),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(28.dp))
              .background(FrostedDarkFill)
              .border(1.dp, FrostedGlassBorder, RoundedCornerShape(28.dp))
              .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Spacer(modifier = Modifier.size(40.dp))
              Text(
                text = dtmfHistory.ifBlank { "Keypad" },
                style = TextStyle(
                  fontFamily = InterFamily,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 20.sp,
                  fontFeatureSettings = "tnum",
                ),
                color = PureWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              IconButton(
                onClick = { showDtmfOverlay = false },
                modifier = Modifier
                  .size(40.dp)
                  .testTag("dtmf_close_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = "Hide Keypad",
                  tint = PureWhite,
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            DTMF_ROWS.forEach { row ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
              ) {
                row.forEach { (digit, sub) ->
                  DtmfKeyButton(
                    digit = digit,
                    sub = sub,
                    onPress = {
                      dtmfHistory += digit
                      onPlayDtmf(digit)
                    },
                    onRelease = onStopDtmf,
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(34.dp))

        // Centered Red End Call Button
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(CallCrimson)
            .clickable(onClick = onEndCall)
            .testTag("incall_end_call_button"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.CallEnd,
            contentDescription = "End Call",
            tint = PureWhite,
            modifier = Modifier.size(34.dp),
          )
        }
      }
    }
  }

  if (showManageConferenceSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showManageConferenceSheet = false },
      sheetState = sheetState,
      containerColor = CharcoalSurface,
      contentColor = PureWhite,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Conference Participants",
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
        )
        uiState.conferenceParticipants.forEach { participant ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(GraphiteSurface)
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = participant.callerName,
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
              )
              Text(
                text = participant.callerNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlate,
              )
            }
            if (participant.canSeparate) {
              IconButton(
                onClick = {
                  onSplitParticipant(participant.callId)
                  showManageConferenceSheet = false
                },
                modifier = Modifier.testTag("split_participant_${participant.callId}"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.CallSplit,
                  contentDescription = "Private / Split",
                  tint = PureWhite,
                )
              }
            }
            IconButton(
              onClick = { onDisconnectParticipant(participant.callId) },
              modifier = Modifier.testTag("drop_participant_${participant.callId}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.CallEnd,
                contentDescription = "Drop Participant",
                tint = CallCrimson,
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun InCallControlButton(
  label: String,
  icon: ImageVector,
  isActive: Boolean,
  enabled: Boolean = true,
  tag: String,
  onClick: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.alpha(if (enabled) 1f else 0.38f),
  ) {
    Box(
      modifier = Modifier
        .size(68.dp)
        .clip(CircleShape)
        .background(if (isActive) PureWhite else FrostedGlassFill)
        .border(1.dp, if (isActive) PureWhite else FrostedGlassBorder, CircleShape)
        .clickable(enabled = enabled, onClick = onClick)
        .testTag(tag),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isActive) OledBlack else PureWhite,
        modifier = Modifier.size(28.dp),
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = PureWhite.copy(alpha = 0.9f),
    )
  }
}

@Composable
private fun DtmfKeyButton(
  digit: Char,
  sub: String,
  onPress: () -> Unit,
  onRelease: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  LaunchedEffect(isPressed) {
    if (isPressed) {
      onPress()
    } else {
      onRelease()
    }
  }

  Box(
    modifier = Modifier
      .size(64.dp)
      .clip(CircleShape)
      .background(if (isPressed) PureWhite.copy(alpha = 0.32f) else FrostedGlassFill)
      .border(1.dp, FrostedGlassBorder, CircleShape)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {},
      )
      .testTag("dtmf_key_$digit"),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = digit.toString(),
        style = TextStyle(
          fontFamily = InterFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 24.sp,
        ),
        color = PureWhite,
      )
      if (sub.isNotEmpty()) {
        Text(
          text = sub,
          style = TextStyle(
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
          ),
          color = PureWhite.copy(alpha = 0.7f),
        )
      }
    }
  }
}
