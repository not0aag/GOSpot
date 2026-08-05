package week11.st695922.finalproject.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import week11.st695922.finalproject.notification.AlertNotifier

/**
 * Receives Cloud Messaging pushes.
 *
 * Two shapes arrive here:
 *  - our own alert payload (a data message carrying [KEY_STATION_ID] and friends),
 *    which a Cloud Function would send to the `station_<id>` topic;
 *  - a bare notification message, which is what "Send test message" in the
 *    Firebase console produces when verifying a topic subscription.
 *
 * Note that [onMessageReceived] only runs for notification-type messages while
 * the app is in the foreground - backgrounded, the system tray handles them
 * directly. Data-only messages always come through here.
 */
class GoSpotMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val stationId = data[KEY_STATION_ID]

        if (stationId != null) {
            AlertNotifier.postLotFillingAlert(
                context = applicationContext,
                stationId = stationId,
                stationName = data[KEY_STATION_NAME].orEmpty(),
                percentFull = data[KEY_PERCENT_FULL]?.toIntOrNull() ?: 0,
                alternateName = data[KEY_ALTERNATE_NAME],
                alternateSpacesFree = data[KEY_ALTERNATE_SPACES_FREE]?.toIntOrNull(),
                alternateDistanceKm = data[KEY_ALTERNATE_DISTANCE_KM]?.toDoubleOrNull()
            )
        } else {
            val notification = message.notification
            if (notification == null) {
                Log.w(TAG, "Message had neither a station payload nor a notification block")
                return
            }
            AlertNotifier.postRawMessage(applicationContext, notification.title, notification.body)
        }
    }

    /**
     * Fired when FCM issues or rotates this device's registration token.
     * Persisting it to the user's profile is wired up in a later step.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM registration token refreshed")
    }

    companion object {
        private const val TAG = "GoSpotMessaging"

        const val KEY_STATION_ID = "stationId"
        const val KEY_STATION_NAME = "stationName"
        const val KEY_PERCENT_FULL = "percentFull"
        const val KEY_ALTERNATE_NAME = "alternateName"
        const val KEY_ALTERNATE_SPACES_FREE = "alternateSpacesFree"
        const val KEY_ALTERNATE_DISTANCE_KM = "alternateDistanceKm"
    }
}
