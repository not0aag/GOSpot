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
import week11.st695922.finalproject.data.CheckInEventRepository
import week11.st695922.finalproject.model.CheckInEvent
import week11.st695922.finalproject.ui.state.UiState

class AlertViewModel(
    private val uid: String,
    private val repository: CheckInEventRepository = CheckInEventRepository()
) : ViewModel() {

    val eventsState: StateFlow<UiState<List<CheckInEvent>>> = repository.eventsFlow(uid)
        .map<List<CheckInEvent>, UiState<List<CheckInEvent>>> { UiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll(uid)
                .onFailure { e -> _actionError.value = e.message ?: "Could not clear alerts" }
        }
    }
}
