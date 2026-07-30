package week11.st695922.finalproject.ui.navigation

/**
 * Screen switching for this app follows the state-driven pattern taught in
 * Week 6.2, Slide 11 (a sealed class deciding which composable to draw) rather
 * than the androidx.navigation.compose library, which is not covered by the
 * course material. That slide's example only covers a two/three-screen toggle;
 * this Route hierarchy is the same idea generalized to this app's larger
 * screen set, not a taught pattern verbatim.
 */
sealed interface Route {
    sealed interface MainTab : Route {
        data object Map : MainTab
        data object Stations : MainTab
        data object Alerts : MainTab
        data object Profile : MainTab
    }

    data class StationDetail(val stationId: String) : Route
    data class CheckInConfirmation(val stationId: String, val isCheckIn: Boolean) : Route
}
