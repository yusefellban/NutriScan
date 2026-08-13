package iti.grad.nutriscan.presentation.scan_history.state

sealed interface ScanHistoryEffect {
    object NavigateBack : ScanHistoryEffect
    data class NavigateToProductDetails(val scanId: String) : ScanHistoryEffect
    data class ShowSuccessMessage(val messageRes: Int) : ScanHistoryEffect
    data class ShowErrorMessage(val messageRes: Int) : ScanHistoryEffect
}
