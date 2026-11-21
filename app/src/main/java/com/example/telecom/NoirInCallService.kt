package com.example.telecom

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import androidx.core.app.NotificationCompat
import com.example.NoirDialApplication
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NoirInCallService : InCallService() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var stateJob: Job? = null
  private lateinit var callStateHolder: CallStateHolder

  override fun onCreate() {
    super.onCreate()
    callStateHolder = CallStateHolder.getInstance(this)
    callStateHolder.attachService(this)
    observeCallNotifications()
  }

  override fun onBind(intent: Intent?): IBinder? {
    callStateHolder.attachService(this)
    return super.onBind(intent)
  }

  override fun onUnbind(intent: Intent?): Boolean {
    callStateHolder.attachService(null)
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    super.onDestroy()
    stateJob?.cancel()
    serviceScope.cancel()
    callStateHolder.attachService(null)
  }

  override fun onCallAdded(call: Call) {
    super.onCallAdded(call)
    callStateHolder.onCallAdded(call)
    val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      call.details.state
    } else {
      @Suppress("DEPRECATION")
      call.state
    }
    postOrUpdateNotification(callStateHolder.uiState.value)
    if (state != Call.STATE_RINGING) {
      launchInCallActivity()
    } else {
      // Also attempt direct launch on lock/unlock; fullScreenIntent handles locked & heads-up
      launchInCallActivity()
    }
  }

  override fun onCallRemoved(call: Call) {
    super.onCallRemoved(call)
    callStateHolder.onCallRemoved(call)
  }

  @Deprecated("Deprecated in Java")
  override fun onCallAudioStateChanged(audioState: CallAudioState?) {
    @Suppress("DEPRECATION")
    super.onCallAudioStateChanged(audioState)
    if (audioState != null) {
      callStateHolder.updateAudioState(audioState)
    }
  }

  private fun observeCallNotifications() {
    stateJob?.cancel()
    stateJob = serviceScope.launch {
      callStateHolder.uiState.collectLatest { state ->
        if (state.hasCalls) {
          postOrUpdateNotification(state)
        } else {
          val nm = getSystemService(NotificationManager::class.java)
          nm?.cancel(NOTIFICATION_ID)
        }
      }
    }
  }

  private fun launchInCallActivity() {
    val intent = Intent(this, InCallActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or
        Intent.FLAG_ACTIVITY_NO_USER_ACTION or
        Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    runCatching { startActivity(intent) }
  }

  private fun postOrUpdateNotification(state: ActiveCallUiState) {
    val primary = state.primaryCall ?: return
    val nm = getSystemService(NotificationManager::class.java) ?: return

    val fullScreenIntent = Intent(this, InCallActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or
        Intent.FLAG_ACTIVITY_NO_USER_ACTION or
        Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val fullScreenPendingIntent = PendingIntent.getActivity(
      this,
      1001,
      fullScreenIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val answerIntent = Intent(this, CallActionReceiver::class.java).apply {
      action = CallActionReceiver.ACTION_ANSWER
      putExtra(CallActionReceiver.EXTRA_CALL_ID, primary.callId)
    }
    val answerPendingIntent = PendingIntent.getBroadcast(
      this,
      1002,
      answerIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val declineIntent = Intent(this, CallActionReceiver::class.java).apply {
      action = CallActionReceiver.ACTION_DECLINE
      putExtra(CallActionReceiver.EXTRA_CALL_ID, primary.callId)
    }
    val declinePendingIntent = PendingIntent.getBroadcast(
      this,
      1003,
      declineIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val isRinging = primary.status == CallUiStatus.RINGING
    val channelId = if (isRinging) {
      NoirDialApplication.CHANNEL_INCOMING_CALLS
    } else {
      NoirDialApplication.CHANNEL_ONGOING_CALLS
    }

    val statusText = when (primary.status) {
      CallUiStatus.RINGING -> getString(R.string.incoming_call_subtitle)
      CallUiStatus.DIALING, CallUiStatus.CONNECTING -> "Calling…"
      CallUiStatus.ACTIVE -> if (state.isConference) "Conference Call" else "Active Call"
      CallUiStatus.HOLDING -> "On Hold"
      CallUiStatus.DISCONNECTED -> "Call Ended"
    }

    val builder = NotificationCompat.Builder(this, channelId)
      .setSmallIcon(android.R.drawable.sym_call_incoming)
      .setContentTitle(primary.callerName)
      .setContentText(statusText)
      .setOngoing(true)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setContentIntent(fullScreenPendingIntent)
      .setPriority(if (isRinging) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_LOW)

    if (isRinging) {
      builder
        .setFullScreenIntent(fullScreenPendingIntent, true)
        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePendingIntent)
        .addAction(android.R.drawable.sym_action_call, "Answer", answerPendingIntent)
    } else {
      builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "End Call", declinePendingIntent)
    }

    val notification: Notification = builder.build()
    nm.notify(NOTIFICATION_ID, notification)
  }

  companion object {
    const val NOTIFICATION_ID = 4201
  }
}
