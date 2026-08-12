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
     * Submit a barcode value for AI nutritional analysis (via JSON to /v1/scans/barcode).
     *
     * ⚠️ Health-critical: Returns a [ScanResult] whose status will initially be PROCESSING.
     * The caller MUST poll via [getScanResult] until status is COMPLETED before surfacing
     * any verdict to the user. Treating a PROCESSING result as safe is a critical failure.
     *
     * @param barcode The raw barcode string detected by ML Kit (e.g. "5922157657516").
     */
    suspend fun submitBarcodeScan(barcode: String): Result<ScanResult>

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

    /**
     * Fetch autocomplete suggestions for product name search.
     * Used in the Scan History search bar with debouncing.
     */
    suspend fun getScanSuggestions(query: String): Result<List<String>>
}
