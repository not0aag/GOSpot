package week11.st695922.finalproject.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import week11.st695922.finalproject.data.StationRepository

/**
 * Nightly correction for occupancy drift from missed geofence EXIT events.
 * Delegates the actual Firestore write to [StationRepository], same as
 * every other write in this app.
 */
class DailyResetWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: StationRepository = StationRepository()
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return repository.resetAllOccupancy().fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}
