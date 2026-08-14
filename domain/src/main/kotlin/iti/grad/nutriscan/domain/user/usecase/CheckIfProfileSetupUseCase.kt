package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.onboarding.repository.IOnboardingRepository
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Determines whether the user has completed the Profile Setup flow
 * by checking if the user's data contains placeholder values.
 */
class CheckIfProfileSetupUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(): Boolean {
        val user = userRepository.getUserData().firstOrNull()
        
        // If the gender is UNKNOWN, they haven't completed profile setup.
        // Once they complete it, gender will be MALE or FEMALE.
        return user != null && user.gender != "UNKNOWN"
    }
}
