package iti.grad.nutriscan.presentation.scan_history.state

sealed interface ScanHistoryEvent {
    object LoadMore : ScanHistoryEvent
    data class FilterSelected(val filter: HistoryFilter) : ScanHistoryEvent
    data class ItemClicked(val scanId: String) : ScanHistoryEvent
    object BackClicked : ScanHistoryEvent
    object RetryLoad : ScanHistoryEvent
    data class DateSelected(val dateMillis: Long?) : ScanHistoryEvent
    data class ShowDatePicker(val show: Boolean) : ScanHistoryEvent
    object ResetFilters : ScanHistoryEvent
}
