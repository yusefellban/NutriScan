package iti.grad.nutriscan.domain.scan.usecase

import iti.grad.nutriscan.domain.scan.repository.ISavedScanRepository
import javax.inject.Inject

class DeleteSavedScanUseCase @Inject constructor(
    private val savedScanRepository: ISavedScanRepository
) {
    suspend operator fun invoke(scanId: String): Result<Unit> {
        return savedScanRepository.deleteScan(scanId)
    }
}
