package week11.st695922.finalproject.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import week11.st695922.finalproject.model.CheckInEvent
import week11.st695922.finalproject.model.CheckInEventType
import week11.st695922.finalproject.model.CheckInOperation
import week11.st695922.finalproject.model.CheckInTransitionPolicy
import week11.st695922.finalproject.model.Station
import week11.st695922.finalproject.model.UserProfile
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


    suspend fun checkIn(station: Station): Result<Unit> =
        transition(station.id, wantsCheckIn = true, automatic = false)

    suspend fun checkOut(station: Station): Result<Unit> =
        transition(station.id, wantsCheckIn = false, automatic = false)

    suspend fun checkInByStationId(stationId: String): Result<Unit> =
        transition(stationId, wantsCheckIn = true, automatic = true)

    suspend fun checkOutByStationId(stationId: String): Result<Unit> =
        transition(stationId, wantsCheckIn = false, automatic = true)

    /**
     * The single check-in/check-out path, manual and automatic alike.
     *
     * Reads and writes `currentOccupancy` inside one transaction so two commuters
     * checking in at the same moment cannot overwrite each other - the earlier
     * manual path computed `currentOccupancy + 1` from a snapshot the caller was
     * already holding, which lost one of the two writes.
     *
     * The station values, active profile state, and lifetime check-in count commit
     * together. Check-in history is intentionally not written until the team's
     * deployed Firestore rules allow access to the per-user events collection.
     */
    private suspend fun transition(
        stationId: String,
        wantsCheckIn: Boolean,
        automatic: Boolean
    ): Result<Unit> {
        val uid = eventsRepository.currentUserId
            ?: return Result.failure(IllegalStateException("No signed-in user"))
        val userRef = firestore.collection("users").document(uid)

        return suspendCancellableCoroutine { cont ->
            firestore.runTransaction { transaction ->
                val profile = transaction.get(userRef).toObject(UserProfile::class.java)
                    ?: throw IllegalStateException("User profile $uid not found")
                val operation = CheckInTransitionPolicy.decide(
                    activeStationId = profile.activeStationId,
                    targetStationId = stationId,
                    wantsCheckIn = wantsCheckIn,
                    automatic = automatic,
                    automaticEnabled = profile.automaticCheckInEnabled
                )

                when (operation) {
                    CheckInOperation.NO_OP -> Unit
                    CheckInOperation.CHECK_OUT -> {
                        val stationRef = stationsRef.document(stationId)
                        val station = transaction.get(stationRef).toStation(stationId)
                        if (profile.activeOccupancyApplied) {
                            updateParkingAvailability(transaction, stationRef, station, deltaFree = 1)
                        }
                        transaction.update(userRef, clearedActiveFields())
                    }
                    CheckInOperation.CHECK_IN,
                    CheckInOperation.SWITCH_STATION -> {
                        val targetRef = stationsRef.document(stationId)
                        val target = transaction.get(targetRef).toStation(stationId)
                        val previous = if (operation == CheckInOperation.SWITCH_STATION) {
                            val previousRef = stationsRef.document(profile.activeStationId)
                            previousRef to transaction.get(previousRef).toStation(profile.activeStationId)
                        } else {
                            null
                        }

                        previous?.let { (previousRef, previousStation) ->
                            if (profile.activeOccupancyApplied) {
                                updateParkingAvailability(
                                    transaction,
                                    previousRef,
                                    previousStation,
                                    deltaFree = 1
                                )
                            }
                        }

                        val occupancyApplied = target.capacityTotal > 0 && target.spacesFree > 0
                        if (occupancyApplied) {
                            updateParkingAvailability(transaction, targetRef, target, deltaFree = -1)
                        }
                        transaction.update(
                            userRef,
                            mapOf(
                                "activeStationId" to stationId,
                                "activeCheckInSource" to if (automatic) SOURCE_AUTOMATIC else SOURCE_MANUAL,
                                "activeOccupancyApplied" to occupancyApplied,
                                "totalCheckIns" to FieldValue.increment(1L)
                            )
                        )

                    }
                }
            }.addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }
    }

    suspend fun disableAutomaticCheckIn(): Result<Unit> {
        val uid = eventsRepository.currentUserId
            ?: return Result.failure(IllegalStateException("No signed-in user"))
        val userRef = firestore.collection("users").document(uid)

        return suspendCancellableCoroutine { cont ->
            firestore.runTransaction { transaction ->
                val profile = transaction.get(userRef).toObject(UserProfile::class.java)
                    ?: throw IllegalStateException("User profile $uid not found")
                val activeStation = if (
                    profile.activeCheckInSource == SOURCE_AUTOMATIC && profile.activeStationId.isNotBlank()
                ) {
                    val stationRef = stationsRef.document(profile.activeStationId)
                    stationRef to transaction.get(stationRef).toStation(profile.activeStationId)
                } else {
                    null
                }

                activeStation?.let { (stationRef, station) ->
                    if (profile.activeOccupancyApplied) {
                        updateParkingAvailability(transaction, stationRef, station, deltaFree = 1)
                    }
                }
                transaction.update(
                    userRef,
                    if (activeStation != null) {
                        clearedActiveFields() + ("automaticCheckInEnabled" to false)
                    } else {
                        mapOf("automaticCheckInEnabled" to false)
                    }
                )
                Unit
            }.addOnSuccessListener { cont.resume(Result.success(it)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }

    suspend fun checkOutActiveUser(): Result<Unit> {
        val uid = eventsRepository.currentUserId ?: return Result.success(Unit)
        val userRef = firestore.collection("users").document(uid)
        return suspendCancellableCoroutine { cont ->
            firestore.runTransaction { transaction ->
                val profile = transaction.get(userRef).toObject(UserProfile::class.java)
                    ?: throw IllegalStateException("User profile $uid not found")
                if (profile.activeStationId.isBlank()) return@runTransaction

                val stationRef = stationsRef.document(profile.activeStationId)
                val station = transaction.get(stationRef).toStation(profile.activeStationId)
                if (profile.activeOccupancyApplied) {
                    updateParkingAvailability(transaction, stationRef, station, deltaFree = 1)
                }
                transaction.update(userRef, clearedActiveFields())
                Unit
            }.addOnSuccessListener { cont.resume(Result.success(it)) }
                .addOnFailureListener { cont.resume(Result.failure(it)) }
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toStation(id: String): Station =
        toObject(Station::class.java)?.copy(id = id)
            ?: throw IllegalStateException("Station $id not found")

    private fun clearedActiveFields(): Map<String, Any> = mapOf(
        "activeStationId" to "",
        "activeCheckInSource" to "",
        "activeOccupancyApplied" to false
    )

    private fun updateParkingAvailability(
        transaction: com.google.firebase.firestore.Transaction,
        stationRef: com.google.firebase.firestore.DocumentReference,
        station: Station,
        deltaFree: Int
    ) {
        val newSpacesFree = (station.spacesFree + deltaFree).coerceIn(0, station.capacityTotal)
        val newOccupancy = (station.capacityTotal - newSpacesFree).coerceAtLeast(0)
        val newPercentFull = if (station.capacityTotal <= 0) {
            0
        } else {
            ((newOccupancy * 100) / station.capacityTotal).coerceIn(0, 100)
        }
        transaction.update(
            stationRef,
            mapOf(
                "spacesFree" to newSpacesFree,
                "currentOccupancy" to newOccupancy,
                "percentFull" to newPercentFull
            )
        )
    }

    private companion object {
        const val SOURCE_MANUAL = "MANUAL"
        const val SOURCE_AUTOMATIC = "AUTOMATIC"
    }
}


class CheckInEventRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository = AuthRepository()
) {
    val currentUserId: String?
        get() = authRepository.currentUserId

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
