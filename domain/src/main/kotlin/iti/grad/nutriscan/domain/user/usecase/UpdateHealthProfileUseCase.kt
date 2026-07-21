package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.repository.IUserRepository
import javax.inject.Inject

class UpdateHealthProfileUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(diseaseIds: List<Int>, allergyIds: List<Int>): Result<Unit> {
        return userRepository.updateHealthProfile(diseaseIds, allergyIds)
    }
}
