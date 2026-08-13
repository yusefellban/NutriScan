package iti.grad.nutriscan.presentation.scan_history.state

import iti.grad.nutriscan.presentation.common.model.HistoryItemUiModel

sealed interface ScanHistoryEvent {
    object LoadMore : ScanHistoryEvent
    data class FilterSelected(val filter: HistoryFilter) : ScanHistoryEvent
    data class ItemClicked(val scanId: String) : ScanHistoryEvent
    object BackClicked : ScanHistoryEvent
    object RetryLoad : ScanHistoryEvent
    data class DateSelected(val dateMillis: Long?) : ScanHistoryEvent
    data class ShowDatePicker(val show: Boolean) : ScanHistoryEvent
    object ResetFilters : ScanHistoryEvent
    
    // Deletion
    data class OnHoldItem(val item: HistoryItemUiModel) : ScanHistoryEvent
    data object ConfirmDelete : ScanHistoryEvent
    data object DismissDeleteDialog : ScanHistoryEvent
    
    // ── Search ──
    data class SearchQueryChanged(val query: String) : ScanHistoryEvent
    data class SuggestionSelected(val suggestion: String) : ScanHistoryEvent
    object SearchSubmitted : ScanHistoryEvent
    object SearchCleared : ScanHistoryEvent
}
