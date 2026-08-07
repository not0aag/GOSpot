package week11.st695922.finalproject.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import week11.st695922.finalproject.data.AlertRepository
import week11.st695922.finalproject.data.CheckInEventRepository
import week11.st695922.finalproject.data.StationRepository
import week11.st695922.finalproject.data.UserProfileRepository
import week11.st695922.finalproject.model.AlertPolicy
import week11.st695922.finalproject.model.Station
import week11.st695922.finalproject.model.UserProfile
import week11.st695922.finalproject.notification.AlertNotifier

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
    private val profileRepository: UserProfileRepository = UserProfileRepository(),
    private val stationRepository: StationRepository = StationRepository(),
    private val eventsRepository: CheckInEventRepository = CheckInEventRepository()
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

    /**
     * Stations already alerted on since they last crossed the threshold.
     *
     * Firestore snapshot listeners re-emit on every write, so without this a
     * busy lot would notify again on each individual check-in. A station is
     * removed once it drops back under the threshold, re-arming the alert.
     *
     * In-memory deliberately: the alert is about a live crossing, so a fresh
     * process legitimately re-evaluates against current occupancy.
     */
    private val alertedStationIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            profile.filterNotNull().collect { reconcileSubscription(it) }
        }
        viewModelScope.launch {
            combine(
                profile.filterNotNull(),
                stationRepository.stationsFlow()
            ) { currentProfile, stations -> currentProfile to stations }
                .collect { (currentProfile, stations) ->
                    evaluateThreshold(currentProfile, stations)
                }
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

    /**
     * Copies this device's FCM registration token onto the user's profile.
     *
     * GoSpotMessagingService.onNewToken cannot cover the common case where FCM
     * issued the token before anyone signed in, so this runs once the user is
     * authenticated. Storing the token means alerts can later be addressed to
     * a specific device instead of fanned out over a station topic.
     */
    fun syncFcmToken() {
        viewModelScope.launch {
            val token = alertRepository.currentToken()
                .onFailure { e -> Log.w(TAG, "Could not read FCM token", e) }
                .getOrNull()
                ?: return@launch

            if (token.isBlank() || token == profile.value?.fcmToken) return@launch

            profileRepository.setFcmToken(uid, token)
                .onFailure { e -> Log.w(TAG, "Could not store FCM token", e) }
        }
    }

    /**
     * Fires the local "lot filling up" notification when the user's home
     * station crosses [AlertPolicy.THRESHOLD_PERCENT], and records the crossing
     * to the Alerts feed.
     *
     * This is the on-device half of the alert path: it needs no server and no
     * billing, and it fires off exactly the same [AlertPolicy] decision a
     * Cloud Function would make.
     */
    private suspend fun evaluateThreshold(profile: UserProfile, stations: List<Station>) {
        val home = stations.find { it.id == profile.homeStationId } ?: return

        if (!AlertPolicy.isOverThreshold(home)) {
            // Back under the threshold - re-arm so the next crossing alerts.
            alertedStationIds.remove(home.id)
            return
        }

        if (!profile.alertsEnabled) return
        if (home.id in alertedStationIds) return

        val alternate = AlertPolicy.suggestAlternate(home, stations)

        // The feed row is written first and is what latches the alert: a blocked
        // notification should still leave a record on the Alerts tab, and
        // granting the permission later must not replay old crossings.
        val recorded = eventsRepository.recordLotWarning(home, alternate)
            .onFailure { e -> Log.w(TAG, "Could not record lot warning for ${home.id}", e) }

        AlertNotifier.postLotFillingAlert(
            context = getApplication(),
            homeStation = home,
            alternate = alternate
        )

        if (recorded.isSuccess) alertedStationIds.add(home.id)
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
