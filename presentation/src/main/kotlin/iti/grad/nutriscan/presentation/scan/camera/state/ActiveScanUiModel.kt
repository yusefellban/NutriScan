package iti.grad.nutriscan.presentation.scan.camera.state

import androidx.annotation.StringRes

data class ActiveScanUiModel(
    val barcode: String,
    val brand: String?,
    val productName: String?,
    val thumbnailUrl: String?,
    @StringRes val statusResId: Int? = null,
    val healthTag: String? = null,
)
