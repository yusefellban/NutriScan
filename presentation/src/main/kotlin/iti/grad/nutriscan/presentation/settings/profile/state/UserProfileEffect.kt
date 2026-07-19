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
    data class NavigateToFamilyMemberDetail(val memberId: String) : UserProfileEffect
    data object NavigateToScanHistory : UserProfileEffect
    data object NavigateToNotifications : UserProfileEffect
    data object NavigateToSettings : UserProfileEffect
    /** Switching to a different bottom-nav tab; PROFILE itself is a no-op since we're already here. */
    data class NavigateToTab(val tab: BottomNavTab) : UserProfileEffect
}
