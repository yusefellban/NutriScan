package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.repository.IUserRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class CheckIfProfileSetupUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(): Boolean {
        val user = userRepository.getUserData().firstOrNull()
        return user != null && 
               user.gender != null && 
               user.dateOfBirth != null && 
               user.heightCm != null && 
               user.weightKg != null
    }
}
