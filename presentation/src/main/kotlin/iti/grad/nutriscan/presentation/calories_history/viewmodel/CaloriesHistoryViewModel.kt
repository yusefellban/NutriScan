package iti.grad.nutriscan.presentation.calories_history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryDayUiModel
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryEffect
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryEvent
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaloriesHistoryViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(CaloriesHistoryState())
    val state: StateFlow<CaloriesHistoryState> = _state.asStateFlow()

    private val _effect = Channel<CaloriesHistoryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadMockedData()
    }

    fun onEvent(event: CaloriesHistoryEvent) {
        when (event) {
            is CaloriesHistoryEvent.NavigateBack -> viewModelScope.launch {
                _effect.send(CaloriesHistoryEffect.NavigateBack)
            }
            is CaloriesHistoryEvent.CalendarClicked -> {
                // Placeholder — date-picker will be wired in a future phase
            }
        }
    }

    private fun loadMockedData() {
        // Mocked data matching the design screenshot.
        // Will be replaced with real repository calls in the backend-integration phase.
        _state.value = CaloriesHistoryState(
            isLoading = false,
            entries = persistentListOf(
                CaloriesHistoryDayUiModel(
                    dateLabel = "23-7-2026",
                    totalMealsKcal = 2400,
                    waterCups = 7,
                    waterTarget = 8,
                    steps = 10000,
                    stepsKcal = 415,
                    exerciseMinutes = 46,
                    exerciseKcal = 2009,
                ),
                CaloriesHistoryDayUiModel(
                    dateLabel = "22-7-2026",
                    totalMealsKcal = 2100,
                    waterCups = 6,
                    waterTarget = 8,
                    steps = 8500,
                    stepsKcal = 352,
                    exerciseMinutes = 30,
                    exerciseKcal = 1540,
                ),
                CaloriesHistoryDayUiModel(
                    dateLabel = "21-7-2026",
                    totalMealsKcal = 2650,
                    waterCups = 8,
                    waterTarget = 8,
                    steps = 12300,
                    stepsKcal = 510,
                    exerciseMinutes = 60,
                    exerciseKcal = 2400,
                ),
                CaloriesHistoryDayUiModel(
                    dateLabel = "20-7-2026",
                    totalMealsKcal = 1900,
                    waterCups = 5,
                    waterTarget = 8,
                    steps = 7200,
                    stepsKcal = 298,
                    exerciseMinutes = 20,
                    exerciseKcal = 980,
                ),
            ),
        )
    }
}
