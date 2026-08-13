package week11.st695922.finalproject.viewmodel

import android.location.Location
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import week11.st695922.finalproject.data.StationRepository
import week11.st695922.finalproject.model.Station

class MapViewModelTest {

    private lateinit var repository: StationRepository
    private lateinit var viewModel: MapViewModel

    @Before
    fun setup() {
        repository = mockk()
        every { repository.stationsFlow() } returns flowOf(emptyList())
        viewModel = MapViewModel(repository)
    }

    @Test
    fun `getNearestAvailableStation returns null when no stations available`() {
        val userLocation = mockk<Location>()
        val result = viewModel.getNearestAvailableStation(userLocation)
        assertNull(result)
    }

    @Test
    fun `getNearestAvailableStation returns the only available station`() {
        val station = Station(
            id = "1",
            name = "Test",
            capacityTotal = 10,
            currentOccupancy = 5,
            lat = 43.0,
            lng = -79.0,
            spacesFree = 5,
            percentFull = 50
        )
        // Note: MapViewModel reads from stationsState, which is populated via stationsFlow() in init.
        // But since stationsState is a StateFlow, we might need to wait for it or mock the state directly if possible.
        // Actually, the current MapViewModel implementation reads repository.stationsFlow() and stateIn.
        // Let's refine the test after checking if we need to add a dependency for testing flows.
    }
}
