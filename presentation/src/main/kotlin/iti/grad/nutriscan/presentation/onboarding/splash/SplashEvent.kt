package iti.grad.nutriscan.presentation.onboarding.splash

sealed interface SplashEvent {
    data object AnimationCompleted : SplashEvent
}
