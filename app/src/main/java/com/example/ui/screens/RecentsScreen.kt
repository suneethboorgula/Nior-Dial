package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CallLogEntry
import com.example.data.CallLogType
import com.example.data.PhoneNumberFormatter
import com.example.ui.MainUiState
import com.example.ui.components.ContactAvatarBadge
import com.example.ui.components.PermissionRequestCard
import com.example.ui.theme.CallCrimson
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElevatedCardBorder
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.PureWhite

@Composable
fun RecentsScreen(
  uiState: MainUiState,
  onCallClick: (String) -> Unit,
  onViewPoster: (CallLogEntry) -> Unit,
  onNavigateKeypad: () -> Unit,
  onBack: () -> Unit,
  onRequestCallLogPermission: () -> Unit,
  modifier: Modifier = Modifier,
  applyStatusBarsPadding: Boolean = true,
) {
  var showMissedOnly by remember { mutableStateOf(false) }

  val displayedLogs = remember(uiState.callLog, showMissedOnly) {
    if (showMissedOnly) {
      uiState.callLog.filter { it.type == CallLogType.MISSED || it.type == CallLogType.REJECTED }
    } else {
      uiState.callLog
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(OledBlack)
      .then(if (applyStatusBarsPadding) Modifier.statusBarsPadding() else Modifier),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 600.dp)
        .padding(horizontal = 20.dp),
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(48.dp)
            .testTag("recents_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            tint = PureWhite,
          )
        }

        // All / Missed segmented filter
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CharcoalSurface)
            .border(1.dp, ElevatedCardBorder, RoundedCornerShape(20.dp))
            .padding(3.dp),
        ) {
          FilterSegment(
            label = "All",
            selected = !showMissedOnly,
            onClick = { showMissedOnly = false },
            tag = "recents_filter_all",
          )
          FilterSegment(
            label = "Missed",
            selected = showMissedOnly,
            onClick = { showMissedOnly = true },
            tag = "recents_filter_missed",
          )
        }
      }

      Text(
        text = stringResource(R.string.recents_header),
        style = MaterialTheme.typography.displayLarge.copy(
          fontFamily = PlayfairDisplayFamily,
          fontSize = 42.sp,
        ),
        color = PureWhite,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "${displayedLogs.size} Calls",
        style = MaterialTheme.typography.bodyMedium,
        color = MutedSlate,
      )

      Spacer(modifier = Modifier.height(14.dp))

      if (!uiState.permissions.hasReadCallLog) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center,
        ) {
          PermissionRequestCard(
            title = "Call Log Permission Needed",
            description = "Allow access to view incoming, outgoing, and missed call history.",
            icon = Icons.Rounded.History,
            buttonTag = "recents_grant_permission_button",
            onGrantClick = onRequestCallLogPermission,
          )
        }
      } else if (displayedLogs.isEmpty()) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (showMissedOnly) "No missed calls" else "No recent calls yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MutedSlate,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentPadding = PaddingValues(bottom = 112.dp),
        ) {
          items(displayedLogs, key = { it.id }) { entry ->
            val norm = PhoneNumberFormatter.matchKey(entry.number)
            val matchedContact = uiState.contacts.firstOrNull { c ->
              c.phoneNumbers.any { PhoneNumberFormatter.matchKey(it) == norm }
            }
            val config = matchedContact?.let { uiState.posterConfigs[it.lookupKey] }
              ?: uiState.posterConfigs[norm]
            val avatarUri = config?.posterUri ?: matchedContact?.photoUri
            val displayName = config?.customCallerName?.takeIf { it.isNotBlank() }
              ?: matchedContact?.displayName
              ?: entry.cachedName
              ?: PhoneNumberFormatter.formatForDisplay(entry.number)

            RecentCallRow(
              entry = entry,
              displayName = displayName,
              avatarUri = avatarUri,
              onCallClick = { onCallClick(entry.number) },
              onPosterClick = { onViewPoster(entry) },
            )
            HorizontalDivider(color = CharcoalSurface, thickness = 0.5.dp)
          }
        }
      }
    }

    // Floating red circular keypad button at bottom center
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = 20.dp)
        .size(64.dp)
        .clip(CircleShape)
        .background(CallCrimson)
        .clickable(onClick = onNavigateKeypad)
        .testTag("recents_floating_keypad_button"),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.Dialpad,
        contentDescription = "Open Keypad",
        tint = PureWhite,
        modifier = Modifier.size(28.dp),
      )
    }
  }
}

@Composable
private fun FilterSegment(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  tag: String,
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(17.dp))
      .background(if (selected) PureWhite else CharcoalSurface)
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 7.dp)
      .testTag(tag),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) OledBlack else MutedSlate,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
private fun RecentCallRow(
  entry: CallLogEntry,
  displayName: String,
  avatarUri: String?,
  onCallClick: () -> Unit,
  onPosterClick: () -> Unit,
) {
  val isMissed = entry.type == CallLogType.MISSED || entry.type == CallLogType.REJECTED
  val typeIcon = when (entry.type) {
    CallLogType.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived
    CallLogType.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade
    CallLogType.MISSED, CallLogType.REJECTED -> Icons.AutoMirrored.Rounded.CallMissed
  }
  val typeLabel = when (entry.type) {
    CallLogType.INCOMING -> "Incoming"
    CallLogType.OUTGOING -> "Outgoing"
    CallLogType.MISSED -> "Missed"
    CallLogType.REJECTED -> "Declined"
  }
  val iconTint = when (entry.type) {
    CallLogType.MISSED, CallLogType.REJECTED -> CallCrimson
    CallLogType.INCOMING -> CallGreen
    CallLogType.OUTGOING -> MutedSlate
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onCallClick)
      .padding(vertical = 10.dp, horizontal = 4.dp)
      .testTag("recent_row_${entry.id}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ContactAvatarBadge(
      displayName = displayName,
      imageUri = avatarUri,
      size = 44.dp,
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = displayName,
        style = MaterialTheme.typography.titleMedium,
        color = if (isMissed) CallCrimson else PureWhite,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = typeIcon,
          contentDescription = typeLabel,
          tint = iconTint,
          modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = "$typeLabel • ${entry.formattedTime}",
          style = MaterialTheme.typography.bodyMedium,
          color = MutedSlate,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    IconButton(
      onClick = onCallClick,
      modifier = Modifier
        .size(48.dp)
        .testTag("recent_call_btn_${entry.id}"),
    ) {
      Icon(
        imageVector = Icons.Rounded.Call,
        contentDescription = "Call $displayName",
        tint = PureWhite,
        modifier = Modifier.size(20.dp),
      )
    }

    IconButton(
      onClick = onPosterClick,
      modifier = Modifier
        .size(48.dp)
        .testTag("recent_poster_btn_${entry.id}"),
    ) {
      Icon(
        imageVector = Icons.Rounded.Info,
        contentDescription = "View Contact Poster",
        tint = MutedSlate,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}
