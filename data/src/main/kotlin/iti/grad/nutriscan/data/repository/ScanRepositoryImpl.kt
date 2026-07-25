package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.remote.api.OpenFoodFactsApiService
import iti.grad.nutriscan.data.remote.api.ScanApiService
import iti.grad.nutriscan.domain.scan.model.ProductResult
import iti.grad.nutriscan.domain.scan.model.ScanResult
import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import iti.grad.nutriscan.data.repository.mapper.toDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import iti.grad.nutriscan.data.di.IoDispatcher
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class ScanRepositoryImpl @Inject constructor(
    private val openFoodFactsApiService: OpenFoodFactsApiService,
    private val scanApiService: ScanApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IScanRepository {

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

    override suspend fun submitScanImage(imageFile: File): Result<ScanResult> {
        return withContext(ioDispatcher) {
            try {
                val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
                val response = scanApiService.submitScan(body)
                Result.success(response.toDomain())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getScanResult(scanId: String): Result<ScanResult> {
        return withContext(ioDispatcher) {
            try {
                val response = scanApiService.getScanResult(scanId)
                Result.success(response.toDomain())
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
