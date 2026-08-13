package week11.st695922.finalproject.ui.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import week11.st695922.finalproject.model.Station

internal data class StationNavigationUris(
    val googleNavigation: String,
    val mapFallback: String
)

internal object StationNavigator {
    fun buildUriStrings(station: Station): StationNavigationUris {
        val destination = station.address.ifBlank { "${station.lat},${station.lng}" }
        val encodedDestination = URLEncoder
            .encode(destination, StandardCharsets.UTF_8.toString())
            .replace("+", "%20")

        return StationNavigationUris(
            googleNavigation = "google.navigation:q=$encodedDestination",
            mapFallback = "geo:0,0?q=$encodedDestination"
        )
    }

    fun navigate(context: Context, station: Station) {
        val uris = buildUriStrings(station)
        val turnByTurn = Intent(Intent.ACTION_VIEW, Uri.parse(uris.googleNavigation))
            .setPackage("com.google.android.apps.maps")
        val anyMapApp = Intent(Intent.ACTION_VIEW, Uri.parse(uris.mapFallback))

        for (intent in listOf(turnByTurn, anyMapApp)) {
            try {
                context.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                Log.d(TAG, "No handler for ${intent.data?.scheme}, trying the next option", e)
            }
        }
        Log.w(TAG, "No installed app can handle navigation to ${station.id}")
    }

    private const val TAG = "StationNavigator"
}
