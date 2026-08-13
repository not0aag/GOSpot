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
                    val station = doc.toObject(Station::class.java)?.copy(id = doc.id)
                        ?: Station(id = doc.id)
                    StationCatalog.applyCanonicalLocation(station)
                })
            }
        }
        awaitClose { listener.remove() }
    }


    suspend fun resetAllOccupancy(): Result<Unit> = suspendCancellableCoroutine { cont ->
        stationsRef.get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { doc -> batch.update(doc.reference, "currentOccupancy", 0) }
                batch.commit()
                    .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                    .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
            }
            .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
    }

    suspend fun checkIn(station: Station): Result<Unit> =
        updateOccupancyAtomic(station.id, delta = 1, eventType = CheckInEventType.CHECK_IN, auto = false)

    suspend fun checkOut(station: Station): Result<Unit> =
        updateOccupancyAtomic(station.id, delta = -1, eventType = CheckInEventType.CHECK_OUT, auto = false)

    suspend fun checkInByStationId(stationId: String): Result<Unit> =
        updateOccupancyAtomic(stationId, delta = 1, eventType = CheckInEventType.CHECK_IN, auto = true)

    suspend fun checkOutByStationId(stationId: String): Result<Unit> =
        updateOccupancyAtomic(stationId, delta = -1, eventType = CheckInEventType.CHECK_OUT, auto = true)

    /**
     * The single check-in/check-out path, manual and automatic alike.
     *
     * Reads and writes `currentOccupancy` inside one transaction so two commuters
     * checking in at the same moment cannot overwrite each other - the earlier
     * manual path computed `currentOccupancy + 1` from a snapshot the caller was
     * already holding, which lost one of the two writes.
     *
     * The event is recorded only after the transaction commits, and the write is
     * awaited rather than fired and forgotten: [week11.st695922.finalproject.receiver.GeofenceBroadcastReceiver]
     * ends its broadcast as soon as this returns, so an un-awaited add can be
     * killed with the process before Firestore flushes it.
     */
    private suspend fun updateOccupancyAtomic(
        stationId: String,
        delta: Int,
        eventType: CheckInEventType,
        auto: Boolean
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
        // A failed event write must not undo a committed occupancy change, so the
        // caller's Result reflects the transaction only.
        txResult.onSuccess { station -> eventsRepository.recordEvent(station, eventType, auto) }
        return txResult.map { }
    }
}


class CheckInEventRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository = AuthRepository()
) {
    suspend fun recordEvent(
        station: Station,
        type: CheckInEventType,
        auto: Boolean = false
    ): Result<Unit> = addEvent(
        CheckInEvent(
            stationId = station.id,
            stationName = station.name,
            type = type.name,
            timestampMillis = System.currentTimeMillis(),
            auto = auto
        )
    )

    /**
     * Records a "lot filling up" crossing so it shows on the Alerts tab, which
     * promises lot warnings alongside check-ins. Written independently of the
     * system notification, which can be suppressed by a missing permission.
     */
    suspend fun recordLotWarning(station: Station, alternate: Station?): Result<Unit> = addEvent(
        CheckInEvent(
            stationId = station.id,
            stationName = station.name,
            type = CheckInEventType.LOT_WARNING.name,
            timestampMillis = System.currentTimeMillis(),
            auto = true,
            percentFull = station.percentFull,
            alternateName = alternate?.name.orEmpty()
        )
    )

    private suspend fun addEvent(event: CheckInEvent): Result<Unit> {
        val uid = authRepository.currentUserId
            ?: return Result.failure(IllegalStateException("No signed-in user to record this event for"))
        return suspendCancellableCoroutine { cont ->
            firestore.collection("users").document(uid).collection("events")
                .add(event)
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }
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
