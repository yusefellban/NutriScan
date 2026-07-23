package iti.grad.nutriscan.presentation.scan.camera.state

import androidx.annotation.StringRes

sealed interface CameraScanEffect {
    data class NavigateToProcessing(val barcode: String) : CameraScanEffect
    data class ShowSnackBarRes(@StringRes val messageResId: Int) : CameraScanEffect
    data object RequestCameraPermission : CameraScanEffect
}
