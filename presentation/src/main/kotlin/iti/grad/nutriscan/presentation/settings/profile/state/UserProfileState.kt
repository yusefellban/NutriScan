package iti.grad.nutriscan.presentation.settings.profile.state

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
    /** Cache-busting token for the avatar image — see [iti.grad.nutriscan.presentation.common.components.rememberAvatarImageRequest]. */
    val avatarUpdatedAt: String? = null,
    val streakDays: Int = 0,
    val familyMembers: ImmutableList<FamilyMemberUiModel> = persistentListOf(),
    /** Non-null while the remove-family-member confirmation dialog is showing. */
    val memberPendingDeletion: FamilyMemberUiModel? = null,
    /**
     * Server-computed Body Mass Index. Null until the first successful profile sync.
     * Read-only: the user cannot edit this value.
     */
    val bmi: Double? = null,
    /**
     * Server-computed Total Daily Energy Expenditure (kcal/day).
     * Read-only: the user cannot edit this value.
     */
    val tdee: Double? = null,
    /** True while the Add Family Member bottom sheet is presented. */
    val isAddMemberSheetVisible: Boolean = false,
    val editingMemberId: String? = null,
    val alertState: ProfileAlertState = ProfileAlertState.None
)
