package iti.grad.nutriscan.presentation.settings.app.state

import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.domain.settings.model.ThemeMode

sealed interface AppSettingsEvent {
    data object BackClicked : AppSettingsEvent
    data object ProfileSettingsClicked : AppSettingsEvent
    data class ThemeModeSelected(val mode: ThemeMode) : AppSettingsEvent
    data class LanguageSelected(val language: AppLanguage) : AppSettingsEvent
    data object TermsAndConditionsClicked : AppSettingsEvent
    data object HelpClicked : AppSettingsEvent
    data object LogoutClicked : AppSettingsEvent
    data object LogoutConfirmed : AppSettingsEvent
    data object LogoutDismissed : AppSettingsEvent
}
