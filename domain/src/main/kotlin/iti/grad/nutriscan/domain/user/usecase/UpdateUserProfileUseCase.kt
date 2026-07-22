package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.user.repository.IUserRepository
import javax.inject.Inject

import iti.grad.nutriscan.domain.user.model.ProfileUpdate

class UpdateUserProfileUseCase @Inject constructor(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(
        firstName: String? = null,
        lastName: String? = null,
        gender: String? = null,
        dateOfBirth: String? = null,
        heightCm: Double? = null,
        weightKg: Double? = null,
        diseaseIds: List<Int>? = null,
        allergyIds: List<Int>? = null,
        avatarUrl: String? = null
    ): Result<Unit> {
        return userRepository.updateProfile(
            ProfileUpdate(
                firstName = firstName,
                lastName = lastName,
                gender = gender,
                dateOfBirth = dateOfBirth,
                heightCm = heightCm,
                weightKg = weightKg,
                diseaseIds = diseaseIds,
                allergyIds = allergyIds,
                avatarUrl = avatarUrl
            )
        )
    }
}
