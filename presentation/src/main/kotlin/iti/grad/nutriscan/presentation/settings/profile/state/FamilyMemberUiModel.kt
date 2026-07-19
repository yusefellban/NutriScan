package iti.grad.nutriscan.presentation.settings.profile.state

/**
 * A single family member shown in the Profile screen's Family Members row.
 */
data class FamilyMemberUiModel(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
)
