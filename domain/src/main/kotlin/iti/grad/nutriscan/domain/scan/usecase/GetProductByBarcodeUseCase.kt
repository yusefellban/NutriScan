package iti.grad.nutriscan.domain.scan.usecase

import iti.grad.nutriscan.domain.scan.model.ProductResult
import iti.grad.nutriscan.domain.scan.repository.IScanRepository
import javax.inject.Inject

class GetProductByBarcodeUseCase @Inject constructor(
    private val scanRepository: IScanRepository
) {
    suspend operator fun invoke(barcode: String): Result<ProductResult> {
        return scanRepository.getProductByBarcode(barcode)
    }
}
