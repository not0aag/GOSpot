package week11.st695922.finalproject.model

internal enum class CheckInOperation {
    NO_OP,
    CHECK_IN,
    CHECK_OUT,
    SWITCH_STATION
}

/** Pure transition rules shared by manual actions and background geofences. */
internal object CheckInTransitionPolicy {
    fun decide(
        activeStationId: String,
        targetStationId: String,
        wantsCheckIn: Boolean,
        automatic: Boolean,
        automaticEnabled: Boolean
    ): CheckInOperation {
        if (automatic && !automaticEnabled) return CheckInOperation.NO_OP

        return if (wantsCheckIn) {
            when {
                activeStationId == targetStationId -> CheckInOperation.NO_OP
                activeStationId.isBlank() -> CheckInOperation.CHECK_IN
                else -> CheckInOperation.SWITCH_STATION
            }
        } else {
            if (activeStationId == targetStationId) {
                CheckInOperation.CHECK_OUT
            } else {
                CheckInOperation.NO_OP
            }
        }
    }
}
