package iti.grad.nutriscan.domain.onboarding.usecase

import iti.grad.nutriscan.domain.onboarding.repository.IOnboardingRepository
import javax.inject.Inject

class IsOnboardingCompletedUseCase @Inject constructor(
    private val onboardingRepository: IOnboardingRepository
) {
    suspend operator fun invoke(): Boolean {
        return onboardingRepository.isOnboardingCompleted()
    }
}
