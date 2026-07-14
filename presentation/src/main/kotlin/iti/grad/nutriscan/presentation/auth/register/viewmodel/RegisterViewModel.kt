package iti.grad.nutriscan.presentation.auth.register.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.presentation.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import iti.grad.nutriscan.presentation.auth.register.state.RegisterEffect
import iti.grad.nutriscan.presentation.auth.register.state.RegisterEvent
import iti.grad.nutriscan.presentation.auth.register.state.RegisterState

@HiltViewModel
class RegisterViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    private val _effect = Channel<RegisterEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    // Pure Kotlin Regex to adhere to "No Android Imports in ViewModel" rule
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.EmailChanged -> _state.update { it.copy(email = event.value, emailErrorResId = null) }
            is RegisterEvent.PasswordChanged -> _state.update { it.copy(password = event.value, passwordErrorResId = null) }
            is RegisterEvent.ConfirmPasswordChanged -> _state.update { it.copy(confirmPassword = event.value, confirmPasswordErrorResId = null) }
            is RegisterEvent.TogglePasswordVisibility -> _state.update { it.copy(passwordVisible = !it.passwordVisible) }
            is RegisterEvent.ToggleConfirmPasswordVisibility -> _state.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
            is RegisterEvent.SignUpClicked -> handleSignUp()
            is RegisterEvent.SignInClicked -> navigateToSignIn()
        }
    }

    private fun handleSignUp() {
        val currentState = _state.value
        
        val emailError = when {
            currentState.email.isBlank() -> R.string.error_empty_field
            !currentState.email.matches(emailRegex) -> R.string.error_invalid_email
            else -> null
        }
        
        val passwordError = when {
            currentState.password.isBlank() -> R.string.error_empty_field
            else -> null
        }
        
        val confirmPasswordError = when {
            currentState.password != currentState.confirmPassword -> R.string.error_password_mismatch
            else -> null
        }
        
        _state.update { 
            it.copy(
                emailErrorResId = emailError,
                passwordErrorResId = passwordError,
                confirmPasswordErrorResId = confirmPasswordError
            )
        }
        
        if (emailError != null || passwordError != null || confirmPasswordError != null) return
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            // Simulate network/domain UseCase delay
            delay(1500)
            _state.update { it.copy(isLoading = false) }
            _effect.send(RegisterEffect.NavigateToHome)
        }
    }

    private fun navigateToSignIn() {
        viewModelScope.launch {
            _effect.send(RegisterEffect.NavigateToSignIn)
        }
    }
}
