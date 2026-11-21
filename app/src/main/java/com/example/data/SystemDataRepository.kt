package com.example.data

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ResolvedCaller(
  val displayName: String,
  val lookupKey: String?,
  val photoUri: String?,
  val contactId: Long? = null,
  val isSavedContact: Boolean = false,
)

class SystemDataRepository(private val context: Context) {

  fun readPermissionSnapshot(): PermissionSnapshot {
    fun granted(perm: String): Boolean =
      ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    val hasNotifications =
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        granted(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        true
      }

    return PermissionSnapshot(
      hasCallPhone = granted(Manifest.permission.CALL_PHONE),
      hasReadContacts = granted(Manifest.permission.READ_CONTACTS),
      hasWriteContacts = granted(Manifest.permission.WRITE_CONTACTS),
      hasReadCallLog = granted(Manifest.permission.READ_CALL_LOG),
      hasPostNotifications = hasNotifications,
    )
  }

  suspend fun loadContacts(): List<ContactItem> = withContext(Dispatchers.IO) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
      != PackageManager.PERMISSION_GRANTED
    ) {
      return@withContext emptyList()
    }

    val contactMap = linkedMapOf<Long, MutableContactBuilder>()
    val projection = arrayOf(
      ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
      ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
      ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
      ContactsContract.CommonDataKinds.Phone.NUMBER,
      ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
    )

    try {
      context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC",
      )?.use { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val lookupIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
        val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
        val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

        while (cursor.moveToNext()) {
          val id = cursor.getLong(idIdx)
          val lookup = cursor.getString(lookupIdx) ?: id.toString()
          val name = cursor.getString(nameIdx)?.trim().orEmpty()
          val number = cursor.getString(numIdx)?.trim().orEmpty()
          val photo = cursor.getString(photoIdx)
          if (number.isBlank()) continue

          val builder = contactMap.getOrPut(id) {
            MutableContactBuilder(
              id = id,
              lookupKey = lookup,
              displayName = name.ifBlank { number },
              photoUri = photo,
            )
          }
          val normalizedCheck = PhoneNumberFormatter.normalizeDigits(number)
          if (builder.numbers.none { PhoneNumberFormatter.normalizeDigits(it) == normalizedCheck }) {
            builder.numbers.add(number)
          }
        }
      }

      // Attach primary email addresses where available
      val emailProjection = arrayOf(
        ContactsContract.CommonDataKinds.Email.CONTACT_ID,
        ContactsContract.CommonDataKinds.Email.ADDRESS,
      )
      context.contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        emailProjection,
        null,
        null,
        null,
      )?.use { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
        val addrIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
        while (cursor.moveToNext()) {
          val id = cursor.getLong(idIdx)
          val email = cursor.getString(addrIdx)?.trim()
          if (!email.isNullOrBlank()) {
            contactMap[id]?.let { if (it.email == null) it.email = email }
          }
        }
      }
    } catch (_: SecurityException) {
      return@withContext emptyList()
    }

    contactMap.values.map { builder ->
      val firstChar = builder.displayName.firstOrNull()?.uppercaseChar()
      val initial = if (firstChar != null && firstChar.isLetter()) firstChar.toString() else "#"
      ContactItem(
        id = builder.id,
        lookupKey = builder.lookupKey,
        displayName = builder.displayName,
        phoneNumbers = builder.numbers.toList(),
        email = builder.email,
        photoUri = builder.photoUri,
        initial = initial,
      )
    }
  }

  suspend fun loadCallLog(limit: Int = 300): List<CallLogEntry> = withContext(Dispatchers.IO) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
      != PackageManager.PERMISSION_GRANTED
    ) {
      return@withContext emptyList()
    }

    val entries = mutableListOf<CallLogEntry>()
    val projection = arrayOf(
      CallLog.Calls._ID,
      CallLog.Calls.NUMBER,
      CallLog.Calls.CACHED_NAME,
      CallLog.Calls.TYPE,
      CallLog.Calls.DATE,
      CallLog.Calls.DURATION,
    )

    try {
      context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        projection,
        null,
        null,
        "${CallLog.Calls.DATE} DESC",
      )?.use { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
        val numIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
        val nameIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
        val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
        val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
        val durIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

        val now = System.currentTimeMillis()
        while (cursor.moveToNext() && entries.size < limit) {
          val id = cursor.getLong(idIdx)
          val number = cursor.getString(numIdx)?.trim().orEmpty().ifBlank { "Unknown" }
          val cachedName = cursor.getString(nameIdx)?.trim()?.takeIf { it.isNotBlank() }
          val rawType = cursor.getInt(typeIdx)
          val dateMillis = cursor.getLong(dateIdx)
          val duration = cursor.getLong(durIdx)

          val type = when (rawType) {
            CallLog.Calls.INCOMING_TYPE -> CallLogType.INCOMING
            CallLog.Calls.OUTGOING_TYPE -> CallLogType.OUTGOING
            CallLog.Calls.MISSED_TYPE -> CallLogType.MISSED
            CallLog.Calls.REJECTED_TYPE -> CallLogType.REJECTED
            else -> CallLogType.INCOMING
          }

          val relTime = DateUtils.getRelativeTimeSpanString(
            dateMillis,
            now,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
          ).toString()

          entries.add(
            CallLogEntry(
              id = id,
              number = number,
              cachedName = cachedName,
              type = type,
              dateMillis = dateMillis,
              durationSeconds = duration,
              formattedTime = relTime,
            )
          )
        }
      }
    } catch (_: SecurityException) {
      return@withContext emptyList()
    }

    entries
  }

  fun lookupCallerSync(phoneNumber: String): ResolvedCaller {
    if (phoneNumber.isBlank()) {
      return ResolvedCaller(displayName = "Unknown", lookupKey = null, photoUri = null)
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
      != PackageManager.PERMISSION_GRANTED
    ) {
      return ResolvedCaller(
        displayName = PhoneNumberFormatter.formatForDisplay(phoneNumber),
        lookupKey = null,
        photoUri = null,
      )
    }

    try {
      val uri = Uri.withAppendedPath(
        ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
        Uri.encode(phoneNumber),
      )
      val projection = arrayOf(
        ContactsContract.PhoneLookup.DISPLAY_NAME,
        ContactsContract.PhoneLookup.LOOKUP_KEY,
        ContactsContract.PhoneLookup.PHOTO_URI,
        ContactsContract.PhoneLookup.CONTACT_ID,
      )
      context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val name = cursor.getString(0)?.trim()?.takeIf { it.isNotBlank() }
          val lookupKey = cursor.getString(1)
          val photoUri = cursor.getString(2)
          val contactId = cursor.getLong(3)
          if (name != null) {
            return ResolvedCaller(
              displayName = name,
              lookupKey = lookupKey,
              photoUri = photoUri,
              contactId = contactId,
              isSavedContact = true,
            )
          }
        }
      }

      // Fallback normalized digit match (handles country code / formatting differences)
      val targetKey = PhoneNumberFormatter.matchKey(phoneNumber)
      if (targetKey.isNotBlank()) {
        val phoneProjection = arrayOf(
          ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
          ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
          ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
          ContactsContract.CommonDataKinds.Phone.NUMBER,
          ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
        )
        context.contentResolver.query(
          ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
          phoneProjection,
          null,
          null,
          null,
        )?.use { cursor ->
          while (cursor.moveToNext()) {
            val number = cursor.getString(3).orEmpty()
            if (PhoneNumberFormatter.matchKey(number) == targetKey) {
              val name = cursor.getString(2)?.trim()?.takeIf { it.isNotBlank() }
              if (name != null) {
                return ResolvedCaller(
                  displayName = name,
                  lookupKey = cursor.getString(1),
                  photoUri = cursor.getString(4),
                  contactId = cursor.getLong(0),
                  isSavedContact = true,
                )
              }
            }
          }
        }
      }
    } catch (_: Exception) {
      // Fall through to formatted number
    }

    return ResolvedCaller(
      displayName = PhoneNumberFormatter.formatForDisplay(phoneNumber),
      lookupKey = null,
      photoUri = null,
      isSavedContact = false,
    )
  }

  suspend fun createContact(
    displayName: String,
    phoneNumber: String,
    email: String?,
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS)
      != PackageManager.PERMISSION_GRANTED
    ) {
      return@withContext Result.failure(SecurityException("WRITE_CONTACTS permission required"))
    }

    try {
      val ops = ArrayList<ContentProviderOperation>()
      ops.add(
        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
          .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
          .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
          .build()
      )
      ops.add(
        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
          .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
          .withValue(
            ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
          )
          .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName.trim())
          .build()
      )
      ops.add(
        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
          .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
          .withValue(
            ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
          )
          .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber.trim())
          .withValue(
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
          )
          .build()
      )
      if (!email.isNullOrBlank()) {
        ops.add(
          ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(
              ContactsContract.Data.MIMETYPE,
              ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
            )
            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.trim())
            .withValue(
              ContactsContract.CommonDataKinds.Email.TYPE,
              ContactsContract.CommonDataKinds.Email.TYPE_HOME,
            )
            .build()
        )
      }
      context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun upsertCallerContact(
    contactId: Long?,
    oldNumber: String?,
    newName: String,
    newNumber: String,
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS)
      != PackageManager.PERMISSION_GRANTED
    ) {
      return@withContext Result.failure(SecurityException("WRITE_CONTACTS permission required"))
    }
    val cleanName = newName.trim()
    val cleanNumber = newNumber.trim()
    if (cleanName.isBlank() && cleanNumber.isBlank()) {
      return@withContext Result.success(Unit)
    }

    if (contactId == null) {
      return@withContext if (cleanName.isNotBlank() && cleanNumber.isNotBlank()) {
        createContact(cleanName, cleanNumber, null)
      } else {
        Result.success(Unit)
      }
    }

    try {
      val ops = ArrayList<ContentProviderOperation>()
      if (cleanName.isNotBlank()) {
        ops.add(
          ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
            .withSelection(
              "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
              arrayOf(
                contactId.toString(),
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
              ),
            )
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, cleanName)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, cleanName)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, "")
            .build(),
        )
      }
      if (cleanNumber.isNotBlank()) {
        val oldNorm = oldNumber?.let { PhoneNumberFormatter.matchKey(it) }.orEmpty()
        var targetDataId: Long? = null
        context.contentResolver.query(
          ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
          arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
          ),
          "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
          arrayOf(contactId.toString()),
          null,
        )?.use { cursor ->
          while (cursor.moveToNext()) {
            val rowId = cursor.getLong(0)
            val rowNum = cursor.getString(1).orEmpty()
            if (targetDataId == null) targetDataId = rowId
            if (oldNorm.isNotBlank() && PhoneNumberFormatter.matchKey(rowNum) == oldNorm) {
              targetDataId = rowId
              break
            }
          }
        }
        val dataId = targetDataId
        if (dataId != null) {
          ops.add(
            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
              .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(dataId.toString()))
              .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, cleanNumber)
              .build(),
          )
        }
      }
      if (ops.isNotEmpty()) {
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private data class MutableContactBuilder(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val photoUri: String?,
    val numbers: MutableList<String> = mutableListOf(),
    var email: String? = null,
  )
}
