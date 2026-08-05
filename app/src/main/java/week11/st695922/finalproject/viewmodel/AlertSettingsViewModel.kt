package week11.st695922.finalproject.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import week11.st695922.finalproject.data.AlertRepository
import week11.st695922.finalproject.data.UserProfileRepository
import week11.st695922.finalproject.model.UserProfile

/**
 * Owns the "Lot filling up alerts" setting.
 *
 * Named to avoid a clash with [AlertViewModel], which drives the check-in event
 * list on the Alerts tab - a different feature that happens to share the word.
 *
 * Firestore is the single source of truth: the toggle writes `alertsEnabled`
 * and nothing else, then the profile snapshot listener drives topic
 * subscription through [reconcileSubscription]. Routing it this way means
 * changing home station re-points the subscription for free, with no separate
 * code path to keep in sync.
 *
 * An [AndroidViewModel] because the threshold watcher needs an application
 * Context to post notifications, following [GeofenceViewModel].
 */
class AlertSettingsViewModel(
    private val uid: String,
    application: Application,
    private val alertRepository: AlertRepository = AlertRepository(),
    private val profileRepository: UserProfileRepository = UserProfileRepository()
) : AndroidViewModel(application) {

    private val profile: StateFlow<UserProfile?> = profileRepository.profileFlow(uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val alertsEnabled: StateFlow<Boolean> = profile
        .map { it?.alertsEnabled == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    /**
     * Station id whose topic this device is currently subscribed to, or null
     * when unsubscribed. Tracked so a home-station change can unsubscribe the
     * lot the user just left.
     */
    private var subscribedStationId: String? = null

    init {
        viewModelScope.launch {
            profile.filterNotNull().collect { reconcileSubscription(it) }
        }
    }

    /**
     * Persists the opt-in. Subscription follows from the resulting profile
     * emission rather than happening here, so the stored flag and the topic
     * can never disagree about intent.
     */
    fun setAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            profileRepository.setAlertsEnabled(uid, enabled)
                .onFailure { e -> _actionError.value = e.message ?: "Could not update alert settings" }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    /** Brings the device's topic subscription in line with the stored profile. */
    private suspend fun reconcileSubscription(profile: UserProfile) {
        val desired = profile.homeStationId
            .takeIf { profile.alertsEnabled && it.isNotBlank() }
        if (desired == subscribedStationId) return

        subscribedStationId?.let { previous ->
            alertRepository.unsubscribeFromStation(previous)
                .onSuccess { subscribedStationId = null }
                .onFailure { e ->
                    Log.w(TAG, "Could not leave topic for station $previous", e)
                    _actionError.value = "Could not turn off alerts for that station"
                    // Leave subscribedStationId set so the next profile emission retries.
                    return
                }
        }

        desired?.let { stationId ->
            alertRepository.subscribeToStation(stationId)
                .onSuccess { subscribedStationId = stationId }
                .onFailure { e ->
                    Log.w(TAG, "Could not join topic for station $stationId", e)
                    _actionError.value = "Could not turn on alerts for that station"
                }
        }
    }

    private companion object {
        const val TAG = "AlertSettingsVM"
    }
}
