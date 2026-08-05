package week11.st695922.finalproject.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Decides *when* to warn a commuter and *where* to send them instead.
 *
 * Deliberately free of Android and Firebase imports so it runs under plain
 * JUnit. That is also why distance uses a local haversine rather than
 * `android.location.Location.distanceBetween`, which is a stubbed no-op in
 * unit tests and would force the whole thing to be mocked.
 *
 * Distances are measured from the *home station*, not the user's current
 * position. That keeps this a pure function of Firestore data, so the same
 * rule can later run server-side in a Cloud Function, where no device
 * location is available.
 */
object AlertPolicy {

    /**
     * Percent-full at which a lot is considered "filling up".
     *
     * A single constant rather than a Firestore field: with topic fan-out one
     * published message reaches every subscriber, so a per-user threshold
     * could not be honoured anyway.
     */
    const val THRESHOLD_PERCENT = 90

    /**
     * [Station.percentFull] already returns 0 for a zero-capacity lot, so this
     * is safe for seed data with capacityTotal unset.
     */
    fun isOverThreshold(station: Station): Boolean =
        station.percentFull >= THRESHOLD_PERCENT

    /**
     * The closest lot to [home] that is below the threshold and has at least
     * one free space, or null when every other lot is just as busy.
     */
    fun suggestAlternate(home: Station, stations: List<Station>): Station? =
        stations
            .filter { it.id != home.id && !isOverThreshold(it) && it.spacesFree > 0 }
            .minByOrNull { distanceKm(home, it) }

    /** Great-circle distance between two stations, in kilometres. */
    fun distanceKm(from: Station, to: Station): Double {
        val lat1 = Math.toRadians(from.lat)
        val lat2 = Math.toRadians(to.lat)
        val deltaLat = Math.toRadians(to.lat - from.lat)
        val deltaLng = Math.toRadians(to.lng - from.lng)

        val a = sin(deltaLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(deltaLng / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a).coerceAtMost(1.0))
    }

    private const val EARTH_RADIUS_KM = 6371.0
}
