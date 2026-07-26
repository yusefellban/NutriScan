package iti.grad.nutriscan.domain.scan.usecase

import iti.grad.nutriscan.domain.scan.model.ScanResult
import iti.grad.nutriscan.domain.scan.repository.ISavedScanRepository
import javax.inject.Inject

class GetSavedScanByIdUseCase @Inject constructor(
    private val savedScanRepository: ISavedScanRepository
) {
    suspend operator fun invoke(scanId: String): Result<ScanResult?> {
        return savedScanRepository.getSavedScanById(scanId)
    }
}
