package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Determines whether the user has completed the Profile Setup flow
 * by checking if the user's data contains placeholder values.
 */
class CheckIfProfileSetupUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(): Boolean {
        // Use filterNotNull().first() to ensure we wait for the latest DB update
        // after fetchAndSyncProfile(), avoiding a race condition where a stale 'null' is returned.
        val user = userRepository.getUserData().filterNotNull().first()
        
        // The backend only accepts MALE or FEMALE.
        // We use weightKg = 170.0 and heightCm = 170.0 as placeholders during registration.
        // If the user's data matches these placeholders exactly, they haven't completed profile setup.
        return !(user.gender == "MALE" &&
                user.dateOfBirth == "2000-01-01" &&
                user.heightCm == 170.0 &&
                user.weightKg == 170.0)
    }
}
