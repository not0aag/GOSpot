package week11.st695922.finalproject.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import week11.st695922.finalproject.model.CheckInEvent
import week11.st695922.finalproject.model.CheckInEventType
import week11.st695922.finalproject.model.Station
import kotlin.coroutines.resume

/**
 * Repository for the shared `stations` collection.
 *
 * The course material wires `addSnapshotListener` straight into a Composable's
 * DisposableEffect (Week 4.1, Slides 2, 8). To honor the MVVM rule that Compose
 * UI never touches Firebase directly (Week 6.1, Slide 6), this repository wraps
 * that same listener in a callbackFlow instead — same underlying API, just
 * exposed as a Flow the ViewModel can turn into a StateFlow.
 */
class StationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val eventsRepository: CheckInEventRepository = CheckInEventRepository(firestore)
) {
    private val stationsRef = firestore.collection("stations")

    fun stationsFlow(): Flow<List<Station>> = callbackFlow {
        val listener = stationsRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                trySend(snapshot.documents.map { doc ->
                    doc.toObject(Station::class.java)?.copy(id = doc.id) ?: Station(id = doc.id)
                })
            }
        }
        awaitClose { listener.remove() }
    }

    /**
     * Manual check-in: this stands in for the design's automatic geofence
     * ENTER trigger, which needs GeofencingClient — not covered by the course
     * material (see scoping note). Reads the current count, computes the new
     * value in Kotlin, then writes only that one field (Week 4.1, Slide 10-11).
     */
    suspend fun checkIn(station: Station): Result<Unit> = updateOccupancy(
        station = station,
        newOccupancy = (station.currentOccupancy + 1).coerceAtMost(station.capacityTotal),
        eventType = CheckInEventType.CHECK_IN
    )

    suspend fun checkOut(station: Station): Result<Unit> = updateOccupancy(
        station = station,
        newOccupancy = (station.currentOccupancy - 1).coerceAtLeast(0),
        eventType = CheckInEventType.CHECK_OUT
    )

    private suspend fun updateOccupancy(
        station: Station,
        newOccupancy: Int,
        eventType: CheckInEventType
    ): Result<Unit> {
        val updateResult = suspendCancellableCoroutine<Result<Unit>> { cont ->
            stationsRef.document(station.id)
                .update("currentOccupancy", newOccupancy)
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }
        if (updateResult.isSuccess) {
            eventsRepository.recordEvent(station, eventType)
        }
        return updateResult
    }
}

/**
 * Writes the `users/{uid}/events/{eventId}` documents that back the Alerts
 * screen. Kept separate from [StationRepository] since it targets a different
 * collection path, but called from there right after a successful occupancy
 * update so every manual check-in/out produces a real Firestore-backed alert.
 */
class CheckInEventRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository = AuthRepository()
) {
    fun recordEvent(station: Station, type: CheckInEventType) {
        val uid = authRepository.currentUserId ?: return
        val event = CheckInEvent(
            stationId = station.id,
            stationName = station.name,
            type = type.name,
            timestampMillis = System.currentTimeMillis()
        )
        firestore.collection("users").document(uid).collection("events").add(event)
    }

    fun eventsFlow(uid: String): Flow<List<CheckInEvent>> = callbackFlow {
        val listener = firestore.collection("users").document(uid).collection("events")
            .orderBy("timestampMillis", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.documents.map { doc ->
                        doc.toObject(CheckInEvent::class.java)?.copy(id = doc.id) ?: CheckInEvent(id = doc.id)
                    })
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun clearAll(uid: String): Result<Unit> = suspendCancellableCoroutine { cont ->
        val eventsRef = firestore.collection("users").document(uid).collection("events")
        eventsRef.get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                    .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
            }
            .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
    }
}
