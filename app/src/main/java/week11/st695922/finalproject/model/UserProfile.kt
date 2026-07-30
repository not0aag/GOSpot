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
    val homeStationName: String = ""
)
