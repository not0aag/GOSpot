package week11.st695922.finalproject.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal object StationProximity {
    fun nearestWithin(
        latitude: Double,
        longitude: Double,
        stations: List<Station>,
        radiusMeters: Double
    ): Station? = stations
        .map { station -> station to distanceMeters(latitude, longitude, station.lat, station.lng) }
        .filter { (_, distance) -> distance <= radiusMeters }
        .minByOrNull { (_, distance) -> distance }
        ?.first

    private fun distanceMeters(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val lat1 = Math.toRadians(latitude1)
        val lat2 = Math.toRadians(latitude2)
        val deltaLat = lat2 - lat1
        val deltaLng = Math.toRadians(longitude2 - longitude1)
        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(lat1) * cos(lat2) * sin(deltaLng / 2) * sin(deltaLng / 2)
        return EARTH_RADIUS_METERS * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    private const val EARTH_RADIUS_METERS = 6_371_000.0
}
