package iti.grad.nutriscan.presentation.settings.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.settings.model.AppLanguage
import iti.grad.nutriscan.domain.settings.model.ThemeMode
import iti.grad.nutriscan.domain.settings.usecase.GetLanguageUseCase
import iti.grad.nutriscan.domain.settings.usecase.GetThemeModeUseCase
import iti.grad.nutriscan.domain.settings.usecase.SetLanguageUseCase
import iti.grad.nutriscan.domain.settings.usecase.SetThemeModeUseCase
import iti.grad.nutriscan.presentation.settings.app.state.AppSettingsEffect
import iti.grad.nutriscan.presentation.settings.app.state.AppSettingsEvent
import iti.grad.nutriscan.presentation.settings.app.state.AppSettingsState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val getThemeModeUseCase: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val getLanguageUseCase: GetLanguageUseCase,
    private val setLanguageUseCase: SetLanguageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AppSettingsState())
    val state: StateFlow<AppSettingsState> = _state.asStateFlow()

    private val _effect = Channel<AppSettingsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadSettings()
    }

    fun onEvent(event: AppSettingsEvent) {
        when (event) {
            AppSettingsEvent.BackClicked -> navigate(AppSettingsEffect.NavigateBack)
            AppSettingsEvent.ProfileSettingsClicked -> navigate(AppSettingsEffect.NavigateToEditProfile)
            is AppSettingsEvent.ThemeModeSelected -> selectThemeMode(event.mode)
            is AppSettingsEvent.LanguageSelected -> selectLanguage(event.language)
            AppSettingsEvent.TermsAndConditionsClicked -> navigate(AppSettingsEffect.NavigateToTermsAndConditions)
            AppSettingsEvent.HelpClicked -> navigate(AppSettingsEffect.NavigateToHelp)
            AppSettingsEvent.LogoutClicked -> _state.update { it.copy(showLogoutConfirmDialog = true) }
            AppSettingsEvent.LogoutDismissed -> _state.update { it.copy(showLogoutConfirmDialog = false) }
            AppSettingsEvent.LogoutConfirmed -> confirmLogout()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val themeMode = getThemeModeUseCase()
            val language = getLanguageUseCase()
            _state.update { it.copy(selectedThemeMode = themeMode, selectedLanguage = language) }
        }
    }

    private fun selectThemeMode(mode: ThemeMode) {
        _state.update { it.copy(selectedThemeMode = mode) }
        viewModelScope.launch { setThemeModeUseCase(mode) }
    }

    private fun selectLanguage(language: AppLanguage) {
        _state.update { it.copy(selectedLanguage = language) }
        viewModelScope.launch {
            setLanguageUseCase(language)
            _effect.send(AppSettingsEffect.ApplyLocale(language))
        }
    }

    private fun confirmLogout() {
        _state.update { it.copy(showLogoutConfirmDialog = false) }
        navigate(AppSettingsEffect.NavigateToLogin)
    }

    private fun navigate(effect: AppSettingsEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
