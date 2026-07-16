package iti.grad.nutriscan.presentation.onboarding.profile_setup.state

sealed interface HealthProfileSetupEvent {
    data class ToggleCondition(val condition: String) : HealthProfileSetupEvent
    data class ToggleAllergy(val allergy: String) : HealthProfileSetupEvent
    
    data object StartAddCustomCondition : HealthProfileSetupEvent
    data class UpdateCustomConditionInput(val input: String) : HealthProfileSetupEvent
    data object SubmitCustomCondition : HealthProfileSetupEvent
    data object CancelAddCustomCondition : HealthProfileSetupEvent

    data object StartAddCustomAllergy : HealthProfileSetupEvent
    data class UpdateCustomAllergyInput(val input: String) : HealthProfileSetupEvent
    data object SubmitCustomAllergy : HealthProfileSetupEvent
    data object CancelAddCustomAllergy : HealthProfileSetupEvent

    data object SaveProfile : HealthProfileSetupEvent
}
