package iti.grad.nutriscan.presentation.settings.app.state

import androidx.compose.runtime.Immutable
import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.domain.settings.model.ThemeMode

@Immutable
data class AppSettingsState(
    val selectedThemeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedLanguage: AppLanguage = AppLanguage.EN,
    val showLogoutConfirmDialog: Boolean = false,
)
