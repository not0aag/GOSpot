package week11.st695922.finalproject.model

enum class CheckInEventType { CHECK_IN, CHECK_OUT }

/**
 * Firestore document shape for `users/{userId}/events/{eventId}`, feeding the
 * Alerts screen. Written manually from [week11.st695922.finalproject.data.StationRepository]
 * check-in/check-out calls — this app has no background geofence trigger
 * (not covered by the course material), so every event here is user-initiated.
 */
data class CheckInEvent(
    val id: String = "",
    val stationId: String = "",
    val stationName: String = "",
    val type: String = CheckInEventType.CHECK_IN.name,
    val timestampMillis: Long = 0L
)
