package week11.st695922.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import week11.st695922.finalproject.data.AuthRepository
import week11.st695922.finalproject.ui.state.AuthUiState

/**
 * ViewModel calls the Repository's suspend functions inside viewModelScope.launch,
 * sets Loading first, then folds the Result into Success/Error (Week 6.1, Slides 9-11, 18).
 * Compose screens only call these functions and collect the StateFlows below.
 */
// @JvmOverloads generates a true zero-arg JVM constructor, which the default
// reflection-based ViewModelProvider.Factory used by viewModel() requires -
// a Kotlin default-valued constructor alone does not produce one.
class AuthViewModel @JvmOverloads constructor(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _formError = MutableStateFlow<String?>(null)
    val formError: StateFlow<String?> = _formError.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    init {
        viewModelScope.launch {
            repository.authStateFlow().collect { uid ->
                _authState.value = if (uid != null) {
                    AuthUiState.Authenticated(uid)
                } else {
                    AuthUiState.AuthRequired
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _formError.value = null
            val result = repository.signIn(email, password)
            _isSubmitting.value = false
            result.onFailure { e -> _formError.value = e.message ?: "Sign in failed" }
        }
    }

    fun signUp(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _formError.value = null
            val result = repository.signUp(fullName, email, password)
            _isSubmitting.value = false
            result.onFailure { e -> _formError.value = e.message ?: "Sign up failed" }
        }
    }

    fun signOut() {
        repository.signOut()
    }

    fun clearFormError() {
        _formError.value = null
    }
}
