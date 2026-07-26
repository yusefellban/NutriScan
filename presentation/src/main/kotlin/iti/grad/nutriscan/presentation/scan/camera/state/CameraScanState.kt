package iti.grad.nutriscan.presentation.scan.camera.state


data class CameraScanState(
    val hasCameraPermission: Boolean = false,
    val isScanning: Boolean = true,
    val activeScan: ActiveScanUiModel? = null,
    val permissionDenied: Boolean = false,
    val showDeleteDialog: Boolean = false,
)
