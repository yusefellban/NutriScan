package iti.grad.nutriscan.domain.scan.repository

import iti.grad.nutriscan.domain.scan.model.ScanResult
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ISavedScanRepository {
    /**
     * Persist a completed [ScanResult] to local storage (Room DB).
     * Called when the user taps the bookmark button on the camera screen.
     */
    suspend fun saveScan(scanResult: ScanResult): Result<Unit>
    
    /**
     * Retrieve all saved scans from local storage as a Flow.
     */
    fun getSavedScans(): Flow<List<ScanResult>>

    /**
     * Delete a scan from local storage by its ID.
     */
    suspend fun deleteScan(scanId: String): Result<Unit>

    /**
     * Retrieve a specific scan by its ID.
     */
    suspend fun getSavedScanById(scanId: String): Result<ScanResult?>

    /**
     * Retries any save/delete that failed to reach the backend earlier.
     * Called by the periodic daily-tracking sync worker.
     */
    suspend fun retryPendingSync(): Result<Unit>

    /**
     * Pull-to-refresh entry point: flushes pending saves and re-reads the backend's favorites
     * immediately, instead of waiting out the background reconcile interval.
     */
    suspend fun refresh(): Result<Unit>
}
