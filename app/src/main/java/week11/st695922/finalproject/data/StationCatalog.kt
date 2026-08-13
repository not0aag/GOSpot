package week11.st695922.finalproject.data

import week11.st695922.finalproject.model.Station

/** Canonical location data for the six Lakeshore West stations used by GOSpot. */
internal object StationCatalog {
    val stations: List<Station> = listOf(
        station("bronte", "Bronte GO", "2104 Wyecroft Road, Oakville, ON", 1730, 588, 43.41722, -79.72222),
        station("oakville", "Oakville GO", "214 Cross Avenue, Oakville, ON", 1987, 1550, 43.4546, -79.6828),
        station("clarkson", "Clarkson GO", "1110 Southdown Road, Mississauga, ON", 2600, 1430, 43.51222, -79.63472),
        station("portcredit", "Port Credit GO", "30 Queen Street East, Mississauga, ON", 1300, 936, 43.5556, -79.5875),
        station("longbranch", "Long Branch GO", "20 Brow Drive, Toronto, ON M8W 3P6", 800, 328, 43.59194, -79.54556),
        station("mimico", "Mimico GO", "315 Royal York Road, Toronto, ON", 400, 352, 43.61639, -79.49722)
    )

    private fun station(
        id: String,
        name: String,
        address: String,
        capacityTotal: Int,
        currentOccupancy: Int,
        lat: Double,
        lng: Double
    ) = Station(
        id = id,
        name = name,
        address = address,
        capacityTotal = capacityTotal,
        currentOccupancy = currentOccupancy,
        lat = lat,
        lng = lng,
        spacesFree = (capacityTotal - currentOccupancy).coerceAtLeast(0),
        percentFull = if (capacityTotal <= 0) 0 else (currentOccupancy * 100) / capacityTotal
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
