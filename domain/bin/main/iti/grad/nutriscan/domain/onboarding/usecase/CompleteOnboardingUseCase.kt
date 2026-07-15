package iti.grad.nutriscan.domain.onboarding.usecase

import iti.grad.nutriscan.domain.onboarding.repository.IOnboardingRepository
import javax.inject.Inject

class CompleteOnboardingUseCase @Inject constructor(
    private val onboardingRepository: IOnboardingRepository
) {
    suspend operator fun invoke() {
        onboardingRepository.completeOnboarding()
    }
}
