package com.example.data

enum class PosterFontStyle(val label: String) {
  EDITORIAL_SERIF("Editorial Serif"),
  CLEAN_SANS("Clean Sans"),
  CONDENSED_BOLD("Condensed Bold"),
}

enum class PosterAlignment(val label: String) {
  TOP("Top"),
  BOTTOM("Bottom"),
}

enum class CallLogType {
  INCOMING,
  OUTGOING,
  MISSED,
  REJECTED,
}

data class ContactItem(
  val id: Long,
  val lookupKey: String,
  val displayName: String,
  val phoneNumbers: List<String>,
  val email: String? = null,
  val photoUri: String? = null,
  val initial: String = displayName.firstOrNull()?.uppercase() ?: "#",
)

data class CallLogEntry(
  val id: Long,
  val number: String,
  val cachedName: String?,
  val type: CallLogType,
  val dateMillis: Long,
  val durationSeconds: Long,
  val formattedTime: String,
)

data class ContactPosterConfig(
  val contactKey: String,
  val posterUri: String? = null,
  val fontStyle: PosterFontStyle = PosterFontStyle.EDITORIAL_SERIF,
  val textColorHex: Long = 0xFFFFFFFFL,
  val nameAlignment: PosterAlignment = PosterAlignment.TOP,
  val notes: String = "",
  val customCallerName: String? = null,
  val customCallerNumber: String? = null,
)

data class PermissionSnapshot(
  val hasCallPhone: Boolean = false,
  val hasReadContacts: Boolean = false,
  val hasWriteContacts: Boolean = false,
  val hasReadCallLog: Boolean = false,
  val hasPostNotifications: Boolean = false,
) {
  val allCoreGranted: Boolean
    get() = hasCallPhone && hasReadContacts && hasReadCallLog
}
