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
        
        val isPlaceholderData = user?.gender == "MALE" &&
                user.dateOfBirth == "2000-01-01" &&
                user.heightCm == 170.0 &&
                user.weightKg == 170.0

        val hasRealServerData = user != null &&
                user.gender != null &&
                user.dateOfBirth != null &&
                user.heightCm != null &&
                user.weightKg != null &&
                !isPlaceholderData

        return hasRealServerData
    }
}
