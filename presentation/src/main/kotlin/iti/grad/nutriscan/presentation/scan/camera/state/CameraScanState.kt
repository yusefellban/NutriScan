package iti.grad.nutriscan.presentation.scan.camera.state


data class CameraScanState(
    val selectedMode: ScanInputMode = ScanInputMode.PHOTO,
    val hasCameraPermission: Boolean = false,
    val isScanning: Boolean = true,
    val isProcessingCenterAction: Boolean = false,
    val pendingGalleryImagePath: String? = null,
    val activeScan: ActiveScanUiModel? = null,
    val permissionDenied: Boolean = false,
    val showDeleteDialog: Boolean = false,
)
