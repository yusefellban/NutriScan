package iti.grad.nutriscan.presentation.profile_setup.state

sealed interface ProfileSetupPagerEvent {
    // Pager navigation
    data object NextClicked : ProfileSetupPagerEvent
    data object BackClicked : ProfileSetupPagerEvent
    data class PageChanged(val page: Int) : ProfileSetupPagerEvent

    // Page 1: Gender
    data class SelectGender(val gender: Gender) : ProfileSetupPagerEvent

    // Health Profile events (migrated from HealthProfileSetupEvent)
    data class ToggleCondition(val condition: String) : ProfileSetupPagerEvent
    data class ToggleAllergy(val allergy: String) : ProfileSetupPagerEvent

    data object StartAddCustomCondition : ProfileSetupPagerEvent
    data class UpdateCustomConditionInput(val input: String) : ProfileSetupPagerEvent
    data object SubmitCustomCondition : ProfileSetupPagerEvent
    data object CancelAddCustomCondition : ProfileSetupPagerEvent

    data object StartAddCustomAllergy : ProfileSetupPagerEvent
    data class UpdateCustomAllergyInput(val input: String) : ProfileSetupPagerEvent
    data object SubmitCustomAllergy : ProfileSetupPagerEvent
    data object CancelAddCustomAllergy : ProfileSetupPagerEvent

    data object SaveProfile : ProfileSetupPagerEvent
}
