package week11.st695922.finalproject.viewmodel

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.util.Log
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

    /**
     * Hands off to turn-by-turn navigation, preferring Google Maps.
     *
     * Tries each intent in turn rather than pre-checking with `resolveActivity`:
     * under the API 30+ package-visibility rules that check returns null for any
     * app not covered by the manifest's `<queries>`, so it used to report "not
     * installed" for Google Maps and silently always take the fallback.
     */
    fun navigateToStation(context: Context, station: Station) {
        val turnByTurn = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=${station.lat},${station.lng}")
        ).setPackage("com.google.android.apps.maps")

        val anyMapApp = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:${station.lat},${station.lng}?q=${station.lat},${station.lng}(${station.name})")
        )

        for (intent in listOf(turnByTurn, anyMapApp)) {
            try {
                context.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                Log.d(TAG, "No handler for ${intent.data?.scheme}, trying the next option", e)
            }
        }
        Log.w(TAG, "No installed app can handle navigation to ${station.id}")
    }

    private companion object {
        const val TAG = "MapViewModel"
    }
}
