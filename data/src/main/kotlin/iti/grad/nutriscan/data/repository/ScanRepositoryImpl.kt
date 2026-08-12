package iti.grad.nutriscan.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import iti.grad.nutriscan.data.remote.api.OpenFoodFactsApiService
import iti.grad.nutriscan.data.remote.api.ScanApiService
import iti.grad.nutriscan.domain.scan.model.ProductResult
import iti.grad.nutriscan.domain.scan.model.ScanResult
import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import java.time.LocalDate
import iti.grad.nutriscan.data.repository.mapper.toDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import iti.grad.nutriscan.data.di.IoDispatcher
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import androidx.core.graphics.scale
import iti.grad.nutriscan.domain.scan.model.ScanHistoryEntry
import iti.grad.nutriscan.domain.scan.model.ScanStatus

class ScanRepositoryImpl @Inject constructor(
    private val openFoodFactsApiService: OpenFoodFactsApiService,
    private val scanApiService: ScanApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IScanRepository {

    private var localRecentScans: MutableList<ScanHistoryEntry>? = null

    override suspend fun getProductByBarcode(barcode: String): Result<ProductResult> {
        return withContext(ioDispatcher) {
            try {
                val cleanBarcode = barcode.trim()
                val response = openFoodFactsApiService.getProduct(cleanBarcode)
                if (response.status == 1 && response.product != null) {
                    val product = response.product
                    val name = product.productName ?: product.productNameFr
                    val brand = product.brands
                    val imageUrl = product.imageFrontSmallUrl
                    val healthTag = product.ecoscoreGrade?.uppercase() 
                        ?: product.nutritionGradesTags?.firstOrNull()?.uppercase()
                    
                    Result.success(
                        ProductResult(
                            barcode = barcode,
                            productName = name,
                            brand = brand,
                            imageUrl = imageUrl,
                            healthTag = healthTag
                        )
                    )
                } else if (response.status == 0) {
                    Result.failure(Exception("Not in OpenFoodFacts DB"))
                } else {
                    Result.failure(Exception("Unknown API Error: Status ${response.status}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun compressImage(imageFile: File): File {
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return imageFile
        
        val maxDimension = 1024
        val ratio =
            (maxDimension.toFloat() / bitmap.width).coerceAtMost(maxDimension.toFloat() / bitmap.height)
        
        val compressedBitmap = if (ratio < 1) {
            bitmap.scale((bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt())
        } else {
            bitmap
        }
        
        val outputFile = File(imageFile.parent, "compressed_${imageFile.name}")
        val fos = java.io.FileOutputStream(outputFile)
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
        fos.flush()
        fos.close()
        
        if (compressedBitmap != bitmap) {
            compressedBitmap.recycle()
        }
        bitmap.recycle()
        
        return outputFile
    }

    override suspend fun submitScanImage(imageFile: File): Result<ScanResult> {
        return withContext(ioDispatcher) {
            try {
                val compressedFile = compressImage(imageFile)
                val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("image", compressedFile.name, requestFile)
                val response = scanApiService.submitScan(body)
                
                if (compressedFile.absolutePath != imageFile.absolutePath) {
                    compressedFile.delete()
                }
                
                val domainResult = response.toDomain()
                
                Result.success(domainResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getScanResult(scanId: String): Result<ScanResult> {
        return withContext(ioDispatcher) {
            try {
                val response = scanApiService.getScanResult(scanId)
                val domainResult = response.toDomain()
                
                if (domainResult.status == ScanStatus.COMPLETED) {
                    localRecentScans?.let { cache ->
                        val newEntry = ScanHistoryEntry(
                            scanId = domainResult.scanId,
                            productName = domainResult.productName,
                            imageUrl = domainResult.imageUrl,
                            verdict = domainResult.foodSafetyResponse?.verdict,
                            scannedAt = domainResult.scannedAt,
                            calories = domainResult.nutritionFacts?.calories,
                            status = domainResult.status
                        )
                        cache.removeAll { it.scanId == newEntry.scanId }
                        cache.add(0, newEntry)
                    }
                }
                
                Result.success(domainResult)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    override suspend fun getRecentScans(
        page: Int,
        size: Int,
        date: String?,
        verdict: String?,
        query: String?,
        scanStatus: String?,
    ): Result<List<ScanHistoryEntry>> {
        return withContext(ioDispatcher) {
            val isFiltered = date != null || verdict != null || query != null || scanStatus != null
            if (!isFiltered && page == 0 && localRecentScans != null && localRecentScans!!.size >= size) {
                return@withContext Result.success(localRecentScans!!.take(size))
            }
            try {
                val response = scanApiService.getRecentScans(page, size, query, date, verdict, scanStatus)
                val domainScans = response.content.map { it.toDomain() }
                if (!isFiltered && page == 0) {
                    localRecentScans = domainScans.toMutableList()
                }
                Result.success(domainScans)
            } catch (e: Exception) {
                if (!isFiltered && page == 0 && localRecentScans != null) {
                    Result.success(localRecentScans!!.take(size))
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    // ponytail: returns null until scan history is persisted (no scan-history storage exists yet)
    override suspend fun getLastScanDate(): LocalDate? = null
}
