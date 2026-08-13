package week11.st695922.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import week11.st695922.finalproject.data.StationRepository
import week11.st695922.finalproject.data.StationSeeder
import week11.st695922.finalproject.model.Station
import week11.st695922.finalproject.ui.state.UiState

// See AuthViewModel for why @JvmOverloads is required here.
class StationViewModel @JvmOverloads constructor(
    private val repository: StationRepository = StationRepository(),
    private val seeder: StationSeeder = StationSeeder()
) : ViewModel() {

    /**
     * Supplies the initial Loading case; once the repository's
     * snapshot listener emits its first snapshot, this
     * flips to Success and stays there for every subsequent real-time update.
     */
    val stationsState: StateFlow<UiState<List<Station>>> = repository.stationsFlow()
        .map<List<Station>, UiState<List<Station>>> { UiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _pendingStationId = MutableStateFlow<String?>(null)
    val pendingStationId: StateFlow<String?> = _pendingStationId.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    /**
     * Tracks which stations *this device* has manually checked into, so the
     * Stations list can show "Check out" instead of "Check in". This is local
     * UI state only, not a Firestore field.
     */
    private val _checkedInStationIds = MutableStateFlow<Set<String>>(emptySet())
    val checkedInStationIds: StateFlow<Set<String>> = _checkedInStationIds.asStateFlow()

    /**
     * The last check-in/out that actually committed, published so the UI can
     * show its confirmation screen only once the Firestore write succeeded.
     * Consumed via [consumeCompletedAction] so it fires once per action.
     */
    private val _completedAction = MutableStateFlow<CompletedAction?>(null)
    val completedAction: StateFlow<CompletedAction?> = _completedAction.asStateFlow()

    init {
        synchronizeStationLocations()
    }

    data class CompletedAction(val stationId: String, val isCheckIn: Boolean)

    fun consumeCompletedAction() {
        _completedAction.value = null
    }

    fun toggleCheckIn(station: Station) {
        val isCheckedIn = station.id in _checkedInStationIds.value
        runStationAction(station, isCheckIn = !isCheckedIn) {
            if (isCheckedIn) repository.checkOut(station) else repository.checkIn(station)
        }
    }

    private fun runStationAction(station: Station, isCheckIn: Boolean, action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _pendingStationId.value = station.id
            val result = action()
            _pendingStationId.value = null
            result.onSuccess {
                _checkedInStationIds.value = if (isCheckIn) {
                    _checkedInStationIds.value + station.id
                } else {
                    _checkedInStationIds.value - station.id
                }
                _completedAction.value = CompletedAction(station.id, isCheckIn)
            }
            result.onFailure { e -> _actionError.value = e.message ?: "Action failed" }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    fun seedDemoStations() {
        synchronizeStationLocations()
    }

    private fun synchronizeStationLocations() {
        viewModelScope.launch {
            seeder.synchronizeStations()
                .onFailure { e -> _actionError.value = e.message ?: "Could not update station locations" }
        }
    }
}
