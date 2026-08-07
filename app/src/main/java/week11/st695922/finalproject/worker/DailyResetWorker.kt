package week11.st695922.finalproject.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import week11.st695922.finalproject.data.StationRepository

/**
 * Nightly correction for occupancy drift from missed geofence EXIT events.
 * Delegates the actual Firestore write to [StationRepository], same as
 * every other write in this app.
 *
 * @JvmOverloads for the same reason [week11.st695922.finalproject.viewmodel.AuthViewModel]
 * needs it: WorkManager's default WorkerFactory reflectively looks up a public
 * `(Context, WorkerParameters)` constructor, and a Kotlin default argument does
 * not emit one. Without this the worker fails to instantiate on every run.
 */
class DailyResetWorker @JvmOverloads constructor(
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
