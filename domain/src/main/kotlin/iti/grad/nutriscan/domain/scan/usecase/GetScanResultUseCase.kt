package iti.grad.nutriscan.domain.scan.usecase

import iti.grad.nutriscan.domain.scan.model.ScanResult
import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import javax.inject.Inject

class GetScanResultUseCase @Inject constructor(
    private val scanRepository: IScanRepository
) {
    suspend operator fun invoke(scanId: String): Result<ScanResult> {
        return scanRepository.getScanResult(scanId)
    }
}
