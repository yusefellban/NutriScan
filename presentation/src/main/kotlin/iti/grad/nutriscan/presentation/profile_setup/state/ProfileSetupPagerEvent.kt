package iti.grad.nutriscan.presentation.profile_setup.state

sealed interface ProfileSetupPagerEvent {
    // Pager navigation
    data object NextClicked : ProfileSetupPagerEvent
    data object BackClicked : ProfileSetupPagerEvent
    data class PageChanged(val page: Int) : ProfileSetupPagerEvent

    // Page 1: Gender
    data class SelectGender(val gender: Gender) : ProfileSetupPagerEvent

    // Page 2: Date of Birth
    data class SelectDateOfBirth(val dateMillis: Long) : ProfileSetupPagerEvent

    // Page 3: Height
    data class SelectHeight(val heightCm: Int) : ProfileSetupPagerEvent

    // Page 4: Weight
    data class SelectWeight(val weightKg: Int) : ProfileSetupPagerEvent

    // Health Profile events (migrated from HealthProfileSetupEvent)
    data class ToggleDisease(val diseaseId: Int) : ProfileSetupPagerEvent
    data object RetryLoadDiseases : ProfileSetupPagerEvent

    data class ToggleAllergy(val allergyId: Int) : ProfileSetupPagerEvent
    data object RetryLoadAllergies : ProfileSetupPagerEvent

    data object SaveProfile : ProfileSetupPagerEvent
}
