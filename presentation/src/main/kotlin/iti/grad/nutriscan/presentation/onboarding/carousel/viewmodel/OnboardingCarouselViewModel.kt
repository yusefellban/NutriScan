package iti.grad.nutriscan.presentation.onboarding.carousel.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import iti.grad.nutriscan.domain.onboarding.usecase.CompleteOnboardingUseCase
import iti.grad.nutriscan.presentation.onboarding.carousel.state.OnboardingCarouselEffect
import iti.grad.nutriscan.presentation.onboarding.carousel.state.OnboardingCarouselEvent
import iti.grad.nutriscan.presentation.onboarding.carousel.state.OnboardingCarouselState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingCarouselViewModel @Inject constructor(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingCarouselState())
    val state: StateFlow<OnboardingCarouselState> = _state.asStateFlow()

    private val _effect = Channel<OnboardingCarouselEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onEvent(event: OnboardingCarouselEvent) {
        when (event) {
            is OnboardingCarouselEvent.NextClicked -> {
                val current = _state.value.currentPage
                val count = _state.value.pageCount
                if (current >= count - 1) {
                    completeOnboarding()
                } else {
                    viewModelScope.launch {
                        _effect.send(OnboardingCarouselEffect.ScrollToPage(current + 1))
                    }
                }
            }
            is OnboardingCarouselEvent.BackClicked -> {
                val current = _state.value.currentPage
                if (current > 0) {
                    viewModelScope.launch {
                        _effect.send(OnboardingCarouselEffect.ScrollToPage(current - 1))
                    }
                }
            }
            is OnboardingCarouselEvent.SkipClicked -> {
                completeOnboarding()
            }
            is OnboardingCarouselEvent.PageChanged -> {
                _state.update { it.copy(currentPage = event.page) }
            }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            completeOnboardingUseCase()
            _effect.send(OnboardingCarouselEffect.NavigateToLogin)
        }
    }
}
