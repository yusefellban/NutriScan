package iti.grad.nutriscan.domain.scan.repository

import iti.grad.nutriscan.domain.scan.model.ProductResult
import iti.grad.nutriscan.domain.scan.model.ScanHistoryEntry
import iti.grad.nutriscan.domain.scan.model.ScanResult
import java.io.File
import java.time.LocalDate

interface IScanRepository {
    suspend fun getProductByBarcode(barcode: String): Result<ProductResult>
    suspend fun getLastScanDate(): LocalDate?

    /**
     * Submit an image for scanning (via multipart/form-data to /v1/scans).
     */
    suspend fun submitScanImage(imageFile: File): Result<ScanResult>

    /**
     * Get the result of an ongoing or completed scan.
     */
    suspend fun getScanResult(scanId: String): Result<ScanResult>

    /**
     * Get a paginated list of recent scans.
     */
    suspend fun getRecentScans(
        page: Int,
        size: Int,
        date: String? = null,
        verdict: String? = null,
        query: String? = null,
        scanStatus: String? = null,
    ): Result<List<ScanHistoryEntry>>
}
