package com.example.telecom

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import androidx.core.content.ContextCompat
import com.example.NoirDialApplication
import com.example.data.ContactPosterConfig
import com.example.data.PhoneNumberFormatter
import com.example.data.PosterConfigRepository
import com.example.data.SystemDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class CallStateHolder(
  private val appContext: Context,
  private val posterRepo: PosterConfigRepository,
  private val systemRepo: SystemDataRepository,
) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private data class CallerOverride(val name: String, val number: String)

  private val managedCalls = linkedMapOf<String, Call>()
  private val callbacks = mutableMapOf<String, Call.Callback>()
  private val callOverrides = mutableMapOf<String, CallerOverride>()
  private var inCallService: InCallService? = null
  private var tickerJob: Job? = null

  private var isMutedState = false
  private var audioRouteState = AudioRouteType.EARPIECE
  private var isBluetoothAvailableState = false
  private var pendingOutgoingEntry: CallEntry? = null
  private var pendingClearJob: Job? = null

  private val _uiState = MutableStateFlow(ActiveCallUiState())
  val uiState: StateFlow<ActiveCallUiState> = _uiState.asStateFlow()

  fun attachService(service: InCallService?) {
    inCallService = service
    if (service != null) {
      @Suppress("DEPRECATION")
      service.callAudioState?.let { updateAudioState(it) }
    }
  }

  fun isDefaultDialer(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = appContext.getSystemService(RoleManager::class.java)
      roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) == true
    } else {
      val telecom = appContext.getSystemService(TelecomManager::class.java)
      telecom?.defaultDialerPackage == appContext.packageName
    }
  }

  fun placeOutgoingCall(context: Context, rawNumber: String): Boolean {
    val cleanNumber = rawNumber.trim()
    if (cleanNumber.isBlank()) return false

    val hasPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.CALL_PHONE,
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) return false

    val uri = Uri.fromParts("tel", cleanNumber, null)
    val telecom = context.getSystemService(TelecomManager::class.java)

    return try {
      if (isDefaultDialer() && telecom != null) {
        val resolved = systemRepo.lookupCallerSync(cleanNumber)
        val posterConfig = posterRepo.resolveConfig(
          lookupKey = resolved.lookupKey,
          phoneNumbers = listOf(cleanNumber),
          fallbackKey = cleanNumber,
        )
        pendingOutgoingEntry = CallEntry(
          callId = "pending_outgoing",
          callerName = posterConfig.customCallerName?.takeIf { it.isNotBlank() } ?: resolved.displayName,
          callerNumber = PhoneNumberFormatter.formatForDisplay(
            posterConfig.customCallerNumber?.takeIf { it.isNotBlank() } ?: cleanNumber,
          ),
          status = CallUiStatus.DIALING,
          isIncoming = false,
          connectTimeMillis = 0L,
          durationSeconds = 0L,
          canHold = false,
          canSeparate = false,
          isConference = false,
          posterConfig = posterConfig,
          fallbackPhotoUri = resolved.photoUri,
        )
        rebuildState()
        pendingClearJob?.cancel()
        pendingClearJob = scope.launch {
          delay(4500L)
          if (pendingOutgoingEntry != null) {
            pendingOutgoingEntry = null
            rebuildState()
          }
        }

        val inCallIntent = Intent(context, InCallActivity::class.java).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(inCallIntent)

        val extras = Bundle().apply {
          putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
        }
        telecom.placeCall(uri, extras)
        true
      } else {
        val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(callIntent)
        true
      }
    } catch (_: SecurityException) {
      pendingOutgoingEntry = null
      rebuildState()
      false
    } catch (_: Exception) {
      pendingOutgoingEntry = null
      rebuildState()
      try {
        val fallback = Intent(Intent.ACTION_DIAL, uri).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallback)
        true
      } catch (_: Exception) {
        false
      }
    }
  }

  fun onCallAdded(call: Call) {
    pendingOutgoingEntry = null
    pendingClearJob?.cancel()
    val id = findIdForCall(call) ?: UUID.randomUUID().toString()
    managedCalls[id] = call

    val callback = object : Call.Callback() {
      override fun onStateChanged(call: Call, state: Int) {
        rebuildState()
        if (state == Call.STATE_DISCONNECTED) {
          scope.launch {
            delay(800)
            if (managedCalls[id]?.details?.state == Call.STATE_DISCONNECTED ||
              getCallCompatState(call) == Call.STATE_DISCONNECTED
            ) {
              removeCallInternal(id)
            }
          }
        }
      }

      override fun onDetailsChanged(call: Call, details: Call.Details) {
        rebuildState()
      }

      override fun onChildrenChanged(call: Call, children: MutableList<Call>) {
        rebuildState()
      }

      override fun onConferenceableCallsChanged(call: Call, conferenceableCalls: MutableList<Call>) {
        rebuildState()
      }
    }

    callbacks[id] = callback
    call.registerCallback(callback)
    rebuildState()
    ensureTickerRunning()
  }

  fun onCallRemoved(call: Call) {
    val id = findIdForCall(call) ?: return
    removeCallInternal(id)
  }

  private fun removeCallInternal(id: String) {
    val call = managedCalls.remove(id)
    callOverrides.remove(id)
    val cb = callbacks.remove(id)
    if (call != null && cb != null) {
      runCatching { call.unregisterCallback(cb) }
    }
    rebuildState()
    if (managedCalls.isEmpty()) {
      tickerJob?.cancel()
      tickerJob = null
      cancelCallNotification()
    }
  }

  @Suppress("DEPRECATION")
  fun updateAudioState(audioState: CallAudioState) {
    isMutedState = audioState.isMuted
    isBluetoothAvailableState = (audioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0
    audioRouteState = when (audioState.route) {
      CallAudioState.ROUTE_SPEAKER -> AudioRouteType.SPEAKER
      CallAudioState.ROUTE_BLUETOOTH -> AudioRouteType.BLUETOOTH
      else -> AudioRouteType.EARPIECE
    }
    rebuildState()
  }

  fun answerCall(callId: String? = null) {
    val target = resolveTargetCall(callId) ?: findRingingCall() ?: return
    target.answer(VideoProfile.STATE_AUDIO_ONLY)
  }

  fun declineOrEndCall(callId: String? = null) {
    if (callId == "pending_outgoing" && managedCalls.isEmpty()) {
      pendingOutgoingEntry = null
      pendingClearJob?.cancel()
      rebuildState()
      return
    }
    val target = resolveTargetCall(callId) ?: _uiState.value.primaryCall?.let { managedCalls[it.callId] }
    target?.disconnect()
  }

  fun updateCallerInfo(callId: String, newName: String, newNumber: String) {
    val cleanName = newName.trim()
    val cleanNumber = newNumber.trim()
    callOverrides[callId] = CallerOverride(name = cleanName, number = cleanNumber)

    if (callId == "pending_outgoing" && pendingOutgoingEntry != null) {
      val currentPending = pendingOutgoingEntry!!
      pendingOutgoingEntry = currentPending.copy(
        callerName = cleanName.ifBlank { currentPending.callerName },
        callerNumber = cleanNumber.takeIf { it.isNotBlank() }
          ?.let { PhoneNumberFormatter.formatForDisplay(it) }
          ?: currentPending.callerNumber,
        posterConfig = currentPending.posterConfig.copy(
          customCallerName = cleanName.takeIf { it.isNotBlank() },
          customCallerNumber = cleanNumber.takeIf { it.isNotBlank() },
        ),
      )
    }

    val call = managedCalls[callId]
    val rawNumber = call?.let { extractNumber(it) }
      ?: pendingOutgoingEntry?.callerNumber.orEmpty()
    val resolved = if (rawNumber.isNotBlank()) systemRepo.lookupCallerSync(rawNumber) else null

    val primaryKey = resolved?.lookupKey?.takeIf { it.isNotBlank() }
      ?: rawNumber.takeIf { it.isNotBlank() }?.let { PhoneNumberFormatter.matchKey(it) }
      ?: cleanNumber.takeIf { it.isNotBlank() }?.let { PhoneNumberFormatter.matchKey(it) }
    if (!primaryKey.isNullOrBlank()) {
      val secondaryKeys = listOfNotNull(
        rawNumber.takeIf { it.isNotBlank() }?.let { PhoneNumberFormatter.matchKey(it) },
        cleanNumber.takeIf { it.isNotBlank() }?.let { PhoneNumberFormatter.matchKey(it) },
      ).distinct()
      val existing = posterRepo.resolveConfig(
        lookupKey = resolved?.lookupKey,
        phoneNumbers = listOfNotNull(rawNumber.takeIf { it.isNotBlank() }, cleanNumber.takeIf { it.isNotBlank() }),
        fallbackKey = primaryKey,
      )
      posterRepo.savePosterConfig(
        primaryKey = primaryKey,
        secondaryKeys = secondaryKeys,
        config = existing.copy(
          customCallerName = cleanName.takeIf { it.isNotBlank() },
          customCallerNumber = cleanNumber.takeIf { it.isNotBlank() },
        ),
      )
    }

    scope.launch(Dispatchers.IO) {
      systemRepo.upsertCallerContact(
        contactId = resolved?.contactId,
        oldNumber = rawNumber.ifBlank { cleanNumber },
        newName = cleanName,
        newNumber = cleanNumber,
      )
    }

    rebuildState()
  }

  fun rejectWithMessage(callId: String? = null, message: String) {
    val target = resolveTargetCall(callId) ?: findRingingCall() ?: return
    val number = extractNumber(target)
    try {
      target.reject(true, message)
    } catch (_: Exception) {
      target.disconnect()
      if (number.isNotBlank()) {
        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).apply {
          putExtra("sms_body", message)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(smsIntent) }
      }
    }
  }

  @SuppressLint("ScheduleExactAlarm")
  fun scheduleCallbackReminder(
    callerName: String,
    callerNumber: String,
    delayMinutes: Int,
  ) {
    val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
    val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000L
    val intent = Intent(appContext, CallReminderReceiver::class.java).apply {
      putExtra(CallReminderReceiver.EXTRA_CALLER_NAME, callerName)
      putExtra(CallReminderReceiver.EXTRA_CALLER_NUMBER, callerNumber)
    }
    val pending = PendingIntent.getBroadcast(
      appContext,
      (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
  }

  fun toggleMute() {
    val service = inCallService ?: return
    service.setMuted(!isMutedState)
  }

  @Suppress("DEPRECATION")
  fun toggleSpeaker() {
    val service = inCallService ?: return
    val nextRoute = if (audioRouteState == AudioRouteType.SPEAKER) {
      if (isBluetoothAvailableState) CallAudioState.ROUTE_BLUETOOTH else CallAudioState.ROUTE_WIRED_OR_EARPIECE
    } else {
      CallAudioState.ROUTE_SPEAKER
    }
    service.setAudioRoute(nextRoute)
  }

  fun toggleHold(callId: String? = null) {
    val target = resolveTargetCall(callId) ?: return
    val state = getCallCompatState(target)
    if (state == Call.STATE_HOLDING) {
      target.unhold()
    } else if (state == Call.STATE_ACTIVE) {
      target.hold()
    }
  }

  fun playDtmf(digit: Char) {
    val target = resolvePrimaryTelecomCall() ?: return
    target.playDtmfTone(digit)
  }

  fun stopDtmf() {
    val target = resolvePrimaryTelecomCall() ?: return
    target.stopDtmfTone()
  }

  fun swapCalls() {
    val calls = topLevelCalls()
    if (calls.size >= 2) {
      val active = calls.firstOrNull { getCallCompatState(it) == Call.STATE_ACTIVE }
      val held = calls.firstOrNull { getCallCompatState(it) == Call.STATE_HOLDING }
      if (active != null && held != null) {
        active.hold()
        held.unhold()
        return
      }
    }
    val primary = resolvePrimaryTelecomCall()
    if (primary != null && primary.details.can(Call.Details.CAPABILITY_SWAP_CONFERENCE)) {
      primary.swapConference()
    }
  }

  fun mergeCalls() {
    val calls = topLevelCalls()
    val primary = resolvePrimaryTelecomCall() ?: return

    val conferenceTarget = primary.conferenceableCalls.firstOrNull()
      ?: calls.firstOrNull { it != primary }

    if (conferenceTarget != null) {
      val invoked = runCatching {
        primary.conference(conferenceTarget)
      }.isSuccess
      if (invoked && !primary.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE)) {
        return
      }
    }

    if (primary.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE)) {
      primary.mergeConference()
    }
  }

  fun splitFromConference(childCallId: String) {
    val allChildren = managedCalls.values.flatMap { it.children }
    val match = allChildren.firstOrNull { child ->
      childKey(child) == childCallId
    }
    match?.splitFromConference()
  }

  fun disconnectConferenceParticipant(childCallId: String) {
    val allChildren = managedCalls.values.flatMap { it.children }
    val match = allChildren.firstOrNull { child ->
      childKey(child) == childCallId
    }
    match?.disconnect()
  }

  private fun ensureTickerRunning() {
    if (tickerJob?.isActive == true) return
    tickerJob = scope.launch {
      while (isActive && managedCalls.isNotEmpty()) {
        delay(1000L)
        rebuildState()
      }
    }
  }

  private fun cancelCallNotification() {
    val nm = appContext.getSystemService(NotificationManager::class.java)
    nm?.cancel(NoirInCallService.NOTIFICATION_ID)
  }

  private fun topLevelCalls(): List<Call> {
    return managedCalls.values.filter { it.parent == null }
  }

  private fun findRingingCall(): Call? {
    return topLevelCalls().firstOrNull { getCallCompatState(it) == Call.STATE_RINGING }
  }

  private fun resolveTargetCall(callId: String?): Call? {
    if (callId != null && managedCalls.containsKey(callId)) return managedCalls[callId]
    return resolvePrimaryTelecomCall()
  }

  private fun resolvePrimaryTelecomCall(): Call? {
    val top = topLevelCalls()
    return top.firstOrNull { getCallCompatState(it) == Call.STATE_RINGING }
      ?: top.firstOrNull { getCallCompatState(it) == Call.STATE_ACTIVE }
      ?: top.firstOrNull { getCallCompatState(it) == Call.STATE_DIALING || getCallCompatState(it) == Call.STATE_CONNECTING }
      ?: top.firstOrNull()
  }

  private fun findIdForCall(call: Call): String? {
    return managedCalls.entries.firstOrNull { it.value == call }?.key
  }

  private fun childKey(call: Call): String {
    return "${call.hashCode()}_${extractNumber(call)}"
  }

  @Suppress("DEPRECATION")
  private fun getCallCompatState(call: Call): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      call.details.state
    } else {
      call.state
    }
  }

  private fun extractNumber(call: Call): String {
    val handle = call.details?.handle
    return handle?.schemeSpecificPart?.trim().orEmpty()
  }

  private fun toCallEntry(id: String, call: Call): CallEntry {
    val details = call.details
    val isConference = details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true || call.children.isNotEmpty()
    val rawNumber = extractNumber(call)
    val resolved = if (isConference) null else systemRepo.lookupCallerSync(rawNumber)

    val posterConfig = if (isConference) {
      ContactPosterConfig(contactKey = "conference")
    } else {
      posterRepo.resolveConfig(
        lookupKey = resolved?.lookupKey,
        phoneNumbers = listOfNotNull(rawNumber.takeIf { it.isNotBlank() }),
        fallbackKey = rawNumber.ifBlank { id },
      )
    }

    val override = callOverrides[id]
    val telecomContactName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      details?.contactDisplayName?.toString()?.trim()?.takeIf { it.isNotBlank() }
    } else {
      null
    }

    // Priority: User in-call override / custom name -> Saved Contact in Contacts ->
    // Telecom contact name -> Carrier/SIM callerDisplayName -> formatted number
    val callerName = when {
      isConference -> "Conference Call (${call.children.size.coerceAtLeast(2)})"
      !override?.name.isNullOrBlank() -> override!!.name
      !posterConfig.customCallerName.isNullOrBlank() -> posterConfig.customCallerName
      resolved?.isSavedContact == true -> resolved.displayName
      !telecomContactName.isNullOrBlank() -> telecomContactName
      !details?.callerDisplayName.isNullOrBlank() -> details.callerDisplayName!!
      rawNumber.isNotBlank() -> PhoneNumberFormatter.formatForDisplay(rawNumber)
      else -> "Unknown Caller"
    }

    val displayedNumber = when {
      isConference -> "${call.children.size} participants"
      !override?.number.isNullOrBlank() -> PhoneNumberFormatter.formatForDisplay(override!!.number)
      !posterConfig.customCallerNumber.isNullOrBlank() -> PhoneNumberFormatter.formatForDisplay(posterConfig.customCallerNumber)
      else -> PhoneNumberFormatter.formatForDisplay(rawNumber)
    }

    val rawState = getCallCompatState(call)
    val status = when (rawState) {
      Call.STATE_RINGING -> CallUiStatus.RINGING
      Call.STATE_DIALING -> CallUiStatus.DIALING
      Call.STATE_CONNECTING, Call.STATE_NEW -> CallUiStatus.CONNECTING
      Call.STATE_ACTIVE -> CallUiStatus.ACTIVE
      Call.STATE_HOLDING -> CallUiStatus.HOLDING
      else -> CallUiStatus.DISCONNECTED
    }

    val connectTime = details?.connectTimeMillis ?: 0L
    val durationSec = if (connectTime > 0L && status == CallUiStatus.ACTIVE) {
      ((System.currentTimeMillis() - connectTime) / 1000L).coerceAtLeast(0L)
    } else {
      0L
    }

    val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      details?.callDirection == Call.Details.DIRECTION_INCOMING || status == CallUiStatus.RINGING
    } else {
      status == CallUiStatus.RINGING
    }

    val childNames = call.children.map { child ->
      val cNum = extractNumber(child)
      systemRepo.lookupCallerSync(cNum).displayName
    }

    return CallEntry(
      callId = id,
      callerName = callerName,
      callerNumber = displayedNumber,
      status = status,
      isIncoming = isIncoming,
      connectTimeMillis = connectTime,
      durationSeconds = durationSec,
      canHold = details?.can(Call.Details.CAPABILITY_HOLD) == true || status == CallUiStatus.HOLDING,
      canSeparate = details?.can(Call.Details.CAPABILITY_SEPARATE_FROM_CONFERENCE) == true,
      isConference = isConference,
      childNames = childNames,
      posterConfig = posterConfig,
      fallbackPhotoUri = resolved?.photoUri,
    )
  }

  private fun rebuildState() {
    val topCalls = managedCalls.entries.filter { it.value.parent == null }
    val entries = topCalls.map { (id, call) -> toCallEntry(id, call) }.ifEmpty {
      listOfNotNull(pendingOutgoingEntry)
    }

    val primary = entries.firstOrNull { it.status == CallUiStatus.RINGING }
      ?: entries.firstOrNull { it.status == CallUiStatus.ACTIVE }
      ?: entries.firstOrNull { it.status == CallUiStatus.DIALING || it.status == CallUiStatus.CONNECTING }
      ?: entries.firstOrNull()

    val secondary = entries.firstOrNull { it.callId != primary?.callId }

    val primaryTelecomCall = primary?.let { managedCalls[it.callId] }
    val conferenceParticipants = primaryTelecomCall?.children?.map { child ->
      toCallEntry(childKey(child), child)
    } ?: emptyList()

    val canMerge = if (primaryTelecomCall != null) {
      primaryTelecomCall.conferenceableCalls.isNotEmpty() ||
        primaryTelecomCall.details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE) ||
        entries.count { it.status == CallUiStatus.ACTIVE || it.status == CallUiStatus.HOLDING } >= 2
    } else {
      false
    }

    val canSwap = entries.count { it.status == CallUiStatus.ACTIVE || it.status == CallUiStatus.HOLDING } >= 2 ||
      (primaryTelecomCall?.details?.can(Call.Details.CAPABILITY_SWAP_CONFERENCE) == true)

    _uiState.value = ActiveCallUiState(
      primaryCall = primary,
      secondaryHeldCall = secondary,
      allCalls = entries,
      conferenceParticipants = conferenceParticipants,
      canAddCall = entries.size < 2 && primary?.status != CallUiStatus.RINGING,
      canMergeCalls = canMerge,
      canSwapCalls = canSwap,
      isConference = primary?.isConference == true,
      isMuted = isMutedState,
      audioRoute = audioRouteState,
      isBluetoothAvailable = isBluetoothAvailableState,
    )
  }

  fun launchInCallUiIfActive() {
    if (managedCalls.isNotEmpty()) {
      val intent = Intent(appContext, InCallActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      }
      appContext.startActivity(intent)
    }
  }

  companion object {
    fun getInstance(context: Context): CallStateHolder {
      return (context.applicationContext as NoirDialApplication).callStateHolder
    }
  }
}
