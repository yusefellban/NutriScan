package iti.grad.nutriscan.data.remote.api

import iti.grad.nutriscan.data.remote.dto.ScanResultResponseDto
import iti.grad.nutriscan.data.remote.dto.ScanSubmissionResponseDto
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ScanApiService {
    @Multipart
    @POST("v1/scans")
    suspend fun submitScan(@Part image: MultipartBody.Part): ScanSubmissionResponseDto

    @GET("v1/scans/{scanId}")
    suspend fun getScanResult(@Path("scanId") scanId: String): ScanResultResponseDto
}
