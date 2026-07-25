package iti.grad.nutriscan.presentation.settings.profile.state

import iti.grad.nutriscan.presentation.common.model.BottomNavTab

/**
 * One-shot side effects emitted by the User Profile ViewModel.
 *
 * Collected in the Composable and forwarded to navController — the ViewModel
 * never holds a reference to NavController (per AGENTS.md §5.3).
 */
sealed interface UserProfileEffect {
    data object NavigateToEditProfile : UserProfileEffect
    data object NavigateToScanHistory : UserProfileEffect
    data object NavigateToNotifications : UserProfileEffect
    data object NavigateToSettings : UserProfileEffect
    data class ShowError(val message: String) : UserProfileEffect
    data class NavigateToTab(val tab: BottomNavTab) : UserProfileEffect
}
