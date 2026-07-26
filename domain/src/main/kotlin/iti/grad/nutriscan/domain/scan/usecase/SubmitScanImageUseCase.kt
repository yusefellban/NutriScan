package iti.grad.nutriscan.domain.scan.usecase

import iti.grad.nutriscan.domain.scan.model.ScanResult
import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import java.io.File
import javax.inject.Inject

class SubmitScanImageUseCase @Inject constructor(
    private val scanRepository: IScanRepository
) {
    suspend operator fun invoke(imageFile: File): Result<ScanResult> {
        return scanRepository.submitScanImage(imageFile)
    }
}
