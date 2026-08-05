package week11.st695922.finalproject.data

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import week11.st695922.finalproject.model.Station
import week11.st695922.finalproject.receiver.GeofenceBroadcastReceiver
import kotlin.coroutines.resume


class GeofenceRepository(context: Context) {

    private val appContext = context.applicationContext
    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(appContext)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            appContext,
            GEOFENCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Replaces the full set of registered geofences with one per station.
     * GeofencingClient has no per-geofence update, so callers just pass the
     * current station list and this re-adds all of them under the same
     * request IDs (Google's own guidance for "the set changed").
     */
    @SuppressLint("MissingPermission")
    suspend fun registerGeofences(stations: List<Station>): Result<Unit> {
        if (stations.isEmpty()) return Result.success(Unit)

        val geofences = stations.map { station ->
            Geofence.Builder()
                .setRequestId(station.id)
                .setCircularRegion(station.lat, station.lng, GEOFENCE_RADIUS_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        return suspendCancellableCoroutine { cont ->
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }
    }

    suspend fun removeGeofences(): Result<Unit> = suspendCancellableCoroutine { cont ->
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener { cont.resume(Result.success(Unit)) }
            .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
    }

    companion object {
        private const val GEOFENCE_REQUEST_CODE = 1001
        private const val GEOFENCE_RADIUS_METERS = 125f
    }
}
