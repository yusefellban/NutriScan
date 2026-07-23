package iti.grad.nutriscan.presentation.settings.profile.state

import iti.grad.nutriscan.presentation.common.model.BottomNavTab
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable UI state for the User Profile screen.
 *
 * All collections use `kotlinx.collections.immutable` to ensure Compose
 * stability and avoid unnecessary recompositions.
 */
data class UserProfileState(
    val userName: String = "",
    val avatarUrl: String? = null,
    val streakDays: Int = 0,
    val familyMembers: ImmutableList<FamilyMemberUiModel> = persistentListOf(),
    val selectedTab: BottomNavTab = BottomNavTab.PROFILE,
    /** Non-null while the remove-family-member confirmation dialog is showing. */
    val memberPendingDeletion: FamilyMemberUiModel? = null,
    val alertState: ProfileAlertState = ProfileAlertState.None
)
