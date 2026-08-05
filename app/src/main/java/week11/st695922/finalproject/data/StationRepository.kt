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
 * Wraps Firestore snapshot listeners in a Flow for use within ViewModels.
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

    suspend fun checkInByStationId(stationId: String): Result<Unit> =
        updateOccupancyByStationIdAtomic(stationId, delta = 1, eventType = CheckInEventType.CHECK_IN)

    suspend fun checkOutByStationId(stationId: String): Result<Unit> =
        updateOccupancyByStationIdAtomic(stationId, delta = -1, eventType = CheckInEventType.CHECK_OUT)

    private suspend fun updateOccupancyByStationIdAtomic(
        stationId: String,
        delta: Int,
        eventType: CheckInEventType
    ): Result<Unit> {
        val stationRef = stationsRef.document(stationId)
        val txResult = suspendCancellableCoroutine<Result<Station>> { cont ->
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(stationRef)
                val station = snapshot.toObject(Station::class.java)?.copy(id = snapshot.id)
                    ?: throw IllegalStateException("Station $stationId not found")
                val newOccupancy = (station.currentOccupancy + delta).coerceIn(0, station.capacityTotal)
                transaction.update(stationRef, "currentOccupancy", newOccupancy)
                station
            }.addOnSuccessListener { station -> cont.resume(Result.success(station)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }
        txResult.onSuccess { station -> eventsRepository.recordEvent(station, eventType, auto = true) }
        return txResult.map { }
    }

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


class CheckInEventRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository = AuthRepository()
) {
    fun recordEvent(station: Station, type: CheckInEventType, auto: Boolean = false) {
        val uid = authRepository.currentUserId ?: return
        val event = CheckInEvent(
            stationId = station.id,
            stationName = station.name,
            type = type.name,
            timestampMillis = System.currentTimeMillis(),
            auto = auto
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
