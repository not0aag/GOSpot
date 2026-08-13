package week11.st695922.finalproject.model

/**
 * Firestore document shape for `users/{userId}`, mirroring the per-user
 * document pattern from Week 6.2, Slide 6.
 */
data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val homeStationId: String = "",
    val homeStationName: String = "",
    /** Whether this user wants "lot filling up" alerts for their home station. */
    val alertsEnabled: Boolean = false,
    /** Opt-in preference for background geofence check-in and check-out. */
    val automaticCheckInEnabled: Boolean = false,
    /** The single station this user is currently checked into, if any. */
    val activeStationId: String = "",
    /** MANUAL or AUTOMATIC. Blank when there is no active check-in. */
    val activeCheckInSource: String = "",
    /** Whether the active check-in actually incremented the station occupancy. */
    val activeOccupancyApplied: Boolean = false,
    /** Lifetime successful check-ins. Clearing the Alerts event list does not reset this value. */
    val totalCheckIns: Long = 0,
    /**
     * This device's FCM registration token. Stored so alerts can later be sent
     * to a specific device rather than fanned out over a station topic.
     */
    val fcmToken: String = ""
)
