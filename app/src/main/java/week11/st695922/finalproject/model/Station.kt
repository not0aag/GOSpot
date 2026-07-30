package week11.st695922.finalproject.model

/**
 * Firestore document shape for `stations/{stationId}`.
 * Field-by-field CRUD (Week 4.1, Slides 10-11) targets [currentOccupancy] only;
 * everything else is treated as read-only seed data for this first draft.
 *
 * No-arg defaults are required so Firestore's `toObject`/`toObjects` reflection
 * (Week 3.2, Slide 3) can deserialize documents into this class.
 */
data class Station(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val capacityTotal: Int = 0,
    val currentOccupancy: Int = 0,
    val lat: Double = 0.0,
    val lng: Double = 0.0
) {
    val spacesFree: Int
        get() = (capacityTotal - currentOccupancy).coerceAtLeast(0)

    val percentFull: Int
        get() = if (capacityTotal <= 0) 0 else ((currentOccupancy * 100) / capacityTotal).coerceIn(0, 100)
}
