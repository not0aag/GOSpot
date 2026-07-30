package week11.st695922.finalproject.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
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
import week11.st695922.finalproject.viewmodel.AlertViewModel
import week11.st695922.finalproject.viewmodel.AuthViewModel
import week11.st695922.finalproject.viewmodel.LocationViewModel
import week11.st695922.finalproject.viewmodel.ProfileViewModel
import week11.st695922.finalproject.viewmodel.StationViewModel

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Root composable. Screen switching follows the state-driven pattern from
 * Week 6.2 Slide 11: this `when` block on AuthUiState decides whether to show
 * the auth flow or the signed-in app, generalized with the Route sealed class
 * (ui/navigation/Route.kt) for the screens inside the signed-in app.
 */
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
    val locationViewModel: LocationViewModel = viewModel()
    LaunchedEffect(Unit) { locationViewModel.refreshLocation() }
    val userLocation by locationViewModel.lastLocation.collectAsState()

    val stationsState by stationViewModel.stationsState.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()

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
                    stationViewModel = stationViewModel,
                    profileViewModel = profileViewModel,
                    alertViewModel = alertViewModel,
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
    stationViewModel: StationViewModel,
    profileViewModel: ProfileViewModel,
    alertViewModel: AlertViewModel,
    profile: UserProfile,
    userLocation: Location?,
    onSignOut: () -> Unit
) {
    var currentTab by remember { mutableStateOf<Route.MainTab>(Route.MainTab.Map) }
    var overlay by remember { mutableStateOf<OverlayRoute?>(null) }

    val stationsState by stationViewModel.stationsState.collectAsState()
    val checkedInIds by stationViewModel.checkedInStationIds.collectAsState()
    val pendingStationId by stationViewModel.pendingStationId.collectAsState()
    val eventsState by alertViewModel.eventsState.collectAsState()
    val stations = (stationsState as? UiState.Success)?.data ?: emptyList()

    when (val activeOverlay = overlay) {
        is OverlayRoute.StationDetail -> {
            val station = stations.find { it.id == activeOverlay.stationId }
            if (station == null) {
                overlay = null
            } else {
                StationDetailScreen(
                    station = station,
                    isCheckedIn = station.id in checkedInIds,
                    onToggleCheckIn = {
                        val willCheckIn = station.id !in checkedInIds
                        stationViewModel.toggleCheckIn(station)
                        overlay = OverlayRoute.CheckInConfirmation(station.id, willCheckIn)
                    },
                    onNavigateBack = { overlay = null }
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
            when (currentTab) {
                Route.MainTab.Map -> MapScreen(
                    stations = stations,
                    homeStation = stations.find { it.id == profile.homeStationId },
                    onStationClick = { overlay = OverlayRoute.StationDetail(it.id) }
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
                    onChangeHomeStation = { overlay = OverlayRoute.ChangeHomeStation },
                    onSignOut = onSignOut
                )
            }
        }
    }
}
