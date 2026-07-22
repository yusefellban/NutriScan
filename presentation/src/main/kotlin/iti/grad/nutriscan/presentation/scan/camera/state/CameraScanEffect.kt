package iti.grad.nutriscan.presentation.scan.camera.state

import androidx.annotation.StringRes

sealed interface CameraScanEffect {
    data class NavigateToProcessing(val barcode: String) : CameraScanEffect
    data object NavigateToHome : CameraScanEffect
    data object NavigateToHistory : CameraScanEffect
    data object NavigateToShopping : CameraScanEffect
    data object NavigateToProfile : CameraScanEffect
    data class ShowSnackBarRes(@StringRes val messageResId: Int) : CameraScanEffect
    data object RequestCameraPermission : CameraScanEffect
}
