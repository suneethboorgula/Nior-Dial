package com.example

import com.example.data.PhoneNumberFormatter
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun contactSearch_filtersByNameEmailAndDigits() {
    assertTrue(
      PhoneNumberFormatter.matchesContactSearch(
        displayName = "Elena Vance",
        phoneNumbers = listOf("+1 (415) 890-4321"),
        email = "elena@noir.app",
        query = "vance",
      ),
    )
    assertFalse(
      PhoneNumberFormatter.matchesContactSearch(
        displayName = "Marcus Cole",
        phoneNumbers = listOf("+1 (212) 555-0199"),
        email = "marcus@noir.app",
        query = "vance",
      ),
    )
    assertTrue(
      PhoneNumberFormatter.matchesContactSearch(
        displayName = "Elena Vance",
        phoneNumbers = listOf("+14158904321"),
        email = null,
        query = "415 890",
      ),
    )
    assertFalse(
      PhoneNumberFormatter.matchesT9OrDigits("Bob Smith", listOf("+15550001111"), "elena"),
    )
  }
}
