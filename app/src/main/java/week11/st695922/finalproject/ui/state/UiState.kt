package week11.st695922.finalproject.ui.state

/**
 * Generic Loading/Success/Error wrapper (Week 6.1, Slide 18) used for any
 * ViewModel-exposed StateFlow that loads data from a Repository.
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

/**
 * App-level auth state (Week 6.2, Slide 11): decides whether the root
 * composable shows the auth flow or the signed-in app content.
 */
sealed class AuthUiState {
    data object Loading : AuthUiState()
    data object AuthRequired : AuthUiState()
    data class Authenticated(val uid: String) : AuthUiState()
}
