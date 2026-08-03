package iti.grad.nutriscan.presentation.calories_history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.dailytracking.model.DailyTrackingSummary
import iti.grad.nutriscan.domain.dailytracking.usecase.GetCaloriesHistoryUseCase
import iti.grad.nutriscan.domain.dailytracking.usecase.GetDayTrackingByDateUseCase
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryDayUiModel
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryEffect
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryEvent
import iti.grad.nutriscan.presentation.calories_history.state.CaloriesHistoryState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val PAGE_SIZE = 10

@HiltViewModel
class CaloriesHistoryViewModel @Inject constructor(
    private val getCaloriesHistory: GetCaloriesHistoryUseCase,
    private val getDayByDate: GetDayTrackingByDateUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CaloriesHistoryState())
    val state: StateFlow<CaloriesHistoryState> = _state.asStateFlow()

    private val _effect = Channel<CaloriesHistoryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("d-M-yyyy")

    init {
        loadFirstPage()
    }

    fun onEvent(event: CaloriesHistoryEvent) {
        when (event) {
            is CaloriesHistoryEvent.NavigateBack -> viewModelScope.launch {
                _effect.send(CaloriesHistoryEffect.NavigateBack)
            }
            is CaloriesHistoryEvent.CalendarClicked -> {
                _state.update { it.copy(showDatePicker = true) }
            }
            is CaloriesHistoryEvent.DateSelected -> {
                _state.update { it.copy(showDatePicker = false) }
                loadSingleDay(event.date)
            }
            is CaloriesHistoryEvent.ClearDateFilter -> {
                _state.update { it.copy(showDatePicker = false, selectedDate = null) }
                loadFirstPage()
            }
            is CaloriesHistoryEvent.DismissDatePicker -> {
                _state.update { it.copy(showDatePicker = false) }
            }
            is CaloriesHistoryEvent.LoadMore -> loadNextPage()
            is CaloriesHistoryEvent.Retry -> {
                val selected = _state.value.selectedDate
                if (selected != null) loadSingleDay(selected) else loadFirstPage()
            }
        }
    }

    private fun loadFirstPage() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, selectedDate = null) }
            getCaloriesHistory(page = 0, size = PAGE_SIZE)
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            entries = page.entries.map { s -> s.toUiModel() }.toImmutableList(),
                            currentPage = page.currentPage,
                            isLastPage = page.isLastPage,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unknown error",
                        )
                    }
                }
        }
    }

    private fun loadNextPage() {
        val current = _state.value
        if (current.selectedDate != null) return // no pagination when filtering by date
        if (current.isLastPage || current.isLoadingMore || current.isLoading) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            val nextPage = current.currentPage + 1
            getCaloriesHistory(page = nextPage, size = PAGE_SIZE)
                .onSuccess { page ->
                    _state.update {
                        val merged = (it.entries + page.entries.map { s -> s.toUiModel() }).toImmutableList()
                        it.copy(
                            isLoadingMore = false,
                            entries = merged,
                            currentPage = page.currentPage,
                            isLastPage = page.isLastPage,
                        )
                    }
                }
                .onFailure {
                    // Pagination failure — silently drop, the user can scroll again to retry
                    _state.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    private fun loadSingleDay(date: LocalDate) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    selectedDate = date,
                    isLastPage = true,
                )
            }
            getDayByDate(date)
                .onSuccess { summary ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            entries = persistentListOf(summary.toUiModel()),
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            entries = persistentListOf(),
                            errorMessage = error.message ?: "Unknown error",
                        )
                    }
                }
        }
    }

    private fun DailyTrackingSummary.toUiModel() =
        CaloriesHistoryDayUiModel(
            dateLabel = date.format(dateFormatter),
            totalMealsKcal = totalMealKcal,
            waterCups = waterCnt,
            waterTarget = targetWaterCnt,
            steps = stepsCnt,
            stepsKcal = stepsKcal,
            exerciseMinutes = exerciseMinutes,
            exerciseKcal = exerciseKcal,
        )
}
