package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.repository.IUserRepository
import javax.inject.Inject

class UpdateHealthProfileUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(
        diseaseIds: List<Int>? = null,
        allergyIds: List<Int>? = null,
        gender: String? = null,
        dateOfBirth: String? = null,
        heightCm: Double? = null,
        weightKg: Double? = null
    ): Result<Unit> {
        return userRepository.updateHealthProfile(
            diseaseIds = diseaseIds,
            allergyIds = allergyIds,
            gender = gender,
            dateOfBirth = dateOfBirth,
            heightCm = heightCm,
            weightKg = weightKg
        )
    }
}
