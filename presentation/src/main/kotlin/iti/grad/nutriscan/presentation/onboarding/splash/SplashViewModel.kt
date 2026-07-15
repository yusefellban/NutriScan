package iti.grad.nutriscan.presentation.onboarding.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.onboarding.usecase.IsOnboardingCompletedUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val isOnboardingCompletedUseCase: IsOnboardingCompletedUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    private val _effect = Channel<SplashEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: SplashEvent) {
        when (event) {
            SplashEvent.AnimationCompleted -> onAnimationCompleted()
        }
    }

    private fun onAnimationCompleted() {
        viewModelScope.launch {
            if (isOnboardingCompletedUseCase()) {
                _effect.send(SplashEffect.NavigateToLogin)
            } else {
                _effect.send(SplashEffect.NavigateToOnboarding)
            }
        }
    }
}
