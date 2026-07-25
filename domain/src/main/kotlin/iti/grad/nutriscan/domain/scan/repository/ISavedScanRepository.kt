package iti.grad.nutriscan.domain.scan.repository

import iti.grad.nutriscan.domain.scan.model.ScanResult
import java.io.File

interface ISavedScanRepository {
    /**
     * Persist a completed [ScanResult] to local storage (Room DB).
     * Called when the user taps the bookmark button on the camera screen.
     */
    suspend fun saveScan(scanResult: ScanResult): Result<Unit>
}
