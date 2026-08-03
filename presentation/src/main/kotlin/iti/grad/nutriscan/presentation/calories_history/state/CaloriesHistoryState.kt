package iti.grad.nutriscan.presentation.calories_history.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class CaloriesHistoryState(
    val isLoading: Boolean = false,
    val entries: ImmutableList<CaloriesHistoryDayUiModel> = persistentListOf(),
)
