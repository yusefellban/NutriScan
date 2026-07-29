package iti.grad.nutriscan.presentation.home.state


/**
 * Events that the Home screen UI can emit to the ViewModel.
 */
sealed interface HomeEvent {
    data object ScanCardClicked : HomeEvent
    data object ViewAllHistoryClicked : HomeEvent
    data object NotificationClicked : HomeEvent
    data object AvatarClicked : HomeEvent
    data class HistoryItemClicked(val itemId: String) : HomeEvent
    data object HealthNewsClicked : HomeEvent
    data object ChatWithAiClicked : HomeEvent
    data object RetryLoadHistory : HomeEvent
    data object RefreshHistorySilently : HomeEvent
}
