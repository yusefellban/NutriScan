package iti.grad.nutriscan.presentation.home.state

import iti.grad.nutriscan.presentation.common.model.BottomNavTab

/**
 * Events that the Home screen UI can emit to the ViewModel.
 */
sealed interface HomeEvent {
    data object ScanCardClicked : HomeEvent
    data object ViewAllHistoryClicked : HomeEvent
    data object NotificationClicked : HomeEvent
    data class BottomNavTabClicked(val tab: BottomNavTab) : HomeEvent
    data class HistoryItemClicked(val itemId: String) : HomeEvent
}
