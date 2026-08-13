package week11.st695922.finalproject.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private var foregroundUpdatesJob: Job? = null

    fun refreshLocation() {
        viewModelScope.launch {
            repository.getCurrentLocation()?.let { _lastLocation.value = it }
        }
    }

    fun startForegroundUpdates() {
        if (foregroundUpdatesJob?.isActive == true) return
        foregroundUpdatesJob = viewModelScope.launch {
            while (true) {
                repository.getCurrentLocation()?.let { _lastLocation.value = it }
                delay(FOREGROUND_REFRESH_MILLIS)
            }
        }
    }

    fun stopForegroundUpdates() {
        foregroundUpdatesJob?.cancel()
        foregroundUpdatesJob = null
    }

    private companion object {
        const val FOREGROUND_REFRESH_MILLIS = 5_000L
    }
}
