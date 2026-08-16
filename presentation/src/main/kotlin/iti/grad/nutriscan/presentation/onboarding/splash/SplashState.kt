package iti.grad.nutriscan.presentation.onboarding.splash

import iti.grad.nutriscan.presentation.common.model.AppErrorType

/**
 * [errorType] is non-null when [iti.grad.nutriscan.domain.user.usecase.FetchAndSyncUserDataUseCase]
 * fails — the screen shows the matching error widget instead of silently routing onward.
 */
data class SplashState(
        val errorType: AppErrorType? = null,
)
