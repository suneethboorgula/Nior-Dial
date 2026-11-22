package com.example

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.PosterConfigRepository
import com.example.ui.MainViewModel
import com.example.ui.screens.ContactPosterProfileScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.KeypadScreen
import com.example.ui.screens.PosterCallPreviewScreen
import com.example.ui.screens.RecentsScreen
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.ElevatedCardBorder
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.NoirDialTheme
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.launch

object DialerRoutes {
  const val ROUTE_KEYPAD = "keypad"
  const val ROUTE_POSTER_PROFILE = "poster_profile?lookup={lookup}&name={name}&number={number}"
  const val ROUTE_POSTER_CALL_PREVIEW = "poster_call_preview?lookup={lookup}&name={name}&number={number}"

  fun posterProfileRoute(lookupKey: String?, name: String, number: String): String {
    return "poster_profile?lookup=${Uri.encode(lookupKey.orEmpty())}&name=${Uri.encode(name)}&number=${Uri.encode(number)}"
  }

  fun posterCallPreviewRoute(lookupKey: String?, name: String, number: String): String {
    return "poster_call_preview?lookup=${Uri.encode(lookupKey.orEmpty())}&name=${Uri.encode(name)}&number=${Uri.encode(number)}"
  }
}

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels()
  private val dialIntentCounter = kotlinx.coroutines.flow.MutableStateFlow(0)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
    )

    handleIncomingDialIntent(intent)

    setContent {
      NoirDialTheme {
        NoirDialAppContent(
          viewModel = viewModel,
          dialIntentCounter = dialIntentCounter,
          onRequestRoleDialer = { launchRoleDialerRequest(it) },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIncomingDialIntent(intent)
  }

  private fun handleIncomingDialIntent(intent: Intent?) {
    if (intent == null) return
    val action = intent.action
    if (action == Intent.ACTION_DIAL || action == Intent.ACTION_VIEW) {
      val data = intent.data
      if (data?.scheme == "tel") {
        val number = data.schemeSpecificPart.orEmpty()
        if (number.isNotBlank()) {
          viewModel.setDialedNumber(number)
        }
      } else if (action == Intent.ACTION_DIAL) {
        viewModel.clearDialedNumber()
      }
      dialIntentCounter.value += 1
    }
  }

  private fun launchRoleDialerRequest(launcher: (Intent) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = getSystemService(RoleManager::class.java)
      if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
        val roleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        launcher(roleIntent)
        return
      }
    }
    @Suppress("DEPRECATION")
    val legacyIntent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
      putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
    }
    launcher(legacyIntent)
  }
}

@Composable
private fun NoirDialAppContent(
  viewModel: MainViewModel,
  dialIntentCounter: kotlinx.coroutines.flow.StateFlow<Int>,
  onRequestRoleDialer: ((Intent) -> Unit) -> Unit,
) {
  val navController = rememberNavController()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val dialEvent by dialIntentCounter.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val lifecycleOwner = LocalLifecycleOwner.current

  LaunchedEffect(dialEvent) {
    if (dialEvent > 0) {
      navController.popBackStack(DialerRoutes.ROUTE_KEYPAD, inclusive = false)
    }
  }

  val permissionsToRequest = remember {
    buildList {
      add(Manifest.permission.CALL_PHONE)
      add(Manifest.permission.READ_PHONE_STATE)
      add(Manifest.permission.READ_CONTACTS)
      add(Manifest.permission.WRITE_CONTACTS)
      add(Manifest.permission.READ_CALL_LOG)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
      }
    }.toTypedArray()
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
    onResult = { viewModel.refreshData() },
  )

  val roleLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
    onResult = { viewModel.refreshData() },
  )

  var hasPromptedRole by rememberSaveable { mutableStateOf(false) }
  var pendingDialWithoutRole by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(uiState.isDefaultDialer) {
    if (!uiState.isDefaultDialer && !hasPromptedRole) {
      hasPromptedRole = true
      onRequestRoleDialer { intent ->
        runCatching { roleLauncher.launch(intent) }
      }
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshData()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(uiState.snackbarMessage) {
    val msg = uiState.snackbarMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeSnackbar()
    }
  }

  val context = androidx.compose.ui.platform.LocalContext.current
  val callStateHolder = remember { (context.applicationContext as NoirDialApplication).callStateHolder }
  val activeCallState by callStateHolder.uiState.collectAsStateWithLifecycle()

  fun initiateCallOrPromptRole(number: String) {
    if (!uiState.permissions.hasCallPhone) {
      permissionLauncher.launch(permissionsToRequest)
      return
    }
    if (!uiState.isDefaultDialer) {
      pendingDialWithoutRole = number
      return
    }
    viewModel.placeCall(context, number)
  }

  Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
      navController = navController,
      startDestination = DialerRoutes.ROUTE_KEYPAD,
      modifier = Modifier.fillMaxSize(),
    ) {
      composable(DialerRoutes.ROUTE_KEYPAD) {
        val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
        val coroutineScope = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current

        LaunchedEffect(dialEvent) {
          if (dialEvent > 0 && pagerState.currentPage != 1) {
            pagerState.animateScrollToPage(1)
          }
        }

        LaunchedEffect(pagerState.currentPage) {
          if (pagerState.currentPage != 2) {
            focusManager.clearFocus()
          }
        }

        BackHandler(enabled = pagerState.currentPage != 1) {
          coroutineScope.launch {
            pagerState.animateScrollToPage(1)
          }
        }

        Column(
          modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .statusBarsPadding(),
        ) {
          // Swipe & tap page indicator: Recents (0) <-> Dial (1) <-> Contacts (2)
          val pageLabels = listOf("Recents", "Dial", "Contacts")
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 6.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(CharcoalSurface)
                .border(1.dp, ElevatedCardBorder, RoundedCornerShape(20.dp))
                .padding(3.dp),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              pageLabels.forEachIndexed { pageIndex, label ->
                val selected = pagerState.currentPage == pageIndex
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) PureWhite else CharcoalSurface)
                    .clickable {
                      coroutineScope.launch {
                        pagerState.animateScrollToPage(pageIndex)
                      }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("pager_tab_${label.lowercase()}"),
                ) {
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) OledBlack else MutedSlate,
                  )
                }
              }
            }
          }

          HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
          ) { page ->
            when (page) {
              0 -> RecentsScreen(
                uiState = uiState,
                onCallClick = { number -> initiateCallOrPromptRole(number) },
                onViewPoster = { entry ->
                  val matched = viewModel.resolveContactByLookupOrNumber(null, entry.number)
                  navController.navigate(
                    DialerRoutes.posterProfileRoute(
                      lookupKey = matched?.lookupKey,
                      name = matched?.displayName ?: entry.cachedName ?: entry.number,
                      number = entry.number,
                    ),
                  )
                },
                onNavigateKeypad = {
                  coroutineScope.launch { pagerState.animateScrollToPage(1) }
                },
                onBack = {
                  coroutineScope.launch { pagerState.animateScrollToPage(1) }
                },
                onRequestCallLogPermission = {
                  permissionLauncher.launch(permissionsToRequest)
                },
                applyStatusBarsPadding = false,
              )

              1 -> KeypadScreen(
                uiState = uiState,
                onDigitPress = viewModel::appendDigit,
                onBackspace = viewModel::backspaceDigit,
                onClearAll = viewModel::clearDialedNumber,
                onCallClick = { number -> initiateCallOrPromptRole(number) },
                onOpenPoster = { match ->
                  navController.navigate(
                    DialerRoutes.posterProfileRoute(
                      lookupKey = match.contact?.lookupKey,
                      name = match.displayName,
                      number = match.phoneNumber,
                    ),
                  )
                },
                onOpenCallPreview = {
                  navController.navigate(
                    DialerRoutes.posterCallPreviewRoute(
                      lookupKey = PosterConfigRepository.DEMO_POSTER_KEY,
                      name = "Elena Vance",
                      number = "+1 415 890 4321",
                    ),
                  )
                },
                onNavigateRecents = {
                  coroutineScope.launch { pagerState.animateScrollToPage(0) }
                },
                onNavigateContacts = {
                  coroutineScope.launch { pagerState.animateScrollToPage(2) }
                },
                onRequestDefaultDialer = {
                  onRequestRoleDialer { intent ->
                    runCatching { roleLauncher.launch(intent) }.onFailure {
                      viewModel.showMessage("Open system Settings > Default Apps > Phone to select Noir Dial")
                    }
                  }
                },
                onRequestPermissions = {
                  permissionLauncher.launch(permissionsToRequest)
                },
                applyStatusBarsPadding = false,
              )

              else -> ContactsScreen(
                uiState = uiState,
                onSearchChange = viewModel::setContactsSearchQuery,
                onContactClick = { contact ->
                  navController.navigate(
                    DialerRoutes.posterProfileRoute(
                      lookupKey = contact.lookupKey,
                      name = contact.displayName,
                      number = contact.phoneNumbers.firstOrNull().orEmpty(),
                    ),
                  )
                },
                onCreateContact = viewModel::createNewContact,
                onNavigateKeypad = {
                  coroutineScope.launch { pagerState.animateScrollToPage(1) }
                },
                onBack = {
                  coroutineScope.launch { pagerState.animateScrollToPage(1) }
                },
                onRequestContactsPermission = {
                  permissionLauncher.launch(permissionsToRequest)
                },
                applyStatusBarsPadding = false,
              )
            }
          }
        }
      }

      composable(
        route = DialerRoutes.ROUTE_POSTER_PROFILE,
        arguments = listOf(
          navArgument("lookup") { type = NavType.StringType; defaultValue = "" },
          navArgument("name") { type = NavType.StringType; defaultValue = "" },
          navArgument("number") { type = NavType.StringType; defaultValue = "" },
        ),
      ) { backStackEntry ->
        val lookupArg = backStackEntry.arguments?.getString("lookup").orEmpty()
        val nameArg = backStackEntry.arguments?.getString("name").orEmpty()
        val numberArg = backStackEntry.arguments?.getString("number").orEmpty()

        val lookupOverride = lookupArg.takeIf { it.isNotBlank() }
        val contact = remember(uiState.contacts, lookupOverride, numberArg) {
          viewModel.resolveContactByLookupOrNumber(
            lookupKey = lookupOverride,
            number = numberArg.takeIf { it.isNotBlank() },
          )
        }
        val config = remember(uiState.posterConfigs, contact, numberArg, lookupOverride) {
          viewModel.resolvePosterConfig(contact, numberArg, lookupOverride)
        }
        val matchingCallLogs = remember(uiState.callLog, contact, numberArg, config.customCallerNumber) {
          val keys = buildSet {
            contact?.phoneNumbers?.forEach {
              val k = com.example.data.PhoneNumberFormatter.matchKey(it)
              if (k.isNotBlank()) add(k)
            }
            if (numberArg.isNotBlank()) {
              val k = com.example.data.PhoneNumberFormatter.matchKey(numberArg)
              if (k.isNotBlank()) add(k)
            }
            config.customCallerNumber?.takeIf { it.isNotBlank() }?.let {
              val k = com.example.data.PhoneNumberFormatter.matchKey(it)
              if (k.isNotBlank()) add(k)
            }
          }
          if (keys.isEmpty()) {
            emptyList()
          } else {
            uiState.callLog.filter { entry ->
              com.example.data.PhoneNumberFormatter.matchKey(entry.number) in keys
            }.take(10)
          }
        }

        ContactPosterProfileScreen(
          contact = contact,
          fallbackName = nameArg,
          fallbackNumber = numberArg,
          posterConfig = config,
          callLogs = matchingCallLogs,
          onBack = { navController.popBackStack() },
          onCall = { number -> initiateCallOrPromptRole(number) },
          onPhotoPicked = { uri ->
            viewModel.updatePosterPhoto(contact, numberArg, uri, lookupOverride)
          },
          onUseSamplePhoto = {
            viewModel.applySamplePoster(contact, numberArg, lookupOverride)
          },
          onPreviewCall = {
            navController.navigate(
              DialerRoutes.posterCallPreviewRoute(
                lookupKey = contact?.lookupKey ?: lookupOverride,
                name = config.customCallerName?.takeIf { it.isNotBlank() }
                  ?: contact?.displayName
                  ?: nameArg,
                number = config.customCallerNumber?.takeIf { it.isNotBlank() }
                  ?: contact?.phoneNumbers?.firstOrNull()
                  ?: numberArg,
              ),
            )
          },
          onEditContactInfo = { newName, newNumber ->
            viewModel.updateCallerInfo(
              contact = contact,
              fallbackNumber = numberArg.ifBlank { contact?.phoneNumbers?.firstOrNull().orEmpty() },
              lookupOverride = lookupOverride,
              newName = newName,
              newNumber = newNumber,
            )
          },
          onUpdateTypography = { font, colorHex, alignment, notes ->
            viewModel.updatePosterTypography(
              contact = contact,
              fallbackNumber = numberArg,
              fontStyle = font,
              textColorHex = colorHex,
              alignment = alignment,
              notes = notes,
              lookupOverride = lookupOverride,
            )
          },
          onResetPoster = {
            viewModel.resetPoster(contact, numberArg, lookupOverride)
          },
          onLaunchSystemIntent = { intent, fallbackMsg ->
            viewModel.launchSystemAction(context, intent, fallbackMsg)
          },
        )
      }

      composable(
        route = DialerRoutes.ROUTE_POSTER_CALL_PREVIEW,
        arguments = listOf(
          navArgument("lookup") { type = NavType.StringType; defaultValue = "" },
          navArgument("name") { type = NavType.StringType; defaultValue = "" },
          navArgument("number") { type = NavType.StringType; defaultValue = "" },
        ),
      ) { backStackEntry ->
        val lookupArg = backStackEntry.arguments?.getString("lookup").orEmpty()
        val nameArg = backStackEntry.arguments?.getString("name").orEmpty()
        val numberArg = backStackEntry.arguments?.getString("number").orEmpty()
        val lookupOverride = lookupArg.takeIf { it.isNotBlank() }
        val contact = remember(uiState.contacts, lookupOverride, numberArg) {
          viewModel.resolveContactByLookupOrNumber(
            lookupKey = lookupOverride,
            number = numberArg.takeIf { it.isNotBlank() },
          )
        }
        val config = remember(uiState.posterConfigs, contact, numberArg, lookupOverride) {
          viewModel.resolvePosterConfig(contact, numberArg, lookupOverride)
        }
        val displayName = config.customCallerName?.takeIf { it.isNotBlank() }
          ?: contact?.displayName
          ?: nameArg.ifBlank { numberArg.ifBlank { "Elena Vance" } }
        val phoneNumber = config.customCallerNumber?.takeIf { it.isNotBlank() }
          ?: contact?.phoneNumbers?.firstOrNull()
          ?: numberArg.ifBlank { "+1 415 890 4321" }

        PosterCallPreviewScreen(
          displayName = displayName,
          phoneNumber = phoneNumber,
          posterConfig = config,
          fallbackPhotoUri = contact?.photoUri,
          onClose = { navController.popBackStack() },
        )
      }
    }

    if (activeCallState.hasCalls) {
      Row(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .statusBarsPadding()
          .padding(top = 8.dp)
          .clip(RoundedCornerShape(24.dp))
          .background(CallGreen)
          .clickable { callStateHolder.launchInCallUiIfActive() }
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Rounded.Call,
          contentDescription = null,
          tint = OledBlack,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Tap to return to call • ${activeCallState.primaryCall?.callerName.orEmpty()}",
          style = MaterialTheme.typography.labelLarge,
          color = OledBlack,
        )
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = 84.dp, start = 16.dp, end = 16.dp),
    ) { data ->
      Snackbar(
        snackbarData = data,
        containerColor = CharcoalSurface,
        contentColor = PureWhite,
      )
    }

    val numberPendingRole = pendingDialWithoutRole
    if (numberPendingRole != null) {
      AlertDialog(
        onDismissRequest = { pendingDialWithoutRole = null },
        containerColor = CharcoalSurface,
        titleContentColor = PureWhite,
        textContentColor = PureWhite,
        title = { Text("Set Noir Dial as Default Phone") },
        text = {
          Text("Android only allows the Default Phone app to display custom call screens and Contact Posters. Set Noir Dial as default now?")
        },
        confirmButton = {
          Button(
            onClick = {
              pendingDialWithoutRole = null
              onRequestRoleDialer { intent ->
                runCatching { roleLauncher.launch(intent) }.onFailure {
                  viewModel.showMessage("Open Settings > Default Apps > Phone to select Noir Dial")
                }
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = OledBlack),
          ) {
            Text("Set Default")
          }
        },
        dismissButton = {
          TextButton(
            onClick = {
              val num = numberPendingRole
              pendingDialWithoutRole = null
              viewModel.placeCall(context, num)
            },
          ) {
            Text("Call Anyway", color = PureWhite)
          }
        },
      )
    }
  }
}
