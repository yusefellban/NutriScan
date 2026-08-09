package iti.grad.nutriscan.presentation.auth.login.viewmodel

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import iti.grad.nutriscan.presentation.common.model.SocialMediaProvider
import androidx.lifecycle.ViewModel
import iti.grad.nutriscan.presentation.common.state.AuthAlertState.Warning
import iti.grad.nutriscan.presentation.common.model.UiText.StringResource
import iti.grad.nutriscan.presentation.auth.login.state.LoginEffect
import iti.grad.nutriscan.presentation.common.state.AuthAlertState.None
import iti.grad.presentation.R
import iti.grad.nutriscan.presentation.common.state.AuthAlertState.InternetError
import iti.grad.nutriscan.domain.user.usecase.FetchAndSyncUserDataUseCase
import iti.grad.nutriscan.domain.common.model.DomainException
import iti.grad.nutriscan.presentation.auth.login.state.LoginState
import iti.grad.nutriscan.presentation.auth.login.state.LoginEvent
import iti.grad.nutriscan.presentation.common.model.UiText.DynamicString
import iti.grad.nutriscan.domain.auth.usecase.SaveGoogleLoginTokensUseCase
import iti.grad.nutriscan.domain.auth.usecase.GetOidcAuthConfigUseCase
import iti.grad.nutriscan.presentation.common.Validation
import iti.grad.nutriscan.presentation.common.state.AuthAlertState.Error
import iti.grad.nutriscan.domain.auth.usecase.LoginWithEmailUseCase
import iti.grad.nutriscan.domain.user.usecase.CheckIfProfileSetupUseCase
import iti.grad.nutriscan.domain.user.model.AccountPendingDeletionException

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginWithEmailUseCase: LoginWithEmailUseCase,
    private val getOidcAuthConfigUseCase: GetOidcAuthConfigUseCase,
    private val saveGoogleLoginTokensUseCase: SaveGoogleLoginTokensUseCase,
    private val fetchAndSyncUserDataUseCase: FetchAndSyncUserDataUseCase,
    private val checkIfProfileSetupUseCase: CheckIfProfileSetupUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> _state.update { 
                it.copy(email = event.value, emailErrorResId = null, alertState = None) 
            }
            is LoginEvent.PasswordChanged -> _state.update { 
                it.copy(password = event.value, passwordErrorResId = null, alertState = None) 
            }
            is LoginEvent.TogglePasswordVisibility -> _state.update { 
                it.copy(passwordVisible = !it.passwordVisible) 
            }
            is LoginEvent.SignInClicked -> handleSignIn()
            is LoginEvent.SocialLoginClicked -> handleSocialLogin(event)
            is LoginEvent.GoogleLoginSuccess -> handleGoogleLoginSuccess(event)
            is LoginEvent.GoogleLoginFailure -> {
                _state.update { it.copy(alertState = Error(message = DynamicString(event.error))) }
            }
            is LoginEvent.SignUpClicked -> {
                viewModelScope.launch { _effect.send(LoginEffect.NavigateToRegister) }
            }
            is LoginEvent.ForgotPasswordClicked -> {
                viewModelScope.launch { _effect.send(LoginEffect.NavigateToForgotPassword) }
            }
            is LoginEvent.DismissAlert -> _state.update { it.copy(alertState = None) }
            is LoginEvent.RetryAction -> handleSignIn()
        }
    }

    private fun handleSignIn() {
        val emailError = Validation.validateEmail(_state.value.email)
        val passwordError = Validation.validatePasswordLength(_state.value.password)

        if (emailError != null || passwordError != null) {
            _state.update {
                it.copy(
                    emailErrorResId = emailError,
                    passwordErrorResId = passwordError,
                    alertState = Warning(message = StringResource(R.string.error_validation_fields))
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, alertState = None) }
            
            val result = loginWithEmailUseCase(_state.value.email, _state.value.password)
            
            result.onSuccess {
                val syncResult = fetchAndSyncUserDataUseCase()
                _state.update { it.copy(isLoading = false) }

                val pendingDeletion = syncResult.exceptionOrNull() as? AccountPendingDeletionException
                if (pendingDeletion != null) {
                    _effect.send(LoginEffect.NavigateToAccountPendingDeletion(pendingDeletion.scheduledDeletionAt))
                    return@launch
                }

                if (checkIfProfileSetupUseCase()) {
                    _effect.send(LoginEffect.NavigateToHome)
                } else {
                    _effect.send(LoginEffect.NavigateToProfileSetup)
                }
            }.onFailure { error ->
                val newAlertState = when (error) {
                    is DomainException.NetworkException -> InternetError
                    is DomainException.UnauthorizedException -> Error(message = StringResource(R.string.error_invalid_credentials))
                    is DomainException.ServerException -> Error(message = StringResource(R.string.error_server_down))
                    else -> Error(message = StringResource(R.string.error_login_failed))
                }
                _state.update { it.copy(isLoading = false, alertState = newAlertState) }
            }
        }
    }

    private fun handleSocialLogin(event: LoginEvent.SocialLoginClicked) {
        viewModelScope.launch {
            if (event.provider == SocialMediaProvider.GOOGLE) {
                val config = getOidcAuthConfigUseCase()
                _effect.send(LoginEffect.LaunchGoogleLogin(config))
            } else {
                _state.update { it.copy(alertState = Error(message = DynamicString("${event.provider.name} login not implemented yet"))) }
            }
        }
    }
    
    private fun handleGoogleLoginSuccess(event: LoginEvent.GoogleLoginSuccess) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = saveGoogleLoginTokensUseCase(event.authTokens)
            
            result.onSuccess {
                val syncResult = fetchAndSyncUserDataUseCase()
                _state.update { it.copy(isLoading = false) }

                val pendingDeletion = syncResult.exceptionOrNull() as? AccountPendingDeletionException
                if (pendingDeletion != null) {
                    _effect.send(LoginEffect.NavigateToAccountPendingDeletion(pendingDeletion.scheduledDeletionAt))
                    return@launch
                }

                if (checkIfProfileSetupUseCase()) {
                    _effect.send(LoginEffect.NavigateToHome)
                } else {
                    _effect.send(LoginEffect.NavigateToProfileSetup)
                }
            }.onFailure { error ->
                _state.update { 
                    it.copy(
                        isLoading = false,
                        alertState = Error(message = StringResource(R.string.error_google_login_failed))
                    )
                }
            }
        }
    }
}
