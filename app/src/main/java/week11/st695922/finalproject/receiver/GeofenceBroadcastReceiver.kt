package week11.st695922.finalproject.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import week11.st695922.finalproject.worker.GeofenceTransitionWorker


class GeofenceBroadcastReceiver : BroadcastReceiver() {
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


        val input = Data.Builder()
            .putInt(GeofenceTransitionWorker.KEY_TRANSITION, transition)
            .putStringArray(GeofenceTransitionWorker.KEY_STATION_IDS, stationIds.toTypedArray())
            .build()
        val request = OneTimeWorkRequestBuilder<GeofenceTransitionWorker>()
            .setInputData(input)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_TRANSITION_WORK,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val UNIQUE_TRANSITION_WORK = "geofence-transitions"
    }
}
