# Noir Dial

A minimalist OLED dark-mode Android default phone and dialer application built with **Kotlin**, **Jetpack Compose (Material 3)**, and the **Android Telecom framework**, featuring customizable full-screen **Contact Posters** and full multi-call management.

---

## Features

### 1. OLED Dialer & Silent T9 Keypad
- Pure `#000000` OLED-black aesthetic with `#1C1C1E` charcoal cards and high-contrast typography.
- Silent custom keypad with instant **T9 contact & number matching**, quick-dial actions, and `tel:` / `ACTION_DIAL` intent handling.
- Quick **Preview** mode to inspect incoming and active Contact Poster call screens anytime.

### 2. Customizable Contact Posters
- Full-screen vertical poster backdrops supporting gallery images via the zero-permission Android **Photo Picker** (`PickVisualMedia`) or a bundled sample poster.
- Clean dark-gradient fallback when no poster image is set.
- Customizable **Typography Styles** (`Playfair Display`, `Inter`, `Oswald`), **Poster Text Color** swatches, **Name Alignment** (`Top` / `Bottom`), and private **Contact Notes**.
- On-demand **Poster Editor** toggled via the **Edit Poster** button at the top of the contact page.
- Dedicated **Poster Call Preview** screen with `Incoming`, `Active Call`, and `Close` modes.

### 3. Contacts & Per-Contact Recent Calls
- Searchable system contacts list (`ContactsContract`) with an in-app contact creator and **Edit Contact Info** sheet (edit display name and phone number directly from the contact page, synced with local poster overrides and system Contacts).
- **Recent Calls** card directly on each contact's profile displaying up to the **last 10 call logs** (`Incoming`, `Outgoing`, `Missed`, `Declined`) with directional status icons, timestamps, and call durations.
- **Smart Caller Name Priority**: Saved contact names and user-edited names take priority over carrier/SIM-registered `callerDisplayName` (CNAP) during incoming and active calls.
- Quick actions for **Message**, **Call**, **Video**, **Mail**, and **Share Contact**.

### 4. Default Phone (`InCallService`) & Multi-Call Management
- Registers for `RoleManager.ROLE_DIALER` / `TelecomManager` default phone role.
- **Incoming Call Screen**: Full-screen lock-screen intent (`showWhenLocked` / `turnScreenOn`), high-priority call notification actions, quick-reply SMS sheet, and `AlarmManager` callback reminders (`CallReminderReceiver`).
- **Active In-Call Screen**: Live call timer, **Mute**, **Keypad** (DTMF tones), **Speaker / Audio Route**, **Hold**, **Swap**, **Add Call**, **Merge** (`Call.conference()`), and **Manage Conference** participant controls.
- Persistent **"Tap to return to call"** status pill across app screens whenever a call is active.

---

## Tech Stack

- **Language:** Kotlin (JVM 11)
- **UI:** Jetpack Compose, Material Design 3, Edge-to-Edge (`enableEdgeToEdge`)
- **Architecture:** MVVM (`MainViewModel` + `StateFlow`) & Telecom `CallStateHolder`
- **Navigation:** Jetpack Navigation Compose
- **Image Loading:** Coil (`coil-compose`)
- **System Integrations:** Android `TelecomManager`, `InCallService`, `RoleManager`, `ContactsContract`, `CallLog`, `AlarmManager`, `ActivityResultContracts.PickVisualMedia`
- **Testing:** JUnit 4, Robolectric, Kotlin Coroutines Test, Compose UI Test (`ui-test-junit4`)
- **SDK Levels:** `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`

---

## Project Structure

```text
app/src/main/java/com/example/
├── MainActivity.kt                  # Navigation host, permissions, ROLE_DIALER prompt & tel: handling
├── NoirDialApplication.kt           # Application entry point & shared CallStateHolder / repositories
├── data/
│   ├── Models.kt                    # Contacts, CallLogEntry, ContactPosterConfig & UI state models
│   ├── PhoneNumberFormatter.kt      # Display formatting & normalized match keys
│   ├── PosterConfigRepository.kt    # Persistent JSON storage for poster styles, notes & overrides
│   └── SystemDataRepository.kt      # ContactsContract & CallLog queries + caller resolution
├── telecom/
│   ├── CallModels.kt                # Call entries, audio routes & conference call models
│   ├── CallStateHolder.kt           # Reactive Telecom call state, DTMF, hold/swap/merge & notifications
│   ├── NoirInCallService.kt         # Android InCallService implementation
│   ├── InCallActivity.kt            # Lock-screen-capable incoming & active call Activity
│   ├── CallActionReceiver.kt        # Notification Answer / Decline / Hangup broadcast receiver
│   └── CallReminderReceiver.kt      # Scheduled callback reminder notification receiver
└── ui/
    ├── MainViewModel.kt             # Dialer, contacts, call logs & poster state management
    ├── components/
    │   ├── ContactPosterCanvas.kt   # Full-screen poster backdrop, gradient & stylized name block
    │   └── PermissionCards.kt       # Default dialer & runtime permission prompt banners
    ├── screens/
    │   ├── KeypadScreen.kt          # T9 dialer keypad & matched contacts list
    │   ├── ContactsScreen.kt        # Searchable contacts directory & new contact sheet
    │   ├── RecentsScreen.kt         # System call history log
    │   ├── ContactPosterProfileScreen.kt # Contact poster, actions, editor, last 10 calls & notes
    │   ├── PosterCallPreviewScreen.kt    # Interactive Incoming / Active poster preview
    │   ├── IncomingCallScreen.kt    # Full-screen incoming call UI with quick SMS & reminder
    │   └── ActiveInCallScreen.kt    # Multi-call active UI with DTMF keypad & conference controls
    └── theme/
        ├── Color.kt                 # OLED black, charcoal, crimson & green palette
        ├── Type.kt                  # Bundled Playfair Display, Inter & Oswald typography
        └── Theme.kt                 # Material 3 NoirDialTheme
```

---

## Permissions & Default Dialer Role

To operate as a full phone and dialer replacement, `AndroidManifest.xml` declares:

- **Calling & Telecom:** `CALL_PHONE`, `READ_PHONE_STATE`, `READ_PHONE_NUMBERS`, `MANAGE_OWN_CALLS`, `MODIFY_AUDIO_SETTINGS`, `WAKE_LOCK`, `VIBRATE`
- **Contacts & Call History:** `READ_CONTACTS`, `WRITE_CONTACTS`, `READ_CALL_LOG`, `WRITE_CALL_LOG`
- **Notifications & Full-Screen Calls:** `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`

Poster images use the Android system **Photo Picker** (`PickVisualMedia`), so no broad `READ_MEDIA_IMAGES` or `READ_EXTERNAL_STORAGE` permissions are required.

> **Note:** On Android 10+ (`API 29+`), Android only binds `InCallService` to display custom incoming and active call screens when **Noir Dial** is selected as the system **Default Phone app** (`RoleManager.ROLE_DIALER`).

---

## Build & Testing

### Build Debug APK
```bash
gradle assembleDebug
```

### Run Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```
Tests are located in `app/src/test/java/com/example/` (`ExampleRobolectricTest.kt` and `ExampleUnitTest.kt`).
