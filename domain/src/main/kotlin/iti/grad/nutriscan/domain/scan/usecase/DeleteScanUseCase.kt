package iti.grad.nutriscan.domain.scan.usecase

import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import javax.inject.Inject

class DeleteScanUseCase @Inject constructor(
    private val repository: IScanRepository,
) {
    suspend operator fun invoke(scanId: String): Result<Unit> {
        return repository.deleteScan(scanId)
    }
}
