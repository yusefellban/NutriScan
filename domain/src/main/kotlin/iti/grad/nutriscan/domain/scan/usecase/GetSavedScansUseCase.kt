package iti.grad.nutriscan.domain.scan.usecase

import iti.grad.nutriscan.domain.scan.model.ScanResult
import iti.grad.nutriscan.domain.scan.repository.ISavedScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSavedScansUseCase @Inject constructor(
    private val savedScanRepository: ISavedScanRepository
) {
    operator fun invoke(): Flow<List<ScanResult>> {
        return savedScanRepository.getSavedScans()
    }
}
