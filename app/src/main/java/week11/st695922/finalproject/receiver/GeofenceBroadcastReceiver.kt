package week11.st695922.finalproject.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Geofence broadcast received")
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
