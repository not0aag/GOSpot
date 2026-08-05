package week11.st695922.finalproject.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

private const val UNIQUE_WORK_NAME = "daily-station-reset"
private val RESET_TIME: LocalTime = LocalTime.of(3, 0)

/**
 * Enqueues [DailyResetWorker] to run once every 24 hours, first firing at
 * the next 3 AM. KEEP means re-calling this on every app start (from
 * MainActivity) is a no-op once the periodic work already exists.
 */
fun scheduleDailyStationReset(context: Context) {
    val request = PeriodicWorkRequestBuilder<DailyResetWorker>(Duration.ofHours(24))
        .setInitialDelay(durationUntilNext(RESET_TIME))
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}

private fun durationUntilNext(time: LocalTime): Duration {
    val now = LocalDateTime.now()
    var next = now.toLocalDate().atTime(time)
    if (!next.isAfter(now)) {
        next = next.plusDays(1)
    }
    return Duration.between(now, next)
}
