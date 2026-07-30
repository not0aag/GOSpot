package week11.st695922.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import week11.st695922.finalproject.data.AuthRepository
import week11.st695922.finalproject.data.UserProfileRepository
import week11.st695922.finalproject.model.Station
import week11.st695922.finalproject.model.UserProfile
import week11.st695922.finalproject.ui.state.UiState

class ProfileViewModel(
    private val uid: String,
    private val repository: UserProfileRepository = UserProfileRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    val profileState: StateFlow<UiState<UserProfile>> = repository.profileFlow(uid)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            if (profile != null) UiState.Success(profile) else UiState.Loading
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    fun changeHomeStation(station: Station) {
        viewModelScope.launch {
            repository.setHomeStation(uid, station)
                .onFailure { e -> _actionError.value = e.message ?: "Could not update home station" }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }
}
