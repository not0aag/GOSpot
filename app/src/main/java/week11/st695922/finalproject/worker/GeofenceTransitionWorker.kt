package week11.st695922.finalproject.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.Geofence
import com.google.firebase.auth.FirebaseAuth
import week11.st695922.finalproject.data.StationRepository

/** Completes geofence writes even when the broadcast process is short-lived. */
class GeofenceTransitionWorker @JvmOverloads constructor(
    context: Context,
    params: WorkerParameters,
    private val repository: StationRepository = StationRepository()
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()

        val transition = inputData.getInt(KEY_TRANSITION, -1)
        val stationIds = inputData.getStringArray(KEY_STATION_IDS).orEmpty()
        if (stationIds.isEmpty()) return Result.success()

        var failed = false
        stationIds.forEach { stationId ->
            val result = when (transition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> repository.checkInByStationId(stationId)
                Geofence.GEOFENCE_TRANSITION_EXIT -> repository.checkOutByStationId(stationId)
                else -> return Result.failure()
            }
            if (result.isFailure) failed = true
        }

        return when {
            !failed -> Result.success()
            runAttemptCount < MAX_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }

    companion object {
        const val KEY_TRANSITION = "transition"
        const val KEY_STATION_IDS = "station_ids"
        private const val MAX_ATTEMPTS = 3
    }
}
