package iti.grad.nutriscan.data.repository

import iti.grad.nutriscan.data.remote.api.OpenFoodFactsApiService
import iti.grad.nutriscan.domain.scan.model.ProductResult
import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import java.time.LocalDate
import javax.inject.Inject

class ScanRepositoryImpl @Inject constructor(
    private val apiService: OpenFoodFactsApiService
) : IScanRepository {

    override suspend fun getProductByBarcode(barcode: String): Result<ProductResult> {
        return try {
            val cleanBarcode = barcode.trim()
            val response = apiService.getProduct(cleanBarcode)
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

    // ponytail: returns null until scan history is persisted (no scan-history storage exists yet)
    override suspend fun getLastScanDate(): LocalDate? = null
}
