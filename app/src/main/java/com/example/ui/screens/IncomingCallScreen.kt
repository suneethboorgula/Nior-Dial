package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.telecom.CallEntry
import com.example.ui.components.ContactPosterBackdrop
import com.example.ui.components.ContactPosterNameBlock
import com.example.ui.theme.CallCrimson
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElevatedCardBorder
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassFill
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PureWhite

private val QUICK_SMS_TEMPLATES = listOf(
  "Can't talk right now. I'll call you back shortly.",
  "In a meeting — text me if it's urgent.",
  "On my way! Give me a few minutes.",
)

private val REMINDER_OPTIONS = listOf(
  15 to "In 15 minutes",
  30 to "In 30 minutes",
  60 to "In 1 hour",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingCallScreen(
  call: CallEntry,
  onAccept: () -> Unit,
  onDecline: () -> Unit,
  onRejectWithMessage: (String) -> Unit,
  onScheduleReminder: (minutes: Int) -> Unit,
  modifier: Modifier = Modifier,
  topOverlayPadding: Dp = 0.dp,
) {
  var showQuickSmsSheet by remember { mutableStateOf(false) }
  var showRemindSheet by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize()) {
    ContactPosterBackdrop(
      displayName = call.callerName,
      posterConfig = call.posterConfig,
      fallbackPhotoUri = call.fallbackPhotoUri,
      modifier = Modifier.fillMaxSize(),
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top section: Subtitle + Large stylized name (or clean name/number if no poster)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 44.dp + topOverlayPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        val hasPosterPhoto = !call.posterConfig.posterUri.isNullOrBlank() ||
          !call.fallbackPhotoUri.isNullOrBlank()
        ContactPosterNameBlock(
          displayName = call.callerName,
          posterConfig = call.posterConfig,
          overline = stringResource(R.string.incoming_call_subtitle),
          subtitle = call.callerNumber.takeIf { it != call.callerName },
          hasPosterPhoto = hasPosterPhoto,
        )
      }

      // Middle + Bottom controls
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 420.dp)
          .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Middle row: Frosted circular Remind Me & Message buttons centered above Decline & Accept
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(120.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier.width(88.dp),
            contentAlignment = Alignment.Center,
          ) {
            FrostedSmallAction(
              label = "Remind Me",
              icon = Icons.Rounded.Alarm,
              tag = "incoming_remind_me_button",
              onClick = { showRemindSheet = true },
            )
          }
          Box(
            modifier = Modifier.width(88.dp),
            contentAlignment = Alignment.Center,
          ) {
            FrostedSmallAction(
              label = "Message",
              icon = Icons.AutoMirrored.Rounded.Message,
              tag = "incoming_message_button",
              onClick = { showQuickSmsSheet = true },
            )
          }
        }

        Spacer(modifier = Modifier.height(42.dp))

        // Bottom row: Large Red Decline (left) & Large Green Accept (right) with 130.dp gap
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(130.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(
            modifier = Modifier.width(78.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Box(
              modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(CallCrimson)
                .clickable(onClick = onDecline)
                .testTag("incoming_decline_button"),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.CallEnd,
                contentDescription = "Decline Call",
                tint = PureWhite,
                modifier = Modifier.size(34.dp),
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Decline",
              style = MaterialTheme.typography.bodyMedium,
              color = PureWhite,
            )
          }

          Column(
            modifier = Modifier.width(78.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Box(
              modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(CallGreen)
                .clickable(onClick = onAccept)
                .testTag("incoming_accept_button"),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = "Accept Call",
                tint = PureWhite,
                modifier = Modifier.size(34.dp),
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Accept",
              style = MaterialTheme.typography.bodyMedium,
              color = PureWhite,
            )
          }
        }
      }
    }
  }

  if (showQuickSmsSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customSms by remember { mutableStateOf("") }
    ModalBottomSheet(
      onDismissRequest = { showQuickSmsSheet = false },
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
          text = "Decline with Message",
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
        )
        QUICK_SMS_TEMPLATES.forEachIndexed { idx, template ->
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(GraphiteSurface)
              .clickable {
                showQuickSmsSheet = false
                onRejectWithMessage(template)
              }
              .padding(14.dp)
              .testTag("quick_sms_template_$idx"),
          ) {
            Text(
              text = template,
              style = MaterialTheme.typography.bodyLarge,
              color = PureWhite,
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = customSms,
            onValueChange = { customSms = it },
            placeholder = { Text("Custom reply…", color = MutedSlate) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = ElevatedCardBorder,
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              cursorColor = PureWhite,
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("quick_sms_custom_input"),
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            enabled = customSms.isNotBlank(),
            onClick = {
              val msg = customSms.trim()
              if (msg.isNotEmpty()) {
                showQuickSmsSheet = false
                onRejectWithMessage(msg)
              }
            },
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(if (customSms.isNotBlank()) PureWhite else GraphiteSurface)
              .testTag("quick_sms_send_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Send,
              contentDescription = "Send Reply",
              tint = if (customSms.isNotBlank()) CharcoalSurface else MutedSlate,
            )
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }

  if (showRemindSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showRemindSheet = false },
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
          text = "Remind Me to Call Back",
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
        )
        REMINDER_OPTIONS.forEach { (minutes, label) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(GraphiteSurface)
              .clickable {
                showRemindSheet = false
                onScheduleReminder(minutes)
              }
              .padding(horizontal = 16.dp, vertical = 14.dp)
              .testTag("remind_option_$minutes"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Alarm,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = label,
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Medium,
              color = PureWhite,
            )
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun FrostedSmallAction(
  label: String,
  icon: ImageVector,
  tag: String,
  onClick: () -> Unit,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .size(54.dp)
        .clip(CircleShape)
        .background(FrostedGlassFill)
        .border(1.dp, FrostedGlassBorder, CircleShape)
        .clickable(onClick = onClick)
        .testTag(tag),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = PureWhite,
        modifier = Modifier.size(24.dp),
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
