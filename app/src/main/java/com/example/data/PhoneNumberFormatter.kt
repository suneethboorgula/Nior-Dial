package com.example.data

import android.telephony.PhoneNumberUtils
import java.util.Locale

object PhoneNumberFormatter {

  fun normalizeDigits(input: String): String {
    if (input.isBlank()) return ""
    val hasLeadingPlus = input.trim().startsWith("+")
    val digits = input.filter { it.isDigit() }
    return if (hasLeadingPlus && digits.isNotEmpty()) "+$digits" else digits
  }

  fun matchKey(number: String): String {
    val digitsOnly = number.filter { it.isDigit() }
    return if (digitsOnly.length > 10) digitsOnly.takeLast(10) else digitsOnly
  }

  fun formatForDisplay(rawNumber: String): String {
    if (rawNumber.isBlank()) return ""
    if (rawNumber.contains('*') || rawNumber.contains('#')) return rawNumber
    val formatted = PhoneNumberUtils.formatNumber(rawNumber, Locale.getDefault().country)
    return formatted ?: rawNumber
  }

  fun charToT9Digit(ch: Char): Char? {
    return when (ch.uppercaseChar()) {
      '0' -> '0'
      '1' -> '1'
      '2', 'A', 'B', 'C' -> '2'
      '3', 'D', 'E', 'F' -> '3'
      '4', 'G', 'H', 'I' -> '4'
      '5', 'J', 'K', 'L' -> '5'
      '6', 'M', 'N', 'O' -> '6'
      '7', 'P', 'Q', 'R', 'S' -> '7'
      '8', 'T', 'U', 'V' -> '8'
      '9', 'W', 'X', 'Y', 'Z' -> '9'
      '*' -> '*'
      '#' -> '#'
      else -> null
    }
  }

  fun toT9Sequence(name: String): String {
    val sb = StringBuilder(name.length)
    for (ch in name) {
      val mapped = charToT9Digit(ch)
      if (mapped != null) sb.append(mapped)
    }
    return sb.toString()
  }

  fun matchesT9OrDigits(displayName: String, phoneNumbers: List<String>, query: String): Boolean {
    if (query.isBlank()) return true
    val cleanQuery = query.filter { it.isDigit() || it == '*' || it == '#' }
    if (cleanQuery.isEmpty()) return false

    // 1. Direct phone digit match
    val numberMatch = phoneNumbers.any { number ->
      val normalized = number.filter { it.isDigit() || it == '*' || it == '#' }
      normalized.contains(cleanQuery)
    }
    if (numberMatch) return true

    // 2. Full name T9 match or word-initial T9 match
    val fullT9 = toT9Sequence(displayName)
    if (fullT9.contains(cleanQuery)) return true

    val words = displayName.split(' ', '-', '_').filter { it.isNotBlank() }
    val wordStartsMatch = words.any { word ->
      toT9Sequence(word).startsWith(cleanQuery)
    }
    if (wordStartsMatch) return true

    // 3. Initials T9 match (e.g. "John Doe" -> "53")
    if (words.size >= cleanQuery.length) {
      val initialsT9 = words.mapNotNull { it.firstOrNull()?.let(::charToT9Digit) }.joinToString("")
      if (initialsT9.startsWith(cleanQuery)) return true
    }

    return false
  }

  fun matchesContactSearch(
    displayName: String,
    phoneNumbers: List<String>,
    email: String?,
    query: String,
    extraName: String? = null,
    extraNumber: String? = null,
  ): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true

    if (displayName.contains(q, ignoreCase = true)) return true
    if (!extraName.isNullOrBlank() && extraName.contains(q, ignoreCase = true)) return true
    if (!email.isNullOrBlank() && email.contains(q, ignoreCase = true)) return true

    val allNumbers = if (!extraNumber.isNullOrBlank()) phoneNumbers + extraNumber else phoneNumbers
    if (allNumbers.any { it.contains(q, ignoreCase = true) }) return true

    val queryDigits = q.filter { it.isDigit() || it == '*' || it == '#' }
    if (queryDigits.isNotEmpty() && q.none { it.isLetter() }) {
      val digitMatch = allNumbers.any { number ->
        val normalized = number.filter { it.isDigit() || it == '*' || it == '#' }
        normalized.contains(queryDigits)
      }
      if (digitMatch) return true
    }

    return false
  }
}
