package iti.grad.nutriscan.presentation.onboarding.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.auth.usecase.CheckIfUserIsLoggedInUseCase
import iti.grad.nutriscan.domain.onboarding.usecase.IsOnboardingCompletedUseCase
import iti.grad.nutriscan.domain.user.model.AccountPendingDeletionException
import iti.grad.nutriscan.domain.user.usecase.CheckIfProfileSetupUseCase
import iti.grad.nutriscan.domain.user.usecase.FetchAndSyncUserDataUseCase
import iti.grad.nutriscan.presentation.common.model.throwableToAppErrorType
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel
@Inject
constructor(
        private val isOnboardingCompletedUseCase: IsOnboardingCompletedUseCase,
        private val checkIfUserIsLoggedInUseCase: CheckIfUserIsLoggedInUseCase,
        private val checkIfProfileSetupUseCase: CheckIfProfileSetupUseCase,
        private val fetchAndSyncUserDataUseCase: FetchAndSyncUserDataUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    private val _effect = Channel<SplashEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: SplashEvent) {
        when (event) {
            SplashEvent.AnimationCompleted -> onAnimationCompleted()
            SplashEvent.RetryClicked -> retrySync()
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
            syncProfileAndRoute()
        }
    }

    private fun retrySync() {
        _state.update { it.copy(errorType = null) }
        viewModelScope.launch { syncProfileAndRoute() }
    }

    /**
     * Fetches the profile and routes onward. On failure this used to fall through and route anyway,
     * leaving the splash stuck behind a screen that never got real data — now it stops and surfaces
     * the matching error widget ([SplashEvent.RetryClicked] re-runs this same step.
     */
    private suspend fun syncProfileAndRoute() {
        // This is critical to detect if the account is pending deletion (HTTP 409)
        // and to populate the local DB so CheckIfProfileSetupUseCase can read real data.
        val syncResult = fetchAndSyncUserDataUseCase()
        val exception = syncResult.exceptionOrNull()

        // 409 ACCOUNT_PENDING_DELETION → redirect immediately before any other check
        if (exception is AccountPendingDeletionException) {
            _effect.send(
                    SplashEffect.NavigateToAccountPendingDeletion(exception.scheduledDeletionAt)
            )
            return
        }

        if (exception != null) {
            _state.update { it.copy(errorType = throwableToAppErrorType(exception)) }
            return
        }

        if (checkIfProfileSetupUseCase()) {
            _effect.send(SplashEffect.NavigateToHome)
        } else {
            _effect.send(SplashEffect.NavigateToProfileSetup)
        }
    }
}
