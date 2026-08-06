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
import iti.grad.nutriscan.domain.auth.usecase.LogoutUseCase
import iti.grad.nutriscan.domain.user.usecase.DeleteAccountUseCase
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
import timber.log.Timber
import javax.inject.Inject

import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.flow.collectLatest

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val getThemeModeUseCase: GetThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val getLanguageUseCase: GetLanguageUseCase,
    private val setLanguageUseCase: SetLanguageUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val userRepository: IUserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AppSettingsState())
    val state: StateFlow<AppSettingsState> = _state.asStateFlow()

    private val _effect = Channel<AppSettingsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        loadSettings()
        viewModelScope.launch {
            userRepository.getUserData().collectLatest { user ->
                if (user != null) {
                    _state.update {
                        it.copy(
                            fullName = "${user.firstName} ${user.lastName ?: ""}".trim(),
                            email = user.email ?: ""
                        )
                    }
                }
            }
        }
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
            AppSettingsEvent.DeleteAccountClicked -> _state.update { it.copy(showDeleteAccountConfirmDialog = true) }
            AppSettingsEvent.DeleteAccountDismissed -> _state.update { it.copy(showDeleteAccountConfirmDialog = false, deleteAccountError = null) }
            AppSettingsEvent.DeleteAccountConfirmed -> confirmDeleteAccount()
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
        viewModelScope.launch { setLanguageUseCase(language) }
    }

    private fun confirmLogout() {
        _state.update { it.copy(showLogoutConfirmDialog = false) }
        viewModelScope.launch {
            logoutUseCase()
            navigate(AppSettingsEffect.NavigateToLogin)
        }
    }

    private fun confirmDeleteAccount() {
        _state.update { it.copy(
            showDeleteAccountConfirmDialog = false,
            isDeletingAccount = true,
            deleteAccountError = null,
        ) }
        viewModelScope.launch {
            deleteAccountUseCase()
                .onSuccess {
                    // Account scheduled for deletion — log out immediately so the user
                    // re-authenticates and hits the AccountPendingDeletion screen on next login.
                    logoutUseCase()
                    navigate(AppSettingsEffect.AccountDeleted)
                }
                .onFailure { error ->
                    Timber.e(error, "deleteAccount failed")
                    _state.update { it.copy(
                        isDeletingAccount = false,
                        deleteAccountError = error.message ?: "Failed to delete account.",
                    ) }
                }
        }
    }

    private fun navigate(effect: AppSettingsEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
