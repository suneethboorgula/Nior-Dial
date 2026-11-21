package com.example.telecom

import com.example.data.ContactPosterConfig

enum class CallUiStatus {
  RINGING,
  DIALING,
  CONNECTING,
  ACTIVE,
  HOLDING,
  DISCONNECTED,
}

enum class AudioRouteType {
  EARPIECE,
  SPEAKER,
  BLUETOOTH,
}

data class CallEntry(
  val callId: String,
  val callerName: String,
  val callerNumber: String,
  val status: CallUiStatus,
  val isIncoming: Boolean,
  val connectTimeMillis: Long,
  val durationSeconds: Long,
  val canHold: Boolean,
  val canSeparate: Boolean,
  val isConference: Boolean,
  val childNames: List<String> = emptyList(),
  val posterConfig: ContactPosterConfig,
  val fallbackPhotoUri: String? = null,
)

data class ActiveCallUiState(
  val primaryCall: CallEntry? = null,
  val secondaryHeldCall: CallEntry? = null,
  val allCalls: List<CallEntry> = emptyList(),
  val conferenceParticipants: List<CallEntry> = emptyList(),
  val canAddCall: Boolean = true,
  val canMergeCalls: Boolean = false,
  val canSwapCalls: Boolean = false,
  val isConference: Boolean = false,
  val isMuted: Boolean = false,
  val audioRoute: AudioRouteType = AudioRouteType.EARPIECE,
  val isBluetoothAvailable: Boolean = false,
) {
  val hasCalls: Boolean
    get() = allCalls.isNotEmpty()
}
