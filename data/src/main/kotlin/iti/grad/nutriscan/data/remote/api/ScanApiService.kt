package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.PageDto
import iti.grad.nutriscan.data.remote.dto.ScanHistoryItemDto
import iti.grad.nutriscan.data.remote.dto.ScanResultResponseDto
import iti.grad.nutriscan.data.remote.dto.ScanSubmissionResponseDto
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ScanApiService {
    @Multipart
    @POST("v1/scans")
    suspend fun submitScan(@Part image: MultipartBody.Part): ScanSubmissionResponseDto

    @GET("v1/scans/{scanId}")
    suspend fun getScanResult(@Path("scanId") scanId: String): ScanResultResponseDto

    @GET("v1/scans")
    suspend fun getRecentScans(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("query") query: String? = null,
        @Query("date") date: String? = null,
        @Query("verdict") verdict: String? = null,
        @Query("scanStatus") scanStatus: String? = null
    ): PageDto<ScanHistoryItemDto>
}
