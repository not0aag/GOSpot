package week11.st695922.finalproject.data

import week11.st695922.finalproject.model.Station

/** Canonical location data for the six Lakeshore West stations used by GOSpot. */
internal object StationCatalog {
    val stations: List<Station> = listOf(
        Station("bronte", "Bronte GO", "2104 Wyecroft Road, Oakville, ON", 1730, 588, 43.41722, -79.72222),
        Station("oakville", "Oakville GO", "214 Cross Avenue, Oakville, ON", 1987, 1550, 43.4546, -79.6828),
        Station("clarkson", "Clarkson GO", "1110 Southdown Road, Mississauga, ON", 2600, 1430, 43.51222, -79.63472),
        Station("portcredit", "Port Credit GO", "30 Queen Street East, Mississauga, ON", 1300, 936, 43.5556, -79.5875),
        Station("longbranch", "Long Branch GO", "20 Brow Drive, Toronto, ON M8W 3P6", 800, 328, 43.59194, -79.54556),
        Station("mimico", "Mimico GO", "315 Royal York Road, Toronto, ON", 400, 352, 43.61639, -79.49722)
    )

    /**
     * Fields that may be corrected on an existing Firestore station document.
     * Parking capacity and occupancy are deliberately excluded.
     */
    fun firestoreLocationFields(station: Station): Map<String, Any> = mapOf(
        "address" to station.address,
        "lat" to station.lat,
        "lng" to station.lng
    )

    fun applyCanonicalLocation(station: Station): Station {
        val canonical = stations.find { it.id == station.id } ?: return station
        return station.copy(
            address = canonical.address,
            lat = canonical.lat,
            lng = canonical.lng
        )
    }
}
