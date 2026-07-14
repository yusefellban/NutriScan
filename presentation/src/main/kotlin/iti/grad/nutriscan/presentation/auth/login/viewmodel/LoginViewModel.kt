package iti.grad.nutriscan.presentation.auth.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.presentation.auth.login.state.LoginEffect
import iti.grad.nutriscan.presentation.auth.login.state.LoginEvent
import iti.grad.nutriscan.presentation.auth.login.state.LoginState
import iti.grad.nutriscan.presentation.common.Validation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.delay

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> _state.update { 
                it.copy(email = event.value, emailErrorResId = null) 
            }
            is LoginEvent.PasswordChanged -> _state.update { 
                it.copy(password = event.value, passwordErrorResId = null) 
            }
            is LoginEvent.TogglePasswordVisibility -> _state.update { 
                it.copy(passwordVisible = !it.passwordVisible) 
            }
            is LoginEvent.SignInClicked -> handleSignIn()
            is LoginEvent.SocialLoginClicked -> handleSocialLogin(event)
            is LoginEvent.SignUpClicked -> {
                viewModelScope.launch { _effect.send(LoginEffect.NavigateToRegister) }
            }
        }
    }

    private fun handleSignIn() {
        viewModelScope.launch {
            _effect.send(LoginEffect.NavigateToHome)

        }
        //     val emailError = Validation.validateEmail(_state.value.email)
        //     val passwordError = Validation.validatePasswordLength(_state.value.password)

        //     if (emailError != null || passwordError != null) {
        //         _state.update {
        //             it.copy(
        //                 emailErrorResId = emailError,
        //                 passwordErrorResId = passwordError
        //             )
        //         }
        //         return
        //     }

        //     viewModelScope.launch {
        //         _state.update { it.copy(isLoading = true) }
        //         // Fake API call
        //         delay(1500)
        //         _state.update { it.copy(isLoading = false) }
        //         _effect.send(LoginEffect.NavigateToHome)
        //     }
        // }
    }

    private fun handleSocialLogin(event: LoginEvent.SocialLoginClicked) {
        viewModelScope.launch {
            _effect.send(LoginEffect.ShowSnackbar(messageStr = "${event.provider.name} login not implemented yet"))
        }
    }
}
