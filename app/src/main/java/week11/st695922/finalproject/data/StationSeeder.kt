package week11.st695922.finalproject.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import week11.st695922.finalproject.model.Station
import kotlin.coroutines.resume

/**
 * Corresponds to Week 3.1 Slide 22's "Add Start Collection" setup step: seeds
 * the `stations` collection with the six Lakeshore West lots shown in the
 * Figma mocks (name/capacity/occupancy read directly off the "Pick home
 * station" and "Stations list" screens; lat/lng are approximate real-world
 * coordinates for these GO stations, not exact).
 */
class StationSeeder(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val demoStations = listOf(
        Station("oakville", "Oakville GO", "214 Cross Ave · Lakeshore West", 1987, 1550, 43.45500, -79.68250),
        Station("bronte", "Bronte GO", "2104 Wyecroft Rd · Lakeshore West", 1730, 588, 	43.418624, -79.725038),
        Station("clarkson", "Clarkson GO", "Clarkson Rd S · Lakeshore West", 2600, 1430, 	43.365126, -80.254255),
        Station("portcredit", "Port Credit GO", "70 Elizabeth St S · Lakeshore West", 1300, 936, 	43.365126, -80.254255),
        Station("mimico", "Mimico GO", "285 Royal York Rd · Lakeshore West", 400, 352, 		43.365126, -80.254255),
        Station("longbranch", "Long Branch GO", "3131 Lakeshore Blvd W · Lakeshore West", 800, 328, 43.365126, -80.254255)
    )

    suspend fun seedIfEmpty(): Result<Unit> = suspendCancellableCoroutine { cont ->
        val stationsRef = firestore.collection("stations")
        stationsRef.limit(1).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    cont.resume(Result.success(Unit))
                    return@addOnSuccessListener
                }
                val batch = firestore.batch()
                demoStations.forEach { station ->
                    batch.set(stationsRef.document(station.id), station)
                }
                batch.commit()
                    .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                    .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
            }
            .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
    }
}
