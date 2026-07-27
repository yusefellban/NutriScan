package iti.grad.nutriscan.domain.scan.usecase

import iti.grad.nutriscan.domain.scan.model.ScanHistoryEntry
import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import javax.inject.Inject

class GetRecentScansUseCase @Inject constructor(
    private val scanRepository: IScanRepository
) {
    suspend operator fun invoke(page: Int, size: Int, date: String? = null, verdict: String? = null): Result<List<ScanHistoryEntry>> {
        return scanRepository.getRecentScans(page, size, date, verdict)
    }
}
