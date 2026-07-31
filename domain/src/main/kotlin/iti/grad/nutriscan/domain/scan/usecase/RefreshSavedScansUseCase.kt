package iti.grad.nutriscan.domain.scan.usecase

import iti.grad.nutriscan.domain.scan.repository.ISavedScanRepository
import javax.inject.Inject

class RefreshSavedScansUseCase @Inject constructor(
    private val savedScanRepository: ISavedScanRepository
) {
    suspend operator fun invoke(): Result<Unit> = savedScanRepository.refresh()
}
