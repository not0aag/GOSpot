package week11.st695922.finalproject.model

/**
 * [LOT_WARNING] rows come from the threshold watcher rather than a user action,
 * so they carry [CheckInEvent.percentFull] / [CheckInEvent.alternateName]
 * instead of describing a parking-space change.
 */
enum class CheckInEventType { CHECK_IN, CHECK_OUT, LOT_WARNING }


data class CheckInEvent(
    val id: String = "",
    val stationId: String = "",
    val stationName: String = "",
    val type: String = CheckInEventType.CHECK_IN.name,
    val timestampMillis: Long = 0L,
    val auto: Boolean = false,
    /** Only meaningful for [CheckInEventType.LOT_WARNING]. */
    val percentFull: Int = 0,
    /** Suggested alternate lot on a warning, or blank when every lot was busy. */
    val alternateName: String = ""
)
