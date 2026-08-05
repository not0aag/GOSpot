package week11.st695922.finalproject.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import week11.st695922.finalproject.model.Station
import week11.st695922.finalproject.model.UserProfile
import kotlin.coroutines.resume

/**
 * Repository for the per-user `users/{uid}` profile document
 * (Week 6.2, Slides 6, 11, 19).
 */
class UserProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun profileFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(UserProfile::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun setHomeStation(uid: String, station: Station): Result<Unit> =
        updateFields(
            uid,
            mapOf(
                "homeStationId" to station.id,
                "homeStationName" to station.name
            )
        )

    /** Opt-in flag for "lot filling up" alerts on the user's home station. */
    suspend fun setAlertsEnabled(uid: String, enabled: Boolean): Result<Unit> =
        updateFields(uid, mapOf("alertsEnabled" to enabled))

    /** Persists this device's FCM registration token onto the profile. */
    suspend fun setFcmToken(uid: String, token: String): Result<Unit> =
        updateFields(uid, mapOf("fcmToken" to token))

    private suspend fun updateFields(uid: String, fields: Map<String, Any>): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            firestore.collection("users").document(uid)
                .update(fields)
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }
}
