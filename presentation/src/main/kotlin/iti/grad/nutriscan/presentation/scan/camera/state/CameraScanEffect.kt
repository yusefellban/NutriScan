package iti.grad.nutriscan.presentation.scan.camera.state

import androidx.annotation.StringRes

sealed interface CameraScanEffect {
    data class ShowSnackBarRes(@StringRes val messageResId: Int) : CameraScanEffect
    data class ShowSnackBar(val message: String) : CameraScanEffect
    data object RequestCameraPermission : CameraScanEffect
    data object TakePicture : CameraScanEffect
}
