package iti.grad.nutriscan.presentation.onboarding.splash

sealed interface SplashEvent {
    data object AnimationCompleted : SplashEvent
    /** Tapped the error widget's retry button — re-runs the same profile sync that failed. */
    data object RetryClicked : SplashEvent
}
