package iti.grad.nutriscan.presentation.product_details.state

import androidx.compose.runtime.Immutable
import iti.grad.nutriscan.domain.scan.model.ProductDetail
import iti.grad.nutriscan.presentation.common.model.AppErrorType

@Immutable
data class ProductDetailsState(
    val isLoading: Boolean = true,
    val productDetail: ProductDetail? = null,
    val errorMessageResId: Int? = null,
    val errorType: AppErrorType = AppErrorType.UNKNOWN,
    val isNotFound: Boolean = false,
    val showDeleteDialog: Boolean = false,
)
