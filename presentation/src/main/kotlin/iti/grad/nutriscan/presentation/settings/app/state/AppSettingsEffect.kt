package iti.grad.nutriscan.presentation.settings.app.state

import iti.grad.nutriscan.domain.settings.model.AppLanguage

sealed interface AppSettingsEffect {
    data object NavigateBack : AppSettingsEffect
    data object NavigateToEditProfile : AppSettingsEffect
    data object NavigateToTermsAndConditions : AppSettingsEffect
    data object NavigateToHelp : AppSettingsEffect
    data object NavigateToLogin : AppSettingsEffect
    data class ApplyLocale(val language: AppLanguage) : AppSettingsEffect
}
