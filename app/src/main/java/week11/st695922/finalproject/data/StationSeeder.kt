package week11.st695922.finalproject.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Creates missing demo stations and corrects location fields on existing ones.
 * Existing parking capacity and occupancy values are never overwritten.
 */
class StationSeeder(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    internal suspend fun synchronizeStations(): Result<Unit> = suspendCancellableCoroutine { cont ->
        val stationsRef = firestore.collection("stations")
        stationsRef.get()
            .addOnSuccessListener { snapshot ->
                val existingStations = snapshot.documents.associateBy { it.id }
                val batch = firestore.batch()
                var hasWrites = false
                StationCatalog.stations.forEach { station ->
                    val document = stationsRef.document(station.id)
                    val existing = existingStations[station.id]
                    if (existing == null) {
                        batch.set(document, station)
                        hasWrites = true
                    } else if (
                        existing.getString("address") != station.address ||
                        existing.getDouble("lat") != station.lat ||
                        existing.getDouble("lng") != station.lng
                    ) {
                        batch.set(
                            document,
                            StationCatalog.firestoreLocationFields(station),
                            SetOptions.merge()
                        )
                        hasWrites = true
                    }
                }
                if (!hasWrites) {
                    cont.resume(Result.success(Unit))
                    return@addOnSuccessListener
                }
                batch.commit()
                    .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                    .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
            }
            .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
    }
}
