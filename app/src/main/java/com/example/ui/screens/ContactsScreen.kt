package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ContactItem
import com.example.data.PhoneNumberFormatter
import com.example.ui.MainUiState
import com.example.ui.components.ContactAvatarBadge
import com.example.ui.components.PermissionRequestCard
import com.example.ui.theme.CallCrimson
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElevatedCardBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PlayfairDisplayFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtleText

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
  uiState: MainUiState,
  onSearchChange: (String) -> Unit,
  onContactClick: (ContactItem) -> Unit,
  onCreateContact: (name: String, phone: String, email: String?, onDone: () -> Unit) -> Unit,
  onNavigateKeypad: () -> Unit,
  onBack: () -> Unit,
  onRequestContactsPermission: () -> Unit,
  modifier: Modifier = Modifier,
  applyStatusBarsPadding: Boolean = true,
) {
  var showAddSheet by remember { mutableStateOf(false) }

  val grouped = remember(uiState.filteredContacts) {
    uiState.filteredContacts.groupBy { it.initial }
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

      // Top bar with Back + "+" Add Contact button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(48.dp)
            .testTag("contacts_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            tint = PureWhite,
          )
        }

        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(CharcoalSurface)
            .border(1.dp, ElevatedCardBorder, CircleShape)
            .clickable {
              if (uiState.permissions.hasWriteContacts) {
                showAddSheet = true
              } else {
                onRequestContactsPermission()
              }
            }
            .testTag("add_contact_button"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Add Contact",
            tint = PureWhite,
          )
        }
      }

      // Large editorial serif "Contacts" header & count subtitle
      Text(
        text = stringResource(R.string.contacts_header),
        style = MaterialTheme.typography.displayLarge.copy(
          fontFamily = PlayfairDisplayFamily,
          fontSize = 42.sp,
        ),
        color = PureWhite,
      )
      Spacer(modifier = Modifier.height(4.dp))
      val countLabel = if (uiState.contactsSearchQuery.isBlank()) {
        "${uiState.contacts.size} Contacts"
      } else {
        "${uiState.filteredContacts.size} of ${uiState.contacts.size} Contacts"
      }
      Text(
        text = countLabel,
        style = MaterialTheme.typography.bodyMedium,
        color = MutedSlate,
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Rounded search bar
      TextField(
        value = uiState.contactsSearchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text("Search name or number", color = MutedSlate) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MutedSlate,
          )
        },
        trailingIcon = {
          if (uiState.contactsSearchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchChange("") }) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Clear search",
                tint = MutedSlate,
              )
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
          focusedContainerColor = CharcoalSurface,
          unfocusedContainerColor = CharcoalSurface,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          cursorColor = PureWhite,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("contacts_search_input"),
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (!uiState.permissions.hasReadContacts) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center,
        ) {
          PermissionRequestCard(
            title = "Contacts Permission Needed",
            description = "Allow access to display your address book, group alphabetically, and customize Contact Posters.",
            icon = Icons.Rounded.Contacts,
            buttonTag = "contacts_grant_permission_button",
            onGrantClick = onRequestContactsPermission,
          )
        }
      } else if (uiState.filteredContacts.isEmpty()) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (uiState.contactsSearchQuery.isBlank()) {
              "No contacts yet. Tap + to create one."
            } else {
              "No contacts match \"${uiState.contactsSearchQuery}\""
            },
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
          grouped.forEach { (letter, list) ->
            stickyHeader(key = "hdr_$letter") {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(OledBlack.copy(alpha = 0.94f))
                  .padding(vertical = 8.dp),
              ) {
                Text(
                  text = letter,
                  style = MaterialTheme.typography.labelLarge,
                  color = MutedSlate,
                )
              }
            }

            items(list, key = { it.id }) { contact ->
              val config = uiState.posterConfigs[contact.lookupKey]
                ?: contact.phoneNumbers.firstOrNull()?.let {
                  uiState.posterConfigs[PhoneNumberFormatter.matchKey(it)]
                }
              val avatarUri = config?.posterUri ?: contact.photoUri

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { onContactClick(contact) }
                  .padding(vertical = 10.dp, horizontal = 4.dp)
                  .testTag("contact_row_${contact.id}"),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                ContactAvatarBadge(
                  displayName = contact.displayName,
                  imageUri = avatarUri,
                  size = 44.dp,
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  contact.phoneNumbers.firstOrNull()?.let { num ->
                    Text(
                      text = PhoneNumberFormatter.formatForDisplay(num),
                      style = MaterialTheme.typography.bodyMedium,
                      color = MutedSlate,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
              HorizontalDivider(color = CharcoalSurface, thickness = 0.5.dp)
            }
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
        .testTag("contacts_floating_keypad_button"),
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

  if (showAddSheet) {
    AddContactSheet(
      onDismiss = { showAddSheet = false },
      onSave = { name, phone, email ->
        onCreateContact(name, phone, email) {
          showAddSheet = false
        }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddContactSheet(
  onDismiss: () -> Unit,
  onSave: (name: String, phone: String, email: String?) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
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
        text = "New Contact",
        style = MaterialTheme.typography.displayLarge.copy(
          fontFamily = PlayfairDisplayFamily,
          fontSize = 30.sp,
        ),
        color = PureWhite,
      )
      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Full Name") },
        singleLine = true,
        colors = noirFieldColors(),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("new_contact_name_input"),
      )
      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Phone Number") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        colors = noirFieldColors(),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("new_contact_phone_input"),
      )
      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email (Optional)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        colors = noirFieldColors(),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("new_contact_email_input"),
      )
      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = { onSave(name, phone, email.takeIf { it.isNotBlank() }) },
        enabled = name.isNotBlank() && phone.isNotBlank(),
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
          .testTag("save_new_contact_button"),
      ) {
        Text("Save Contact", fontWeight = FontWeight.SemiBold)
      }
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun noirFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = PureWhite,
  unfocusedBorderColor = ElevatedCardBorder,
  focusedTextColor = PureWhite,
  unfocusedTextColor = PureWhite,
  focusedLabelColor = SubtleText,
  unfocusedLabelColor = MutedSlate,
  cursorColor = PureWhite,
)
