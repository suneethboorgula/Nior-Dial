package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ContactPosterConfig
import com.example.data.PhoneNumberFormatter
import com.example.data.PosterAlignment
import com.example.data.PosterConfigRepository
import com.example.data.PosterFontStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Noir Dial", appName)
  }

  @Test
  fun `t9 matching matches contact names and numbers`() {
    assertTrue(PhoneNumberFormatter.matchesT9OrDigits("Elena Vance", listOf("+15551234567"), "35362"))
    assertTrue(PhoneNumberFormatter.matchesT9OrDigits("Elena Vance", listOf("+15551234567"), "826"))
    assertTrue(PhoneNumberFormatter.matchesT9OrDigits("Elena Vance", listOf("+15551234567"), "38"))
    assertTrue(PhoneNumberFormatter.matchesT9OrDigits("Elena Vance", listOf("+15551234567"), "1234"))
    assertFalse(PhoneNumberFormatter.matchesT9OrDigits("Elena Vance", listOf("+15551234567"), "9999"))
  }

  @Test
  fun `poster config repository persists json across instances`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo1 = PosterConfigRepository(context)
    val config = ContactPosterConfig(
      contactKey = "5551234567",
      posterUri = "content://media/picker/123",
      fontStyle = PosterFontStyle.CONDENSED_BOLD,
      textColorHex = 0xFFFFD166L,
      nameAlignment = PosterAlignment.BOTTOM,
      notes = "VIP caller",
    )
    repo1.savePosterConfig("lookup_elena", listOf("5551234567"), config)

    val repo2 = PosterConfigRepository(context)
    val loaded = repo2.resolveConfig(lookupKey = "lookup_elena")
    assertEquals("content://media/picker/123", loaded.posterUri)
    assertEquals(PosterFontStyle.CONDENSED_BOLD, loaded.fontStyle)
    assertEquals(0xFFFFD166L, loaded.textColorHex)
    assertEquals(PosterAlignment.BOTTOM, loaded.nameAlignment)
    assertEquals("VIP caller", loaded.notes)
  }

  @Test
  fun `demo poster preloads bundled dummy image uri`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = PosterConfigRepository(context)
    val demo = repo.resolveConfig(lookupKey = PosterConfigRepository.DEMO_POSTER_KEY)
    assertEquals(repo.samplePosterUri(), demo.posterUri)
    assertTrue(demo.posterUri!!.startsWith("android.resource://"))
  }

  @Test
  fun `custom caller name and number persist across repository instances`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo1 = PosterConfigRepository(context)
    repo1.savePosterConfig(
      primaryKey = "9876543210",
      secondaryKeys = listOf("9876543210"),
      config = ContactPosterConfig(
        contactKey = "9876543210",
        customCallerName = "Saved Friend",
        customCallerNumber = "+1 987 654 3210",
      ),
    )

    val repo2 = PosterConfigRepository(context)
    val loaded = repo2.resolveConfig(phoneNumbers = listOf("+91 98765 43210"))
    assertEquals("Saved Friend", loaded.customCallerName)
    assertEquals("+1 987 654 3210", loaded.customCallerNumber)
  }
}
