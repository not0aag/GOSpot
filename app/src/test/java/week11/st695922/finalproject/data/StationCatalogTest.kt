package week11.st695922.finalproject.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import week11.st695922.finalproject.model.Station

class StationCatalogTest {

    @Test
    fun `catalog contains the approved station locations`() {
        val stations = StationCatalog.stations.associateBy { it.id }
        val expectedLocations = mapOf(
            "bronte" to Triple("2104 Wyecroft Road, Oakville, ON", 43.41722, -79.72222),
            "oakville" to Triple("214 Cross Avenue, Oakville, ON", 43.4546, -79.6828),
            "clarkson" to Triple("1110 Southdown Road, Mississauga, ON", 43.51222, -79.63472),
            "portcredit" to Triple("30 Queen Street East, Mississauga, ON", 43.5556, -79.5875),
            "longbranch" to Triple("20 Brow Drive, Toronto, ON M8W 3P6", 43.59194, -79.54556),
            "mimico" to Triple("315 Royal York Road, Toronto, ON", 43.61639, -79.49722)
        )

        assertEquals(expectedLocations.keys, stations.keys)
        expectedLocations.forEach { (id, expected) ->
            val station = stations.getValue(id)
            assertEquals(expected.first, station.address)
            assertEquals(expected.second, station.lat, 0.0)
            assertEquals(expected.third, station.lng, 0.0)
        }
    }

    @Test
    fun `firestore correction excludes parking fields`() {
        val fields = StationCatalog.firestoreLocationFields(StationCatalog.stations.first())

        assertEquals(setOf("address", "lat", "lng"), fields.keys)
        assertFalse(fields.containsKey("capacityTotal"))
        assertFalse(fields.containsKey("currentOccupancy"))
    }

    @Test
    fun `canonical location overlay preserves parking values`() {
        val stale = Station(
            id = "clarkson",
            address = "Wrong address",
            capacityTotal = 999,
            currentOccupancy = 321,
            lat = 0.0,
            lng = 0.0
        )

        val corrected = StationCatalog.applyCanonicalLocation(stale)

        assertEquals("1110 Southdown Road, Mississauga, ON", corrected.address)
        assertEquals(43.51222, corrected.lat, 0.0)
        assertEquals(-79.63472, corrected.lng, 0.0)
        assertEquals(999, corrected.capacityTotal)
        assertEquals(321, corrected.currentOccupancy)
    }
}
