package week11.st695922.finalproject.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import week11.st695922.finalproject.MainActivity
import week11.st695922.finalproject.R
import java.text.NumberFormat
import java.util.Locale

/**
 * The only place in the app that talks to [NotificationManager].
 *
 * Both alert paths funnel through here: the on-device threshold watcher in
 * `AlertSettingsViewModel` (which has real [Station] objects) and
 * `GoSpotMessagingService` (which only has a string data payload off the wire).
 *
 * minSdk is 36, so notification channels and the POST_NOTIFICATIONS runtime
 * permission always apply - there is no legacy branch to guard.
 */
object AlertNotifier {

    /** Creates the alerts channel. Safe to call repeatedly; re-creation is a no-op. */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            context.getString(R.string.alert_channel_id),
            context.getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.alert_channel_description)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    /**
     * Posts the "lot filling up" alert.
     *
     * The notification id is derived from [stationId] so a second alert for the
     * same station replaces the first instead of stacking.
     *
     * @return false when notifications are blocked (permission not granted, or
     *         the user switched them off in system settings), so callers can
     *         tell "posted" from "silently dropped".
     */
    @SuppressLint("MissingPermission") // guarded by canPostNotifications below
    fun postLotFillingAlert(
        context: Context,
        stationId: String,
        stationName: String,
        percentFull: Int,
        alternateName: String?,
        alternateSpacesFree: Int?,
        alternateDistanceKm: Double?
    ): Boolean {
        if (!canPostNotifications(context)) return false
        ensureChannel(context)

        val title = "$stationName is filling up"
        val body = buildBody(percentFull, alternateName, alternateSpacesFree, alternateDistanceKm)

        val notification = baseBuilder(context, stationId)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        NotificationManagerCompat.from(context).notify(stationId.hashCode(), notification)
        return true
    }

    /**
     * Renders a plain title/body notification. Used for messages that arrive
     * without our data payload - notably a "Send test message" from the
     * Firebase console, which is how topic subscriptions get verified.
     */
    @SuppressLint("MissingPermission") // guarded by canPostNotifications below
    fun postRawMessage(context: Context, title: String?, body: String?): Boolean {
        if (!canPostNotifications(context)) return false
        ensureChannel(context)

        val notification = baseBuilder(context, tag = title.orEmpty())
            .setContentTitle(title ?: context.getString(R.string.app_name))
            .setContentText(body.orEmpty())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.orEmpty()))
            .build()

        NotificationManagerCompat.from(context).notify(RAW_MESSAGE_NOTIFICATION_ID, notification)
        return true
    }

    fun canPostNotifications(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun baseBuilder(context: Context, tag: String): NotificationCompat.Builder =
        NotificationCompat.Builder(context, context.getString(R.string.alert_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, tag))

    private fun openAppIntent(context: Context, tag: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            tag.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildBody(
        percentFull: Int,
        alternateName: String?,
        alternateSpacesFree: Int?,
        alternateDistanceKm: Double?
    ): String {
        val lead = "$percentFull% full."
        if (alternateName == null) {
            return "$lead Nearby lots are busy too - leave a little earlier."
        }
        val spaces = alternateSpacesFree
            ?.let { "${NumberFormat.getIntegerInstance(Locale.getDefault()).format(it)} spaces free" }
        val distance = alternateDistanceKm?.let { String.format(Locale.getDefault(), "%.1f km away", it) }
        val detail = listOfNotNull(spaces, distance).joinToString(", ")
        return if (detail.isEmpty()) {
            "$lead Try $alternateName instead."
        } else {
            "$lead Try $alternateName instead - $detail."
        }
    }

    private const val RAW_MESSAGE_NOTIFICATION_ID = 2001
}
