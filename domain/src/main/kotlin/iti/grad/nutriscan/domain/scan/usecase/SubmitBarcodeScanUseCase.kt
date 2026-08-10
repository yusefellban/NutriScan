package iti.grad.nutriscan.domain.scan.usecase

import iti.grad.nutriscan.domain.scan.model.ScanResult
import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import javax.inject.Inject

/**
 * Submit a barcode value for AI nutritional analysis.
 *
 * This use case is the domain entry point for the Barcode scan mode. It delegates
 * directly to [IScanRepository.submitBarcodeScan] and returns a [ScanResult] whose
 * [ScanResult.status] will be PROCESSING.
 *
 * ⚠️ Health-critical: The caller (ViewModel) MUST poll [GetScanResultUseCase] until the
 * status transitions to COMPLETED or FAILED before surfacing any verdict to the user.
 * A PROCESSING result carries no safety information and must never be displayed as such.
 *
 * @param barcode The raw barcode string detected by ML Kit (e.g. "5922157657516").
 * @return [Result.success] with a PROCESSING [ScanResult] containing the scanId for polling,
 *         or [Result.failure] if the network request failed.
 */
class SubmitBarcodeScanUseCase @Inject constructor(
    private val scanRepository: IScanRepository,
) {
    suspend operator fun invoke(barcode: String): Result<ScanResult> =
        scanRepository.submitBarcodeScan(barcode)
}
