package com.example.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val callId = intent.getStringExtra(EXTRA_CALL_ID)
    val holder = CallStateHolder.getInstance(context)
    when (intent.action) {
      ACTION_ANSWER -> {
        holder.answerCall(callId)
        holder.launchInCallUiIfActive()
      }
      ACTION_DECLINE -> {
        holder.declineOrEndCall(callId)
      }
    }
  }

  companion object {
    const val ACTION_ANSWER = "com.example.telecom.ACTION_ANSWER"
    const val ACTION_DECLINE = "com.example.telecom.ACTION_DECLINE"
    const val EXTRA_CALL_ID = "extra_call_id"
  }
}
