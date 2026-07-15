package iti.grad.nutriscan.presentation.onboarding.carousel.state

import androidx.compose.runtime.Immutable

@Immutable
data class OnboardingCarouselState(
    val currentPage: Int = 0,
    val pageCount: Int = 3
)
