package iti.grad.nutriscan.presentation.settings.profile.add_member.state

/**
 * Events the Add Family Member bottom sheet UI can emit to
 * [iti.grad.nutriscan.presentation.settings.profile.add_member.viewmodel.AddFamilyMemberViewModel].
 */
sealed interface AddFamilyMemberEvent {
    data class NameChanged(val name: String) : AddFamilyMemberEvent
    data class ToggleDisease(val id: Int) : AddFamilyMemberEvent
    data class ToggleAllergy(val id: Int) : AddFamilyMemberEvent
    data object RetryLoadDiseases : AddFamilyMemberEvent
    data object RetryLoadAllergies : AddFamilyMemberEvent
    data object SaveClicked : AddFamilyMemberEvent
    data object DismissRequested : AddFamilyMemberEvent
}
