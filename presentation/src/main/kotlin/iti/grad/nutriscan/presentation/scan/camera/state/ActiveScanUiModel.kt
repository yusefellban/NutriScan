package iti.grad.nutriscan.presentation.scan.camera.state

import androidx.annotation.StringRes

import iti.grad.nutriscan.domain.scan.model.ScanResult

data class ActiveScanUiModel(
    val scanId: String,
    val thumbnailUrl: String?,
    @StringRes val statusResId: Int? = null,
    @StringRes val healthTagResId: Int? = null,
    val isProcessing: Boolean = false,
    val isFailed: Boolean = false,
    val isSaved: Boolean = false,
    val fullResult: ScanResult? = null
)
