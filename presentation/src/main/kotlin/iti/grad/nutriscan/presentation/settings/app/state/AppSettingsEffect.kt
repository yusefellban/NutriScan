package iti.grad.nutriscan.presentation.settings.app.state

import androidx.annotation.StringRes
import iti.grad.nutriscan.domain.settings.model.AppLanguage

sealed interface AppSettingsEffect {
    data object NavigateBack : AppSettingsEffect
    data object NavigateToUserProfile : AppSettingsEffect
    data object NavigateToLogin : AppSettingsEffect
    data class ApplyLocale(val language: AppLanguage) : AppSettingsEffect
    data class ShowSnackbarRes(@StringRes val messageResId: Int) : AppSettingsEffect
}
