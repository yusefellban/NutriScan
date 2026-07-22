package iti.grad.nutriscan.presentation.product_details.state

import androidx.compose.runtime.Immutable
import iti.grad.nutriscan.domain.scan.model.ProductDetail

@Immutable
data class ProductDetailsState(
    val isLoading: Boolean = true,
    val productDetail: ProductDetail? = null,
    val error: String? = null,
)
