package iti.grad.nutriscan.presentation.onboarding.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.user.model.AccountPendingDeletionException
import iti.grad.nutriscan.domain.user.usecase.CheckIfProfileSetupUseCase
import iti.grad.nutriscan.domain.user.usecase.FetchAndSyncUserDataUseCase
import iti.grad.nutriscan.domain.onboarding.usecase.IsOnboardingCompletedUseCase
import iti.grad.nutriscan.domain.auth.usecase.CheckIfUserIsLoggedInUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val isOnboardingCompletedUseCase: IsOnboardingCompletedUseCase,
    private val checkIfUserIsLoggedInUseCase: CheckIfUserIsLoggedInUseCase,
    private val checkIfProfileSetupUseCase: CheckIfProfileSetupUseCase,
    private val fetchAndSyncUserDataUseCase: FetchAndSyncUserDataUseCase,
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
            if (!isOnboardingCompletedUseCase()) {
                _effect.send(SplashEffect.NavigateToOnboarding)
                return@launch
            }

            if (!checkIfUserIsLoggedInUseCase()) {
                _effect.send(SplashEffect.NavigateToLogin)
                return@launch
            }

            // User is logged in — fetch latest profile from server before routing.
            // This is critical to detect if the account is pending deletion (HTTP 409)
            // and to populate the local DB so CheckIfProfileSetupUseCase can read real data.
            val syncResult = fetchAndSyncUserDataUseCase()

            // 409 ACCOUNT_PENDING_DELETION → redirect immediately before any other check
            val pendingDeletion = syncResult.exceptionOrNull() as? AccountPendingDeletionException
            if (pendingDeletion != null) {
                _effect.send(SplashEffect.NavigateToAccountPendingDeletion(pendingDeletion.scheduledDeletionAt))
                return@launch
            }

            if (checkIfProfileSetupUseCase()) {
                _effect.send(SplashEffect.NavigateToHome)
            } else {
                _effect.send(SplashEffect.NavigateToProfileSetup)
            }
        }
    }
}
