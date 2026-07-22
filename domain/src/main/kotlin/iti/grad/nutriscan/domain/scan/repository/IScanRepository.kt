package iti.grad.nutriscan.domain.scan.repository

import iti.grad.nutriscan.domain.scan.model.ProductResult

interface IScanRepository {
    suspend fun getProductByBarcode(barcode: String): Result<ProductResult>
}
