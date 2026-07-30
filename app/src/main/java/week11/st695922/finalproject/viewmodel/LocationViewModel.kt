package week11.st695922.finalproject.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import week11.st695922.finalproject.data.LocationRepository

/**
 * Needs Application context for FusedLocationProviderClient, hence AndroidViewModel
 * rather than a plain ViewModel (Week 8, Slides 14, 23, 26).
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocationRepository(application)

    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    fun refreshLocation() {
        viewModelScope.launch {
            _lastLocation.value = repository.getCurrentLocation()
        }
    }
}
