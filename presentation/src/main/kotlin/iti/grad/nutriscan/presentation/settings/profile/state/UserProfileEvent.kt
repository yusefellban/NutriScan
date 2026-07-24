package iti.grad.nutriscan.presentation.settings.profile.state


/**
 * Events that the User Profile screen UI can emit to the ViewModel.
 */
sealed interface UserProfileEvent {
    data object EditProfileClicked : UserProfileEvent
    data object AddMemberClicked : UserProfileEvent
    data class FamilyMemberDetailClicked(val memberId: String) : UserProfileEvent
    data class FamilyMemberLongPressed(val memberId: String) : UserProfileEvent
    data object ConfirmRemoveMemberClicked : UserProfileEvent
    data object CancelRemoveMemberClicked : UserProfileEvent
    data object ScanHistoryClicked : UserProfileEvent
    data object NotificationsClicked : UserProfileEvent
    data object SettingsClicked : UserProfileEvent
    data class BottomNavTabClicked(val tab: BottomNavTab) : UserProfileEvent
    object DismissAlert : UserProfileEvent
    object RetryAction : UserProfileEvent
}
