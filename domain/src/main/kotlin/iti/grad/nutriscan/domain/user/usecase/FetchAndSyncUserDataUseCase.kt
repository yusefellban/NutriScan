package iti.grad.nutriscan.domain.user.usecase

import iti.grad.nutriscan.domain.allergy.usecase.SyncAllergiesUseCase
import iti.grad.nutriscan.domain.disease.usecase.SyncDiseasesUseCase
import iti.grad.nutriscan.domain.user.repository.IUserRepository
import javax.inject.Inject

class FetchAndSyncUserDataUseCase @Inject constructor(
    private val userRepository: IUserRepository,
    private val syncDiseasesUseCase: SyncDiseasesUseCase,
    private val syncAllergiesUseCase: SyncAllergiesUseCase
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            val userResult = userRepository.fetchAndSyncProfile()
            if (userResult.isFailure) return userResult
            
            val diseaseResult = syncDiseasesUseCase()
            if (diseaseResult.isFailure) return diseaseResult
            
            val allergyResult = syncAllergiesUseCase()
            if (allergyResult.isFailure) return allergyResult
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
