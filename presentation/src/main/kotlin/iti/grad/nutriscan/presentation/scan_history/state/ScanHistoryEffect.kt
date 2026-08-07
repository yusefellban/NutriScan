package iti.grad.nutriscan.presentation.scan_history.state

sealed interface ScanHistoryEffect {
    object NavigateBack : ScanHistoryEffect
    data class NavigateToProductDetails(val scanId: String) : ScanHistoryEffect
}
