package iti.grad.nutriscan.presentation.home.state

/**
 * One-shot side effects emitted by [HomeViewModel].
 *
 * Collected in the Composable and forwarded to navController — the ViewModel
 * never holds a reference to NavController (per AGENTS.md §5.3).
 */
sealed interface HomeEffect {
    data object NavigateToScan : HomeEffect
    data object NavigateToHistory : HomeEffect
    data object NavigateToNotifications : HomeEffect
    data object NavigateToEditProfile : HomeEffect
    data class NavigateToScanResult(val scanId: String) : HomeEffect
    data object NavigateToNews : HomeEffect
    data object NavigateToChatWithAi : HomeEffect
    data class NavigateToAccountPendingDeletion(val scheduledDeletionAt: String) : HomeEffect
}
