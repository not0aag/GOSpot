package week11.st695922.finalproject.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import week11.st695922.finalproject.data.StationRepository


class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val repository = StationRepository()

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.w(TAG, "Broadcast intent had no geofencing event")
            return
        }
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofence error: ${GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)}")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transition != Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            // ENTER/EXIT are the only transition types this app registers for.
            return
        }

        val stationIds = geofencingEvent.triggeringGeofences.orEmpty().map { it.requestId }
        if (stationIds.isEmpty()) return


        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                stationIds.forEach { stationId ->
                    val result = if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                        repository.checkInByStationId(stationId)
                    } else {
                        repository.checkOutByStationId(stationId)
                    }
                    result.onFailure { e ->
                        Log.e(TAG, "Failed to record automatic transition for station $stationId", e)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
