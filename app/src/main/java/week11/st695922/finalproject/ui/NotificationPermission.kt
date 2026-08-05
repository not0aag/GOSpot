package week11.st695922.finalproject.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Whether the user has granted POST_NOTIFICATIONS.
 *
 * minSdk is 36, so this permission is always a runtime grant - unlike
 * ACCESS_BACKGROUND_LOCATION in GoSpotApp, there is no older API level to
 * branch on here.
 *
 * Kept in its own file rather than beside the location helpers so the alerts
 * work does not collide with the geofencing work in GoSpotApp.kt.
 */
fun hasNotificationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
