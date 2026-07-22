package iti.grad.nutriscan.presentation.auth.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.auth.usecase.GetOidcAuthConfigUseCase
import iti.grad.nutriscan.domain.auth.usecase.LoginWithEmailUseCase
import iti.grad.nutriscan.domain.auth.usecase.SaveGoogleLoginTokensUseCase
import iti.grad.nutriscan.presentation.auth.login.state.LoginEffect
import iti.grad.nutriscan.presentation.auth.login.state.LoginEvent
import iti.grad.nutriscan.presentation.auth.login.state.LoginState
import iti.grad.nutriscan.presentation.common.Validation
import iti.grad.nutriscan.presentation.common.model.SocialMediaProvider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import iti.grad.nutriscan.domain.user.repository.IUserRepository

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginWithEmailUseCase: LoginWithEmailUseCase,
    private val getOidcAuthConfigUseCase: GetOidcAuthConfigUseCase,
    private val saveGoogleLoginTokensUseCase: SaveGoogleLoginTokensUseCase,
    private val userRepository: IUserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> _state.update { 
                it.copy(email = event.value, emailErrorResId = null, genericErrorMessage = null) 
            }
            is LoginEvent.PasswordChanged -> _state.update { 
                it.copy(password = event.value, passwordErrorResId = null, genericErrorMessage = null) 
            }
            is LoginEvent.TogglePasswordVisibility -> _state.update { 
                it.copy(passwordVisible = !it.passwordVisible) 
            }
            is LoginEvent.SignInClicked -> handleSignIn()
            is LoginEvent.SocialLoginClicked -> handleSocialLogin(event)
            is LoginEvent.GoogleLoginSuccess -> handleGoogleLoginSuccess(event)
            is LoginEvent.GoogleLoginFailure -> {
                viewModelScope.launch {
                    _effect.send(LoginEffect.ShowErrorDialog(messageStr = event.error))
                }
            }
            is LoginEvent.SignUpClicked -> {
                viewModelScope.launch { _effect.send(LoginEffect.NavigateToRegister) }
            }
            is LoginEvent.ForgotPasswordClicked -> {
                viewModelScope.launch { _effect.send(LoginEffect.NavigateToForgotPassword) }
            }
        }
    }

    private fun handleSignIn() {
        val emailError = Validation.validateEmail(_state.value.email)
        val passwordError = Validation.validatePasswordLength(_state.value.password)

        if (emailError != null || passwordError != null) {
            _state.update {
                it.copy(
                    emailErrorResId = emailError,
                    passwordErrorResId = passwordError
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, genericErrorMessage = null) }
            
            val result = loginWithEmailUseCase(_state.value.email, _state.value.password)
            
            _state.update { it.copy(isLoading = false) }
            
            result.onSuccess {
                // Fetch profile immediately after login so we have it offline
                userRepository.fetchAndSyncProfile()
                _effect.send(LoginEffect.NavigateToHome)
            }.onFailure { error ->
                _state.update { it.copy(genericErrorMessage = error.message) }
                _effect.send(LoginEffect.ShowErrorDialog(messageStr = error.message ?: "Login failed"))
            }
        }
    }

    private fun handleSocialLogin(event: LoginEvent.SocialLoginClicked) {
        viewModelScope.launch {
            if (event.provider == SocialMediaProvider.GOOGLE) {
                val config = getOidcAuthConfigUseCase()
                _effect.send(LoginEffect.LaunchGoogleLogin(config))
            } else {
                _effect.send(LoginEffect.ShowErrorDialog(messageStr = "${event.provider.name} login not implemented yet"))
            }
        }
    }
    
    private fun handleGoogleLoginSuccess(event: LoginEvent.GoogleLoginSuccess) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = saveGoogleLoginTokensUseCase(event.authTokens)
            _state.update { it.copy(isLoading = false) }
            
            result.onSuccess {
                userRepository.fetchAndSyncProfile()
                _effect.send(LoginEffect.NavigateToHome)
            }.onFailure { error ->
                _effect.send(LoginEffect.ShowErrorDialog(messageStr = "Failed to save Google login tokens: ${error.message}"))
            }
        }
    }
}
