package week11.st695922.finalproject.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import week11.st695922.finalproject.data.GeofenceRepository
import week11.st695922.finalproject.model.Station

class GeofenceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GeofenceRepository(application)

    private val _registeredStationIds = MutableStateFlow<Set<String>>(emptySet())
    val registeredStationIds: StateFlow<Set<String>> = _registeredStationIds.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun registerGeofences(stations: List<Station>) {
        if (stations.isEmpty()) return
        viewModelScope.launch {
            repository.registerGeofences(stations)
                .onSuccess { _registeredStationIds.value = stations.map { it.id }.toSet() }
                .onFailure { e -> _error.value = e.message ?: "Could not register geofences" }
        }
    }

    fun removeGeofences() {
        viewModelScope.launch {
            repository.removeGeofences()
                .onSuccess { _registeredStationIds.value = emptySet() }
                .onFailure { e -> _error.value = e.message ?: "Could not remove geofences" }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
