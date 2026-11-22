package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NoirDialApplication
import com.example.data.CallLogEntry
import com.example.data.ContactItem
import com.example.data.ContactPosterConfig
import com.example.data.PermissionSnapshot
import com.example.data.PhoneNumberFormatter
import com.example.data.PosterAlignment
import com.example.data.PosterFontStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DialerMatchItem(
  val id: String,
  val displayName: String,
  val phoneNumber: String,
  val formattedNumber: String,
  val contact: ContactItem?,
  val avatarUri: String?,
  val isRecent: Boolean,
)

data class MainUiState(
  val dialedNumber: String = "",
  val contacts: List<ContactItem> = emptyList(),
  val callLog: List<CallLogEntry> = emptyList(),
  val posterConfigs: Map<String, ContactPosterConfig> = emptyMap(),
  val contactsSearchQuery: String = "",
  val matchingDialerItems: List<DialerMatchItem> = emptyList(),
  val filteredContacts: List<ContactItem> = emptyList(),
  val isDefaultDialer: Boolean = false,
  val permissions: PermissionSnapshot = PermissionSnapshot(),
  val snackbarMessage: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val app = application as NoirDialApplication
  private val posterRepo = app.posterRepository
  private val systemRepo = app.systemRepository
  private val callStateHolder = app.callStateHolder

  private val dialedNumber = MutableStateFlow("")
  private val contacts = MutableStateFlow<List<ContactItem>>(emptyList())
  private val callLog = MutableStateFlow<List<CallLogEntry>>(emptyList())
  private val contactsSearchQuery = MutableStateFlow("")
  private val isDefaultDialer = MutableStateFlow(callStateHolder.isDefaultDialer())
  private val permissions = MutableStateFlow(systemRepo.readPermissionSnapshot())
  private val snackbarMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<MainUiState> = combine(
    dialedNumber,
    contacts,
    callLog,
    posterRepo.configs,
    contactsSearchQuery,
    isDefaultDialer,
    permissions,
    snackbarMessage,
  ) { values ->
    val dial = values[0] as String
    @Suppress("UNCHECKED_CAST")
    val allContacts = values[1] as List<ContactItem>
    @Suppress("UNCHECKED_CAST")
    val logs = values[2] as List<CallLogEntry>
    @Suppress("UNCHECKED_CAST")
    val configs = values[3] as Map<String, ContactPosterConfig>
    val search = values[4] as String
    val defaultDialer = values[5] as Boolean
    val perms = values[6] as PermissionSnapshot
    val msg = values[7] as String?

    val dialMatches = buildDialerMatches(dial, allContacts, logs, configs)
    val filteredContacts = if (search.isBlank()) {
      allContacts
    } else {
      allContacts.filter { contact ->
        val config = configs[contact.lookupKey]
          ?: contact.phoneNumbers.firstOrNull()?.let { configs[PhoneNumberFormatter.matchKey(it)] }
        PhoneNumberFormatter.matchesContactSearch(
          displayName = contact.displayName,
          phoneNumbers = contact.phoneNumbers,
          email = contact.email,
          query = search,
          extraName = config?.customCallerName,
          extraNumber = config?.customCallerNumber,
        )
      }
    }

    MainUiState(
      dialedNumber = dial,
      contacts = allContacts,
      callLog = logs,
      posterConfigs = configs,
      contactsSearchQuery = search,
      matchingDialerItems = dialMatches,
      filteredContacts = filteredContacts,
      isDefaultDialer = defaultDialer,
      permissions = perms,
      snackbarMessage = msg,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = MainUiState(
      isDefaultDialer = callStateHolder.isDefaultDialer(),
      permissions = systemRepo.readPermissionSnapshot(),
    ),
  )

  init {
    refreshData()
  }

  fun refreshData() {
    isDefaultDialer.value = callStateHolder.isDefaultDialer()
    val snap = systemRepo.readPermissionSnapshot()
    permissions.value = snap

    viewModelScope.launch {
      if (snap.hasReadContacts) {
        contacts.value = systemRepo.loadContacts()
      }
      if (snap.hasReadCallLog) {
        callLog.value = systemRepo.loadCallLog()
      }
    }
  }

  fun appendDigit(digit: String) {
    dialedNumber.value += digit
  }

  fun backspaceDigit() {
    val current = dialedNumber.value
    if (current.isNotEmpty()) {
      dialedNumber.value = current.dropLast(1)
    }
  }

  fun clearDialedNumber() {
    dialedNumber.value = ""
  }

  fun setDialedNumber(number: String) {
    dialedNumber.value = number
  }

  fun setContactsSearchQuery(query: String) {
    contactsSearchQuery.value = query
  }

  fun consumeSnackbar() {
    snackbarMessage.value = null
  }

  fun showMessage(message: String) {
    snackbarMessage.value = message
  }

  fun placeCall(context: Context, number: String) {
    val target = number.trim()
    if (target.isBlank()) return
    val ok = callStateHolder.placeOutgoingCall(context, target)
    if (!ok) {
      snackbarMessage.value = "Grant Phone permission to place calls"
    }
  }

  fun resolveContactByLookupOrNumber(lookupKey: String?, number: String?): ContactItem? {
    val all = contacts.value
    if (!lookupKey.isNullOrBlank()) {
      all.firstOrNull { it.lookupKey == lookupKey }?.let { return it }
    }
    if (!number.isNullOrBlank()) {
      val norm = PhoneNumberFormatter.matchKey(number)
      all.firstOrNull { contact ->
        contact.phoneNumbers.any { PhoneNumberFormatter.matchKey(it) == norm }
      }?.let { return it }
    }
    return null
  }

  fun resolvePosterConfig(
    contact: ContactItem?,
    fallbackNumber: String?,
    lookupOverride: String? = null,
  ): ContactPosterConfig {
    return posterRepo.resolveConfig(
      lookupKey = contact?.lookupKey?.takeIf { it.isNotBlank() }
        ?: lookupOverride?.takeIf { it.isNotBlank() },
      phoneNumbers = contact?.phoneNumbers ?: listOfNotNull(fallbackNumber),
      fallbackKey = fallbackNumber ?: "unknown",
    )
  }

  fun updatePosterPhoto(
    contact: ContactItem?,
    fallbackNumber: String?,
    pickedUri: Uri,
    lookupOverride: String? = null,
  ) {
    val primaryKey = contact?.lookupKey?.takeIf { it.isNotBlank() }
      ?: lookupOverride?.takeIf { it.isNotBlank() }
      ?: fallbackNumber?.let { PhoneNumberFormatter.matchKey(it) }
      ?: return
    val secondaryKeys = (contact?.phoneNumbers ?: listOfNotNull(fallbackNumber))
      .map { PhoneNumberFormatter.matchKey(it) }
    val persistedUri = posterRepo.persistPickedImageUri(pickedUri, primaryKey)
    val current = resolvePosterConfig(contact, fallbackNumber, lookupOverride)
    posterRepo.savePosterConfig(
      primaryKey = primaryKey,
      secondaryKeys = secondaryKeys,
      config = current.copy(posterUri = persistedUri),
    )
  }

  fun applySamplePoster(
    contact: ContactItem?,
    fallbackNumber: String?,
    lookupOverride: String? = null,
  ) {
    val primaryKey = contact?.lookupKey?.takeIf { it.isNotBlank() }
      ?: lookupOverride?.takeIf { it.isNotBlank() }
      ?: fallbackNumber?.let { PhoneNumberFormatter.matchKey(it) }
      ?: return
    val secondaryKeys = (contact?.phoneNumbers ?: listOfNotNull(fallbackNumber))
      .map { PhoneNumberFormatter.matchKey(it) }
    val current = resolvePosterConfig(contact, fallbackNumber, lookupOverride)
    posterRepo.savePosterConfig(
      primaryKey = primaryKey,
      secondaryKeys = secondaryKeys,
      config = current.copy(posterUri = posterRepo.samplePosterUri()),
    )
  }

  fun updatePosterTypography(
    contact: ContactItem?,
    fallbackNumber: String?,
    fontStyle: PosterFontStyle? = null,
    textColorHex: Long? = null,
    alignment: PosterAlignment? = null,
    notes: String? = null,
    lookupOverride: String? = null,
  ) {
    val primaryKey = contact?.lookupKey?.takeIf { it.isNotBlank() }
      ?: lookupOverride?.takeIf { it.isNotBlank() }
      ?: fallbackNumber?.let { PhoneNumberFormatter.matchKey(it) }
      ?: return
    val secondaryKeys = (contact?.phoneNumbers ?: listOfNotNull(fallbackNumber))
      .map { PhoneNumberFormatter.matchKey(it) }
    val current = resolvePosterConfig(contact, fallbackNumber, lookupOverride)
    posterRepo.savePosterConfig(
      primaryKey = primaryKey,
      secondaryKeys = secondaryKeys,
      config = current.copy(
        fontStyle = fontStyle ?: current.fontStyle,
        textColorHex = textColorHex ?: current.textColorHex,
        nameAlignment = alignment ?: current.nameAlignment,
        notes = notes ?: current.notes,
      ),
    )
  }

  fun resetPoster(
    contact: ContactItem?,
    fallbackNumber: String?,
    lookupOverride: String? = null,
  ) {
    val primaryKey = contact?.lookupKey?.takeIf { it.isNotBlank() }
      ?: lookupOverride?.takeIf { it.isNotBlank() }
      ?: fallbackNumber?.let { PhoneNumberFormatter.matchKey(it) }
      ?: return
    val secondaryKeys = (contact?.phoneNumbers ?: listOfNotNull(fallbackNumber))
      .map { PhoneNumberFormatter.matchKey(it) }
    posterRepo.resetPosterVisuals(primaryKey, secondaryKeys)
  }

  fun updateCallerInfo(
    contact: ContactItem?,
    fallbackNumber: String?,
    lookupOverride: String? = null,
    newName: String,
    newNumber: String,
  ) {
    val cleanName = newName.trim()
    val cleanNumber = newNumber.trim()
    val primaryKey = contact?.lookupKey?.takeIf { it.isNotBlank() }
      ?: lookupOverride?.takeIf { it.isNotBlank() }
      ?: fallbackNumber?.takeIf { it.isNotBlank() }?.let { PhoneNumberFormatter.matchKey(it) }
      ?: cleanNumber.takeIf { it.isNotBlank() }?.let { PhoneNumberFormatter.matchKey(it) }
      ?: return
    val secondaryKeys = ((contact?.phoneNumbers ?: emptyList()) + listOfNotNull(fallbackNumber, cleanNumber))
      .map { PhoneNumberFormatter.matchKey(it) }
      .filter { it.isNotBlank() }
      .distinct()
    val current = resolvePosterConfig(contact, fallbackNumber, lookupOverride)
    posterRepo.savePosterConfig(
      primaryKey = primaryKey,
      secondaryKeys = secondaryKeys,
      config = current.copy(
        customCallerName = cleanName.takeIf { it.isNotBlank() },
        customCallerNumber = cleanNumber.takeIf { it.isNotBlank() },
      ),
    )

    if (lookupOverride != com.example.data.PosterConfigRepository.DEMO_POSTER_KEY) {
      viewModelScope.launch {
        val oldNum = contact?.phoneNumbers?.firstOrNull() ?: fallbackNumber
        systemRepo.upsertCallerContact(
          contactId = contact?.id,
          oldNumber = oldNum,
          newName = cleanName,
          newNumber = cleanNumber,
        )
        refreshData()
      }
    }
  }

  fun createNewContact(name: String, phone: String, email: String?, onSuccess: () -> Unit) {
    if (name.isBlank() || phone.isBlank()) {
      snackbarMessage.value = "Name and phone number are required"
      return
    }
    viewModelScope.launch {
      val res = systemRepo.createContact(name, phone, email)
      if (res.isSuccess) {
        refreshData()
        snackbarMessage.value = "Saved $name to Contacts"
        onSuccess()
      } else {
        snackbarMessage.value = res.exceptionOrNull()?.message ?: "Could not save contact"
      }
    }
  }

  fun launchSystemAction(context: Context, intent: Intent, fallbackMsg: String) {
    runCatching {
      context.startActivity(intent)
    }.onFailure {
      snackbarMessage.value = fallbackMsg
    }
  }

  private fun buildDialerMatches(
    query: String,
    allContacts: List<ContactItem>,
    logs: List<CallLogEntry>,
    configs: Map<String, ContactPosterConfig>,
  ): List<DialerMatchItem> {
    val items = mutableListOf<DialerMatchItem>()
    val seenNumbers = hashSetOf<String>()

    if (query.isBlank()) {
      // Show recent callers first, then top contacts
      for (entry in logs) {
        val norm = PhoneNumberFormatter.matchKey(entry.number)
        if (norm.isBlank() || !seenNumbers.add(norm)) continue
        val matchedContact = allContacts.firstOrNull { c ->
          c.phoneNumbers.any { PhoneNumberFormatter.matchKey(it) == norm }
        }
        val config = posterRepo.resolveConfig(
          lookupKey = matchedContact?.lookupKey,
          phoneNumbers = matchedContact?.phoneNumbers ?: listOf(entry.number),
          fallbackKey = norm,
        )
        val effectiveNumber = config.customCallerNumber?.takeIf { it.isNotBlank() } ?: entry.number
        val effectiveName = config.customCallerName?.takeIf { it.isNotBlank() }
          ?: matchedContact?.displayName
          ?: entry.cachedName
          ?: PhoneNumberFormatter.formatForDisplay(effectiveNumber)
        items.add(
          DialerMatchItem(
            id = "log_${entry.id}",
            displayName = effectiveName,
            phoneNumber = effectiveNumber,
            formattedNumber = PhoneNumberFormatter.formatForDisplay(effectiveNumber),
            contact = matchedContact,
            avatarUri = config.posterUri ?: matchedContact?.photoUri,
            isRecent = true,
          ),
        )
        if (items.size >= 15) break
      }

      if (items.size < 15) {
        for (contact in allContacts) {
          val primaryNum = contact.phoneNumbers.firstOrNull() ?: continue
          val norm = PhoneNumberFormatter.matchKey(primaryNum)
          if (!seenNumbers.add(norm)) continue
          val config = configs[contact.lookupKey] ?: configs[norm]
          items.add(
            DialerMatchItem(
              id = "c_${contact.id}",
              displayName = contact.displayName,
              phoneNumber = primaryNum,
              formattedNumber = PhoneNumberFormatter.formatForDisplay(primaryNum),
              contact = contact,
              avatarUri = config?.posterUri ?: contact.photoUri,
              isRecent = false,
            ),
          )
          if (items.size >= 20) break
        }
      }
      return items
    }

    // Silent T9 & digit filtering across Contacts
    for (contact in allContacts) {
      if (PhoneNumberFormatter.matchesT9OrDigits(contact.displayName, contact.phoneNumbers, query)) {
        val primaryNum = contact.phoneNumbers.firstOrNull {
          it.filter { ch -> ch.isDigit() || ch == '*' || ch == '#' }.contains(query)
        } ?: contact.phoneNumbers.firstOrNull() ?: continue
        val norm = PhoneNumberFormatter.matchKey(primaryNum)
        if (!seenNumbers.add(norm)) continue
        val config = configs[contact.lookupKey] ?: configs[norm]
        items.add(
          DialerMatchItem(
            id = "t9_${contact.id}",
            displayName = contact.displayName,
            phoneNumber = primaryNum,
            formattedNumber = PhoneNumberFormatter.formatForDisplay(primaryNum),
            contact = contact,
            avatarUri = config?.posterUri ?: contact.photoUri,
            isRecent = false,
          ),
        )
      }
    }

    // Also match recent raw numbers not saved in Contacts
    for (entry in logs) {
      val norm = PhoneNumberFormatter.matchKey(entry.number)
      if (norm.isBlank() || seenNumbers.contains(norm)) continue
      val name = entry.cachedName ?: entry.number
      if (PhoneNumberFormatter.matchesT9OrDigits(name, listOf(entry.number), query)) {
        seenNumbers.add(norm)
        val config = configs[norm]
        items.add(
          DialerMatchItem(
            id = "log_t9_${entry.id}",
            displayName = entry.cachedName ?: PhoneNumberFormatter.formatForDisplay(entry.number),
            phoneNumber = entry.number,
            formattedNumber = PhoneNumberFormatter.formatForDisplay(entry.number),
            contact = null,
            avatarUri = config?.posterUri,
            isRecent = true,
          ),
        )
      }
    }

    return items
  }
}
