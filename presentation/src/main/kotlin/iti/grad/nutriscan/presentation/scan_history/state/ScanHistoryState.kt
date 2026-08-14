package iti.grad.nutriscan.presentation.scan_history.state

import iti.grad.nutriscan.presentation.common.model.AppErrorType
import iti.grad.nutriscan.presentation.common.model.HistoryItemUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class HistoryFilter {
    ALL,
    SAFE,
    CAUTION,
    UNSAFE
}

data class ScanHistoryState(
    val allHistoryItems: List<HistoryItemUiModel> = emptyList(),
    val displayedHistoryItems: ImmutableList<HistoryItemUiModel> = persistentListOf(),
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val isLoading: Boolean = false,
    val isPaginationLoading: Boolean = false,
    val error: String? = null,
    val errorType: AppErrorType = AppErrorType.UNKNOWN,
    val page: Int = 0,
    val isLastPage: Boolean = false,
    val selectedDate: String? = null,
    val showDatePicker: Boolean = false,
    // ── Search ──
    val searchQuery: String = "",
    val committedQuery: String = "",  // the query that was actually fetched (via suggestion or submit)
    val suggestions: ImmutableList<String> = persistentListOf(),
    val isSuggestionsLoading: Boolean = false,
    val isSearchActive: Boolean = false,

    // Dialog state for deletion
    val itemToDelete: HistoryItemUiModel? = null,
)
