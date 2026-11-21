package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CallLogEntry
import com.example.data.CallLogType
import com.example.data.ContactItem
import com.example.data.ContactPosterConfig
import com.example.data.PhoneNumberFormatter
import com.example.data.PosterAlignment
import com.example.data.PosterFontStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.components.ContactPosterBackdrop
import com.example.ui.components.ContactPosterNameBlock
import com.example.ui.components.posterFontFamily
import com.example.ui.theme.CallCrimson
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElevatedCardBorder
import com.example.ui.theme.FrostedDarkFill
import com.example.ui.theme.FrostedGlassBorder
import com.example.ui.theme.FrostedGlassFill
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PosterTextSwatches
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtleText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPosterProfileScreen(
  contact: ContactItem?,
  fallbackName: String,
  fallbackNumber: String,
  posterConfig: ContactPosterConfig,
  callLogs: List<CallLogEntry>,
  onBack: () -> Unit,
  onCall: (String) -> Unit,
  onPhotoPicked: (Uri) -> Unit,
  onUseSamplePhoto: () -> Unit,
  onPreviewCall: () -> Unit,
  onEditContactInfo: (newName: String, newNumber: String) -> Unit,
  onUpdateTypography: (
    fontStyle: PosterFontStyle?,
    textColorHex: Long?,
    alignment: PosterAlignment?,
    notes: String?,
  ) -> Unit,
  onResetPoster: () -> Unit,
  onLaunchSystemIntent: (Intent, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val displayName = posterConfig.customCallerName?.takeIf { it.isNotBlank() }
    ?: contact?.displayName
    ?: fallbackName.ifBlank {
      PhoneNumberFormatter.formatForDisplay(fallbackNumber)
    }
  val primaryNumber = posterConfig.customCallerNumber?.takeIf { it.isNotBlank() }
    ?: contact?.phoneNumbers?.firstOrNull()
    ?: fallbackNumber
  val formattedPrimary = PhoneNumberFormatter.formatForDisplay(primaryNumber)

  var showEditorControls by remember { mutableStateOf(false) }
  var showEditContactSheet by remember { mutableStateOf(false) }
  var notesText by remember(posterConfig.contactKey, posterConfig.notes) {
    mutableStateOf(posterConfig.notes)
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        onPhotoPicked(uri)
      }
    },
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(OledBlack)
      .verticalScroll(rememberScrollState())
      .navigationBarsPadding(),
  ) {
    // Full-screen style hero Contact Poster section
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(540.dp),
    ) {
      ContactPosterBackdrop(
        displayName = displayName,
        posterConfig = posterConfig,
        fallbackPhotoUri = contact?.photoUri,
        modifier = Modifier.fillMaxSize(),
      )

      // Top navigation controls
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(FrostedDarkFill)
            .border(1.dp, FrostedGlassBorder, CircleShape)
            .clickable(onClick = onBack)
            .testTag("poster_back_button"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            tint = PureWhite,
          )
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(22.dp))
              .background(FrostedDarkFill)
              .border(1.dp, FrostedGlassBorder, RoundedCornerShape(22.dp))
              .clickable(onClick = onPreviewCall)
              .padding(horizontal = 14.dp, vertical = 10.dp)
              .testTag("poster_preview_call_button"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Preview",
              style = MaterialTheme.typography.labelLarge,
              color = PureWhite,
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(22.dp))
              .background(FrostedDarkFill)
              .border(1.dp, FrostedGlassBorder, RoundedCornerShape(22.dp))
              .clickable { showEditorControls = !showEditorControls }
              .padding(horizontal = 16.dp, vertical = 10.dp)
              .testTag("poster_edit_toggle_button"),
          ) {
            Text(
              text = if (showEditorControls) "Done" else "Edit Poster",
              style = MaterialTheme.typography.labelLarge,
              color = PureWhite,
            )
          }
        }
      }

      // Stylized stacked contact name (respects Top vs Bottom alignment)
      val nameAlignment = if (posterConfig.nameAlignment == PosterAlignment.TOP) {
        Alignment.TopCenter
      } else {
        Alignment.BottomCenter
      }
      val namePadding = if (posterConfig.nameAlignment == PosterAlignment.TOP) {
        Modifier.padding(top = 112.dp, start = 24.dp, end = 24.dp)
      } else {
        Modifier.padding(bottom = 118.dp, start = 24.dp, end = 24.dp)
      }

      val hasPosterPhoto = !posterConfig.posterUri.isNullOrBlank() || !contact?.photoUri.isNullOrBlank()
      ContactPosterNameBlock(
        displayName = displayName,
        posterConfig = posterConfig,
        subtitle = formattedPrimary.takeIf { it != displayName },
        hasPosterPhoto = hasPosterPhoto,
        modifier = Modifier
          .align(nameAlignment)
          .then(namePadding),
      )

      // Frosted-glass 4-button action row pinned at bottom of poster
      Row(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        PosterActionSquare(
          label = "Message",
          icon = Icons.AutoMirrored.Rounded.Message,
          tag = "poster_action_message",
          modifier = Modifier.weight(1f),
          onClick = {
            if (primaryNumber.isNotBlank()) {
              val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(primaryNumber)}"))
              onLaunchSystemIntent(intent, "No SMS app available")
            }
          },
        )
        PosterActionSquare(
          label = "Call",
          icon = Icons.Rounded.Call,
          tag = "poster_action_call",
          modifier = Modifier.weight(1f),
          onClick = {
            if (primaryNumber.isNotBlank()) onCall(primaryNumber)
          },
        )
        PosterActionSquare(
          label = "Video",
          icon = Icons.Rounded.Videocam,
          tag = "poster_action_video",
          modifier = Modifier.weight(1f),
          onClick = {
            if (primaryNumber.isNotBlank()) {
              val videoIntent = Intent("com.android.phone.Videocall").apply {
                putExtra("videocall", true)
                data = Uri.fromParts("tel", primaryNumber, null)
              }
              val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tel:${Uri.encode(primaryNumber)}"))
              runCatching { context.startActivity(videoIntent) }.onFailure {
                onLaunchSystemIntent(fallbackIntent, "No video calling app available")
              }
            }
          },
        )
        PosterActionSquare(
          label = "Mail",
          icon = Icons.Rounded.Email,
          tag = "poster_action_mail",
          modifier = Modifier.weight(1f),
          onClick = {
            val email = contact?.email.orEmpty()
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(email)}"))
            onLaunchSystemIntent(intent, "No email app available")
          },
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Poster Editor Card — only shown when "Edit Poster" at top is toggled on
      AnimatedVisibility(visible = showEditorControls) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CharcoalSurface)
            .border(1.dp, ElevatedCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp),
        ) {
          Text(
            text = "POSTER EDITOR",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSlate,
          )
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = PureWhite,
                contentColor = OledBlack,
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1.4f)
                .height(48.dp)
                .testTag("choose_gallery_photo_cta"),
            ) {
              Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Gallery Photo",
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            OutlinedButton(
              onClick = onUseSamplePhoto,
              shape = RoundedCornerShape(14.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, ElevatedCardBorder),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("use_sample_poster_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Sample Pic",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "TYPOGRAPHY STYLE",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSlate,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PosterFontStyle.entries.forEach { style ->
              val selected = posterConfig.fontStyle == style
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .background(if (selected) PureWhite else GraphiteSurface)
                  .clickable { onUpdateTypography(style, null, null, null) }
                  .padding(vertical = 10.dp, horizontal = 6.dp)
                  .testTag("font_style_${style.name}"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = style.label.split(" ").last(),
                  fontFamily = posterFontFamily(style),
                  fontWeight = FontWeight.Bold,
                  color = if (selected) OledBlack else PureWhite,
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "POSTER TEXT COLOR",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSlate,
          )
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            PosterTextSwatches.forEachIndexed { idx, hex ->
              val selected = posterConfig.textColorHex == hex
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(Color(hex))
                  .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) CallCrimson else ElevatedCardBorder,
                    shape = CircleShape,
                  )
                  .clickable { onUpdateTypography(null, hex, null, null) }
                  .testTag("color_swatch_$idx"),
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "NAME ALIGNMENT",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSlate,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            PosterAlignment.entries.forEach { alignment ->
              val selected = posterConfig.nameAlignment == alignment
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .background(if (selected) PureWhite else GraphiteSurface)
                  .clickable { onUpdateTypography(null, null, alignment, null) }
                  .padding(vertical = 10.dp)
                  .testTag("align_${alignment.name}"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "${alignment.label} Aligned",
                  style = MaterialTheme.typography.labelLarge,
                  color = if (selected) OledBlack else PureWhite,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          TextButton(
            onClick = onResetPoster,
            modifier = Modifier
              .align(Alignment.End)
              .testTag("reset_poster_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = null,
              tint = CallCrimson,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset Poster", color = CallCrimson)
          }
        }
      }

      // Contact Name & Mobile Numbers card with Edit capability
      val baseNumbers = contact?.phoneNumbers?.takeIf { it.isNotEmpty() }
        ?: listOfNotNull(fallbackNumber.takeIf { it.isNotBlank() })
      val numbers = if (!posterConfig.customCallerNumber.isNullOrBlank()) {
        listOf(posterConfig.customCallerNumber) + baseNumbers.filterNot {
          PhoneNumberFormatter.matchKey(it) == PhoneNumberFormatter.matchKey(posterConfig.customCallerNumber)
        }
      } else {
        baseNumbers
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(CharcoalSurface)
          .border(1.dp, ElevatedCardBorder, RoundedCornerShape(20.dp))
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "CONTACT & MOBILE",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSlate,
          )
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(GraphiteSurface)
              .clickable { showEditContactSheet = true }
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .testTag("contact_edit_info_button"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = "Edit Name and Number",
              tint = PureWhite,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "Edit",
              style = MaterialTheme.typography.labelLarge,
              color = PureWhite,
            )
          }
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showEditContactSheet = true }
            .padding(vertical = 2.dp),
        ) {
          Text(
            text = "Name",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSlate,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = PureWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        numbers.forEach { num ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { onCall(num) }
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Mobile",
                style = MaterialTheme.typography.labelSmall,
                color = MutedSlate,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = PhoneNumberFormatter.formatForDisplay(num),
                style = MaterialTheme.typography.bodyLarge,
                color = PureWhite,
              )
            }
            Icon(
              imageVector = Icons.Rounded.Call,
              contentDescription = "Call $num",
              tint = SubtleText,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      // Last 10 Call Logs for this contact / number
      val recentLogs = remember(callLogs) { callLogs.take(10) }
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(CharcoalSurface)
          .border(1.dp, ElevatedCardBorder, RoundedCornerShape(20.dp))
          .padding(18.dp)
          .testTag("contact_recent_calls_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "RECENT CALLS",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSlate,
          )
          Text(
            text = if (recentLogs.isEmpty()) "No calls" else "Last ${recentLogs.size}",
            style = MaterialTheme.typography.labelSmall,
            color = MutedSlate,
          )
        }

        if (recentLogs.isEmpty()) {
          Text(
            text = "No recent calls recorded for this number",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSlate,
          )
        } else {
          recentLogs.forEachIndexed { idx, entry ->
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
            val formattedDateTime = remember(entry.dateMillis, entry.formattedTime) {
              if (entry.dateMillis > 0L) {
                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(entry.dateMillis))
              } else {
                entry.formattedTime
              }
            }
            val durationLabel = remember(entry.durationSeconds, isMissed) {
              when {
                isMissed || entry.durationSeconds <= 0L -> "0s"
                entry.durationSeconds >= 60L -> {
                  val mins = entry.durationSeconds / 60L
                  val secs = entry.durationSeconds % 60L
                  "${mins}m ${secs}s"
                }
                else -> "${entry.durationSeconds}s"
              }
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("contact_call_log_${entry.id}"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = typeIcon,
                contentDescription = typeLabel,
                tint = iconTint,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = typeLabel,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = if (isMissed) CallCrimson else PureWhite,
                )
                Text(
                  text = formattedDateTime,
                  style = MaterialTheme.typography.labelSmall,
                  color = MutedSlate,
                )
              }
              Text(
                text = if (isMissed) "—" else durationLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlate,
              )
            }

            if (idx < recentLogs.lastIndex) {
              HorizontalDivider(color = GraphiteSurface, thickness = 0.5.dp)
            }
          }
        }
      }

      // Notes card
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(CharcoalSurface)
          .border(1.dp, ElevatedCardBorder, RoundedCornerShape(20.dp))
          .padding(18.dp),
      ) {
        Text(
          text = "NOTES",
          style = MaterialTheme.typography.labelSmall,
          color = MutedSlate,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = notesText,
          onValueChange = { updated ->
            notesText = updated
            onUpdateTypography(null, null, null, updated)
          },
          placeholder = { Text("Add private notes for $displayName", color = MutedSlate) },
          minLines = 2,
          maxLines = 5,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElevatedCardBorder,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            cursorColor = PureWhite,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("poster_notes_input"),
        )
      }

      // Share Contact button
      Button(
        onClick = {
          val shareText = buildString {
            append(displayName)
            if (formattedPrimary.isNotBlank()) append("\n$formattedPrimary")
            if (!contact?.email.isNullOrBlank()) append("\n${contact?.email}")
          }
          val shareIntent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, shareText)
            },
            "Share Contact",
          )
          onLaunchSystemIntent(shareIntent, "Could not open share sheet")
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = CharcoalSurface,
          contentColor = PureWhite,
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .border(1.dp, ElevatedCardBorder, RoundedCornerShape(20.dp))
          .testTag("share_contact_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Share,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Share Contact", style = MaterialTheme.typography.titleMedium)
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  if (showEditContactSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editName by remember(showEditContactSheet, displayName) {
      mutableStateOf(displayName)
    }
    var editNumber by remember(showEditContactSheet, primaryNumber) {
      mutableStateOf(primaryNumber)
    }

    ModalBottomSheet(
      onDismissRequest = { showEditContactSheet = false },
      sheetState = sheetState,
      containerColor = CharcoalSurface,
      contentColor = PureWhite,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 24.dp, vertical = 12.dp),
      ) {
        Text(
          text = "Edit Contact Info",
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = editName,
          onValueChange = { editName = it },
          label = { Text("Name") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PureWhite,
            unfocusedBorderColor = ElevatedCardBorder,
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedLabelColor = SubtleText,
            unfocusedLabelColor = MutedSlate,
            cursorColor = PureWhite,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_edit_name_input"),
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = editNumber,
          onValueChange = { editNumber = it },
          label = { Text("Phone Number") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PureWhite,
            unfocusedBorderColor = ElevatedCardBorder,
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            focusedLabelColor = SubtleText,
            unfocusedLabelColor = MutedSlate,
            cursorColor = PureWhite,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_edit_number_input"),
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = {
            showEditContactSheet = false
            onEditContactInfo(editName, editNumber)
          },
          enabled = editName.isNotBlank() || editNumber.isNotBlank(),
          colors = ButtonDefaults.buttonColors(
            containerColor = PureWhite,
            contentColor = OledBlack,
            disabledContainerColor = GraphiteSurface,
            disabledContentColor = MutedSlate,
          ),
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("contact_save_info_button"),
        ) {
          Text("Save Changes", fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun PosterActionSquare(
  label: String,
  icon: ImageVector,
  tag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(FrostedGlassFill)
      .border(1.dp, FrostedGlassBorder, RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp)
      .testTag(tag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = PureWhite,
      modifier = Modifier.size(22.dp),
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = PureWhite,
    )
  }
}
