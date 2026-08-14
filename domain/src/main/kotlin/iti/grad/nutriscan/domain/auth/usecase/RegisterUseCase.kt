package iti.grad.nutriscan.domain.auth.usecase

import iti.grad.nutriscan.domain.auth.repository.IAuthRepository
import iti.grad.nutriscan.domain.onboarding.repository.IOnboardingRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
    private val onboardingRepository: IOnboardingRepository
) {
    suspend operator fun invoke(firstName: String, lastName: String, email: String, password: String): Result<Unit> {
        val result = authRepository.register(firstName, lastName, email, password)
        if (result.isSuccess) {
            onboardingRepository.clearUserSpecificPreferences()
        }
        return result
    }
}
