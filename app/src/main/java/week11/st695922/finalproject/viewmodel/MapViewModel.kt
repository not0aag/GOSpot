package week11.st695922.finalproject.viewmodel

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import week11.st695922.finalproject.data.StationRepository
import week11.st695922.finalproject.model.Station
import week11.st695922.finalproject.ui.navigation.StationNavigator
import week11.st695922.finalproject.ui.state.UiState

class MapViewModel @JvmOverloads constructor(
    private val repository: StationRepository = StationRepository()
) : ViewModel() {

    val stationsState: StateFlow<UiState<List<Station>>> = repository.stationsFlow()
        .map<List<Station>, UiState<List<Station>>> { UiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _selectedStation = MutableStateFlow<Station?>(null)
    val selectedStation: StateFlow<Station?> = _selectedStation.asStateFlow()

    // Default camera position: Oakville GO
    val cameraPositionState = CameraPositionState(
        position = CameraPosition.fromLatLngZoom(LatLng(43.4409, -79.6673), 11f)
    )

    fun selectStation(station: Station?) {
        _selectedStation.value = station
    }

    fun getNearestAvailableStation(userLocation: Location?): Station? {
        val stations = (stationsState.value as? UiState.Success)?.data ?: return null
        val availableStations = stations.filter { it.spacesFree > 0 }
        if (availableStations.isEmpty()) return null
        if (userLocation == null) return availableStations.firstOrNull()

        return availableStations.minByOrNull { station ->
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                station.lat, station.lng,
                results
            )
            results[0]
        }
    }

    fun navigateToStation(context: Context, station: Station) {
        StationNavigator.navigate(context, station)
    }
}
