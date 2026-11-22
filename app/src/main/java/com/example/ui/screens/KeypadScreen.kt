package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.ui.theme.ElevatedCardBorder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PhoneNumberFormatter
import com.example.ui.DialerMatchItem
import com.example.ui.MainUiState
import com.example.ui.components.ContactAvatarBadge
import com.example.ui.components.DefaultDialerBanner
import com.example.ui.components.PermissionRequestCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.InterFamily
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtleText

private data class KeypadKey(
  val digit: String,
  val subLetters: String,
  val longPressValue: String? = null,
)

private val KEYPAD_ROWS = listOf(
  listOf(KeypadKey("1", ""), KeypadKey("2", "ABC"), KeypadKey("3", "DEF")),
  listOf(KeypadKey("4", "GHI"), KeypadKey("5", "JKL"), KeypadKey("6", "MNO")),
  listOf(KeypadKey("7", "PQRS"), KeypadKey("8", "TUV"), KeypadKey("9", "WXYZ")),
  listOf(KeypadKey("*", ""), KeypadKey("0", "+", "+"), KeypadKey("#", "")),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadScreen(
  uiState: MainUiState,
  onDigitPress: (String) -> Unit,
  onBackspace: () -> Unit,
  onClearAll: () -> Unit,
  onCallClick: (String) -> Unit,
  onOpenPoster: (DialerMatchItem) -> Unit,
  onOpenCallPreview: () -> Unit,
  onNavigateRecents: () -> Unit,
  onNavigateContacts: () -> Unit,
  onRequestDefaultDialer: () -> Unit,
  onRequestPermissions: () -> Unit,
  modifier: Modifier = Modifier,
  applyStatusBarsPadding: Boolean = true,
) {
  val haptics = LocalHapticFeedback.current

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(OledBlack)
      .then(if (applyStatusBarsPadding) Modifier.statusBarsPadding() else Modifier)
      .navigationBarsPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 560.dp)
        .padding(horizontal = 20.dp),
    ) {
      if (!uiState.isDefaultDialer) {
        Spacer(modifier = Modifier.height(8.dp))
        DefaultDialerBanner(onSetDefaultClick = onRequestDefaultDialer)
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.titleMedium,
          color = SubtleText,
        )
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CharcoalSurface)
            .border(1.dp, ElevatedCardBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenCallPreview)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("keypad_call_preview_button"),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = PureWhite,
            modifier = Modifier.size(15.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Preview",
            style = MaterialTheme.typography.labelLarge,
            color = PureWhite,
          )
        }
      }

      // Top Section: Scrollable recent / T9-matching contacts
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(top = 8.dp),
      ) {
        if (!uiState.permissions.allCoreGranted && uiState.matchingDialerItems.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            PermissionRequestCard(
              title = "Enable Phone & Contacts",
              description = "Grant Phone, Contacts, and Call Log permissions to filter matches as you dial and place calls.",
              icon = Icons.Rounded.Phone,
              buttonTag = "keypad_grant_permission_button",
              onGrantClick = onRequestPermissions,
            )
          }
        } else if (uiState.matchingDialerItems.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (uiState.dialedNumber.isBlank()) {
                "Recent & favorite contacts appear here"
              } else {
                "No matching contacts"
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MutedSlate,
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            items(uiState.matchingDialerItems, key = { it.id }) { item ->
              DialerMatchRow(
                item = item,
                onRowClick = { onOpenPoster(item) },
                onQuickCall = { onCallClick(item.phoneNumber) },
              )
            }
          }
        }
      }

      // Middle Section: Centered formatted phone number display + backspace
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(68.dp)
          .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Spacer(modifier = Modifier.size(48.dp))

        val formattedDisplay = if (uiState.dialedNumber.isBlank()) {
          ""
        } else {
          PhoneNumberFormatter.formatForDisplay(uiState.dialedNumber)
        }

        Text(
          text = formattedDisplay,
          style = TextStyle(
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = if (formattedDisplay.length > 14) 26.sp else 34.sp,
            fontFeatureSettings = "tnum",
            letterSpacing = 0.5.sp,
          ),
          color = PureWhite,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .weight(1f)
            .testTag("dialed_number_display"),
        )

        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(
              enabled = uiState.dialedNumber.isNotEmpty(),
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onBackspace()
              },
              onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClearAll()
              },
            )
            .testTag("keypad_backspace_button"),
          contentAlignment = Alignment.Center,
        ) {
          if (uiState.dialedNumber.isNotEmpty()) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Backspace,
              contentDescription = "Backspace",
              tint = SubtleText,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Bottom Section: 3x4 circular dark gray keypad grid
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        KEYPAD_ROWS.forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            row.forEach { key ->
              Box(
                modifier = Modifier
                  .size(76.dp)
                  .clip(CircleShape)
                  .background(CharcoalSurface)
                  .combinedClickable(
                    onClick = {
                      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      onDigitPress(key.digit)
                    },
                    onLongClick = {
                      if (key.longPressValue != null) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDigitPress(key.longPressValue)
                      }
                    },
                  )
                  .testTag("keypad_key_${key.digit}"),
                contentAlignment = Alignment.Center,
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = key.digit,
                    style = TextStyle(
                      fontFamily = InterFamily,
                      fontWeight = FontWeight.Normal,
                      fontSize = 30.sp,
                      fontFeatureSettings = "tnum",
                    ),
                    color = PureWhite,
                  )
                  if (key.subLetters.isNotEmpty()) {
                    Text(
                      text = key.subLetters,
                      style = TextStyle(
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.6.sp,
                      ),
                      color = MutedSlate,
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Bottom action row: Recents (left) — White Call Button (center) — Contacts (right)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onNavigateRecents)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("nav_recents_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = stringResource(R.string.recents_label),
            tint = PureWhite,
            modifier = Modifier.size(26.dp),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = stringResource(R.string.recents_label),
            style = MaterialTheme.typography.labelSmall,
            color = SubtleText,
          )
        }

        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(PureWhite)
            .clickable {
              val toCall = uiState.dialedNumber.ifBlank {
                uiState.callLog.firstOrNull()?.number.orEmpty()
              }
              if (toCall.isNotBlank()) {
                onCallClick(toCall)
              }
            }
            .testTag("keypad_call_button"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Call,
            contentDescription = "Place Call",
            tint = OledBlack,
            modifier = Modifier.size(32.dp),
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onNavigateContacts)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("nav_contacts_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = stringResource(R.string.contacts_label),
            tint = PureWhite,
            modifier = Modifier.size(26.dp),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = stringResource(R.string.contacts_label),
            style = MaterialTheme.typography.labelSmall,
            color = SubtleText,
          )
        }
      }
    }
  }
}

@Composable
private fun DialerMatchRow(
  item: DialerMatchItem,
  onRowClick: () -> Unit,
  onQuickCall: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onRowClick)
      .padding(horizontal = 10.dp, vertical = 8.dp)
      .testTag("dialer_match_${item.id}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ContactAvatarBadge(
      displayName = item.displayName,
      imageUri = item.avatarUri,
      size = 42.dp,
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = item.displayName,
        style = MaterialTheme.typography.titleMedium,
        color = PureWhite,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = item.formattedNumber,
        style = MaterialTheme.typography.bodyMedium,
        color = MutedSlate,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    IconButton(
      onClick = onQuickCall,
      modifier = Modifier
        .size(48.dp)
        .testTag("dialer_quick_call_${item.id}"),
    ) {
      Icon(
        imageVector = Icons.Rounded.Call,
        contentDescription = "Call ${item.displayName}",
        tint = PureWhite,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}
