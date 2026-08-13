package week11.st695922.finalproject.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationProximityTest {
    private val bronte = Station(id = "bronte", lat = 43.41722, lng = -79.72222)
    private val oakville = Station(id = "oakville", lat = 43.4546, lng = -79.6828)

    @Test
    fun `exact station coordinate is inside radius`() {
        val station = StationProximity.nearestWithin(
            latitude = 43.41722,
            longitude = -79.72222,
            stations = listOf(bronte, oakville),
            radiusMeters = 300.0
        )

        assertEquals("bronte", station?.id)
    }

    @Test
    fun `location outside every radius returns null`() {
        val station = StationProximity.nearestWithin(
            latitude = 43.65,
            longitude = -79.38,
            stations = listOf(bronte, oakville),
            radiusMeters = 300.0
        )

        assertNull(station)
    }
}
