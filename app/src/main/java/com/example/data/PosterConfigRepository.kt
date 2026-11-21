package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class PosterConfigRepository(private val context: Context) {

  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val _configs = MutableStateFlow<Map<String, ContactPosterConfig>>(loadAll())
  val configs: StateFlow<Map<String, ContactPosterConfig>> = _configs.asStateFlow()

  fun samplePosterUri(): String {
    return "android.resource://${context.packageName}/${R.drawable.img_dummy_poster_1795246756410}"
  }

  private fun defaultDemoConfig(): ContactPosterConfig = ContactPosterConfig(
    contactKey = DEMO_POSTER_KEY,
    posterUri = samplePosterUri(),
    fontStyle = PosterFontStyle.EDITORIAL_SERIF,
    textColorHex = 0xFFFFFFFFL,
    nameAlignment = PosterAlignment.TOP,
    notes = "Sample Contact Poster preview — customize font, color, or alignment anytime.",
  )

  private fun loadAll(): Map<String, ContactPosterConfig> {
    val rawJson = prefs.getString(KEY_POSTERS_JSON, null)
    if (rawJson.isNullOrBlank()) {
      return mapOf(DEMO_POSTER_KEY to defaultDemoConfig())
    }
    return try {
      val root = JSONObject(rawJson)
      val result = mutableMapOf<String, ContactPosterConfig>()
      val keys = root.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val obj = root.optJSONObject(key) ?: continue
        val fontName = obj.optString("fontStyle", PosterFontStyle.EDITORIAL_SERIF.name)
        val alignName = obj.optString("nameAlignment", PosterAlignment.TOP.name)
        result[key] = ContactPosterConfig(
          contactKey = key,
          posterUri = obj.optString("posterUri").takeIf { it.isNotBlank() },
          fontStyle = runCatching { PosterFontStyle.valueOf(fontName) }
            .getOrDefault(PosterFontStyle.EDITORIAL_SERIF),
          textColorHex = obj.optLong("textColorHex", 0xFFFFFFFFL),
          nameAlignment = runCatching { PosterAlignment.valueOf(alignName) }
            .getOrDefault(PosterAlignment.TOP),
          notes = obj.optString("notes", ""),
          customCallerName = obj.optString("customCallerName").takeIf { it.isNotBlank() },
          customCallerNumber = obj.optString("customCallerNumber").takeIf { it.isNotBlank() },
        )
      }
      if (!result.containsKey(DEMO_POSTER_KEY)) {
        result[DEMO_POSTER_KEY] = defaultDemoConfig()
      }
      result
    } catch (_: Exception) {
      mapOf(DEMO_POSTER_KEY to defaultDemoConfig())
    }
  }

  private fun persistAll(map: Map<String, ContactPosterConfig>) {
    val root = JSONObject()
    map.forEach { (key, config) ->
      val obj = JSONObject().apply {
        put("posterUri", config.posterUri ?: "")
        put("fontStyle", config.fontStyle.name)
        put("textColorHex", config.textColorHex)
        put("nameAlignment", config.nameAlignment.name)
        put("notes", config.notes)
        put("customCallerName", config.customCallerName ?: "")
        put("customCallerNumber", config.customCallerNumber ?: "")
      }
      root.put(key, obj)
    }
    prefs.edit().putString(KEY_POSTERS_JSON, root.toString()).apply()
  }

  fun resolveConfig(
    lookupKey: String? = null,
    phoneNumbers: List<String> = emptyList(),
    fallbackKey: String = "unknown",
  ): ContactPosterConfig {
    val map = _configs.value
    if (!lookupKey.isNullOrBlank() && map.containsKey(lookupKey)) {
      return map.getValue(lookupKey)
    }
    for (num in phoneNumbers) {
      val normalized = PhoneNumberFormatter.matchKey(num)
      if (normalized.isNotBlank() && map.containsKey(normalized)) {
        return map.getValue(normalized)
      }
    }
    val primaryKey = lookupKey?.takeIf { it.isNotBlank() }
      ?: phoneNumbers.firstOrNull()?.let { PhoneNumberFormatter.matchKey(it) }
      ?: fallbackKey
    return ContactPosterConfig(contactKey = primaryKey)
  }

  fun savePosterConfig(
    primaryKey: String,
    secondaryKeys: List<String> = emptyList(),
    config: ContactPosterConfig,
  ) {
    val updated = _configs.value.toMutableMap()
    val allKeys = (listOf(primaryKey) + secondaryKeys).filter { it.isNotBlank() }.distinct()
    for (k in allKeys) {
      updated[k] = config.copy(contactKey = k)
    }
    _configs.value = updated
    persistAll(updated)
  }

  fun resetPosterVisuals(primaryKey: String, secondaryKeys: List<String> = emptyList()) {
    val existing = resolveConfig(lookupKey = primaryKey, phoneNumbers = secondaryKeys, fallbackKey = primaryKey)
    savePosterConfig(
      primaryKey = primaryKey,
      secondaryKeys = secondaryKeys,
      config = ContactPosterConfig(
        contactKey = primaryKey,
        posterUri = null,
        fontStyle = PosterFontStyle.EDITORIAL_SERIF,
        textColorHex = 0xFFFFFFFFL,
        nameAlignment = PosterAlignment.TOP,
        notes = existing.notes,
        customCallerName = existing.customCallerName,
        customCallerNumber = existing.customCallerNumber,
      ),
    )
  }

  /**
   * Attempts ContentResolver.takePersistableUriPermission first as requested;
   * if the Android Photo Picker URI doesn't support persistable grants on this OS version,
   * falls back to copying into app-private storage so the poster never breaks after reboot.
   */
  fun persistPickedImageUri(uri: Uri, contactKey: String): String {
    var persistableSucceeded = false
    try {
      context.contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION,
      )
      persistableSucceeded = true
    } catch (_: SecurityException) {
      persistableSucceeded = false
    } catch (_: Exception) {
      persistableSucceeded = false
    }

    if (persistableSucceeded) {
      return uri.toString()
    }

    return try {
      val dir = File(context.filesDir, "posters").apply { mkdirs() }
      val safeName = contactKey.replace(Regex("[^a-zA-Z0-9_-]"), "_") + "_" + System.currentTimeMillis() + ".jpg"
      val dest = File(dir, safeName)
      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(dest).use { output ->
          input.copyTo(output)
        }
      }
      Uri.fromFile(dest).toString()
    } catch (_: Exception) {
      uri.toString()
    }
  }

  companion object {
    private const val PREFS_NAME = "noir_dial_posters"
    private const val KEY_POSTERS_JSON = "contact_posters_json"
    const val DEMO_POSTER_KEY = "demo_elena_vance"
  }
}
