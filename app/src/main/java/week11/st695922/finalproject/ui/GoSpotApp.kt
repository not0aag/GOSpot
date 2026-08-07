package week11.st695922.finalproject.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import week11.st695922.finalproject.model.CheckInEventType
import week11.st695922.finalproject.model.UserProfile
import week11.st695922.finalproject.ui.components.ErrorBanner
import week11.st695922.finalproject.ui.components.FullScreenLoading
import week11.st695922.finalproject.ui.components.GoSpotBottomNavBar
import week11.st695922.finalproject.ui.navigation.Route
import week11.st695922.finalproject.ui.screens.AlertsScreen
import week11.st695922.finalproject.ui.screens.CheckInConfirmationScreen
import week11.st695922.finalproject.ui.screens.CreateAccountScreen
import week11.st695922.finalproject.ui.screens.LocationPermissionScreen
import week11.st695922.finalproject.ui.screens.MapScreen
import week11.st695922.finalproject.ui.screens.PickHomeStationScreen
import week11.st695922.finalproject.ui.screens.ProfileScreen
import week11.st695922.finalproject.ui.screens.SignInScreen
import week11.st695922.finalproject.ui.screens.SplashScreen
import week11.st695922.finalproject.ui.screens.StationDetailScreen
import week11.st695922.finalproject.ui.screens.StationsListScreen
import week11.st695922.finalproject.ui.state.AuthUiState
import week11.st695922.finalproject.ui.state.UiState
import week11.st695922.finalproject.viewmodel.AlertSettingsViewModel
import week11.st695922.finalproject.viewmodel.AlertViewModel
import week11.st695922.finalproject.viewmodel.AuthViewModel
import week11.st695922.finalproject.viewmodel.GeofenceViewModel
import week11.st695922.finalproject.viewmodel.LocationViewModel
import week11.st695922.finalproject.viewmodel.MapViewModel
import week11.st695922.finalproject.viewmodel.ProfileViewModel
import week11.st695922.finalproject.viewmodel.StationViewModel

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED


private fun hasBackgroundLocationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }


@Composable
fun GoSpotApp() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    when (val state = authState) {
        AuthUiState.Loading -> SplashScreen()
        AuthUiState.AuthRequired -> AuthFlow(authViewModel)
        is AuthUiState.Authenticated -> MainAppFlow(uid = state.uid, authViewModel = authViewModel)
    }
}

@Composable
private fun AuthFlow(authViewModel: AuthViewModel) {
    var showCreateAccount by remember { mutableStateOf(false) }
    val formError by authViewModel.formError.collectAsState()
    val isSubmitting by authViewModel.isSubmitting.collectAsState()
    val passwordResetSent by authViewModel.passwordResetSent.collectAsState()

    if (showCreateAccount) {
        CreateAccountScreen(
            formError = formError,
            isSubmitting = isSubmitting,
            onCreateAccount = { fullName, email, password -> authViewModel.signUp(fullName, email, password) },
            onNavigateBack = {
                authViewModel.clearFormError()
                showCreateAccount = false
            }
        )
    } else {
        SignInScreen(
            formError = formError,
            isSubmitting = isSubmitting,
            passwordResetSent = passwordResetSent,
            onSignIn = { email, password -> authViewModel.signIn(email, password) },
            onForgotPassword = { email -> authViewModel.sendPasswordReset(email) },
            onNavigateToCreateAccount = {
                authViewModel.clearFormError()
                authViewModel.clearPasswordResetSent()
                showCreateAccount = true
            }
        )
    }
}

/** Gates onboarding (location permission, then pick-home-station) before the main tabs. */
@Composable
private fun MainAppFlow(uid: String, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    var locationPermissionResolved by remember { mutableStateOf(hasLocationPermission(context)) }

    if (!locationPermissionResolved) {
        LocationPermissionScreen(onPermissionResolved = { locationPermissionResolved = true })
        return
    }

    val stationViewModel: StationViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel(
        factory = viewModelFactory { initializer { ProfileViewModel(uid) } }
    )
    val alertViewModel: AlertViewModel = viewModel(
        factory = viewModelFactory { initializer { AlertViewModel(uid) } }
    )
    val mapViewModel: MapViewModel = viewModel()
    val locationViewModel: LocationViewModel = viewModel()
    LaunchedEffect(Unit) { locationViewModel.refreshLocation() }
    val userLocation by locationViewModel.lastLocation.collectAsState()

    val stationsState by stationViewModel.stationsState.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()

    val geofenceViewModel: GeofenceViewModel = viewModel()
    val stationsForGeofencing = (stationsState as? UiState.Success)?.data ?: emptyList()
    LaunchedEffect(stationsForGeofencing.map { it.id }) {
        if (stationsForGeofencing.isNotEmpty() &&
            hasLocationPermission(context) &&
            hasBackgroundLocationPermission(context)
        ) {
            geofenceViewModel.registerGeofences(stationsForGeofencing)
        }
    }

    when (val profile = profileState) {
        is UiState.Loading -> FullScreenLoading()
        is UiState.Error -> ErrorBanner(profile.message, modifier = Modifier.padding(24.dp))
        is UiState.Success -> {
            if (profile.data.homeStationId.isBlank()) {
                val stations = (stationsState as? UiState.Success)?.data ?: emptyList()
                PickHomeStationScreen(
                    stations = stations,
                    onContinue = { station -> profileViewModel.changeHomeStation(station) },
                    onSeedDemoStations = { stationViewModel.seedDemoStations() }
                )
            } else {
                SignedInApp(
                    uid = uid,
                    stationViewModel = stationViewModel,
                    profileViewModel = profileViewModel,
                    alertViewModel = alertViewModel,
                    mapViewModel = mapViewModel,
                    profile = profile.data,
                    userLocation = userLocation,
                    onSignOut = { authViewModel.signOut() }
                )
            }
        }
    }
}

private sealed interface OverlayRoute {
    data class StationDetail(val stationId: String) : OverlayRoute
    data class CheckInConfirmation(val stationId: String, val isCheckIn: Boolean) : OverlayRoute
    data object ChangeHomeStation : OverlayRoute
}

@Composable
private fun SignedInApp(
    uid: String,
    stationViewModel: StationViewModel,
    profileViewModel: ProfileViewModel,
    alertViewModel: AlertViewModel,
    mapViewModel: MapViewModel,
    profile: UserProfile,
    userLocation: Location?,
    onSignOut: () -> Unit
) {
    var currentTab by remember { mutableStateOf<Route.MainTab>(Route.MainTab.Map) }
    var overlay by remember { mutableStateOf<OverlayRoute?>(null) }

    val application = LocalContext.current.applicationContext as Application
    val alertSettingsViewModel: AlertSettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { AlertSettingsViewModel(uid, application) } }
    )
    val alertsEnabled by alertSettingsViewModel.alertsEnabled.collectAsState()
    LaunchedEffect(uid) { alertSettingsViewModel.syncFcmToken() }

    val stationsState by stationViewModel.stationsState.collectAsState()
    val checkedInIds by stationViewModel.checkedInStationIds.collectAsState()
    val pendingStationId by stationViewModel.pendingStationId.collectAsState()
    val eventsState by alertViewModel.eventsState.collectAsState()
    val stations = (stationsState as? UiState.Success)?.data ?: emptyList()

    val stationActionError by stationViewModel.actionError.collectAsState()
    val alertSettingsError by alertSettingsViewModel.actionError.collectAsState()

    // The confirmation screen is only for check-ins started from the detail
    // screen; toggling straight from the Stations list stays inline.
    var awaitingConfirmationFor by remember { mutableStateOf<String?>(null) }
    val completedAction by stationViewModel.completedAction.collectAsState()
    LaunchedEffect(completedAction) {
        val completed = completedAction ?: return@LaunchedEffect
        if (completed.stationId == awaitingConfirmationFor) {
            overlay = OverlayRoute.CheckInConfirmation(completed.stationId, completed.isCheckIn)
            awaitingConfirmationFor = null
        }
        stationViewModel.consumeCompletedAction()
    }

    when (val activeOverlay = overlay) {
        is OverlayRoute.StationDetail -> {
            val station = stations.find { it.id == activeOverlay.stationId }
            if (station == null) {
                overlay = null
            } else {
                StationDetailScreen(
                    station = station,
                    isCheckedIn = station.id in checkedInIds,
                    isPending = station.id == pendingStationId,
                    errorMessage = stationActionError,
                    onDismissError = { stationViewModel.clearActionError() },
                    onToggleCheckIn = {
                        // The overlay switches only once the write commits, so a
                        // failed check-in shows the error banner instead of a
                        // green "Checked in" screen it never earned.
                        awaitingConfirmationFor = station.id
                        stationViewModel.toggleCheckIn(station)
                    },
                    onNavigateBack = {
                        awaitingConfirmationFor = null
                        overlay = null
                    }
                )
                return
            }
        }
        is OverlayRoute.CheckInConfirmation -> {
            val station = stations.find { it.id == activeOverlay.stationId }
            if (station == null) {
                overlay = null
            } else {
                CheckInConfirmationScreen(
                    station = station,
                    isCheckIn = activeOverlay.isCheckIn,
                    onDone = { overlay = null }
                )
                return
            }
        }
        OverlayRoute.ChangeHomeStation -> {
            PickHomeStationScreen(
                stations = stations,
                onContinue = { station ->
                    profileViewModel.changeHomeStation(station)
                    overlay = null
                },
                onSeedDemoStations = { stationViewModel.seedDemoStations() }
            )
            return
        }
        null -> Unit
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        bottomBar = {
            GoSpotBottomNavBar(currentTab = currentTab, onTabSelected = { currentTab = it })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Every ViewModel here computes failures; without this they were all
            // written to StateFlows nothing ever read, so writes failed silently.
            (stationActionError ?: alertSettingsError)?.let { message ->
                ErrorBanner(
                    message = message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    onDismiss = {
                        stationViewModel.clearActionError()
                        alertSettingsViewModel.clearActionError()
                    }
                )
            }

            when (currentTab) {
                Route.MainTab.Map -> MapScreen(
                    mapViewModel = mapViewModel,
                    homeStation = stations.find { it.id == profile.homeStationId },
                    userLocation = userLocation
                )
                Route.MainTab.Stations -> StationsListScreen(
                    stations = stations,
                    checkedInStationIds = checkedInIds,
                    homeStationId = profile.homeStationId,
                    pendingStationId = pendingStationId,
                    userLocation = userLocation,
                    onStationClick = { overlay = OverlayRoute.StationDetail(it.id) },
                    onToggleCheckIn = { stationViewModel.toggleCheckIn(it) }
                )
                Route.MainTab.Alerts -> {
                    val events = (eventsState as? UiState.Success)?.data ?: emptyList()
                    AlertsScreen(events = events, onClearAll = { alertViewModel.clearAll() })
                }
                Route.MainTab.Profile -> ProfileScreen(
                    profile = profile,
                    checkInsCount = (eventsState as? UiState.Success)?.data
                        ?.count { it.type == CheckInEventType.CHECK_IN.name }
                        ?: 0,
                    alertsEnabled = alertsEnabled,
                    onAlertsEnabledChange = { alertSettingsViewModel.setAlertsEnabled(it) },
                    onChangeHomeStation = { overlay = OverlayRoute.ChangeHomeStation },
                    onSignOut = onSignOut
                )
            }
        }
    }
}
