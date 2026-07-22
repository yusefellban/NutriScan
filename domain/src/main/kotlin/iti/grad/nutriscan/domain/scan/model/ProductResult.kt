package iti.grad.nutriscan.domain.scan.model

data class ProductResult(
    val barcode: String,
    val productName: String?,
    val brand: String?,
    val imageUrl: String?,
    val healthTag: String?
)
