package week11.st695922.finalproject.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import week11.st695922.finalproject.data.GeofenceRepository
import week11.st695922.finalproject.data.StationRepository
import week11.st695922.finalproject.data.UserProfileRepository
import week11.st695922.finalproject.model.Station
import week11.st695922.finalproject.model.StationProximity

class GeofenceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GeofenceRepository(application)
    private val stationRepository = StationRepository()
    private val profileRepository = UserProfileRepository()
    private val geofenceOperationMutex = Mutex()

    private val _registeredStationIds = MutableStateFlow<Set<String>>(emptySet())
    val registeredStationIds: StateFlow<Set<String>> = _registeredStationIds.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _settingUpdateInProgress = MutableStateFlow(false)
    val settingUpdateInProgress: StateFlow<Boolean> = _settingUpdateInProgress.asStateFlow()

    private var registeredSignature: String? = null
    private var foregroundEvaluationInProgress = false

    fun registerGeofences(stations: List<Station>) {
        if (stations.isEmpty()) return
        val signature = stations
            .sortedBy { it.id }
            .joinToString("|") { "${it.id}:${it.lat}:${it.lng}" }
        if (signature == registeredSignature) return
        viewModelScope.launch {
            geofenceOperationMutex.withLock { repository.registerGeofences(stations) }
                .onSuccess {
                    registeredSignature = signature
                    _registeredStationIds.value = stations.map { it.id }.toSet()
                    _error.value = null
                }
                .onFailure { e -> _error.value = e.message ?: "Could not register geofences" }
        }
    }

    fun removeGeofences() {
        viewModelScope.launch {
            geofenceOperationMutex.withLock { repository.removeGeofences() }
                .onSuccess {
                    registeredSignature = null
                    _registeredStationIds.value = emptySet()
                    _error.value = null
                }
                .onFailure { e -> _error.value = e.message ?: "Could not remove geofences" }
        }
    }

    fun setAutomaticCheckInEnabled(uid: String, enabled: Boolean) {
        if (_settingUpdateInProgress.value) return
        viewModelScope.launch {
            _settingUpdateInProgress.value = true
            val result = if (enabled) {
                profileRepository.setAutomaticCheckInEnabled(uid, true)
            } else {
                stationRepository.disableAutomaticCheckIn()
            }
            result.onFailure { e ->
                _error.value = e.message ?: "Could not update automatic check-in"
            }
            result.onSuccess { _error.value = null }
            if (!enabled && result.isSuccess) {
                geofenceOperationMutex.withLock { repository.removeGeofences() }
                    .onSuccess {
                        registeredSignature = null
                        _registeredStationIds.value = emptySet()
                    }
                    .onFailure { e -> _error.value = e.message ?: "Could not remove geofences" }
            }
            _settingUpdateInProgress.value = false
        }
    }

    fun evaluateForegroundProximity(
        location: Location,
        stations: List<Station>,
        activeStationId: String,
        automaticCheckInEnabled: Boolean
    ) {
        if (!automaticCheckInEnabled || foregroundEvaluationInProgress) return

        val nearbyStation = StationProximity.nearestWithin(
            latitude = location.latitude,
            longitude = location.longitude,
            stations = stations,
            radiusMeters = FOREGROUND_RADIUS_METERS
        )
        if (nearbyStation?.id == activeStationId) return
        if (nearbyStation == null && activeStationId.isBlank()) return

        foregroundEvaluationInProgress = true
        viewModelScope.launch {
            try {
                val result = if (nearbyStation != null) {
                    stationRepository.checkInByStationId(nearbyStation.id)
                } else {
                    stationRepository.checkOutByStationId(activeStationId)
                }
                result.onFailure { e ->
                    _error.value = e.message ?: "Could not update automatic check-in"
                }
            } finally {
                foregroundEvaluationInProgress = false
            }
        }
    }

    fun prepareForSignOut(onFinished: () -> Unit) {
        if (_settingUpdateInProgress.value) return
        viewModelScope.launch {
            _settingUpdateInProgress.value = true
            val checkoutResult = stationRepository.checkOutActiveUser()
            if (checkoutResult.isFailure) {
                _error.value = checkoutResult.exceptionOrNull()?.message
                    ?: "Could not check out before signing out"
                _settingUpdateInProgress.value = false
                return@launch
            }
            val removalResult = geofenceOperationMutex.withLock { repository.removeGeofences() }
            if (removalResult.isFailure) {
                _error.value = removalResult.exceptionOrNull()?.message
                    ?: "Could not remove automatic check-in"
                _settingUpdateInProgress.value = false
                return@launch
            }
            registeredSignature = null
            _registeredStationIds.value = emptySet()
            _settingUpdateInProgress.value = false
            onFinished()
        }
    }

    fun clearError() {
        _error.value = null
    }

    private companion object {
        const val FOREGROUND_RADIUS_METERS = 300.0
    }
}
