package com.example.telecom

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.NoirDialApplication
import com.example.ui.screens.ActiveInCallScreen
import com.example.ui.screens.IncomingCallScreen
import com.example.ui.theme.NoirDialTheme

class InCallActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
      val keyguard = getSystemService(KeyguardManager::class.java)
      keyguard?.requestDismissKeyguard(this, null)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
          WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
      )
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
    )

    val app = application as NoirDialApplication
    val callStateHolder = app.callStateHolder

    setContent {
      NoirDialTheme {
        val uiState by callStateHolder.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(uiState.hasCalls) {
          if (!uiState.hasCalls) {
            finish()
          }
        }

        val primary = uiState.primaryCall
        if (primary != null) {
          if (primary.status == CallUiStatus.RINGING) {
            IncomingCallScreen(
              call = primary,
              onAccept = { callStateHolder.answerCall(primary.callId) },
              onDecline = { callStateHolder.declineOrEndCall(primary.callId) },
              onRejectWithMessage = { msg -> callStateHolder.rejectWithMessage(primary.callId, msg) },
              onScheduleReminder = { minutes ->
                callStateHolder.scheduleCallbackReminder(
                  callerName = primary.callerName,
                  callerNumber = primary.callerNumber,
                  delayMinutes = minutes,
                )
                callStateHolder.declineOrEndCall(primary.callId)
              },
            )
          } else {
            ActiveInCallScreen(
              uiState = uiState,
              onToggleMute = { callStateHolder.toggleMute() },
              onToggleSpeaker = { callStateHolder.toggleSpeaker() },
              onToggleHold = { callStateHolder.toggleHold(primary.callId) },
              onSwapCalls = { callStateHolder.swapCalls() },
              onMergeCalls = { callStateHolder.mergeCalls() },
              onAddCall = { launchSystemDialerForSecondCall() },
              onSplitParticipant = { childId -> callStateHolder.splitFromConference(childId) },
              onDisconnectParticipant = { childId -> callStateHolder.disconnectConferenceParticipant(childId) },
              onPlayDtmf = { digit -> callStateHolder.playDtmf(digit) },
              onStopDtmf = { callStateHolder.stopDtmf() },
              onEndCall = { callStateHolder.declineOrEndCall(primary.callId) },
            )
          }
        }
      }
    }
  }

  private fun launchSystemDialerForSecondCall() {
    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(dialIntent) }
  }
}
