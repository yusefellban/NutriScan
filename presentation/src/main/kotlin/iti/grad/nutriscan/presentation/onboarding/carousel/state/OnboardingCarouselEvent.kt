package iti.grad.nutriscan.presentation.onboarding.carousel.state

sealed interface OnboardingCarouselEvent {
    data object NextClicked : OnboardingCarouselEvent
    data object BackClicked : OnboardingCarouselEvent
    data object SkipClicked : OnboardingCarouselEvent
    data class PageChanged(val page: Int) : OnboardingCarouselEvent
}
