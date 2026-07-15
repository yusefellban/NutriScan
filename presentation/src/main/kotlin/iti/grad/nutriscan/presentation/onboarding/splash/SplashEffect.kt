package iti.grad.nutriscan.presentation.onboarding.splash

sealed interface SplashEffect {
    data object NavigateToOnboarding : SplashEffect
    data object NavigateToLogin : SplashEffect
}
