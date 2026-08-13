package week11.st695922.finalproject.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import week11.st695922.finalproject.model.Station

class StationNavigatorTest {

    @Test
    fun `navigation uses the encoded street address`() {
        val station = Station(
            id = "longbranch",
            name = "Long Branch GO",
            address = "20 Brow Drive, Toronto, ON M8W 3P6",
            lat = 43.59194,
            lng = -79.54556
        )

        val uris = StationNavigator.buildUriStrings(station)

        val encodedAddress = "20%20Brow%20Drive%2C%20Toronto%2C%20ON%20M8W%203P6"
        assertEquals("google.navigation:q=$encodedAddress", uris.googleNavigation)
        assertEquals("geo:0,0?q=$encodedAddress", uris.mapFallback)
    }

    @Test
    fun `navigation falls back to coordinates when address is blank`() {
        val station = Station(id = "test", lat = 43.5, lng = -79.6)

        val uris = StationNavigator.buildUriStrings(station)

        assertTrue(uris.googleNavigation.endsWith("43.5%2C-79.6"))
        assertTrue(uris.mapFallback.endsWith("43.5%2C-79.6"))
    }
}
