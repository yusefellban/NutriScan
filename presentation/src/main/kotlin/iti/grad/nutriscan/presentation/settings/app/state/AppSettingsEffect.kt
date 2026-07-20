package iti.grad.nutriscan.presentation.settings.app.state

sealed interface AppSettingsEffect {
    data object NavigateBack : AppSettingsEffect
    data object NavigateToEditProfile : AppSettingsEffect
    data object NavigateToTermsAndConditions : AppSettingsEffect
    data object NavigateToHelp : AppSettingsEffect
    data object NavigateToLogin : AppSettingsEffect
}
