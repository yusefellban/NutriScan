package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.BarcodeScanRequestDto
import iti.grad.nutriscan.data.remote.dto.PageDto
import iti.grad.nutriscan.data.remote.dto.ScanHistoryItemDto
import iti.grad.nutriscan.data.remote.dto.ScanResultResponseDto
import iti.grad.nutriscan.data.remote.dto.ScanSubmissionResponseDto
import iti.grad.nutriscan.data.remote.dto.UpdateScanDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ScanApiService {
    @Multipart
    @POST("v1/scans")
    suspend fun submitScan(@Part image: MultipartBody.Part): ScanSubmissionResponseDto

    /**
     * Submit a barcode value for AI nutritional analysis.
     *
     * POST /v1/scans/barcode
     * Body: { "barcode": "5922157657516" }
     * Response: { "scanId": "...", "status": "PROCESSING" }
     *
     * The returned [ScanSubmissionResponseDto.scanId] must be polled via [getScanResult]
     * until status transitions to COMPLETED or FAILED.
     */
    @POST("v1/scans/barcode")
    suspend fun submitBarcodeScan(@Body body: BarcodeScanRequestDto): ScanSubmissionResponseDto

    @GET("v1/scans/{scanId}")
    suspend fun getScanResult(@Path("scanId") scanId: String): ScanResultResponseDto

    @GET("v1/scans")
    suspend fun getRecentScans(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageDto<ScanHistoryItemDto>

    @PATCH("v1/scans/{scanId}")
    suspend fun updateScan(
        @Path("scanId") scanId: String,
        @Body body: UpdateScanDto,
    ): ScanResultResponseDto

    @GET("v1/scans/favorites")
    suspend fun getFavoriteScans(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageDto<ScanHistoryItemDto>
}
