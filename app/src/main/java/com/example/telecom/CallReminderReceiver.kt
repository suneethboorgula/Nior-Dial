package com.example.telecom

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NoirDialApplication

class CallReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val callerName = intent.getStringExtra(EXTRA_CALLER_NAME).orEmpty().ifBlank { "Missed Caller" }
    val callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER).orEmpty()

    val dialIntent = if (callerNumber.isNotBlank()) {
      Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", callerNumber, null)).apply {
        setClass(context, MainActivity::class.java)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    } else {
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
      dialIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, NoirDialApplication.CHANNEL_CALL_REMINDERS)
      .setSmallIcon(android.R.drawable.sym_call_missed)
      .setContentTitle("Call back $callerName")
      .setContentText(if (callerNumber.isNotBlank()) "Tap to call $callerNumber" else "Scheduled callback reminder")
      .setAutoCancel(true)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setContentIntent(pendingIntent)
      .build()

    val nm = context.getSystemService(NotificationManager::class.java)
    nm?.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
  }

  companion object {
    const val EXTRA_CALLER_NAME = "extra_caller_name"
    const val EXTRA_CALLER_NUMBER = "extra_caller_number"
  }
}
