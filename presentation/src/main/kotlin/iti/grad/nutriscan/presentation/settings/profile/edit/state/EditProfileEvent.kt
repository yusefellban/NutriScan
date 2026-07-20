package iti.grad.nutriscan.presentation.settings.profile.edit.state

sealed interface EditProfileEvent {
    data class UpdateName(val name: String) : EditProfileEvent
    data class UpdateUsername(val username: String) : EditProfileEvent
    data class UpdateEmail(val email: String) : EditProfileEvent
    data class UpdatePassword(val password: String) : EditProfileEvent
    data class ToggleCondition(val condition: String) : EditProfileEvent
    data class ToggleAllergy(val allergy: String) : EditProfileEvent
    object StartAddCustomCondition : EditProfileEvent
    data class UpdateCustomConditionInput(val value: String) : EditProfileEvent
    object SubmitCustomCondition : EditProfileEvent
    object CancelAddCustomCondition : EditProfileEvent
    object StartAddCustomAllergy : EditProfileEvent
    data class UpdateCustomAllergyInput(val value: String) : EditProfileEvent
    object SubmitCustomAllergy : EditProfileEvent
    object CancelAddCustomAllergy : EditProfileEvent
    object SaveClicked : EditProfileEvent
    object ConfirmSave : EditProfileEvent
    object DismissSaveConfirmation : EditProfileEvent
    object BackClicked : EditProfileEvent
    data class SelectAvatar(val avatarUrl: String) : EditProfileEvent
}
