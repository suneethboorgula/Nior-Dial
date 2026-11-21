package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.data.PosterConfigRepository
import com.example.data.SystemDataRepository
import com.example.telecom.CallStateHolder

class NoirDialApplication : Application() {

  lateinit var posterRepository: PosterConfigRepository
    private set
  lateinit var systemRepository: SystemDataRepository
    private set
  lateinit var callStateHolder: CallStateHolder
    private set

  override fun onCreate() {
    super.onCreate()
    posterRepository = PosterConfigRepository(this)
    systemRepository = SystemDataRepository(this)
    callStateHolder = CallStateHolder(this, posterRepository, systemRepository)
    createNotificationChannels()
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = getSystemService(NotificationManager::class.java) ?: return

    val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    val incomingChannel = NotificationChannel(
      CHANNEL_INCOMING_CALLS,
      getString(R.string.incoming_channel_name),
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = getString(R.string.incoming_channel_desc)
      setSound(
        ringtoneUri,
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build(),
      )
      enableVibration(true)
      lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
    }

    val ongoingChannel = NotificationChannel(
      CHANNEL_ONGOING_CALLS,
      getString(R.string.ongoing_channel_name),
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = getString(R.string.ongoing_channel_desc)
      setSound(null, null)
    }

    val reminderChannel = NotificationChannel(
      CHANNEL_CALL_REMINDERS,
      getString(R.string.reminder_channel_name),
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = getString(R.string.reminder_channel_desc)
      enableVibration(true)
    }

    nm.createNotificationChannels(listOf(incomingChannel, ongoingChannel, reminderChannel))
  }

  companion object {
    const val CHANNEL_INCOMING_CALLS = "noir_incoming_calls"
    const val CHANNEL_ONGOING_CALLS = "noir_ongoing_calls"
    const val CHANNEL_CALL_REMINDERS = "noir_call_reminders"
  }
}
