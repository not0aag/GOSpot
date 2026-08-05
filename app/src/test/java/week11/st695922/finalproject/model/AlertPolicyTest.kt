package week11.st695922.finalproject.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [AlertPolicy] is pure, so these run on plain JUnit with no Robolectric,
 * no mocking and no Firebase.
 *
 * Coordinates are the real Lakeshore West ones seeded by StationSeeder, so the
 * distance ordering being asserted is the ordering the app will actually see.
 */
class AlertPolicyTest {

    private fun station(
        id: String,
        capacity: Int,
        occupancy: Int,
        lat: Double = 0.0,
        lng: Double = 0.0
    ) = Station(
        id = id,
        name = id.replaceFirstChar { it.uppercase() },
        capacityTotal = capacity,
        currentOccupancy = occupancy,
        lat = lat,
        lng = lng
    )

    // Real seed coordinates.
    private val oakville = station("oakville", 1987, 1900, 43.4409, -79.6673) // 95% full
    private val bronte = station("bronte", 1730, 588, 43.3956, -79.7134) // 34%, ~6 km away
    private val clarkson = station("clarkson", 2600, 1430, 43.5183, -79.6392) // 55%, ~9 km away

    // -- isOverThreshold ---------------------------------------------------

    @Test
    fun `station just under the threshold does not alert`() {
        assertFalse(AlertPolicy.isOverThreshold(station("s", capacity = 100, occupancy = 89)))
    }

    @Test
    fun `station exactly at the threshold alerts`() {
        assertTrue(AlertPolicy.isOverThreshold(station("s", capacity = 100, occupancy = 90)))
    }

    @Test
    fun `station over the threshold alerts`() {
        assertTrue(AlertPolicy.isOverThreshold(station("s", capacity = 100, occupancy = 91)))
    }

    @Test
    fun `zero-capacity station does not divide by zero or alert`() {
        assertFalse(AlertPolicy.isOverThreshold(station("s", capacity = 0, occupancy = 0)))
    }

    // -- suggestAlternate --------------------------------------------------

    @Test
    fun `suggests the nearest station with room`() {
        val alternate = AlertPolicy.suggestAlternate(oakville, listOf(oakville, bronte, clarkson))
        assertEquals(bronte.id, alternate?.id)
    }

    @Test
    fun `skips a nearer station that is itself over the threshold`() {
        val fullBronte = bronte.copy(currentOccupancy = 1700) // 98% full
        val alternate = AlertPolicy.suggestAlternate(oakville, listOf(oakville, fullBronte, clarkson))
        assertEquals(clarkson.id, alternate?.id)
    }

    @Test
    fun `never suggests the home station itself`() {
        // A half-empty home station would otherwise win outright at distance 0.
        val roomyHome = oakville.copy(currentOccupancy = 100)
        val alternate = AlertPolicy.suggestAlternate(roomyHome, listOf(roomyHome, bronte, clarkson))
        assertEquals(bronte.id, alternate?.id)
    }

    @Test
    fun `returns null when every other lot is also full`() {
        val alternate = AlertPolicy.suggestAlternate(
            oakville,
            listOf(oakville, bronte.copy(currentOccupancy = 1700), clarkson.copy(currentOccupancy = 2550))
        )
        assertNull(alternate)
    }

    @Test
    fun `returns null when there are no other stations`() {
        assertNull(AlertPolicy.suggestAlternate(oakville, listOf(oakville)))
    }

    @Test
    fun `skips a station with no capacity recorded`() {
        // 0 of 0 reads as 0% full but has no free spaces, so it is useless advice.
        val unknown = station("unknown", capacity = 0, occupancy = 0, lat = 43.44, lng = -79.66)
        val alternate = AlertPolicy.suggestAlternate(oakville, listOf(oakville, unknown, bronte))
        assertEquals(bronte.id, alternate?.id)
    }

    // -- distanceKm --------------------------------------------------------

    @Test
    fun `distance between Oakville and Bronte is about six kilometres`() {
        assertEquals(6.3, AlertPolicy.distanceKm(oakville, bronte), 0.5)
    }

    @Test
    fun `distance is symmetric and zero for a station against itself`() {
        assertEquals(0.0, AlertPolicy.distanceKm(oakville, oakville), 0.0001)
        assertEquals(
            AlertPolicy.distanceKm(oakville, clarkson),
            AlertPolicy.distanceKm(clarkson, oakville),
            0.0001
        )
    }
}
