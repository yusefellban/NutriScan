package iti.grad.nutriscan.presentation.main.calories.stephistory.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.steps.history.model.StepHistoryPeriod
import iti.grad.nutriscan.domain.steps.history.usecase.GetStepHistoryUseCase
import iti.grad.nutriscan.presentation.main.calories.stephistory.state.StepHistoryEffect
import iti.grad.nutriscan.presentation.main.calories.stephistory.state.StepHistoryEvent
import iti.grad.nutriscan.presentation.main.calories.stephistory.state.StepHistoryState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StepHistoryViewModel @Inject constructor(
    private val getStepHistoryUseCase: GetStepHistoryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(StepHistoryState())
    val state: StateFlow<StepHistoryState> = _state.asStateFlow()

    private val _effect = Channel<StepHistoryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadData()
    }

    fun onEvent(event: StepHistoryEvent) {
        when (event) {
            is StepHistoryEvent.SelectPeriod -> {
                _state.update { it.copy(selectedPeriod = event.period) }
                loadData()
            }
            StepHistoryEvent.Retry -> {
                loadData()
            }
            StepHistoryEvent.NavigateBack -> {
                viewModelScope.launch {
                    _effect.send(StepHistoryEffect.NavigateBack)
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getStepHistoryUseCase(_state.value.selectedPeriod).onSuccess { summary ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        summary = summary,
                        error = null
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
                _effect.send(StepHistoryEffect.ShowError(error.message))
            }
        }
    }
}
