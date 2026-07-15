package iti.grad.nutriscan.presentation.onboarding.carousel.state

sealed interface OnboardingCarouselEffect {
    data object NavigateToLogin : OnboardingCarouselEffect
    data class ScrollToPage(val page: Int) : OnboardingCarouselEffect
}
