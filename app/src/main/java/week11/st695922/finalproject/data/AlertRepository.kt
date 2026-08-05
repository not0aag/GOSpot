package week11.st695922.finalproject.data

import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Isolates every FirebaseMessaging call behind suspend functions, the same way
 * [AuthRepository] does for FirebaseAuth (Week 6.1, Slides 9-11). Compose and
 * the ViewModels never touch the messaging SDK directly.
 *
 * One device subscribes to one topic per station it wants alerts for. Topic
 * fan-out is what lets a future Cloud Function notify every interested user
 * with a single publish, without enumerating tokens.
 */
class AlertRepository(
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()
) {
    /** This device's FCM registration token, fetched fresh from the SDK. */
    suspend fun currentToken(): Result<String> = suspendCancellableCoroutine { cont ->
        messaging.token
            .addOnSuccessListener { token -> cont.resume(Result.success(token)) }
            .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
    }

    suspend fun subscribeToStation(stationId: String): Result<Unit> {
        val topic = topicFor(stationId) ?: return blankStationFailure()
        return awaitCompletion(messaging.subscribeToTopic(topic))
    }

    suspend fun unsubscribeFromStation(stationId: String): Result<Unit> {
        val topic = topicFor(stationId) ?: return blankStationFailure()
        return awaitCompletion(messaging.unsubscribeFromTopic(topic))
    }

    private suspend fun awaitCompletion(task: Task<Void>): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }

    private fun blankStationFailure(): Result<Unit> =
        Result.failure(IllegalArgumentException("Station id is blank"))

    companion object {
        /**
         * Station ids are Firestore document ids seeded from [StationSeeder]
         * ("oakville", "bronte", ...), which already satisfy FCM's
         * `[a-zA-Z0-9-_.~%]+` topic rule, so no sanitising is needed - but a
         * blank id would produce an invalid topic, hence the null return.
         */
        fun topicFor(stationId: String): String? =
            if (stationId.isBlank()) null else "station_$stationId"
    }
}
