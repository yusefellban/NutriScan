package iti.grad.nutriscan.presentation.calories_history.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.LocalDate

import iti.grad.nutriscan.presentation.common.model.AppErrorType

@Immutable
data class CaloriesHistoryState(
    val isLoading: Boolean = false,
    val entries: ImmutableList<CaloriesHistoryDayUiModel> = persistentListOf(),
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorType: AppErrorType? = null,
    val showDatePicker: Boolean = false,
    val selectedDate: LocalDate? = null,
)
