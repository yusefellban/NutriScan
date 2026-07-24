package iti.grad.nutriscan.domain.scan.repository

import iti.grad.nutriscan.domain.scan.model.ProductResult
import java.time.LocalDate

interface IScanRepository {
    suspend fun getProductByBarcode(barcode: String): Result<ProductResult>
    suspend fun getLastScanDate(): LocalDate?
}
