package iti.grad.nutriscan.presentation.scan.camera.state

import androidx.annotation.StringRes
import iti.grad.nutriscan.presentation.common.components.SnackbarType
import iti.grad.nutriscan.presentation.common.model.ProductUiModel

sealed interface CameraScanEffect {
    data class ShowSnackBarRes(@StringRes val messageResId: Int) : CameraScanEffect
    data class ShowSnackBar(
        val message: String,
        val snackbarType: SnackbarType = SnackbarType.SUCCESS
    ) : CameraScanEffect
    data object RequestCameraPermission : CameraScanEffect
    data object TakePicture : CameraScanEffect
    data object OpenGalleryPicker : CameraScanEffect
    data class NavigateToProductDetail(val product: ProductUiModel) : CameraScanEffect
}
