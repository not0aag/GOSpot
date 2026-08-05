package week11.st695922.finalproject.model

enum class CheckInEventType { CHECK_IN, CHECK_OUT }


data class CheckInEvent(
    val id: String = "",
    val stationId: String = "",
    val stationName: String = "",
    val type: String = CheckInEventType.CHECK_IN.name,
    val timestampMillis: Long = 0L,
    val auto: Boolean = false
)
